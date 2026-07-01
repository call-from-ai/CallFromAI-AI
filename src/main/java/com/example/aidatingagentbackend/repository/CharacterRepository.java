package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<Character, Long> {
}
