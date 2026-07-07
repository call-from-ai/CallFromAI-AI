package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.AgentGoal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AgentGoalResponse {

    private Long id;

    private Long userId;

    private String goalType;

    private String description;

    private Integer priority;

    private String status;

    public static AgentGoalResponse from(AgentGoal goal) {
        AgentGoalResponse response = new AgentGoalResponse();
        response.setId(goal.getId());
        response.setUserId(goal.getUserId());
        response.setGoalType(goal.getGoalType());
        response.setDescription(goal.getDescription());
        response.setPriority(goal.getPriority());
        response.setStatus(goal.getStatus());
        return response;
    }
}
