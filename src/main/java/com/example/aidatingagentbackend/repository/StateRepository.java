package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {

    Optional<State> findByCharacterId(Long characterId);
}
