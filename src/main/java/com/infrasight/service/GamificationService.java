package com.infrasight.service;

import com.infrasight.db.model.GamifyConfigDoc;
import com.infrasight.service.MongoConfigService;
import com.infrasight.data.PointsRequest;
import com.infrasight.db.model.PointsLog;
import com.infrasight.db.model.UserGamify;
import com.infrasight.db.repository.PointsLogRepository;
import com.infrasight.db.repository.UserGamifyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {
    
    private final MongoConfigService configService;
    private final UserGamifyRepository userGamifyRepository;
    private final PointsLogRepository pointsLogRepository;

    public UserGamify awardLoginPoints(String userId) {
        GamifyConfigDoc cfg = configService.getConfig();
        UserGamify user = getUserGamify(userId);

        int delta = cfg.getLoginPoints();

        if (isWelcomeBack(user.getLastActivity(), cfg.getWelcomeBackGap())) {
            delta += cfg.getWelcomeBackBonus();
            log.info("Welcome back bonus awarded to user: {}", userId);
        }
        
        user.setStreakDays(updateStreak(user.getLastActivity(), LocalDate.now()));
        user.setLastActivity(LocalDate.now());
        user.setTotalPoints(user.getTotalPoints() + delta);
        
        // Log the points award
        PointsLog pointsLog = PointsLog.builder()
                .userId(userId)
                .event("login")
                .pointsAwarded(delta)
                .timestamp(LocalDate.now())
                .actionUuid("login_" + userId + "_" + LocalDate.now())
                .build();
        
        pointsLogRepository.save(pointsLog);
        return userGamifyRepository.save(user);
    }

    public UserGamify awardCommandPoints(PointsRequest request, String userId) {
        // Check for idempotency
        if (pointsLogRepository.existsByActionUuid(request.getActionUuid())) {
            log.warn("Duplicate action UUID detected: {}", request.getActionUuid());
            return getUserGamify(userId);
        }
        
        GamifyConfigDoc cfg = configService.getConfig();
        UserGamify user = getUserGamify(userId);

        int base = cfg.getBaseScore();
        int complexity = complexityScore(request.getParameters(), cfg);

        double commandWeight = (request.getParameters() == null || request.getParameters().isEmpty())
            ? cfg.getCommandWeight().getOrDefault("default", 1.0)
            : cfg.getCommandWeight().getOrDefault("param", 1.0);

        double envWeight = cfg.getAccessTierWeight().getOrDefault(request.getEnvironment(), 1.0);
        double serverScale = serverScale(request.getServers() != null ? request.getServers().size() : 1, cfg);
        double streakMult = streakMultiplier(user.getStreakDays(), cfg);
        
        int delta = (int) Math.ceil((base + complexity) * commandWeight * envWeight * serverScale * streakMult);
        
        // Update user stats
        user.setTotalPoints(user.getTotalPoints() + delta);
        user.setTotalCommands(user.getTotalCommands() + 1);
        user.setLastActivity(LocalDate.now());
        
        if (request.getServers() != null) {
            user.getUniqueServers().addAll(request.getServers());
        }
        
        if ("prod".equals(request.getEnvironment())) {
            user.setProdCommands(user.getProdCommands() + 1);
        }
        
        // Log the points award
        PointsLog pointsLog = PointsLog.builder()
                .userId(userId)
                .event(request.getEvent())
                .pointsAwarded(delta)
                .timestamp(LocalDate.now())
                .actionUuid(request.getActionUuid())
                .environment(request.getEnvironment())
                .servers(request.getServers())
                .build();
        
        pointsLogRepository.save(pointsLog);
        return userGamifyRepository.save(user);
    }

    public UserGamify getUserGamify(String userId) {
        return userGamifyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserGamify newUser = new UserGamify();
                    newUser.setUserId(userId);
                    return userGamifyRepository.save(newUser);
                });
    }

    private double serverScale(int serverCount, GamifyConfigDoc cfg) {
        if (serverCount <= 1) return 1.0;

        String function = cfg.getServerScaling().getFunction();
        int logBase = cfg.getServerScaling().getLogBase();
        
        if ("log".equals(function)) {
            return 1.0 + (Math.log(serverCount) / Math.log(logBase)) * 0.1;
        }
        
        return 1.0;
    }

    private double streakMultiplier(int days, GamifyConfigDoc cfg) {
        return cfg.getStreakMultiplier().entrySet().stream()
                .filter(entry -> days >= entry.getKey())
                .mapToDouble(Map.Entry::getValue)
                .max()
                .orElse(1.0);
    }

    private int complexityScore(List<String> parameters, GamifyConfigDoc cfg) {
        if (parameters == null || parameters.isEmpty()) {
            return 0;
        }
        
        return parameters.stream()
                .mapToInt(param -> {
                    if (param.contains("*") || param.contains("?")) {
                        return (int) (cfg.getParameterWeight().getOrDefault("wildcard", 2.0) * 5);
                    } else if (param.matches(".*[\\[\\]\\$\\{\\}\\^\\$\\|\\\\].*")) {
                        return (int) (cfg.getParameterWeight().getOrDefault("regex", 3.0) * 5);
                    } else {
                        return (int) (cfg.getParameterWeight().getOrDefault("simple", 1.0) * 5);
                    }
                })
                .sum();
    }

    private boolean isWelcomeBack(LocalDate lastActivity, int gap) {
        if (lastActivity == null) return true;

        long daysBetween = ChronoUnit.DAYS.between(lastActivity, LocalDate.now());
        return daysBetween >= gap;
    }

    private int updateStreak(LocalDate lastActivity, LocalDate today) {
        if (lastActivity == null) return 1;
        
        long daysBetween = ChronoUnit.DAYS.between(lastActivity, today);
        
        if (daysBetween == 1) {
            // Consecutive day - increment streak
            return getUserGamify("temp").getStreakDays() + 1;
        } else if (daysBetween == 0) {
            // Same day - maintain streak
            return getUserGamify("temp").getStreakDays();
        } else {
            // Gap in activity - reset streak
            return 1;
        }
    }
}
