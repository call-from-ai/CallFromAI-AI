package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.AgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    Optional<AgentProfile> findByUserId(Long userId);
}
