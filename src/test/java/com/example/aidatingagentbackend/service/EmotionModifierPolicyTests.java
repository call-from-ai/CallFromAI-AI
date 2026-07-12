package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.EmotionDelta;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.MessageSignals;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmotionModifierPolicyTests {

    private final EmotionTraitModifier traitModifier = new EmotionTraitModifier();
    private final RelationshipStageEmotionPolicy stagePolicy = new RelationshipStageEmotionPolicy();

    @Test
    void highJealousyIncreasesHurtAndInsecurityForJealousyEvent() {
        EmotionDelta base = new EmotionDelta(0.0, 0.0, 0.2, 0.1, 0.2, 0.0, 0.0);
        CharacterTraitProfile neutral = traits(5, 5, 5, 5);
        CharacterTraitProfile highJealousy = traits(5, 5, 10, 5);
        EventAnalysis jealousyEvent = new EventAnalysis(
                AgentEventType.IGNORE_OR_COLD,
                0.5,
                0.7,
                false,
                false,
                "jealousy",
                "사용자가 다른 사람을 언급했다."
        );

        EmotionDelta neutralDelta = traitModifier.apply(base, neutral, jealousyEvent, emptySignals());
        EmotionDelta jealousDelta = traitModifier.apply(base, highJealousy, jealousyEvent, emptySignals());

        assertThat(jealousDelta.hurt()).isGreaterThan(neutralDelta.hurt());
        assertThat(jealousDelta.insecurity()).isGreaterThan(neutralDelta.insecurity());
        assertThat(jealousDelta.anger()).isGreaterThan(neutralDelta.anger());
    }

    @Test
    void highAttachmentIncreasesInsecurityForColdEvent() {
        EmotionDelta base = new EmotionDelta(0.0, 0.0, 0.0, 0.0, 0.2, 0.0, 0.0);
        EventAnalysis coldEvent = EventAnalysis.fallback(AgentEventType.IGNORE_OR_COLD);

        EmotionDelta neutralDelta = traitModifier.apply(base, traits(5, 5, 5, 5), coldEvent, emptySignals());
        EmotionDelta attachedDelta = traitModifier.apply(base, traits(10, 5, 5, 5), coldEvent, emptySignals());

        assertThat(attachedDelta.insecurity()).isGreaterThan(neutralDelta.insecurity());
    }

    @Test
    void highEmotionalStabilityReducesNegativeDelta() {
        EmotionDelta base = new EmotionDelta(0.0, 0.0, 0.4, 0.3, 0.3, 0.2, 0.0);
        EventAnalysis insult = EventAnalysis.fallback(AgentEventType.INSULT);

        EmotionDelta neutralDelta = traitModifier.apply(base, traits(5, 5, 5, 5), insult, emptySignals());
        EmotionDelta stableDelta = traitModifier.apply(base, traits(5, 5, 5, 10), insult, emptySignals());

        assertThat(stableDelta.hurt()).isLessThan(neutralDelta.hurt());
        assertThat(stableDelta.anger()).isLessThan(neutralDelta.anger());
        assertThat(stableDelta.insecurity()).isLessThan(neutralDelta.insecurity());
    }

    @Test
    void highEmotionalStabilityIncreasesRecoveryAndDecay() {
        EmotionDelta recovery = new EmotionDelta(0.0, 0.0, -0.2, -0.1, -0.1, 0.0, 0.0);
        EventAnalysis apology = EventAnalysis.fallback(AgentEventType.APOLOGY);

        EmotionDelta neutralDelta = traitModifier.apply(recovery, traits(5, 5, 5, 5), apology, emptySignals());
        EmotionDelta stableDelta = traitModifier.apply(recovery, traits(5, 5, 5, 10), apology, emptySignals());

        assertThat(stableDelta.hurt()).isLessThan(neutralDelta.hurt());
        assertThat(traitModifier.decayModifier(traits(5, 5, 5, 10))).isGreaterThan(traitModifier.decayModifier(traits(5, 5, 5, 5)));
    }

    @Test
    void longTermIsLessAnxiousThanCrushForLowSeverityColdEvent() {
        EmotionDelta base = new EmotionDelta(0.0, 0.0, 0.0, 0.0, 0.2, 0.0, 0.0);
        EventAnalysis lowCold = new EventAnalysis(AgentEventType.IGNORE_OR_COLD, 0.3, 0.7, false, false, "distant", "답장이 늦었다.");

        EmotionDelta crushDelta = stagePolicy.apply(base, RelationshipStage.CRUSH, lowCold, emptySignals());
        EmotionDelta longTermDelta = stagePolicy.apply(base, RelationshipStage.LONG_TERM, lowCold, emptySignals());

        assertThat(longTermDelta.insecurity()).isLessThan(crushDelta.insecurity());
    }

    @Test
    void longTermDoesNotIgnoreHighSeverityBreakup() {
        EmotionDelta base = new EmotionDelta(0.0, -0.3, 0.7, 0.35, 0.6, 0.45, 0.4);
        EventAnalysis breakup = EventAnalysis.fallback(AgentEventType.BREAKUP_DECLARATION);

        EmotionDelta longTermDelta = stagePolicy.apply(base, RelationshipStage.LONG_TERM, breakup, emptySignals());

        assertThat(longTermDelta.hurt()).isEqualTo(base.hurt());
        assertThat(longTermDelta.insecurity()).isEqualTo(base.insecurity());
    }

    @Test
    void expressivenessDoesNotAffectEmotionDelta() {
        EmotionDelta base = new EmotionDelta(0.0, 0.0, 0.3, 0.2, 0.2, 0.1, 0.0);
        EventAnalysis insult = EventAnalysis.fallback(AgentEventType.INSULT);
        CharacterTraitProfile lowExpressive = traits(5, 5, 5, 5);
        lowExpressive.setExpressiveness(0);
        CharacterTraitProfile highExpressive = traits(5, 5, 5, 5);
        highExpressive.setExpressiveness(10);

        EmotionDelta lowDelta = traitModifier.apply(base, lowExpressive, insult, emptySignals());
        EmotionDelta highDelta = traitModifier.apply(base, highExpressive, insult, emptySignals());

        assertThat(highDelta).isEqualTo(lowDelta);
    }

    @Test
    void neutralTraitsKeepBaseDeltaNearlySame() {
        EmotionDelta base = new EmotionDelta(0.1, 0.0, 0.3, 0.2, 0.2, 0.1, 0.05);

        EmotionDelta delta = traitModifier.apply(base, traits(5, 5, 5, 5), EventAnalysis.fallback(AgentEventType.INSULT), emptySignals());

        assertThat(delta).isEqualTo(base);
    }

    private CharacterTraitProfile traits(int attachment, int affection, int jealousy, int emotionalStability) {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setAttachment(attachment);
        profile.setAffection(affection);
        profile.setJealousy(jealousy);
        profile.setEmotionalStability(emotionalStability);
        profile.setHumor(5);
        profile.setPlayfulness(5);
        profile.setEmpathy(5);
        profile.setDominance(5);
        profile.setConfidence(5);
        profile.setExpressiveness(5);
        return profile;
    }

    private MessageSignals emptySignals() {
        return new MessageSignals("", Set.of());
    }
}
