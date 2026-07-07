package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.ReflectionCandidate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReflectionCandidateResponse {

    private Long id;

    private Long userId;

    private String eventType;

    private Double severity;

    private Boolean processed;

    private String userMessage;

    private String eventSummary;

    private String deltaReason;

    private LocalDateTime createdAt;

    public static ReflectionCandidateResponse from(ReflectionCandidate candidate) {
        ReflectionCandidateResponse response = new ReflectionCandidateResponse();
        response.setId(candidate.getId());
        response.setUserId(candidate.getUserId());
        response.setEventType(candidate.getEventType());
        response.setSeverity(candidate.getSeverity());
        response.setProcessed(candidate.getProcessed());
        response.setUserMessage(candidate.getUserMessage());
        response.setEventSummary(candidate.getEventSummary());
        response.setDeltaReason(candidate.getDeltaReason());
        response.setCreatedAt(candidate.getCreatedAt());
        return response;
    }
}
