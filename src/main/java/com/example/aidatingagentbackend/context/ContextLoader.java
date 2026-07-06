package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.ChatMessageRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import org.springframework.stereotype.Service;

@Service
public class ContextLoader {

    private final CharacterRepository characterRepository;
    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final MemoryRetrievalService memoryRetrievalService;
    private final TurningPointRepository turningPointRepository;
    private final ChatMessageRepository chatRepository;

    public ContextLoader(
            CharacterRepository characterRepository,
            StateRepository stateRepository,
            RelationshipRepository relationshipRepository,
            MemoryRetrievalService memoryRetrievalService,
            TurningPointRepository turningPointRepository,
            ChatMessageRepository chatRepository
    ) {
        this.characterRepository = characterRepository;
        this.stateRepository = stateRepository;
        this.relationshipRepository = relationshipRepository;
        this.memoryRetrievalService = memoryRetrievalService;
        this.turningPointRepository = turningPointRepository;
        this.chatRepository = chatRepository;
    }

    public Context load(Long characterId, String userMessage) {

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

                memoryRetrievalService.retrieve(userMessage, state),

                turningPointRepository.findTop10ByOrderByCreatedAtDesc(),

                chatRepository
                        .findTop20ByCharacterIdOrderByCreatedAtDesc(characterId)

        );

    }

}
