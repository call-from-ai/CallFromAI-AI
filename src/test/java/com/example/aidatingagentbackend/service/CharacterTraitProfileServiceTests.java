package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterTraitProfileRequest;
import com.example.aidatingagentbackend.dto.CharacterTraitProfileResponse;
import com.example.aidatingagentbackend.dto.CharacterTraitsResponse;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import com.example.aidatingagentbackend.repository.CharacterTraitProfileRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CharacterTraitProfileServiceTests {

    @Test
    void characterTraitProfileIsSavedByCharacterId() {
        CharacterTraitProfileRepository repository = mock(CharacterTraitProfileRepository.class);
        when(repository.findByCharacterId(12L)).thenReturn(Optional.empty());
        when(repository.save(any(CharacterTraitProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CharacterTraitProfileService service = new CharacterTraitProfileService(
                repository,
                new PersonalityTraitResolver()
        );

        CharacterTraitProfileRequest request = new CharacterTraitProfileRequest();
        request.setCharacterId(12L);
        request.setPersonalityKeywords(Set.of(PersonalityKeyword.CLINGY));

        CharacterTraitProfileResponse response = service.save(request);

        assertThat(response.getCharacterId()).isEqualTo(12L);
        assertThat(response.getAttachment()).isEqualTo(10);
        assertThat(response.getEmotionalStability()).isEqualTo(0);
        verify(repository).findByCharacterId(12L);
        verify(repository).save(any(CharacterTraitProfile.class));
    }

    @Test
    void duplicateKeywordsAreRemovedWhenSavedThroughCharacterSettingsApiService() {
        CharacterTraitProfileRepository repository = mock(CharacterTraitProfileRepository.class);
        when(repository.findByCharacterId(12L)).thenReturn(Optional.empty());
        when(repository.save(any(CharacterTraitProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CharacterTraitProfileService service = new CharacterTraitProfileService(
                repository,
                new PersonalityTraitResolver()
        );
        LinkedHashSet<PersonalityKeyword> keywords = new LinkedHashSet<>();
        keywords.add(PersonalityKeyword.PLAYFUL);
        keywords.add(PersonalityKeyword.PLAYFUL);
        keywords.add(PersonalityKeyword.SMOOTH);

        CharacterTraitsResponse response = service.saveForCharacter(12L, keywords);

        assertThat(response.getPersonalityKeywords()).containsExactly(PersonalityKeyword.PLAYFUL, PersonalityKeyword.SMOOTH);
        assertThat(response.getCalculatedTraits().getPlayfulness()).isEqualTo(10);
        assertThat(response.getCalculatedTraits().getConfidence()).isEqualTo(10);
    }

    @Test
    void keywordUpdateRecalculatesTraits() {
        CharacterTraitProfile existing = new CharacterTraitProfile();
        existing.setCharacterId(12L);
        CharacterTraitProfileRepository repository = mock(CharacterTraitProfileRepository.class);
        when(repository.findByCharacterId(12L)).thenReturn(Optional.of(existing));
        when(repository.save(any(CharacterTraitProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CharacterTraitProfileService service = new CharacterTraitProfileService(
                repository,
                new PersonalityTraitResolver()
        );

        CharacterTraitsResponse response = service.saveForCharacter(12L, Set.of(PersonalityKeyword.GOOD_LISTENER));

        assertThat(response.getCalculatedTraits().getEmpathy()).isEqualTo(10);
        assertThat(response.getCalculatedTraits().getAttachment()).isEqualTo(3);
    }

    @Test
    void missingCharacterTraitProfileReturnsDefaultSettings() {
        CharacterTraitProfileRepository repository = mock(CharacterTraitProfileRepository.class);
        when(repository.findByCharacterId(12L)).thenReturn(Optional.empty());
        CharacterTraitProfileService service = new CharacterTraitProfileService(
                repository,
                new PersonalityTraitResolver()
        );

        CharacterTraitsResponse response = service.findTraitsByCharacterId(12L);

        assertThat(response.getCharacterId()).isEqualTo(12L);
        assertThat(response.getPersonalityKeywords()).isEmpty();
        assertThat(response.getCalculatedTraits().getHumor()).isEqualTo(3);
        assertThat(response.getCalculationVersion()).isEqualTo("TRAIT_RULE_V2");
    }

    @Test
    void characterTraitProfileDoesNotStoreRelationshipStageOrTemperature() {
        Set<String> fieldNames = Set.of(fieldNames(CharacterTraitProfile.class));

        assertThat(fieldNames).doesNotContain("relationshipStage");
        assertThat(fieldNames).doesNotContain("relationshipTemperatureScore");
    }

    private String[] fieldNames(Class<?> type) {
        Field[] fields = type.getDeclaredFields();
        String[] names = new String[fields.length];
        for (int index = 0; index < fields.length; index++) {
            names[index] = fields[index].getName();
        }
        return names;
    }
}
