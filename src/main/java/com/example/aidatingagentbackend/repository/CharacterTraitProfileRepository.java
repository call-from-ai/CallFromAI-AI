package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterTraitProfileRepository extends JpaRepository<CharacterTraitProfile, Long> {

    Optional<CharacterTraitProfile> findByCharacterId(Long characterId);
}
