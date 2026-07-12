package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CharacterTraitProfileRequest {

    private Long characterId;

    private Set<PersonalityKeyword> personalityKeywords;
}
