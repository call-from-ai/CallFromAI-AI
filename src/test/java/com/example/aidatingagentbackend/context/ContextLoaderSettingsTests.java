package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.dto.PreferenceQuestionPlan;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import com.example.aidatingagentbackend.service.AgentGoalService;
import com.example.aidatingagentbackend.service.AgentInitiativeService;
import com.example.aidatingagentbackend.service.AgentLifeEventService;
import com.example.aidatingagentbackend.service.AgentProfileService;
import com.example.aidatingagentbackend.service.AgentWorldStateService;
import com.example.aidatingagentbackend.service.CharacterExampleService;
import com.example.aidatingagentbackend.service.CharacterPreferenceService;
import com.example.aidatingagentbackend.service.CharacterTraitProfileService;
import com.example.aidatingagentbackend.service.ConversationEventService;
import com.example.aidatingagentbackend.service.ConversationTopicService;
import com.example.aidatingagentbackend.service.RelationshipStageResolver;
import com.example.aidatingagentbackend.service.RelationshipTemperatureScoreResolver;
import com.example.aidatingagentbackend.service.SettingsDefaultPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextLoaderSettingsTests {

    @Test
    void missingRelationshipAndTraitsUseDefaultsWithoutInsertingRelationship() {
        Fixture fixture = fixture();
        when(fixture.relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.empty());

        var context = fixture.loader.load(
                1L,
                "안녕",
                EventAnalysis.fallback(AgentEventType.NORMAL),
                null
        );

        assertThat(context.relationshipStage()).isEqualTo(RelationshipStage.CRUSH);
        assertThat(context.relationshipTemperatureScore()).isEqualTo(50);
        assertThat(context.characterTraitProfile().getHumor()).isEqualTo(5);
        verify(fixture.relationshipRepository, never()).save(any(Relationship.class));
    }

    @Test
    void storedRelationshipTemperatureScoreOverridesLegacyEnum() {
        Fixture fixture = fixture();
        Relationship relationship = new Relationship();
        relationship.setCharacterId(1L);
        relationship.setRelationshipStage("LONG_TERM");
        relationship.setRelationshipTemperatureScore(90);
        when(fixture.relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.of(relationship));

        var context = fixture.loader.load(
                1L,
                "안녕",
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipTemperature.FRIENDLY
        );

        assertThat(context.relationshipStage()).isEqualTo(RelationshipStage.LONG_TERM);
        assertThat(context.relationshipTemperatureScore()).isEqualTo(90);
    }

    @Test
    void romanceStyleScoreIsLoadedSeparatelyFromRelationshipTemperatureScore() {
        Fixture fixture = fixture();
        Character character = new Character();
        character.setId(1L);
        character.setRomanceStyleScore(90);
        when(fixture.characterRepository.findById(1L)).thenReturn(Optional.of(character));
        Relationship relationship = new Relationship();
        relationship.setCharacterId(1L);
        relationship.setRelationshipStage("CRUSH");
        relationship.setRelationshipTemperatureScore(25);
        when(fixture.relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.of(relationship));

        var context = fixture.loader.load(1L, "안녕", EventAnalysis.fallback(AgentEventType.NORMAL), null);

        assertThat(context.romanceStyleScore()).isEqualTo(90);
        assertThat(context.relationshipTemperatureScore()).isEqualTo(25);
    }

    @Test
    void legacyTemperatureEnumIsUsedWhenRelationshipHasNoStoredScore() {
        Fixture fixture = fixture();
        Relationship relationship = new Relationship();
        relationship.setCharacterId(1L);
        relationship.setRelationshipStage("EARLY_DATING");
        relationship.setRelationshipTemperatureScore(null);
        when(fixture.relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.of(relationship));

        var context = fixture.loader.load(
                1L,
                "안녕",
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipTemperature.SPICY
        );

        assertThat(context.relationshipTemperatureScore()).isEqualTo(85);
    }

    @Test
    void settingsAreLoadedOnceAndReusedInsideContextLoader() {
        Fixture fixture = fixture();
        Relationship relationship = new Relationship();
        relationship.setCharacterId(1L);
        relationship.setRelationshipStage("EARLY_DATING");
        relationship.setRelationshipTemperatureScore(65);
        when(fixture.relationshipRepository.findByCharacterId(1L)).thenReturn(Optional.of(relationship));

        fixture.loader.load(
                1L,
                "안녕",
                EventAnalysis.fallback(AgentEventType.NORMAL),
                RelationshipTemperature.NEUTRAL
        );

        verify(fixture.relationshipRepository).findByCharacterId(1L);
        verify(fixture.traitProfileService).findEntityOrDefault(1L);
        verify(fixture.agentSelfStateRepository).findByCharacterId(1L);
        verify(fixture.characterExampleService).findRelevantEntities(
                eq(1L),
                any(EventAnalysis.class),
                eq(RelationshipTemperature.NEUTRAL),
                eq(RelationshipStage.EARLY_DATING),
                eq(65),
                eq(50),
                any(CharacterTraitProfile.class)
        );
    }

    private Fixture fixture() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        StateRepository stateRepository = mock(StateRepository.class);
        RelationshipRepository relationshipRepository = mock(RelationshipRepository.class);
        AgentSelfStateRepository agentSelfStateRepository = mock(AgentSelfStateRepository.class);
        MemoryRetrievalService memoryRetrievalService = mock(MemoryRetrievalService.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        CharacterExampleService characterExampleService = mock(CharacterExampleService.class);
        CharacterTraitProfileService traitProfileService = mock(CharacterTraitProfileService.class);
        AgentProfileService agentProfileService = mock(AgentProfileService.class);
        AgentWorldStateService agentWorldStateService = mock(AgentWorldStateService.class);
        AgentGoalService agentGoalService = mock(AgentGoalService.class);
        AgentInitiativeService agentInitiativeService = mock(AgentInitiativeService.class);
        AgentLifeEventService agentLifeEventService = mock(AgentLifeEventService.class);
        ConversationEventService conversationEventService = mock(ConversationEventService.class);
        CharacterPreferenceService characterPreferenceService = mock(CharacterPreferenceService.class);
        ConversationTopicService conversationTopicService = mock(ConversationTopicService.class);
        SettingsDefaultPolicy settingsDefaultPolicy = new SettingsDefaultPolicy();

        State state = new State();
        state.setEmotion("neutral");
        CharacterTraitProfile traits = new CharacterTraitProfile();
        traits.setCharacterId(1L);
        traits.setHumor(5);
        traits.setPlayfulness(5);
        traits.setAffection(5);
        traits.setEmpathy(5);
        traits.setAttachment(5);
        traits.setJealousy(5);
        traits.setDominance(5);
        traits.setConfidence(5);
        traits.setExpressiveness(5);
        traits.setEmotionalStability(5);
        AgentSelfState selfState = new AgentSelfState();

        when(characterRepository.findById(1L)).thenReturn(Optional.empty());
        when(stateRepository.findByCharacterId(1L)).thenReturn(Optional.of(state));
        when(agentSelfStateRepository.findByCharacterId(1L)).thenReturn(Optional.of(selfState));
        when(traitProfileService.findEntityOrDefault(1L)).thenReturn(traits);
        when(agentLifeEventService.ensureAndFindForPrompt(1L)).thenReturn(List.of());
        when(characterPreferenceService.plan(eq(1L), any()))
                .thenReturn(new PreferenceQuestionPlan("NONE", null, null, null, null, null));
        when(characterPreferenceService.findForPrompt(eq(1L), any())).thenReturn(List.of());
        when(conversationEventService.findRecentForPrompt(1L)).thenReturn(List.of());
        when(characterExampleService.findRelevantEntities(
                eq(1L),
                any(EventAnalysis.class),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of());
        when(memoryRetrievalService.retrieve(
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of());
        when(chatMessageRepository.findTop20ByCharacterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        ContextLoader loader = new ContextLoader(
                characterRepository,
                stateRepository,
                relationshipRepository,
                agentSelfStateRepository,
                memoryRetrievalService,
                chatMessageRepository,
                characterExampleService,
                traitProfileService,
                agentProfileService,
                agentWorldStateService,
                agentGoalService,
                agentInitiativeService,
                agentLifeEventService,
                conversationEventService,
                characterPreferenceService,
                conversationTopicService,
                new RelationshipStageResolver(settingsDefaultPolicy),
                new RelationshipTemperatureScoreResolver(settingsDefaultPolicy),
                settingsDefaultPolicy
        );

        return new Fixture(loader, characterRepository, relationshipRepository, traitProfileService, agentSelfStateRepository, characterExampleService);
    }

    private record Fixture(
            ContextLoader loader,
            CharacterRepository characterRepository,
            RelationshipRepository relationshipRepository,
            CharacterTraitProfileService traitProfileService,
            AgentSelfStateRepository agentSelfStateRepository,
            CharacterExampleService characterExampleService
    ) {
    }
}
