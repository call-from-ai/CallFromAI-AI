package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.AgentGoalResponse;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.Reflection;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.repository.AgentGoalRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentGoalService {

    private final AgentGoalRepository agentGoalRepository;
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final RelationshipRepository relationshipRepository;
    private final ReflectionService reflectionService;

    public AgentGoalService(
            AgentGoalRepository agentGoalRepository,
            AgentSelfStateRepository agentSelfStateRepository,
            RelationshipRepository relationshipRepository,
            ReflectionService reflectionService
    ) {
        this.agentGoalRepository = agentGoalRepository;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.relationshipRepository = relationshipRepository;
        this.reflectionService = reflectionService;
    }

    @Transactional
    public AgentGoal selectCurrentGoal(Long userId) {
        AgentSelfState selfState = agentSelfStateRepository.findByCharacterId(userId).orElse(null);
        Relationship relationship = relationshipRepository.findByCharacterId(userId).orElse(null);
        List<Reflection> reflections = reflectionService.findRelevantForPrompt(userId);
        GoalDecision decision = decide(selfState, relationship, reflections);

        return agentGoalRepository.findTopByUserIdAndStatusOrderByPriorityDescCreatedAtDesc(userId, "ACTIVE")
                .filter(goal -> goal.getGoalType().equals(decision.goalType()))
                .orElseGet(() -> agentGoalRepository.save(createGoal(userId, decision)));
    }

    @Transactional(readOnly = true)
    public AgentGoal findCurrentGoal(Long userId) {
        return agentGoalRepository.findTopByUserIdAndStatusOrderByPriorityDescCreatedAtDesc(userId, "ACTIVE")
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AgentGoalResponse findCurrentGoalResponse(Long userId) {
        AgentGoal goal = findCurrentGoal(userId);
        return goal == null ? null : AgentGoalResponse.from(goal);
    }

    private GoalDecision decide(
            AgentSelfState selfState,
            Relationship relationship,
            List<Reflection> reflections
    ) {
        if (selfState != null && value(selfState.getHurt()) > 0.6) {
            return new GoalDecision("REPAIR_RELATIONSHIP", "상처받은 감정을 무시하지 않으면서 관계를 천천히 회복한다.", 95);
        }
        if (selfState != null && value(selfState.getEmotionalDistance()) > 0.5) {
            return new GoalDecision("RESPECT_DISTANCE", "상대에게 매달리지 않고 적당한 정서적 거리를 지킨다.", 85);
        }
        if (relationship != null && value(relationship.getBreakupRisk()) > 45) {
            return new GoalDecision("REPAIR_RELATIONSHIP", "흔들린 관계를 성급하지 않게 안정시키려 한다.", 82);
        }
        if (hasPastEventReflection(reflections)) {
            return new GoalDecision("ASK_ABOUT_PAST_EVENT", "최근 사용자가 말한 중요한 일을 조심스럽게 다시 물어본다.", 72);
        }
        if (selfState != null && value(selfState.getAffection()) > 0.65 && lowConflict(relationship)) {
            return new GoalDecision("EXPRESS_AFFECTION", "부담스럽지 않은 방식으로 애정을 표현한다.", 68);
        }

        return new GoalDecision("CHECK_IN", "사용자가 오늘 어떻게 지냈는지 자연스럽게 확인한다.", 55);
    }

    private boolean hasPastEventReflection(List<Reflection> reflections) {
        if (reflections == null) {
            return false;
        }

        return reflections.stream()
                .anyMatch(reflection -> reflection.getSummary() != null
                        && (reflection.getSummary().contains("힘")
                        || reflection.getSummary().contains("중요")
                        || reflection.getCategory().contains("PATTERN")));
    }

    private boolean lowConflict(Relationship relationship) {
        return relationship == null
                || (value(relationship.getConflictLevel()) < 25 && value(relationship.getBreakupRisk()) < 25);
    }

    private AgentGoal createGoal(Long userId, GoalDecision decision) {
        AgentGoal goal = new AgentGoal();
        goal.setUserId(userId);
        goal.setGoalType(decision.goalType());
        goal.setDescription(decision.description());
        goal.setPriority(decision.priority());
        goal.setStatus("ACTIVE");
        return goal;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private record GoalDecision(String goalType, String description, Integer priority) {
    }
}
