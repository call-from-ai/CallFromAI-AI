package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.CharacterExample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterExampleRepository extends JpaRepository<CharacterExample, Long> {

    List<CharacterExample> findTop5ByCharacterIdOrderByPriorityDescIdAsc(Long characterId);
}
