package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentProfile;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.Reflection;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.entity.TurningPoint;

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

        List<CharacterExample> characterExamples,

        List<Memory> memories,

        List<Reflection> reflections,

        List<TurningPoint> turningPoints,

        List<ChatMessage> history

) {
}
