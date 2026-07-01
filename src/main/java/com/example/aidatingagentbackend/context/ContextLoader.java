package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import org.springframework.stereotype.Service;

@Service
public class ContextLoader {

    private final CharacterRepository characterRepository;
    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final MemoryRepository memoryRepository;
    private final ChatMessageRepository chatRepository;

    public ContextLoader(
            CharacterRepository characterRepository,
            StateRepository stateRepository,
            RelationshipRepository relationshipRepository,
            MemoryRepository memoryRepository,
            ChatMessageRepository chatRepository
    ) {
        this.characterRepository = characterRepository;
        this.stateRepository = stateRepository;
        this.relationshipRepository = relationshipRepository;
        this.memoryRepository = memoryRepository;
        this.chatRepository = chatRepository;
    }

    public Context load(Long characterId) {

        Character character =
                characterRepository.findById(characterId)
                        .orElse(null);

        State state =
                stateRepository.findTopByOrderByIdDesc()
                        .orElse(new State());

        Relationship relationship =
                relationshipRepository.findTopByOrderByIdDesc()
                        .orElse(new Relationship());

        return new Context(

                character,

                state,

                relationship,

                memoryRepository.findAll(),

                chatRepository
                        .findTop20ByCharacterIdOrderByCreatedAtDesc(characterId)

        );

    }

}