package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    List<Memory> findByCharacterId(Long characterId);

    List<Memory> findByCharacterIdAndTypeNot(Long characterId, com.example.aidatingagentbackend.entity.MemoryType type);

    List<Memory> findByTypeNot(com.example.aidatingagentbackend.entity.MemoryType type);

    List<Memory> findTop5ByCharacterIdAndTypeOrderByOccurredAtDescIdDesc(
            Long characterId,
            com.example.aidatingagentbackend.entity.MemoryType type
    );

    Optional<Memory> findByRequestId(String requestId);

    void deleteByCharacterId(Long characterId);
}
