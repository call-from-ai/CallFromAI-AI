package com.example.aidatingagentbackend.engine;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventDetector {

    private static final List<String> BREAKUP_DECLARATION_KEYWORDS = List.of(
            "헤어지자", "우리 끝내자", "끝내자", "그만 만나자", "그만 만나", "이별하자"
    );

    private static final List<String> BREAKUP_RETRACTION_KEYWORDS = List.of(
            "아니야 농담", "농담이야", "방금 말 취소", "취소할게", "아니야 괜찮아", "아냐 괜찮아"
    );

    private static final List<String> APOLOGY_KEYWORDS = List.of(
            "미안", "죄송", "내가 잘못", "잘못했어", "사과할게"
    );

    private static final List<String> AFFECTION_KEYWORDS = List.of(
            "사랑해", "좋아해", "보고 싶어", "보고싶어", "네 생각났어"
    );

    private static final List<String> INSULT_KEYWORDS = List.of(
            "짜증나", "한심", "최악", "싫어", "질려", "귀찮아"
    );

    private static final List<String> IGNORE_OR_COLD_KEYWORDS = List.of(
            "몰라", "상관없어", "됐어", "말 걸지마", "답하기 싫어", "귀찮으니까"
    );

    public AgentEventType detect(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return AgentEventType.NORMAL;
        }

        String normalized = userMessage.toLowerCase();
        if (containsAny(normalized, BREAKUP_DECLARATION_KEYWORDS)) {
            return AgentEventType.BREAKUP_DECLARATION;
        }
        if (containsAny(normalized, BREAKUP_RETRACTION_KEYWORDS)) {
            return AgentEventType.BREAKUP_RETRACTION;
        }
        if (containsAny(normalized, APOLOGY_KEYWORDS)) {
            return AgentEventType.APOLOGY;
        }
        if (containsAny(normalized, AFFECTION_KEYWORDS)) {
            return AgentEventType.AFFECTION;
        }
        if (containsAny(normalized, INSULT_KEYWORDS)) {
            return AgentEventType.INSULT;
        }
        if (containsAny(normalized, IGNORE_OR_COLD_KEYWORDS)) {
            return AgentEventType.IGNORE_OR_COLD;
        }

        return AgentEventType.NORMAL;
    }

    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream()
                .map(String::toLowerCase)
                .anyMatch(message::contains);
    }
}
