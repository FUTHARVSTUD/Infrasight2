package com.infrasight.configuration;

import com.infrasight.db.model.GamifyConfigDoc;
import com.infrasight.db.model.GamifyConfigDoc.ServerScaling;
import com.infrasight.db.repository.GamifyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Inserts a default gamification configuration document into MongoDB
 * if none exists. This avoids relying on a YAML file.
 */
@Component
@RequiredArgsConstructor
public class ConfigSeeder implements CommandLineRunner {

    private final GamifyConfigRepository repo;

    @Override
    public void run(String... args) {
        if (repo.existsById("default")) {
            return;
        }

        GamifyConfigDoc doc = new GamifyConfigDoc();
        doc.setId("default");
        doc.setBaseScore(10);
        doc.setLoginPoints(5);
        doc.setWelcomeBackBonus(20);
        doc.setWelcomeBackGap(7);

        Map<String, Double> cmdWeight = new HashMap<>();
        cmdWeight.put("default", 1.0);
        cmdWeight.put("param", 1.2);
        doc.setCommandWeight(cmdWeight);

        Map<String, Double> paramWeight = new HashMap<>();
        paramWeight.put("simple", 1.0);
        paramWeight.put("regex", 3.0);
        paramWeight.put("wildcard", 2.0);
        doc.setParameterWeight(paramWeight);

        Map<String, Double> tierWeight = new HashMap<>();
        tierWeight.put("dev", 0.8);
        tierWeight.put("udt", 1.0);
        tierWeight.put("prod", 1.2);
        doc.setAccessTierWeight(tierWeight);

        ServerScaling scaling = new ServerScaling();
        scaling.setFunction("log");
        scaling.setLogBase(2);
        doc.setServerScaling(scaling);

        Map<Integer, Double> streakMult = new HashMap<>();
        streakMult.put(1, 1.0);
        streakMult.put(3, 1.1);
        streakMult.put(7, 1.2);
        streakMult.put(14, 1.35);
        streakMult.put(30, 1.5);
        doc.setStreakMultiplier(streakMult);

        repo.save(doc);
    }
}
