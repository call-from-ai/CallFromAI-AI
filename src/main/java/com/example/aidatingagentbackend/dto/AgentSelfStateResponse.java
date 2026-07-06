package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AgentSelfStateResponse {

    private Long id;

    private Long characterId;

    private Double affection;

    private Double trust;

    private Double hurt;

    private Double anger;

    private Double insecurity;

    private Double disappointment;

    private Double emotionalDistance;

    private String lastEmotion;

    private String lastSignificantEvent;

    private LocalDateTime updatedAt;

    public static AgentSelfStateResponse from(AgentSelfState state) {
        AgentSelfStateResponse response = new AgentSelfStateResponse();
        response.setId(state.getId());
        response.setCharacterId(state.getCharacterId());
        response.setAffection(state.getAffection());
        response.setTrust(state.getTrust());
        response.setHurt(state.getHurt());
        response.setAnger(state.getAnger());
        response.setInsecurity(state.getInsecurity());
        response.setDisappointment(state.getDisappointment());
        response.setEmotionalDistance(state.getEmotionalDistance());
        response.setLastEmotion(state.getLastEmotion());
        response.setLastSignificantEvent(state.getLastSignificantEvent());
        response.setUpdatedAt(state.getUpdatedAt());
        return response;
    }
}
