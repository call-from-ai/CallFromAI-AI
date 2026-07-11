package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.entity.CharacterPreference;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import com.example.aidatingagentbackend.entity.AgentProfile;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.ConversationEvent;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.State;

import java.util.List;

public record Context(

        Character character,

        State state,

        Relationship relationship,

        AgentSelfState agentSelfState,

        AgentProfile agentProfile,

        AgentWorldState agentWorldState,

        AgentGoal agentGoal,

        AgentInitiative agentInitiative,

        RelationshipTemperature relationshipTemperature,

        List<AgentLifeEvent> agentLifeEvents,

        List<ConversationEvent> conversationEvents,

        PreferenceQuestionPlan preferenceQuestionPlan,

        ConversationTopicPlan conversationTopicPlan,

        List<CharacterPreference> characterPreferences,

        List<CharacterExample> characterExamples,

        List<Memory> memories,

        List<ChatMessage> history

) {
}
