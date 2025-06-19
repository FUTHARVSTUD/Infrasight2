package com.infrasight.db.repository;

import com.infrasight.db.model.GamifyConfigDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GamifyConfigRepository extends MongoRepository<GamifyConfigDoc, String> {
}
