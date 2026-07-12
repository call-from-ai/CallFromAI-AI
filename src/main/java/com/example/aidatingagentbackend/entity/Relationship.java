package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "relationships", uniqueConstraints =
        @UniqueConstraint(name = "uk_relationship_character", columnNames = "character_id"))
@Getter
@Setter
@NoArgsConstructor
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    private Integer trust;

    private Integer closeness;

    private Integer conflictLevel;

    private Integer repairProgress;

    private Integer breakupRisk;

    private String relationshipStage;

    private Integer relationshipTemperatureScore;

    private Integer daysTogether;
}
