package com.example.aidatingagentbackend.engine;

import org.springframework.stereotype.Component;

@Component
public class EventDetector {

    private final MessageSignalDetector messageSignalDetector;

    public EventDetector(MessageSignalDetector messageSignalDetector) {
        this.messageSignalDetector = messageSignalDetector;
    }

    public AgentEventType detect(String userMessage) {
        return detectWithConfidence(userMessage).eventType();
    }

    public RuleBasedEventDetection detectWithConfidence(String userMessage) {
        MessageSignals signals = messageSignalDetector.detect(userMessage);

        if (signals.normalizedMessage() == null || signals.normalizedMessage().isBlank()) {
            return new RuleBasedEventDetection(AgentEventType.NORMAL, 0.95, false);
        }
        if (signals.has(MessageSignalType.BREAKUP_DECLARATION)) {
            return new RuleBasedEventDetection(AgentEventType.BREAKUP_DECLARATION, 0.95, true);
        }
        if (signals.has(MessageSignalType.BREAKUP_RETRACTION)) {
            return new RuleBasedEventDetection(AgentEventType.BREAKUP_RETRACTION, 0.9, true);
        }
        if (signals.has(MessageSignalType.APOLOGY)) {
            return new RuleBasedEventDetection(AgentEventType.APOLOGY, 0.9, true);
        }
        if (signals.has(MessageSignalType.AFFECTION)) {
            return new RuleBasedEventDetection(AgentEventType.AFFECTION, 0.85, false);
        }
        if (signals.has(MessageSignalType.INSULT)) {
            return new RuleBasedEventDetection(AgentEventType.INSULT, 0.9, true);
        }
        if (signals.has(MessageSignalType.IGNORE_OR_COLD)) {
            return new RuleBasedEventDetection(AgentEventType.IGNORE_OR_COLD, 0.85, true);
        }
        if (signals.has(MessageSignalType.AMBIGUOUS_IMPORTANT)) {
            return new RuleBasedEventDetection(AgentEventType.NORMAL, 0.45, true);
        }

        return new RuleBasedEventDetection(AgentEventType.NORMAL, 0.95, false);
    }

    public record RuleBasedEventDetection(
            AgentEventType eventType,
            Double confidence,
            Boolean needsLlmClarification
    ) {
    }
}
