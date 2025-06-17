package com.infrasight.data;

import lombok.Data;

import java.util.List;

@Data
public class PointsRequest {
    private String event;
    private String environment;
    private List<String> servers;
    private List<String> parameters;
    private String actionUuid;
}
