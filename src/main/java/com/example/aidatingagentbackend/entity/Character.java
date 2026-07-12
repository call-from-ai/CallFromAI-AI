package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String mind;

    @Column(name = "character_values", columnDefinition = "TEXT")
    private String values;

    @Column(columnDefinition = "TEXT")
    private String habit;

    @Column(columnDefinition = "TEXT")
    private String responseStyle;

    private String gender;
    private Integer age;
    private String job;
    @Enumerated(EnumType.STRING)
    private AgentLifeType lifeType;
    private Integer romanceStyleScore;
    @Enumerated(EnumType.STRING)
    private Mbti mbti;
    @Enumerated(EnumType.STRING)
    private SpeechStyle speechStyle;
    @Enumerated(EnumType.STRING)
    private RelationshipStage relationshipStage;
}
