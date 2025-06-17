package com.infrasight.db.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Document(collection = "points_log")
public class PointsLog {
    @Id
    private String id;
    private String userId;
    private String event;
    private int pointsAwarded;
    private LocalDate timestamp;
    private String actionUuid;
    private String environment;
    private List<String> servers;
}
