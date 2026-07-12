package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.dto.PreferenceQuestionPlan;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipStage;
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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextLoader {

    private final CharacterRepository characterRepository;
    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final MemoryRetrievalService memoryRetrievalService;
    private final ChatMessageRepository chatRepository;
    private final CharacterExampleService characterExampleService;
    private final CharacterTraitProfileService characterTraitProfileService;
    private final AgentProfileService agentProfileService;
    private final AgentWorldStateService agentWorldStateService;
    private final AgentGoalService agentGoalService;
    private final AgentInitiativeService agentInitiativeService;
    private final AgentLifeEventService agentLifeEventService;
    private final ConversationEventService conversationEventService;
    private final CharacterPreferenceService characterPreferenceService;
    private final ConversationTopicService conversationTopicService;
    private final RelationshipStageResolver relationshipStageResolver;
    private final RelationshipTemperatureScoreResolver relationshipTemperatureScoreResolver;
    private final SettingsDefaultPolicy settingsDefaultPolicy;

    public ContextLoader(
            CharacterRepository characterRepository,
            StateRepository stateRepository,
            RelationshipRepository relationshipRepository,
            AgentSelfStateRepository agentSelfStateRepository,
            MemoryRetrievalService memoryRetrievalService,
            ChatMessageRepository chatRepository,
            CharacterExampleService characterExampleService,
            CharacterTraitProfileService characterTraitProfileService,
            AgentProfileService agentProfileService,
            AgentWorldStateService agentWorldStateService,
            AgentGoalService agentGoalService,
            AgentInitiativeService agentInitiativeService,
            AgentLifeEventService agentLifeEventService,
            ConversationEventService conversationEventService,
            CharacterPreferenceService characterPreferenceService,
            ConversationTopicService conversationTopicService,
            RelationshipStageResolver relationshipStageResolver,
            RelationshipTemperatureScoreResolver relationshipTemperatureScoreResolver,
            SettingsDefaultPolicy settingsDefaultPolicy
    ) {
        this.characterRepository = characterRepository;
        this.stateRepository = stateRepository;
        this.relationshipRepository = relationshipRepository;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.memoryRetrievalService = memoryRetrievalService;
        this.chatRepository = chatRepository;
        this.characterExampleService = characterExampleService;
        this.characterTraitProfileService = characterTraitProfileService;
        this.agentProfileService = agentProfileService;
        this.agentWorldStateService = agentWorldStateService;
        this.agentGoalService = agentGoalService;
        this.agentInitiativeService = agentInitiativeService;
        this.agentLifeEventService = agentLifeEventService;
        this.conversationEventService = conversationEventService;
        this.characterPreferenceService = characterPreferenceService;
        this.conversationTopicService = conversationTopicService;
        this.relationshipStageResolver = relationshipStageResolver;
        this.relationshipTemperatureScoreResolver = relationshipTemperatureScoreResolver;
        this.settingsDefaultPolicy = settingsDefaultPolicy;
    }

    public Context load(Long characterId, String userMessage) {
        return load(characterId, userMessage, AgentEventType.NORMAL, RelationshipTemperature.NEUTRAL);
    }

    public Context load(
            Long characterId,
            String userMessage,
            AgentEventType eventType,
            RelationshipTemperature relationshipTemperature
    ) {
        return load(characterId, userMessage, EventAnalysis.fallback(eventType == null ? AgentEventType.NORMAL : eventType), relationshipTemperature);
    }

    public Context load(
            Long characterId,
            String userMessage,
            EventAnalysis eventAnalysis,
            RelationshipTemperature relationshipTemperature
    ) {
        EventAnalysis resolvedEventAnalysis = eventAnalysis == null
                ? EventAnalysis.fallback(AgentEventType.NORMAL)
                : eventAnalysis;
        AgentEventType resolvedEventType = resolvedEventAnalysis.eventType() == null
                ? AgentEventType.NORMAL
                : resolvedEventAnalysis.eventType();
        RelationshipTemperature resolvedTemperature = relationshipTemperature == null
                ? RelationshipTemperature.NEUTRAL
                : relationshipTemperature;

        Character character =
                characterRepository.findById(characterId)
                        .orElse(null);

        State state =
                stateRepository.findByCharacterId(characterId)
                        .orElseGet(() -> createDefaultState(characterId));

        Relationship relationship =
                relationshipRepository.findByCharacterId(characterId)
                        .orElseGet(() -> createDefaultRelationship(characterId));
        CharacterTraitProfile characterTraitProfile = characterTraitProfileService.findEntityOrDefault(characterId);
        RelationshipStage relationshipStage = relationshipStageResolver.resolve(relationship.getRelationshipStage());
        Integer relationshipTemperatureScore = relationshipTemperatureScoreResolver.resolveScore(
                relationship.getRelationshipTemperatureScore(),
                resolvedTemperature
        );

        AgentSelfState agentSelfState = agentSelfStateRepository.findByCharacterId(characterId)
                .orElse(null);
        AgentWorldState agentWorldState = agentWorldStateService.findByUserId(characterId);
        AgentGoal agentGoal = agentGoalService.findCurrentGoal(characterId);
        List<AgentLifeEvent> agentLifeEvents = agentLifeEventService.ensureAndFindForPrompt(characterId);
        PreferenceQuestionPlan preferenceQuestionPlan = characterPreferenceService.plan(characterId, userMessage);

        return new Context(

                character,

                state,

                relationship,

                characterTraitProfile,

                relationshipStage,

                relationshipTemperatureScore,

                agentSelfState,

                agentProfileService.findOrDefault(characterId),

                agentWorldState,

                agentGoal,

                agentInitiativeService.plan(userMessage, resolvedTemperature, agentSelfState, agentWorldState, agentGoal, relationship, agentLifeEvents),

                resolvedTemperature,

                agentLifeEvents,

                conversationEventService.findRecentForPrompt(characterId),

                preferenceQuestionPlan,

                conversationTopicService.plan(userMessage, preferenceQuestionPlan),

                characterPreferenceService.findForPrompt(characterId, preferenceQuestionPlan),

                characterExampleService.findRelevantEntities(
                        characterId,
                        resolvedEventAnalysis,
                        resolvedTemperature,
                        relationshipStage,
                        relationshipTemperatureScore,
                        characterTraitProfile
                ),

                preferenceQuestionPlan.active()
                        ? List.of()
                        : memoryRetrievalService.retrieve(
                                characterId,
                                userMessage,
                                state,
                                characterTraitProfile,
                                relationshipStage,
                                relationshipTemperatureScore,
                                resolvedEventAnalysis
                        ),

                chatRepository
                        .findTop20ByCharacterIdOrderByCreatedAtDesc(characterId)

        );

    }

    private State createDefaultState(Long characterId) {
        State state = new State();
        state.setCharacterId(characterId);
        state.setEmotion("neutral");
        state.setEmotionIntensity(0);
        state.setEnergy(50);
        state.setStress(20);
        return stateRepository.save(state);
    }

    private Relationship createDefaultRelationship(Long characterId) {
        Relationship relationship = new Relationship();
        relationship.setCharacterId(characterId);
        relationship.setTrust(50);
        relationship.setCloseness(30);
        relationship.setConflictLevel(0);
        relationship.setRepairProgress(0);
        relationship.setBreakupRisk(0);
        relationship.setRelationshipStage(settingsDefaultPolicy.defaultRelationshipStageValue());
        relationship.setDaysTogether(0);
        return relationship;
    }

}
