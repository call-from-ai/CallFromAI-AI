package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryRetrievalServiceTraitTests {

    @Test
    void semanticSimilarityIsNotOverriddenByTraitBonus() {
        Fixture fixture = fixture("생일 선물 뭐 좋아해?");
        Memory semantic = memory(1L, "사용자는 생일 선물 이야기를 했다", "semantic", 5);
        Memory attachment = memory(2L, "사용자는 연락 약속과 이전 갈등을 중요하게 여긴다", "attachment", 5);
        when(fixture.repository.findByCharacterId(1L)).thenReturn(List.of(attachment, semantic));
        fixture.similarity("semantic", 0.92);
        fixture.similarity("attachment", 0.30);

        List<Memory> result = fixture.service.retrieve(
                1L,
                "생일 선물 뭐 좋아해?",
                state("neutral"),
                traits(10, 5, 5, 5),
                RelationshipStage.EARLY_DATING,
                50,
                EventAnalysis.fallback(AgentEventType.NORMAL)
        );

        assertThat(result).first().isSameAs(semantic);
    }

    @Test
    void highAttachmentLightlyPrioritizesContactMemory() {
        Fixture fixture = fixture("연락 왜 늦었어");
        Memory neutral = memory(1L, "사용자는 영화 이야기를 했다", "neutral", 5);
        Memory contact = memory(2L, "사용자는 연락과 답장 약속을 중요하게 말했다", "contact", 5);
        when(fixture.repository.findByCharacterId(1L)).thenReturn(List.of(neutral, contact));
        fixture.similarity("neutral", 0.50);
        fixture.similarity("contact", 0.50);

        List<Memory> result = fixture.service.retrieve(
                1L,
                "연락 왜 늦었어",
                state("neutral"),
                traits(10, 5, 5, 5),
                RelationshipStage.EARLY_DATING,
                50,
                EventAnalysis.fallback(AgentEventType.NORMAL)
        );

        assertThat(result).first().isSameAs(contact);
    }

    @Test
    void highJealousyDoesNotApplyWithoutJealousyEvent() {
        Fixture fixture = fixture("오늘 뭐해");
        Memory neutral = memory(1L, "사용자는 오늘 일정 이야기를 했다", "neutral", 5);
        Memory jealous = memory(2L, "사용자는 다른 사람과 있었던 일을 말했다", "jealous", 5);
        when(fixture.repository.findByCharacterId(1L)).thenReturn(List.of(neutral, jealous));
        fixture.similarity("neutral", 0.50);
        fixture.similarity("jealous", 0.50);

        List<Memory> result = fixture.service.retrieve(
                1L,
                "오늘 뭐해",
                state("neutral"),
                traits(5, 5, 10, 5),
                RelationshipStage.EARLY_DATING,
                50,
                EventAnalysis.fallback(AgentEventType.NORMAL)
        );

        assertThat(result).first().isSameAs(neutral);
    }

    @Test
    void highEmpathyLightlyPrioritizesConcernMemory() {
        Fixture fixture = fixture("나 고민 있어");
        Memory casual = memory(1L, "사용자는 카페 이야기를 했다", "casual", 5);
        Memory concern = memory(2L, "사용자는 피곤하고 스트레스를 받는다고 말했다", "concern", 5);
        when(fixture.repository.findByCharacterId(1L)).thenReturn(List.of(casual, concern));
        fixture.similarity("casual", 0.50);
        fixture.similarity("concern", 0.50);

        List<Memory> result = fixture.service.retrieve(
                1L,
                "나 고민 있어",
                state("sad"),
                traits(5, 10, 5, 5),
                RelationshipStage.EARLY_DATING,
                50,
                new EventAnalysis(AgentEventType.NORMAL, 0.4, 0.8, false, false, "sadness", "사용자가 고민을 말했다")
        );

        assertThat(result).first().isSameAs(concern);
    }

    @Test
    void existingMemoryEmbeddingsDoNotTriggerAdditionalEmbeddingCalls() {
        Fixture fixture = fixture("query");
        Memory memory = memory(1L, "이미 임베딩된 기억", "stored", 5);
        when(fixture.repository.findByCharacterId(1L)).thenReturn(List.of(memory));
        fixture.similarity("stored", 0.50);

        fixture.service.retrieve(
                1L,
                "query",
                state("neutral"),
                traits(5, 5, 5, 5),
                RelationshipStage.CRUSH,
                50,
                EventAnalysis.fallback(AgentEventType.NORMAL)
        );

        verify(fixture.embeddingService).embed("query");
        verify(fixture.embeddingService, never()).embed("이미 임베딩된 기억");
    }

    private Fixture fixture(String userMessage) {
        MemoryRepository repository = mock(MemoryRepository.class);
        MemoryEmbeddingService embeddingService = mock(MemoryEmbeddingService.class);
        double[] queryEmbedding = new double[]{1.0, 0.0};
        when(embeddingService.embed(userMessage)).thenReturn(queryEmbedding);
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new Fixture(
                repository,
                embeddingService,
                queryEmbedding,
                new MemoryRetrievalService(repository, embeddingService)
        );
    }

    private Memory memory(Long id, String summary, String embedding, int importance) {
        Memory memory = new Memory();
        memory.setId(id);
        memory.setCharacterId(1L);
        memory.setSummary(summary);
        memory.setEmbedding(embedding);
        memory.setImportance(importance);
        memory.setCreatedAt(LocalDateTime.now().minusDays(30));
        return memory;
    }

    private CharacterTraitProfile traits(int attachment, int empathy, int jealousy, int affection) {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setAttachment(attachment);
        profile.setEmpathy(empathy);
        profile.setJealousy(jealousy);
        profile.setAffection(affection);
        return profile;
    }

    private State state(String emotion) {
        State state = new State();
        state.setEmotion(emotion);
        return state;
    }

    private record Fixture(
            MemoryRepository repository,
            MemoryEmbeddingService embeddingService,
            double[] queryEmbedding,
            MemoryRetrievalService service
    ) {

        void similarity(String embedding, double similarity) {
            double[] memoryEmbedding = new double[]{similarity, 1.0 - similarity};
            when(embeddingService.deserialize(embedding)).thenReturn(memoryEmbedding);
            when(embeddingService.cosineSimilarity(queryEmbedding, memoryEmbedding)).thenReturn(similarity);
        }
    }
}
