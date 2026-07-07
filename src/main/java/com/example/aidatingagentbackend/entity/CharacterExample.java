package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(columnDefinition = "TEXT")
    private String userExample;

    @Column(columnDefinition = "TEXT")
    private String assistantExample;

    private String toneTag;

    private Integer priority;
}
