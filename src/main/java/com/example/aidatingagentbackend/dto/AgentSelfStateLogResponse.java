package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentSelfStateLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AgentSelfStateLogResponse {

    private Long id;

    private Long userId;

    private Double previousHurt;

    private Double nextHurt;

    private Double previousTrust;

    private Double nextTrust;

    private Double previousAnger;

    private Double nextAnger;

    private Double previousInsecurity;

    private Double nextInsecurity;

    private String eventType;

    private Double severity;

    private String userMessage;

    private String deltaReason;

    private LocalDateTime createdAt;

    public static AgentSelfStateLogResponse from(AgentSelfStateLog log) {
        AgentSelfStateLogResponse response = new AgentSelfStateLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setPreviousHurt(log.getPreviousHurt());
        response.setNextHurt(log.getNextHurt());
        response.setPreviousTrust(log.getPreviousTrust());
        response.setNextTrust(log.getNextTrust());
        response.setPreviousAnger(log.getPreviousAnger());
        response.setNextAnger(log.getNextAnger());
        response.setPreviousInsecurity(log.getPreviousInsecurity());
        response.setNextInsecurity(log.getNextInsecurity());
        response.setEventType(log.getEventType());
        response.setSeverity(log.getSeverity());
        response.setUserMessage(log.getUserMessage());
        response.setDeltaReason(log.getDeltaReason());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
