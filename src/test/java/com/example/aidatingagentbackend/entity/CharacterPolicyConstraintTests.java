package com.example.aidatingagentbackend.entity;

import com.example.aidatingagentbackend.repository.CharacterTraitProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
class CharacterPolicyConstraintTests {
    @Autowired CharacterTraitProfileRepository repository;

    @Test
    void duplicateCharacterProfileFails() {
        repository.saveAndFlush(profile(1L, List.of()));
        assertThatThrownBy(() -> repository.saveAndFlush(profile(1L, List.of())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePriorityFails() {
        CharacterTraitProfile profile = profile(2L, List.of(
                new SelectedPersonalityTrait(PersonalityKeyword.HUMOROUS, 1),
                new SelectedPersonalityTrait(PersonalityKeyword.PLAYFUL, 1)));
        assertThatThrownBy(() -> repository.saveAndFlush(profile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateKeywordFails() {
        CharacterTraitProfile profile = profile(3L, List.of(
                new SelectedPersonalityTrait(PersonalityKeyword.HUMOROUS, 1),
                new SelectedPersonalityTrait(PersonalityKeyword.HUMOROUS, 2)));
        assertThatThrownBy(() -> repository.saveAndFlush(profile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CharacterTraitProfile profile(Long characterId, List<SelectedPersonalityTrait> selected) {
        CharacterTraitProfile profile = new CharacterTraitProfile();
        profile.setCharacterId(characterId);
        profile.setSelectedTraits(selected);
        return profile;
    }
}
