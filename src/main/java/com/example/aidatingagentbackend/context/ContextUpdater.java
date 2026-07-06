package com.example.aidatingagentbackend.context;

import com.example.aidatingagentbackend.engine.EmotionEngine;
import com.example.aidatingagentbackend.engine.MemoryEngine;
import com.example.aidatingagentbackend.engine.RelationshipEngine;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.repository.StateRepository;
import org.springframework.stereotype.Service;

@Service
public class ContextUpdater {

    private final EmotionEngine emotionEngine;
    private final RelationshipEngine relationshipEngine;
    private final MemoryEngine memoryEngine;

    private final StateRepository stateRepository;
    private final RelationshipRepository relationshipRepository;
    private final MemoryRepository memoryRepository;


        public ContextUpdater(
                EmotionEngine emotionEngine,
                RelationshipEngine relationshipEngine,
                MemoryEngine memoryEngine,
                StateRepository stateRepository,
                RelationshipRepository relationshipRepository,
                MemoryRepository memoryRepository
        ) {
            this.emotionEngine = emotionEngine;
            this.relationshipEngine = relationshipEngine;
            this.memoryEngine = memoryEngine;
            this.stateRepository = stateRepository;
            this.relationshipRepository = relationshipRepository;
            this.memoryRepository = memoryRepository;
        }


    public void updateBeforeResponse(String userMessage){

        State state =
                stateRepository.findTopByOrderByIdDesc()
                        .orElseGet(State::new);

        Relationship relationship =
                relationshipRepository.findTopByOrderByIdDesc()
                        .orElseGet(Relationship::new);

        var emotion =
                emotionEngine.analyze(state,userMessage);

        var relation =
                relationshipEngine.analyze(relationship,userMessage);

        state.setEmotion(emotion.emotion());
        state.setEmotionIntensity(emotion.emotionIntensity());

        relationship.setTrust(relation.trust());
        relationship.setCloseness(relation.closeness());

        stateRepository.save(state);
        relationshipRepository.save(relationship);
    }

    public void updateMemoryAfterResponse(String userMessage,String reply){

        State state =
                stateRepository.findTopByOrderByIdDesc()
                        .orElseGet(State::new);

        var decision =
                memoryEngine.analyze(
                        userMessage+"\n"+reply,
                        state.getEmotion(),
                        state.getEmotionIntensity());

        if(Boolean.TRUE.equals(decision.shouldCreate())){

            Memory memory=new Memory();

            memory.setType(decision.memoryType());
            memory.setSummary(decision.episodeSummary());
            memory.setImportance(decision.importance());

            memoryRepository.save(memory);
        }

    }

}
