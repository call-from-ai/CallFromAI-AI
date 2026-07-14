package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.CharacterPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterPreferenceRepository extends JpaRepository<CharacterPreference, Long> {

    Optional<CharacterPreference> findByCharacterIdAndPreferenceKey(Long characterId, String preferenceKey);

    List<CharacterPreference> findTop12ByCharacterIdOrderByUpdatedAtDescIdDesc(Long characterId);

    void deleteByCharacterId(Long characterId);
}
