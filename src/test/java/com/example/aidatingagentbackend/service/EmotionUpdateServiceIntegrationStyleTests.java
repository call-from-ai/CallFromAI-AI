package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.engine.EventAnalyzer;
import com.example.aidatingagentbackend.engine.EventDetector;
import com.example.aidatingagentbackend.engine.MessageSignalDetector;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmotionUpdateServiceIntegrationStyleTests {

    private final MessageSignalDetector signalDetector = new MessageSignalDetector();

    @Test
    void emotionUpdateClampsValuesAfterModifiers() {
        AgentSelfState state = defaultState();
        state.setHurt(0.95);
        AgentSelfStateRepository selfStateRepository = selfStateRepository(state);
        EmotionUpdateService service = service(
                selfStateRepository,
                EventAnalysis.fallback(AgentEventType.BREAKUP_DECLARATION),
                traits(10, 10),
                relationship("CRUSH")
        );

        AgentSelfState updated = service.updateBeforeResponse(1L, "헤어지자").agentSelfState();

        assertThat(updated.getHurt()).isEqualTo(1.0);
        assertThat(updated.getInsecurity()).isBetween(0.0, 1.0);
    }

    @Test
    void traitAndStageLoadingDoesNotAddGeminiEventAnalyzerCalls() {
        EventAnalyzer analyzer = mock(EventAnalyzer.class);
        when(analyzer.analyze(any(), any(), any())).thenReturn(EventAnalysis.fallback(AgentEventType.AFFECTION));
        ChatMessageRepository chatRepository = mock(ChatMessageRepository.class);
        when(chatRepository.findTop20ByCharacterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        AgentSelfStateRepository selfStateRepository = selfStateRepository(defaultState());
        EmotionUpdateService service = service(
                selfStateRepository,
                analyzer,
                chatRepository,
                traits(5, 5),
                relationship("EARLY_DATING")
        );

        service.updateBeforeResponse(1L, "보고 싶어");

        verify(analyzer, times(1)).analyze(any(), any(), any());
    }

    @Test
    void optimisticLockingRetriesOnce() {
        AgentSelfStateRepository selfStateRepository = mock(AgentSelfStateRepository.class);
        when(selfStateRepository.findByCharacterId(1L))
                .thenReturn(Optional.of(defaultState()))
                .thenReturn(Optional.of(defaultState()));
        when(selfStateRepository.saveAndFlush(any(AgentSelfState.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        EmotionUpdateService service = service(
                selfStateRepository,
                EventAnalysis.fallback(AgentEventType.AFFECTION),
                traits(5, 5),
                relationship("CRUSH")
        );

        AgentSelfState updated = service.updateBeforeResponse(1L, "보고 싶어").agentSelfState();

        assertThat(updated.getAffection()).isGreaterThan(0.55);
        verify(selfStateRepository, times(2)).saveAndFlush(any(AgentSelfState.class));
    }

    private EmotionUpdateService service(
            AgentSelfStateRepository selfStateRepository,
            EventAnalysis eventAnalysis,
            CharacterTraitProfile traitProfile,
            Relationship relationship
    ) {
        EventAnalyzer analyzer = mock(EventAnalyzer.class);
        when(analyzer.analyze(any(), any(), any())).thenReturn(eventAnalysis);
        ChatMessageRepository chatRepository = mock(ChatMessageRepository.class);
        when(chatRepository.findTop20ByCharacterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        return service(selfStateRepository, analyzer, chatRepository, traitProfile, relationship);
    }

    private EmotionUpdateService service(
            AgentSelfStateRepository selfStateRepository,
            EventAnalyzer analyzer,
            ChatMessageRepository chatRepository,
            CharacterTraitProfile traitProfile,
            Relationship relationship
    ) {
        CharacterTraitProfileService traitProfileService = mock(CharacterTraitProfileService.class);
        when(traitProfileService.findEntityOrDefault(1L)).thenReturn(traitProfile);
        RelationshipRepository relationshipRepository = mock(RelationshipRepository.class);
        when(relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.of(relationship));
        SettingsDefaultPolicy defaultPolicy = new SettingsDefaultPolicy();
        return new EmotionUpdateService(
                selfStateRepository,
                analyzer,
                new EventDetector(signalDetector),
                chatRepository,
                null,
                null,
                signalDetector,
                traitProfileService,
                relationshipRepository,
                new RelationshipStageResolver(defaultPolicy),
                new EmotionTraitModifier(),
                new RelationshipStageEmotionPolicy()
        );
    }

    private AgentSelfStateRepository selfStateRepository(AgentSelfState state) {
        AgentSelfStateRepository repository = mock(AgentSelfStateRepository.class);
        when(repository.findByCharacterId(1L)).thenReturn(Optional.of(state));
        when(repository.saveAndFlush(any(AgentSelfState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return repository;
    }

    private CharacterTraitProfile traits(int attachment, int emotionalStability) {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setAttachment(attachment);
        profile.setAffection(5);
        profile.setJealousy(5);
        profile.setEmotionalStability(emotionalStability);
        profile.setHumor(5);
        profile.setPlayfulness(5);
        profile.setEmpathy(5);
        profile.setDominance(5);
        profile.setConfidence(5);
        profile.setExpressiveness(5);
        return profile;
    }

    private Relationship relationship(String stage) {
        Relationship relationship = new Relationship();
        relationship.setCharacterId(1L);
        relationship.setRelationshipStage(stage);
        return relationship;
    }

    private AgentSelfState defaultState() {
        AgentSelfState state = new AgentSelfState();
        state.setCharacterId(1L);
        state.setAffection(0.55);
        state.setTrust(0.6);
        state.setHurt(0.0);
        state.setAnger(0.0);
        state.setInsecurity(0.15);
        state.setDisappointment(0.0);
        state.setEmotionalDistance(0.15);
        state.setLastEmotion("calm");
        state.setLastSignificantEvent("none");
        state.setUpdatedAt(LocalDateTime.now());
        return state;
    }
}
