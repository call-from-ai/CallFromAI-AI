package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;

import java.util.List;

public record Context(

        Character character,

        State state,

        Relationship relationship,

        List<Memory> memories,

        List<ChatMessage> history

) {
}