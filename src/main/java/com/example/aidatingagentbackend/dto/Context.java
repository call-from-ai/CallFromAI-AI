package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.*;
import java.util.List;

public record Context(
        CharacterSnapshot character,
        State state,
        RelationshipSnapshot relationship,
        RelationshipDelta relationshipDelta,
        CharacterTraitSnapshot characterTraitProfile,
        RelationshipStage relationshipStage,
        Integer relationshipTemperatureScore,
        Integer romanceStyleScore,
        AgentSelfState agentSelfState,
        AgentWorldState agentWorldState,
        AgentGoal agentGoal,
        AgentInitiative agentInitiative,
        RelationshipStrategy relationshipStrategy,
        List<AgentLifeEvent> agentLifeEvents,
        List<ConversationEvent> conversationEvents,
        PreferenceQuestionPlan preferenceQuestionPlan,
        ConversationTopicPlan conversationTopicPlan,
        List<CharacterPreference> characterPreferences,
        List<CharacterExample> characterExamples,
        List<Memory> memories,
        List<ChatHistoryItem> history
) {}
