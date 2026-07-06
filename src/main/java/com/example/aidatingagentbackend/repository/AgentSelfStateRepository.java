package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.AgentSelfState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentSelfStateRepository extends JpaRepository<AgentSelfState, Long> {

    Optional<AgentSelfState> findByCharacterId(Long characterId);
}
