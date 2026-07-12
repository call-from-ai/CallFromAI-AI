package com.example.aidatingagentbackend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "character_trait_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CharacterTraitProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long characterId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "character_trait_profile_keywords",
            joinColumns = @JoinColumn(name = "character_trait_profile_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "personality_keyword")
    private Set<PersonalityKeyword> personalityKeywords = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_trait_selections", joinColumns = @JoinColumn(name = "character_trait_profile_id"))
    private List<SelectedPersonalityTrait> selectedTraits = new ArrayList<>();

    private Integer humor;

    private Integer playfulness;

    private Integer affection;

    private Integer empathy;

    private Integer attachment;

    private Integer jealousy;

    private Integer dominance;

    private Integer confidence;

    private Integer expressiveness;

    private Integer emotionalStability;

    private Integer calculationVersion;

    private LocalDateTime calculatedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
