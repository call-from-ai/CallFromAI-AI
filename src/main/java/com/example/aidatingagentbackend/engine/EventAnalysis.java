package com.example.aidatingagentbackend.engine;

public record EventAnalysis(
        AgentEventType eventType,
        Double severity,
        Double sincerity,
        Boolean isJoke,
        Boolean isManipulative,
        String primaryEmotion,
        String summary
) {

    public static EventAnalysis fallback(AgentEventType eventType) {
        return new EventAnalysis(
                eventType,
                defaultSeverity(eventType),
                defaultSincerity(eventType),
                false,
                false,
                defaultPrimaryEmotion(eventType),
                "Analyzed by rule-based fallback detector."
        );
    }

    private static double defaultSeverity(AgentEventType eventType) {
        return switch (eventType) {
            case BREAKUP_DECLARATION -> 0.9;
            case INSULT -> 0.7;
            case BREAKUP_RETRACTION, APOLOGY -> 0.5;
            case IGNORE_OR_COLD -> 0.45;
            case AFFECTION -> 0.35;
            case NORMAL -> 0.1;
        };
    }

    private static double defaultSincerity(AgentEventType eventType) {
        return switch (eventType) {
            case BREAKUP_RETRACTION -> 0.35;
            case NORMAL -> 0.5;
            default -> 0.75;
        };
    }

    private static String defaultPrimaryEmotion(AgentEventType eventType) {
        return switch (eventType) {
            case BREAKUP_DECLARATION -> "hurt";
            case BREAKUP_RETRACTION -> "confused";
            case APOLOGY -> "softened";
            case AFFECTION -> "affection";
            case INSULT -> "upset";
            case IGNORE_OR_COLD -> "distant";
            case NORMAL -> "neutral";
        };
    }
}
