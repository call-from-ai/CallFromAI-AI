package com.example.aidatingagentbackend.dto;

import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CharacterTraitsResponse {

    private Long characterId;

    private Set<PersonalityKeyword> personalityKeywords;
    private List<PersonalityTraitSelection> selectedTraits;

    private CalculatedTraitsResponse calculatedTraits;

    private String calculationVersion;

    public static CharacterTraitsResponse from(CharacterTraitProfile profile) {
        CharacterTraitsResponse response = new CharacterTraitsResponse();
        response.setCharacterId(profile.getCharacterId());
        response.setPersonalityKeywords(profile.getPersonalityKeywords());
        response.setSelectedTraits(profile.getSelectedTraits() == null ? List.of() : profile.getSelectedTraits().stream()
                .map(item -> new PersonalityTraitSelection(item.getTrait(), item.getPriority())).toList());
        response.setCalculatedTraits(CalculatedTraitsResponse.from(profile));
        response.setCalculationVersion("TRAIT_RULE_V" + profile.getCalculationVersion());
        return response;
    }
}
