package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CharacterTraitsRequest {

    private List<PersonalityTraitSelection> traits;

    /** @deprecated use traits with explicit priority */
    @Deprecated
    private Set<PersonalityKeyword> personalityKeywords;
}
