package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.TurningPoint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TurningPointResponse {

    private Long id;

    private String eventType;

    private String summary;

    private String impactEmotion;

    private Integer impactScore;

    private LocalDateTime createdAt;

    public static TurningPointResponse from(TurningPoint turningPoint) {
        TurningPointResponse response = new TurningPointResponse();
        response.setId(turningPoint.getId());
        response.setEventType(turningPoint.getEventType());
        response.setSummary(turningPoint.getSummary());
        response.setImpactEmotion(turningPoint.getImpactEmotion());
        response.setImpactScore(turningPoint.getImpactScore());
        response.setCreatedAt(turningPoint.getCreatedAt());
        return response;
    }
}
