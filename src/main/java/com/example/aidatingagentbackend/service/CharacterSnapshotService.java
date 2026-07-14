package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.entity.CharacterSnapshotEntity;
import com.example.aidatingagentbackend.repository.CharacterSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterSnapshotService {
    private final CharacterSnapshotRepository repository;

    public CharacterSnapshotService(CharacterSnapshotRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void upsert(Long pathCharacterId, CharacterSnapshot snapshot) {
        if (pathCharacterId == null || !pathCharacterId.equals(snapshot.characterId())) {
            throw new IllegalArgumentException("path characterId must match body characterId");
        }
        if (snapshot.traits().calculationVersion() == null) {
            throw new IllegalArgumentException("character.traits.calculationVersion is required");
        }
        var existing = repository.findByCharacterId(pathCharacterId);
        CharacterSnapshotEntity entity = existing.orElseGet(() -> new CharacterSnapshotEntity(snapshot));
        if (existing.isEmpty() || snapshot.traits().calculationVersion() >= entity.getCalculationVersion()) {
            entity.updateFrom(snapshot);
            repository.save(entity);
        }
    }
}
