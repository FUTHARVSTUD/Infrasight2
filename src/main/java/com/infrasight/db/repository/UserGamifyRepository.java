package com.infrasight.db.repository;

import com.infrasight.db.model.UserGamify;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGamifyRepository extends MongoRepository<UserGamify, String> {
    Optional<UserGamify> findByUserId(String userId);
}
