package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CharacterExampleRepository extends JpaRepository<CharacterExample, Long> {

    List<CharacterExample> findTop5ByCharacterIdOrderByPriorityDescIdAsc(Long characterId);

    @Query("""
            select example
            from CharacterExample example
            where example.characterId = :characterId
              and (example.eventType = :eventType or example.eventType is null)
              and (example.relationshipTemperature = :relationshipTemperature or example.relationshipTemperature is null)
            order by
              case when example.eventType = :eventType then 0 else 1 end,
              case when example.relationshipTemperature = :relationshipTemperature then 0 else 1 end,
              example.priority desc,
              example.id asc
            """)
    List<CharacterExample> findRelevantStyleExamples(
            @Param("characterId") Long characterId,
            @Param("eventType") AgentEventType eventType,
            @Param("relationshipTemperature") RelationshipTemperature relationshipTemperature,
            Pageable pageable
    );
}
