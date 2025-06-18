package com.wellsfargo.infrasight.controller;

import com.wellsfargo.infrasight.domain.UserGamify;
import com.wellsfargo.infrasight.repository.UserGamifyRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple pass-through controller for inserting and fetching UserGamify docs.
 */
@RestController
@RequestMapping(path = "/api/gamify", 
                consumes = MediaType.APPLICATION_JSON_VALUE, 
                produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "http://localhost:3000")
public class GamifyController {

    private final UserGamifyRepository userRepo;

    public GamifyController(UserGamifyRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * POST /api/gamify/me
     * Accepts a JSON UserGamify and saves it to Mongo.
     */
    @PostMapping("/me")
    public ResponseEntity<UserGamify> saveUser(@RequestBody UserGamify user) {
        UserGamify saved = userRepo.save(user);
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/gamify/me/{userId}
     * Fetches the UserGamify by its ID.
     */
    @GetMapping("/me/{userId}")
    public ResponseEntity<UserGamify> getUser(@PathVariable("userId") String userId) {
        return userRepo.findById(userId)
                       .map(ResponseEntity::ok)
                       .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
