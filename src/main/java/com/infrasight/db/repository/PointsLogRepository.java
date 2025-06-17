package com.infrasight.db.repository;

import com.infrasight.db.model.PointsLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointsLogRepository extends MongoRepository<PointsLog, String> {
    boolean existsByActionUuid(String actionUuid);
}
