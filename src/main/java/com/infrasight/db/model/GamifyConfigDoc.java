Mongo-based Config for Gamification

With the plan change to store all scoring & badge definitions in MongoDB instead of points.yaml, here’s a step-by-step outline plus example classes to implement a Mongo‑backed configuration:


---

1. Create a Config Domain Document

GamifyConfigDoc.java (in db/model)

package com.wellsfargo.infrasight.db.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "gamify_config")
@Data
public class GamifyConfigDoc {
    @Id
    private String id;                 // e.g. "default"

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


---

2. Create a Config Repository

GamifyConfigRepository.java (in repository)

package com.wellsfargo.infrasight.repository;

import com.wellsfargo.infrasight.db.model.GamifyConfigDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GamifyConfigRepository extends MongoRepository<GamifyConfigDoc, String> {
    // Optionally, add findById("default") usage
}


---

3. Create a Config Service

MongoConfigService.java (in service)

package com.wellsfargo.infrasight.service;

import com.wellsfargo.infrasight.db.model.GamifyConfigDoc;
import com.wellsfargo.infrasight.repository.GamifyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class MongoConfigService {

    private final GamifyConfigRepository configRepo;
    private GamifyConfigDoc cfg;

    @PostConstruct
    public void init() {
        // Load the single config document (id="default") once on startup
        this.cfg = configRepo.findById("default")
            .orElseThrow(() -> new IllegalStateException("Missing gamify config in Mongo (id=default)"));
    }

    public GamifyConfigDoc getConfig() {
        return cfg;
    }

    /**
     * For dynamic reload, you could add:
     * public void reload() { this.cfg = configRepo.findById("default").orElse(...) }
     */
}


---

4. Update GamificationService to use MongoConfigService

Replace any GamifyConfig usage with mongoConfigService.getConfig():

@Service
@RequiredArgsConstructor
public class GamificationService {
    private final MongoConfigService cfgService;
    ...
    public PointsResponse awardLoginPoints(...) {
        GamifyConfigDoc cfg = cfgService.getConfig();
        int base = cfg.getLoginPoints();
        ...
    }
    ...
}


---

5. Seed the Config in Mongo

Use a one-time script or CommandLineRunner to insert your default document:

@Component
@RequiredArgsConstructor
public class ConfigSeeder implements CommandLineRunner {
    private final GamifyConfigRepository repo;

    @Override
    public void run(String... args) {
        if (!repo.existsById("default")) {
            GamifyConfigDoc doc = new GamifyConfigDoc();
            doc.setId("default");
            doc.setBaseScore(10);
            // ... populate all fields and nested maps ...
            repo.save(doc);
        }
    }
}


---

With this setup, all scoring parameters and badge definitions live in MongoDB, can be edited on the fly via Compass or your admin UI, and are fetched once at startup (or on demand) by your service code.


