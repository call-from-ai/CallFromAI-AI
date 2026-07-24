package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.springframework.stereotype.Service;

@Service
public class ProactiveContactPolicyService {

    public boolean shouldSend(Context context) {
        if (context == null) {
            return false;
        }

        AgentSelfState selfState = context.agentSelfState();
        CharacterTraitSnapshot traits = context.characterTraitProfile();
        AgentGoal goal = context.agentGoal();
        int temperatureScore = context.relationshipTemperatureScore() == null
                ? 50
                : context.relationshipTemperatureScore();
        if (high(value(selfState == null ? null : selfState.getHurt()))
                && !isRepairGoal(goal)) {
            return false;
        }
        if (high(value(selfState == null ? null : selfState.getAnger()))) {
            return false;
        }
        if (temperatureScore >= 81
                && highTrait(traits == null ? null : traits.getJealousy())
                && !isRepairGoal(goal)
                && !isCheckInGoal(goal)) {
            return false;
        }
        if (context.relationshipStage() == RelationshipStage.CRUSH
                && temperatureScore <= 40
                && isHighPressureGoal(goal)) {
            return false;
        }
        return true;
    }

    private boolean isRepairGoal(AgentGoal goal) {
        return goal != null && "REPAIR_RELATIONSHIP".equals(goal.getGoalType());
    }

    private boolean isCheckInGoal(AgentGoal goal) {
        return goal != null && "CHECK_IN".equals(goal.getGoalType());
    }

    private boolean isHighPressureGoal(AgentGoal goal) {
        if (goal == null || goal.getGoalType() == null) {
            return false;
        }
        return "EXPRESS_AFFECTION".equals(goal.getGoalType())
                || "ASK_ABOUT_PAST_EVENT".equals(goal.getGoalType());
    }

    private boolean high(double value) {
        return value >= 0.65;
    }

    private boolean highTrait(Integer value) {
        return value != null && value >= 8;
    }

    private double value(Double value) {
        return value == null ? 0.0 : Math.max(0.0, Math.min(1.0, value));
    }
}

