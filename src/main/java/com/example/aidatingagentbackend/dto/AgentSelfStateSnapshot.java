package com.example.aidatingagentbackend.dto;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import java.time.LocalDateTime;
public record AgentSelfStateSnapshot(Double affection, Double trust, Double hurt, Double anger, Double insecurity,
        Double disappointment, Double emotionalDistance, String lastEmotion, String lastSignificantEvent,
        Long version, LocalDateTime updatedAt) {
    public static AgentSelfStateSnapshot from(AgentSelfState state) {
        if (state == null) return null;
        return new AgentSelfStateSnapshot(state.getAffection(), state.getTrust(), state.getHurt(), state.getAnger(), state.getInsecurity(),
                state.getDisappointment(), state.getEmotionalDistance(), state.getLastEmotion(), state.getLastSignificantEvent(), state.getVersion(), state.getUpdatedAt());
    }
}
