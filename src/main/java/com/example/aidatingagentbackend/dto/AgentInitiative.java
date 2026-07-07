package com.example.aidatingagentbackend.dto;

public record AgentInitiative(
        String conversationAct,
        String selfDisclosure,
        String agentQuestion,
        String topicShift,
        boolean shouldAskQuestion
) {
}
