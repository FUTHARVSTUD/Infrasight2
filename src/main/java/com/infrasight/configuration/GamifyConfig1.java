package com.wellsfargo.infrasight.configuration;

import lombok.Data; import org.springframework.boot.context.properties.ConfigurationProperties; import org.springframework.context.annotation.Configuration; import org.springframework.context.annotation.PropertySource; import com.wellsfargo.infrasight.config.YamlPropertySourceFactory;

import java.util.Map;

/**

Binds settings from points.yaml under prefix 'gamify'. */ @Configuration @PropertySource(value = "classpath:points.yaml", factory = YamlPropertySourceFactory.class) @ConfigurationProperties(prefix = "gamify") @Data public class GamifyConfig { // core scoring private int baseScore;

// login bonus and welcome-back private int loginPoints; private int welcomeBackBonus; private int welcomeBackGap;

// weights for commands private Map<String, Double> commandWeight; private Map<String, Double> parameterWeight;

// environment tier weight private Map<String, Double> accessTierWeight;

// server scaling settings private ServerScaling serverScaling;

// streak multipliers (days => multiplier) private Map<Integer, Double> streakMultiplier;

// badge definitions private Map<String, BadgeDefinition> badges;

@Data public static class ServerScaling { private String function; private int logBase; }

@Data public static class BadgeDefinition { private String name; private String description; private String condition; private int bonus; private String iconUrl; } }



