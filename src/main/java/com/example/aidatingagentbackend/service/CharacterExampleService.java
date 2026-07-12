package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.dto.RelationshipStrategy;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.engine.EventAnalysis;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.repository.CharacterExampleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CharacterExampleService {
    private final CharacterExampleRepository repository;
    private final CharacterExampleReranker reranker;

    public CharacterExampleService(CharacterExampleRepository repository, CharacterExampleReranker reranker) {
        this.repository = repository; this.reranker = reranker;
    }

    @Transactional(readOnly = true)
    public List<CharacterExample> findRelevantEntities(Long characterId, EventAnalysis analysis,
            RelationshipStrategy strategy, RelationshipStage stage, Integer relationshipTemperatureScore,
            Integer romanceStyleScore, CharacterTraitSnapshot traits) {
        EventAnalysis resolved = analysis == null ? EventAnalysis.fallback(AgentEventType.NORMAL) : analysis;
        List<CharacterExample> result = reranker.rerank(repository.findCandidateStyleExamples(characterId, resolved.eventType()),
                resolved, stage, relationshipTemperatureScore, romanceStyleScore, traits);
        if (!result.isEmpty()) return result;
        RelationshipTemperature legacy = strategy == RelationshipStrategy.CONFLICT_REPAIR
                ? RelationshipTemperature.CONFLICT_REPAIR : RelationshipTemperature.NEUTRAL;
        return repository.findRelevantStyleExamples(characterId, resolved.eventType(), legacy, PageRequest.of(0, 5));
    }
}
