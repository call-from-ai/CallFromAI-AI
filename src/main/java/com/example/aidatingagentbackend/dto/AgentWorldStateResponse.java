package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentWorldState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AgentWorldStateResponse {

    private Long id;

    private Long userId;

    private String currentActivity;

    private String location;

    private String timeContext;

    private String mood;

    private Integer energy;

    private Integer stress;

    private Integer loneliness;

    private String pendingThought;

    public static AgentWorldStateResponse from(AgentWorldState state) {
        AgentWorldStateResponse response = new AgentWorldStateResponse();
        response.setId(state.getId());
        response.setUserId(state.getUserId());
        response.setCurrentActivity(state.getCurrentActivity());
        response.setLocation(state.getLocation());
        response.setTimeContext(state.getTimeContext());
        response.setMood(state.getMood());
        response.setEnergy(state.getEnergy());
        response.setStress(state.getStress());
        response.setLoneliness(state.getLoneliness());
        response.setPendingThought(state.getPendingThought());
        return response;
    }
}
