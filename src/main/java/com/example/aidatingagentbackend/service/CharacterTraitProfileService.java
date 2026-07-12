package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.domain.CharacterTrait;
import com.example.aidatingagentbackend.dto.CharacterTraitProfileRequest;
import com.example.aidatingagentbackend.dto.CharacterTraitProfileResponse;
import com.example.aidatingagentbackend.dto.CharacterTraitsResponse;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.entity.Mbti;
import com.example.aidatingagentbackend.entity.SelectedPersonalityTrait;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import com.example.aidatingagentbackend.repository.CharacterTraitProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;

@Service
public class CharacterTraitProfileService {

    private static final int CALCULATION_VERSION = 2;

    private final CharacterTraitProfileRepository characterTraitProfileRepository;
    private final PersonalityTraitResolver personalityTraitResolver;

    public CharacterTraitProfileService(
            CharacterTraitProfileRepository characterTraitProfileRepository,
            PersonalityTraitResolver personalityTraitResolver
    ) {
        this.characterTraitProfileRepository = characterTraitProfileRepository;
        this.personalityTraitResolver = personalityTraitResolver;
    }

    @Transactional
    public CharacterTraitProfileResponse save(CharacterTraitProfileRequest request) {
        CharacterTraitProfile profile = characterTraitProfileRepository.findByCharacterId(request.getCharacterId())
                .orElseGet(CharacterTraitProfile::new);
        profile.setCharacterId(request.getCharacterId());
        applyKeywordsAndTraits(profile, request.getPersonalityKeywords());
        return CharacterTraitProfileResponse.from(characterTraitProfileRepository.save(profile));
    }

    @Transactional
    public CharacterTraitsResponse saveForCharacter(Long characterId, Set<PersonalityKeyword> personalityKeywords) {
        CharacterTraitProfile profile = characterTraitProfileRepository.findByCharacterId(characterId)
                .orElseGet(CharacterTraitProfile::new);
        profile.setCharacterId(characterId);
        applyKeywordsAndTraits(profile, personalityKeywords);
        return CharacterTraitsResponse.from(characterTraitProfileRepository.save(profile));
    }

    public CharacterTraitProfile saveForCharacter(
            Long characterId, List<PersonalityTraitSelection> selections, Mbti mbti
    ) {
        CharacterTraitProfile profile = characterTraitProfileRepository.findByCharacterId(characterId)
                .orElseGet(CharacterTraitProfile::new);
        profile.setCharacterId(characterId);
        List<PersonalityTraitSelection> ordered = selections == null ? List.of() : selections;
        profile.setPersonalityKeywords(ordered.stream().map(PersonalityTraitSelection::getTrait)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        profile.setSelectedTraits(ordered.stream()
                .map(item -> new SelectedPersonalityTrait(item.getTrait(), item.getPriority())).toList());
        applyCalculatedTraits(profile, personalityTraitResolver.resolve(ordered, mbti));
        return characterTraitProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public CharacterTraitProfileResponse findByCharacterId(Long characterId) {
        CharacterTraitProfile profile = characterTraitProfileRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultProfile(characterId));
        return CharacterTraitProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public CharacterTraitsResponse findTraitsByCharacterId(Long characterId) {
        return CharacterTraitsResponse.from(findEntityOrDefault(characterId));
    }

    @Transactional(readOnly = true)
    public CharacterTraitProfile findEntityOrDefault(Long characterId) {
        return characterTraitProfileRepository.findByCharacterId(characterId)
                .orElseGet(() -> createDefaultProfile(characterId));
    }

    private CharacterTraitProfile createDefaultProfile(Long characterId) {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setCharacterId(characterId);
        applyKeywordsAndTraits(profile, Set.of());
        return profile;
    }

    private void applyKeywordsAndTraits(
            CharacterTraitProfile profile,
            Set<PersonalityKeyword> personalityKeywords
    ) {
        Set<PersonalityKeyword> keywords = personalityKeywords == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(personalityKeywords);
        profile.setPersonalityKeywords(keywords);

        CharacterTrait trait = personalityTraitResolver.resolve(keywords);
        profile.setSelectedTraits(new java.util.ArrayList<>());
        applyCalculatedTraits(profile, trait);
    }

    private void applyCalculatedTraits(CharacterTraitProfile profile, CharacterTrait trait) {
        profile.setHumor(trait.humor());
        profile.setPlayfulness(trait.playfulness());
        profile.setAffection(trait.affection());
        profile.setEmpathy(trait.empathy());
        profile.setAttachment(trait.attachment());
        profile.setJealousy(trait.jealousy());
        profile.setDominance(trait.dominance());
        profile.setConfidence(trait.confidence());
        profile.setExpressiveness(trait.expressiveness());
        profile.setEmotionalStability(trait.emotionalStability());
        profile.setCalculationVersion(CALCULATION_VERSION);
        profile.setCalculatedAt(LocalDateTime.now());
    }
}
