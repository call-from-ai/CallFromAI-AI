package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ResponseQualityEvaluationResponse {

    private Long id;

    private Long userId;

    private Boolean matchesSelfState;

    private Boolean tooSubmissive;

    private Boolean tooAggressive;

    private Boolean boundaryRespected;

    private Boolean characterConsistent;

    private Boolean safetyIssue;

    private Boolean regenerated;

    private Double score;

    private String reason;

    private String userMessage;

    private String assistantReply;

    private LocalDateTime createdAt;

    public static ResponseQualityEvaluationResponse from(ResponseQualityEvaluation evaluation) {
        ResponseQualityEvaluationResponse response = new ResponseQualityEvaluationResponse();
        response.setId(evaluation.getId());
        response.setUserId(evaluation.getUserId());
        response.setMatchesSelfState(evaluation.getMatchesSelfState());
        response.setTooSubmissive(evaluation.getTooSubmissive());
        response.setTooAggressive(evaluation.getTooAggressive());
        response.setBoundaryRespected(evaluation.getBoundaryRespected());
        response.setCharacterConsistent(evaluation.getCharacterConsistent());
        response.setSafetyIssue(evaluation.getSafetyIssue());
        response.setRegenerated(evaluation.getRegenerated());
        response.setScore(evaluation.getScore());
        response.setReason(evaluation.getReason());
        response.setUserMessage(evaluation.getUserMessage());
        response.setAssistantReply(evaluation.getAssistantReply());
        response.setCreatedAt(evaluation.getCreatedAt());
        return response;
    }
}
