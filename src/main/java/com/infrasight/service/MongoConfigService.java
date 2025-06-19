package com.infrasight.service;

import com.infrasight.db.model.GamifyConfigDoc;
import com.infrasight.db.repository.GamifyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Service that loads gamification configuration from MongoDB.
 */
@Service
@RequiredArgsConstructor
public class MongoConfigService {

    private final GamifyConfigRepository repo;
    private GamifyConfigDoc config;

    @PostConstruct
    public void load() {
        this.config = repo.findById("default")
                .orElseThrow(() -> new IllegalStateException("Missing gamify config in Mongo (id=default)"));
    }

    public GamifyConfigDoc getConfig() {
        return config;
    }
}
