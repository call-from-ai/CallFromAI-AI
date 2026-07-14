package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.CharacterSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterSnapshotRepository extends JpaRepository<CharacterSnapshotEntity, Long> {
    Optional<CharacterSnapshotEntity> findByCharacterId(Long characterId);
    long countByCharacterId(Long characterId);
    void deleteByCharacterId(Long characterId);
}
