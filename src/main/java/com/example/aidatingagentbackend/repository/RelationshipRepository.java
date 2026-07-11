package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    Optional<Relationship> findByCharacterId(Long characterId);
}
