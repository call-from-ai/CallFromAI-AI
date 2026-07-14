package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.AgentSelfStateLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentSelfStateLogRepository extends JpaRepository<AgentSelfStateLog, Long> {
    List<AgentSelfStateLog> findByCharacterIdOrderByCreatedAtDesc(Long characterId);

    void deleteByCharacterId(Long characterId);
}
