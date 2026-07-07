package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Reflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReflectionResponse {

    private Long id;

    private Long userId;

    private String category;

    private String summary;

    private String userPattern;

    private String agentLearning;

    private Double importance;

    private String sourceEventType;

    private Double sourceSeverity;

    private LocalDateTime createdAt;

    public static ReflectionResponse from(Reflection reflection) {
        ReflectionResponse response = new ReflectionResponse();
        response.setId(reflection.getId());
        response.setUserId(reflection.getUserId());
        response.setCategory(reflection.getCategory());
        response.setSummary(reflection.getSummary());
        response.setUserPattern(reflection.getUserPattern());
        response.setAgentLearning(reflection.getAgentLearning());
        response.setImportance(reflection.getImportance());
        response.setSourceEventType(reflection.getSourceEventType());
        response.setSourceSeverity(reflection.getSourceSeverity());
        response.setCreatedAt(reflection.getCreatedAt());
        return response;
    }
}
