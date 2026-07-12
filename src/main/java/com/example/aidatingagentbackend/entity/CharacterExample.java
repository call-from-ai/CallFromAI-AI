package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.example.aidatingagentbackend.engine.AgentEventType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "character_examples")
@Getter
@Setter
@NoArgsConstructor
public class CharacterExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long characterId;

    @Enumerated(EnumType.STRING)
    private AgentEventType eventType;

    @Enumerated(EnumType.STRING)
    private RelationshipTemperature relationshipTemperature;

    private String relationshipStage;

    private Integer minTemperatureScore;

    private Integer maxTemperatureScore;

    @Enumerated(EnumType.STRING)
    private RomanceStyleBand romanceStyleBand;

    @Column(columnDefinition = "TEXT")
    private String userExample;

    @Column(columnDefinition = "TEXT")
    private String assistantExample;

    private String toneTag;

    private Integer priority;

    private Boolean active;
}
