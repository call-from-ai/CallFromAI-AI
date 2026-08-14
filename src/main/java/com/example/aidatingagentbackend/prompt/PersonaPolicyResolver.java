package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.entity.RelationshipStage;

import java.util.ArrayList;
import java.util.List;

public final class PersonaPolicyResolver {
    private final TraitInstructionResolver traitInstructionResolver;
    private final PersonaBehaviorSelector behaviorSelector = new PersonaBehaviorSelector();

    public PersonaPolicyResolver(TraitInstructionResolver traitInstructionResolver) {
        this.traitInstructionResolver = traitInstructionResolver;
    }

    public PersonaPolicy resolve(CharacterSnapshot character, CharacterTraitSnapshot traits, RelationshipStage stage,
                                 String userMessage) {
        if (character == null) return null;

        int activeIndex = behaviorSelector.selectIndex(character.keywords(), userMessage);
        String activeBehavior = activeIndex < 0 ? null
                : KeywordBehaviorResolver.resolve(List.of(character.keywords().get(activeIndex))).stream()
                .findFirst().orElse(null);
        List<String> supporting = new ArrayList<>();
        for (int i = 0; i < character.keywords().size() && supporting.size() < 1; i++) {
            if (i == activeIndex) continue;
            KeywordBehaviorResolver.resolve(List.of(character.keywords().get(i))).stream()
                    .findFirst().ifPresent(supporting::add);
        }

        CharacterTraitSnapshot resolvedTraits = traits == null ? character.traits() : traits;
        traitInstructionResolver.resolve(resolvedTraits, stage, userMessage).stream()
                .filter(instruction -> !supporting.contains(instruction))
                .limit(activeBehavior == null ? 2 : 1)
                .forEach(supporting::add);

        return new PersonaPolicy(character.name(), character.resolvedCorePersona(), character.mbti(),
                activeBehavior, supporting);
    }
}
