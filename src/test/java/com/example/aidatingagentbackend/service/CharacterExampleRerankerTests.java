package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterExampleRerankerTests {

    private final CharacterExampleReranker reranker = new CharacterExampleReranker(
            new CharacterExampleToneTagPolicy(),
            new RelationshipTemperatureScoreResolver(new SettingsDefaultPolicy()),
            new CharacterExampleRelevantTraitPolicy()
    );

    @Test
    void jealousyEventWithHighJealousyPrioritizesJealousTone() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.IGNORE_OR_COLD, "warm-soft", 90, "괜찮아"),
                        example(2L, AgentEventType.IGNORE_OR_COLD, "jealous-expressive", 80, "누구랑 있었는데")
                ),
                new EventAnalysis(AgentEventType.IGNORE_OR_COLD, 0.5, 0.7, false, false, "jealousy", "사용자가 다른 사람 이야기를 했다"),
                RelationshipStage.EARLY_DATING,
                75,
                traits(5, 5, 5, 6, 9, 10, 5, 6, 10, 6)
        );

        assertThat(result).extracting(CharacterExample::getToneTag)
                .startsWith("jealous-expressive");
    }

    @Test
    void sadnessContextWithHighEmpathyPrioritizesWarmTone() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.NORMAL, "spicy-pushpull", 95, "뭐 그렇게까지 힘들어하냐"),
                        example(2L, AgentEventType.NORMAL, "warm-soft", 80, "그건 좀 힘들었겠다")
                ),
                new EventAnalysis(AgentEventType.NORMAL, 0.3, 0.7, false, false, "sadness", "사용자가 고민과 힘듦을 말했다"),
                RelationshipStage.EARLY_DATING,
                35,
                traits(5, 5, 6, 10, 5, 5, 4, 5, 6, 9)
        );

        assertThat(result).extracting(CharacterExample::getToneTag)
                .startsWith("warm-soft");
    }

    @Test
    void crushStageRemovesStrongPossessiveExamples() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.AFFECTION, "possessive-dominant", 100, "넌 내꺼야"),
                        example(2L, AgentEventType.AFFECTION, "warm-affection", 70, "그 말은 좀 좋네")
                ),
                EventAnalysis.fallback(AgentEventType.AFFECTION),
                RelationshipStage.CRUSH,
                75,
                traits(5, 5, 8, 6, 6, 5, 9, 9, 8, 6)
        );

        assertThat(result).extracting(CharacterExample::getToneTag)
                .doesNotContain("possessive-dominant")
                .contains("warm-affection");
    }

    @Test
    void temperatureNinetyPrioritizesConfidentTone() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.NORMAL, "warm-gentle", 95, "오늘 어땠어"),
                        example(2L, AgentEventType.NORMAL, "confident-dominant", 90, "드디어 왔네")
                ),
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipStage.LONG_TERM,
                90,
                traits(7, 8, 6, 5, 5, 5, 10, 10, 7, 6)
        );

        assertThat(result).extracting(CharacterExample::getToneTag)
                .startsWith("confident-dominant");
    }

    @Test
    void temperatureNinetyDoesNotForceJealousToneWithoutJealousyEvent() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.NORMAL, "jealous-expressive", 100, "누구랑 있었는데"),
                        example(2L, AgentEventType.NORMAL, "confident-dominant", 70, "늦었네")
                ),
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipStage.LONG_TERM,
                90,
                traits(5, 7, 5, 5, 9, 10, 10, 10, 10, 5)
        );

        assertThat(result).extracting(CharacterExample::getToneTag)
                .doesNotContain("jealous-expressive")
                .contains("confident-dominant");
    }

    @Test
    void resultIsLimitedToTopFive() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.NORMAL, "warm-1", 100, "1"),
                        example(2L, AgentEventType.NORMAL, "spicy-2", 99, "2"),
                        example(3L, AgentEventType.NORMAL, "playful-3", 98, "3"),
                        example(4L, AgentEventType.NORMAL, "repair-4", 97, "4"),
                        example(5L, AgentEventType.NORMAL, "confident-5", 96, "5"),
                        example(6L, AgentEventType.NORMAL, "neutral-6", 95, "6")
                ),
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipStage.LONG_TERM,
                50,
                traits(5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
        );

        assertThat(result).hasSize(5);
    }

    @Test
    void duplicateExampleTextIsRemoved() {
        List<CharacterExample> result = reranker.rerank(
                List.of(
                        example(1L, AgentEventType.NORMAL, "warm-a", 100, "같은 문장"),
                        example(2L, AgentEventType.NORMAL, "warm-b", 90, "같은 문장"),
                        example(3L, AgentEventType.NORMAL, "spicy-a", 80, "다른 문장")
                ),
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipStage.LONG_TERM,
                50,
                traits(5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
        );

        assertThat(result).extracting(CharacterExample::getAssistantExample)
                .containsExactlyInAnyOrder("같은 문장", "다른 문장");
    }

    @Test
    void missingStageMetadataIsNeutralFallback() {
        CharacterExample legacy = example(1L, AgentEventType.NORMAL, "warm-soft", 80, "기존 예시");
        legacy.setRelationshipStage(null);

        List<CharacterExample> result = reranker.rerank(
                List.of(legacy),
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipStage.CRUSH,
                35,
                traits(5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
        );

        assertThat(result).containsExactly(legacy);
    }

    @Test
    void zeroCandidatesReturnsEmptyList() {
        List<CharacterExample> result = reranker.rerank(
                List.of(),
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipStage.CRUSH,
                50,
                traits(5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
        );

        assertThat(result).isEmpty();
    }

    private CharacterExample example(
            Long id,
            AgentEventType eventType,
            String toneTag,
            int priority,
            String assistantExample
    ) {
        CharacterExample example = new CharacterExample();
        example.setId(id);
        example.setCharacterId(1L);
        example.setEventType(eventType);
        example.setRelationshipTemperature(RelationshipTemperature.NEUTRAL);
        example.setToneTag(toneTag);
        example.setPriority(priority);
        example.setUserExample("user-" + id);
        example.setAssistantExample(assistantExample);
        example.setActive(true);
        return example;
    }

    private CharacterTraitProfile traits(
            int humor,
            int playfulness,
            int affection,
            int empathy,
            int attachment,
            int jealousy,
            int dominance,
            int confidence,
            int expressiveness,
            int emotionalStability
    ) {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setHumor(humor);
        profile.setPlayfulness(playfulness);
        profile.setAffection(affection);
        profile.setEmpathy(empathy);
        profile.setAttachment(attachment);
        profile.setJealousy(jealousy);
        profile.setDominance(dominance);
        profile.setConfidence(confidence);
        profile.setExpressiveness(expressiveness);
        profile.setEmotionalStability(emotionalStability);
        return profile;
    }
}
