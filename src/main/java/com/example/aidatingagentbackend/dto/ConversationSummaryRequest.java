package com.example.aidatingagentbackend.dto;

import java.util.List;

public record ConversationSummaryRequest(
        Long relationshipId,
        String userName,
        String characterName,
        String previousSummary,
        List<SummaryMessage> messages,
        Integer maxCharacters
) {
}
