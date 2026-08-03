package com.example.aidatingagentbackend.dto;

import java.util.List;

public record CallTopicRequest(
        Long callId,
        List<SummaryMessage> messages,
        Integer maxCharacters
) {
}
