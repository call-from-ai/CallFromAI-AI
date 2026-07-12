package com.example.aidatingagentbackend.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SelectedPersonalityTrait {
    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(name = "trait", nullable = false)
    private PersonalityKeyword trait;
    @jakarta.persistence.Column(name = "priority", nullable = false)
    private Integer priority;

    public SelectedPersonalityTrait(PersonalityKeyword trait, Integer priority) {
        this.trait = trait;
        this.priority = priority;
    }
}
