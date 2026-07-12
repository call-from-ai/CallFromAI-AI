package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.EmotionDelta;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.MessageSignalType;
import com.example.aidatingagentbackend.engine.MessageSignals;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import org.springframework.stereotype.Service;

@Service
public class EmotionTraitModifier {

    public EmotionDelta apply(
            EmotionDelta baseDelta,
            CharacterTraitProfile traitProfile,
            EventAnalysis eventAnalysis,
            MessageSignals signals
    ) {
        if (baseDelta == null) {
            return EmotionDelta.none();
        }
        CharacterTraitProfile traits = traitProfile == null ? defaultTraits() : traitProfile;
        boolean negativeEvent = isNegative(eventAnalysis, signals);
        boolean positiveEvent = isPositive(eventAnalysis, signals);
        boolean attachmentRelevant = isAttachmentRelevant(eventAnalysis, signals);
        boolean jealousyRelevant = isJealousyRelevant(eventAnalysis);

        double stabilityPositive = positiveModifier(value(traits.getEmotionalStability()));
        double stabilityInverse = inverseModifier(value(traits.getEmotionalStability()));
        double attachmentPositive = attachmentRelevant ? positiveModifier(value(traits.getAttachment())) : 1.0;
        double jealousyPositive = jealousyRelevant ? positiveModifier(value(traits.getJealousy())) : 1.0;
        double affectionPositive = positiveEvent ? positiveModifier(value(traits.getAffection())) : 1.0;

        double hurtModifier = negativeEvent ? stabilityInverse : 1.0;
        double angerModifier = negativeEvent ? stabilityInverse : 1.0;
        double insecurityModifier = negativeEvent ? stabilityInverse : 1.0;
        double disappointmentModifier = negativeEvent ? stabilityInverse : 1.0;
        double distanceModifier = 1.0;

        if (attachmentRelevant) {
            hurtModifier *= attachmentPositive;
            insecurityModifier *= attachmentPositive;
            distanceModifier *= attachmentPositive;
        }
        if (jealousyRelevant) {
            hurtModifier *= jealousyPositive;
            insecurityModifier *= jealousyPositive;
            angerModifier *= mildPositiveModifier(value(traits.getJealousy()));
        }

        if (isRecovery(baseDelta)) {
            double recoveryModifier = stabilityPositive;
            return baseDelta.multiply(
                    affectionPositive,
                    recoveryModifier,
                    recoveryModifier,
                    recoveryModifier,
                    recoveryModifier,
                    recoveryModifier
            );
        }

        return baseDelta.multiply(
                affectionPositive,
                hurtModifier,
                angerModifier,
                insecurityModifier,
                disappointmentModifier,
                distanceModifier
        );
    }

    public double decayModifier(CharacterTraitProfile traitProfile) {
        return positiveModifier(value((traitProfile == null ? defaultTraits() : traitProfile).getEmotionalStability()));
    }

    private boolean isNegative(EventAnalysis eventAnalysis, MessageSignals signals) {
        AgentEventType eventType = eventAnalysis == null ? AgentEventType.NORMAL : eventAnalysis.eventType();
        return switch (eventType) {
            case BREAKUP_DECLARATION, INSULT, IGNORE_OR_COLD -> true;
            case BREAKUP_RETRACTION, APOLOGY, AFFECTION, NORMAL -> false;
        } || (signals != null && signals.has(MessageSignalType.USER_SKIPPED_MEAL));
    }

    private boolean isPositive(EventAnalysis eventAnalysis, MessageSignals signals) {
        AgentEventType eventType = eventAnalysis == null ? AgentEventType.NORMAL : eventAnalysis.eventType();
        return eventType == AgentEventType.AFFECTION
                || eventType == AgentEventType.APOLOGY
                || eventType == AgentEventType.BREAKUP_RETRACTION
                || (signals != null && signals.hasAny(MessageSignalType.AFFECTION, MessageSignalType.USER_RETURNED_TO_TALK));
    }

    private boolean isAttachmentRelevant(EventAnalysis eventAnalysis, MessageSignals signals) {
        AgentEventType eventType = eventAnalysis == null ? AgentEventType.NORMAL : eventAnalysis.eventType();
        return eventType == AgentEventType.BREAKUP_DECLARATION
                || eventType == AgentEventType.BREAKUP_RETRACTION
                || eventType == AgentEventType.IGNORE_OR_COLD
                || eventType == AgentEventType.AFFECTION
                || (signals != null && signals.hasAny(MessageSignalType.AFFECTION, MessageSignalType.USER_RETURNED_TO_TALK));
    }

    private boolean isJealousyRelevant(EventAnalysis eventAnalysis) {
        if (eventAnalysis == null) {
            return false;
        }
        String emotion = eventAnalysis.primaryEmotion() == null ? "" : eventAnalysis.primaryEmotion().toLowerCase();
        String summary = eventAnalysis.summary() == null ? "" : eventAnalysis.summary().toLowerCase();
        return emotion.contains("jealous")
                || emotion.contains("질투")
                || summary.contains("질투")
                || summary.contains("다른 사람")
                || summary.contains("전 애인");
    }

    private boolean isRecovery(EmotionDelta delta) {
        return delta.hurt() < 0.0
                || delta.anger() < 0.0
                || delta.insecurity() < 0.0
                || delta.disappointment() < 0.0
                || delta.emotionalDistance() < 0.0;
    }

    private double positiveModifier(int trait) {
        return 1.0 + normalized(trait) * 0.3;
    }

    private double mildPositiveModifier(int trait) {
        return 1.0 + normalized(trait) * 0.15;
    }

    private double inverseModifier(int trait) {
        return 1.0 - normalized(trait) * 0.3;
    }

    private double normalized(int trait) {
        return (trait - 5) / 5.0;
    }

    private int value(Integer value) {
        return value == null ? 5 : Math.max(0, Math.min(10, value));
    }

    private CharacterTraitProfile defaultTraits() {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setHumor(5);
        profile.setPlayfulness(5);
        profile.setAffection(5);
        profile.setEmpathy(5);
        profile.setAttachment(5);
        profile.setJealousy(5);
        profile.setDominance(5);
        profile.setConfidence(5);
        profile.setExpressiveness(5);
        profile.setEmotionalStability(5);
        return profile;
    }
}
