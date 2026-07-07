package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.AgentWorldState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentWorldStateRepository extends JpaRepository<AgentWorldState, Long> {

    Optional<AgentWorldState> findByUserId(Long userId);
}
