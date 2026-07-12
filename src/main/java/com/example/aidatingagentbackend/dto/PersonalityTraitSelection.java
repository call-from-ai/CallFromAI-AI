package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonalityTraitSelection {
    private PersonalityKeyword trait;
    private Integer priority;

    public PersonalityTraitSelection(PersonalityKeyword trait, Integer priority) {
        this.trait = trait;
        this.priority = priority;
    }
}
