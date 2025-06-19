package com.infrasight.service;

import com.infrasight.db.model.GamifyConfigDoc;
import com.infrasight.service.MongoConfigService;
import com.infrasight.data.PointsRequest;
import com.infrasight.db.model.UserGamify;
import com.infrasight.db.repository.PointsLogRepository;
import com.infrasight.db.repository.UserGamifyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private MongoConfigService configService;
    
    @Mock
    private UserGamifyRepository userGamifyRepository;
    
    @Mock
    private PointsLogRepository pointsLogRepository;
    
    @InjectMocks
    private GamificationService gamificationService;

    private UserGamify testUser;
    private GamifyConfigDoc config;

    @BeforeEach
    void setUp() {
        testUser = new UserGamify();
        testUser.setUserId("test-user");
        testUser.setTotalPoints(0);
        testUser.setStreakDays(0);
        
        // Build configuration document
        config = new GamifyConfigDoc();
        config.setBaseScore(10);
        config.setLoginPoints(5);
        config.setWelcomeBackBonus(20);
        config.setWelcomeBackGap(7);

        Map<String, Double> commandWeight = new HashMap<>();
        commandWeight.put("default", 1.0);
        commandWeight.put("param", 1.2);
        config.setCommandWeight(commandWeight);

        Map<String, Double> parameterWeight = new HashMap<>();
        parameterWeight.put("simple", 1.0);
        parameterWeight.put("regex", 3.0);
        parameterWeight.put("wildcard", 2.0);
        config.setParameterWeight(parameterWeight);

        Map<String, Double> accessTierWeight = new HashMap<>();
        accessTierWeight.put("dev", 0.8);
        accessTierWeight.put("udt", 1.0);
        accessTierWeight.put("prod", 1.2);
        config.setAccessTierWeight(accessTierWeight);

        GamifyConfigDoc.ServerScaling scaling = new GamifyConfigDoc.ServerScaling();
        scaling.setFunction("log");
        scaling.setLogBase(2);
        config.setServerScaling(scaling);

        Map<Integer, Double> streakMultiplier = new HashMap<>();
        streakMultiplier.put(1, 1.0);
        streakMultiplier.put(3, 1.1);
        streakMultiplier.put(7, 1.2);
        config.setStreakMultiplier(streakMultiplier);

        when(configService.getConfig()).thenReturn(config);
    }

    @Test
    void testLoginWithoutGap() {
        testUser.setLastActivity(LocalDate.now().minusDays(1));
        
        when(userGamifyRepository.findByUserId("test-user")).thenReturn(Optional.of(testUser));
        when(userGamifyRepository.save(any(UserGamify.class))).thenReturn(testUser);
        when(pointsLogRepository.save(any())).thenReturn(null);
        
        UserGamify result = gamificationService.awardLoginPoints("test-user");
        
        assertEquals(5, result.getTotalPoints());
        verify(pointsLogRepository).save(any());
    }

    @Test
    void testLoginAfter8Days() {
        testUser.setLastActivity(LocalDate.now().minusDays(8));
        
        when(userGamifyRepository.findByUserId("test-user")).thenReturn(Optional.of(testUser));
        when(userGamifyRepository.save(any(UserGamify.class))).thenReturn(testUser);
        when(pointsLogRepository.save(any())).thenReturn(null);
        
        UserGamify result = gamificationService.awardLoginPoints("test-user");
        
        assertEquals(25, result.getTotalPoints()); // 5 + 20 welcome back bonus
        verify(pointsLogRepository).save(any());
    }

    @Test
    void testCommandWithComplexity() {
        PointsRequest request = new PointsRequest();
        request.setEvent("cmd.param");
        request.setEnvironment("prod");
        request.setServers(Arrays.asList("server1", "server2"));
        request.setParameters(Arrays.asList("regex.*pattern"));
        request.setActionUuid("test-uuid");
        
        testUser.setStreakDays(3);
        
        when(userGamifyRepository.findByUserId("test-user")).thenReturn(Optional.of(testUser));
        when(userGamifyRepository.save(any(UserGamify.class))).thenReturn(testUser);
        when(pointsLogRepository.existsByActionUuid(anyString())).thenReturn(false);
        when(pointsLogRepository.save(any())).thenReturn(null);
        
        UserGamify result = gamificationService.awardCommandPoints(request, "test-user");
        
        // Base: 10, Complexity: 15 (regex), CommandWeight: 1.2, EnvWeight: 1.2, ServerScale: ~1.1, StreakMult: 1.1
        // Expected: ceil((10 + 15) * 1.2 * 1.2 * 1.1 * 1.1) = ceil(43.56) = 44
        assertTrue(result.getTotalPoints() > 40);
        assertEquals(1, result.getTotalCommands());
        assertEquals(1, result.getProdCommands());
        verify(pointsLogRepository).save(any());
    }

    @Test
    void testIdempotency() {
        PointsRequest request = new PointsRequest();
        request.setActionUuid("duplicate-uuid");
        
        when(pointsLogRepository.existsByActionUuid("duplicate-uuid")).thenReturn(true);
        when(userGamifyRepository.findByUserId("test-user")).thenReturn(Optional.of(testUser));
        
        UserGamify result = gamificationService.awardCommandPoints(request, "test-user");
        
        assertEquals(0, result.getTotalPoints());
        verify(pointsLogRepository, never()).save(any());
        verify(userGamifyRepository, never()).save(any());
    }
}
