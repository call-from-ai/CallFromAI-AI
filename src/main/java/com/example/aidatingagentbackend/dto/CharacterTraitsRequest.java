package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CharacterTraitsRequest {

    private Set<PersonalityKeyword> personalityKeywords;
}
