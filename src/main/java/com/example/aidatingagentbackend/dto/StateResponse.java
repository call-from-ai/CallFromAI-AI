package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.State;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StateResponse {

    private Long id;

    private Long characterId;

    private String emotion;

    private Integer emotionIntensity;

    private Integer energy;

    private Integer stress;

    private String thinking;

    private String goal;

    private LocalDateTime updatedAt;

    public static StateResponse from(State state) {
        StateResponse response = new StateResponse();
        response.setId(state.getId());
        response.setCharacterId(state.getCharacterId());
        response.setEmotion(state.getEmotion());
        response.setEmotionIntensity(state.getEmotionIntensity());
        response.setEnergy(state.getEnergy());
        response.setStress(state.getStress());
        response.setThinking(state.getThinking());
        response.setGoal(state.getGoal());
        response.setUpdatedAt(state.getUpdatedAt());
        return response;
    }
}
