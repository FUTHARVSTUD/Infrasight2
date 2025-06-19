package com.infrasight.db.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * Mongo-backed configuration document for gamification settings.
 */
@Data
@Document(collection = "gamify_config")
public class GamifyConfigDoc {

    @Id
    private String id;

    private int baseScore;
    private int loginPoints;
    private int welcomeBackBonus;
    private int welcomeBackGap;

    private Map<String, Double> commandWeight;
    private Map<String, Double> parameterWeight;
    private Map<String, Double> accessTierWeight;

    private ServerScaling serverScaling;
    private Map<Integer, Double> streakMultiplier;
    private Map<String, BadgeDef> badges;

    @Data
    public static class ServerScaling {
        private String function;
        private int logBase;
    }

    @Data
    public static class BadgeDef {
        private String name;
        private String description;
        private String condition;
        private int bonus;
        private String iconUrl;
    }
}
