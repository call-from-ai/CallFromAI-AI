package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.AgentGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentGoalRepository extends JpaRepository<AgentGoal, Long> {

    Optional<AgentGoal> findTopByCharacterIdAndStatusOrderByPriorityDescCreatedAtDesc(Long characterId, String status);

    void deleteByCharacterId(Long characterId);
}
