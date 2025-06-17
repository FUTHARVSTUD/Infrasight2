package com.infrasight.db.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Document(collection = "user_gamify")
public class UserGamify {
    @Id
    private String id;
    private String userId;
    private int totalPoints = 0;
    private int streakDays = 0;
    private LocalDate lastActivity;
    private Set<String> uniqueServers = new HashSet<>();
    private int totalCommands = 0;
    private int prodCommands = 0;
    private List<String> badges = new ArrayList<>();
}
