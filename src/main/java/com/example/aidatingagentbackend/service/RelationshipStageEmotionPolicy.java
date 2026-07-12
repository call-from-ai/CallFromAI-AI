package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.EmotionDelta;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.MessageSignalType;
import com.example.aidatingagentbackend.engine.MessageSignals;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.springframework.stereotype.Service;

@Service
public class RelationshipStageEmotionPolicy {

    public EmotionDelta apply(
            EmotionDelta baseDelta,
            RelationshipStage stage,
            EventAnalysis eventAnalysis,
            MessageSignals signals
    ) {
        if (baseDelta == null) {
            return EmotionDelta.none();
        }
        RelationshipStage resolvedStage = stage == null ? RelationshipStage.CRUSH : stage;
        double severity = eventAnalysis == null || eventAnalysis.severity() == null ? 0.0 : eventAnalysis.severity();
        AgentEventType eventType = eventAnalysis == null ? AgentEventType.NORMAL : eventAnalysis.eventType();
        boolean lowSeverityCold = eventType == AgentEventType.IGNORE_OR_COLD && severity < 0.6;
        boolean positiveAffection = eventType == AgentEventType.AFFECTION
                || (signals != null && signals.has(MessageSignalType.AFFECTION));

        return switch (resolvedStage) {
            case CRUSH -> {
                double insecurityModifier = lowSeverityCold ? 1.15 : 1.0;
                double affectionModifier = positiveAffection ? 1.1 : 1.0;
                yield baseDelta.multiply(affectionModifier, 1.0, 1.0, insecurityModifier, 1.0, 1.0);
            }
            case DATING, EARLY_DATING -> {
                double affectionModifier = positiveAffection ? 1.15 : 1.0;
                double hurtModifier = eventType == AgentEventType.BREAKUP_DECLARATION ? 1.05 : 1.0;
                yield baseDelta.multiply(affectionModifier, hurtModifier, 1.0, 1.0, 1.0, 1.0);
            }
            case DEEP_LOVE, LONG_TERM -> {
                double insecurityModifier = lowSeverityCold ? 0.75 : 1.0;
                yield baseDelta.multiply(1.0, 1.0, 1.0, insecurityModifier, 1.0, 1.0);
            }
        };
    }
}
