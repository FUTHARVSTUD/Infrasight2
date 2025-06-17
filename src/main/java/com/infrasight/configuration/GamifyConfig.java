package com.infrasight.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class GamifyConfig {
    private int baseScore;
    private int loginPoints;
    private int welcomeBackBonus;
    private int welcomeBackGap;
    private CommandWeight commandWeight;
    private Map<String, Double> parameterWeight;
    private Map<String, Double> accessTierWeight;
    private ServerScaling serverScaling;
    private Map<String, Double> streakMultiplier;

    @Data
    public static class CommandWeight {
        private Cmd cmd;

        @Data
        public static class Cmd {
            private double defaultValue;
            private double param;

            public double getDefault() {
                return defaultValue;
            }

            public void setDefault(double defaultValue) {
                this.defaultValue = defaultValue;
            }
        }
    }

    @Data
    public static class ServerScaling {
        private String function;
        private int logBase;
    }
}
