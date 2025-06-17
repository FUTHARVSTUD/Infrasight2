package com.infrasight.controller;

import com.infrasight.data.PointsRequest;
import com.infrasight.db.model.UserGamify;
import com.infrasight.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gamify")
@RequiredArgsConstructor
public class GamificationController {
    
    private final GamificationService gamificationService;

    @PostMapping("/login")
    public ResponseEntity<UserGamify> awardLoginPoints() {
        // In a real application, you would extract userId from authentication context
        String userId = "default-user"; // Placeholder for authenticated user ID
        
        try {
            UserGamify result = gamificationService.awardLoginPoints(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error awarding login points for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/points")
    public ResponseEntity<UserGamify> awardPoints(@RequestBody PointsRequest request) {
        // In a real application, you would extract userId from authentication context
        String userId = "default-user"; // Placeholder for authenticated user ID
        
        try {
            UserGamify result = gamificationService.awardCommandPoints(request, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error awarding command points for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserGamify> getUserGamification() {
        // In a real application, you would extract userId from authentication context
        String userId = "default-user"; // Placeholder for authenticated user ID
        
        try {
            UserGamify result = gamificationService.getUserGamify(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error retrieving gamification data for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
