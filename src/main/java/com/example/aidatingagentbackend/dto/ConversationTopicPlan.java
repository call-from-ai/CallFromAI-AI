package com.example.aidatingagentbackend.dto;

public record ConversationTopicPlan(
        String topic,
        boolean allowTopicChange,
        String instruction
) {
}
