package com.wellsfargo.infrasight.controller;

import com.wellsfargo.infrasight.domain.UserGamify;
import com.wellsfargo.infrasight.repository.UserGamifyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${apiOpenPrefix:/api}/gamify")   // uses apiOpenPrefix or falls back to /api
@CrossOrigin(origins = "http://localhost:3000")
public class GamifyController {

    private final UserGamifyRepository userRepo;

    public GamifyController(UserGamifyRepository userRepo) {
        this.userRepo = userRepo;
    }

    /** POST /{prefix}/gamify/me   — insert or update a UserGamify document */
    @PostMapping("/me")
    public ResponseEntity<UserGamify> saveUser(@RequestBody UserGamify user) {
        UserGamify saved = userRepo.save(user);
        return ResponseEntity.ok(saved);
    }

    /** GET /{prefix}/gamify/me/{userId} — fetch a UserGamify by ID */
    @GetMapping("/me/{userId}")
    public ResponseEntity<UserGamify> getUser(@PathVariable String userId) {
        return userRepo.findById(userId)
                       .map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }
}
