package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.CharacterTraitsRequest;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.domain.CharacterTrait;
import com.example.aidatingagentbackend.entity.*;
import com.example.aidatingagentbackend.repository.CharacterTraitProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@Import({CharacterService.class, CharacterTraitProfileService.class, PersonalityTraitResolver.class})
class CharacterTraitsUpdatePersistenceTests {
    @Autowired CharacterService characterService;
    @Autowired CharacterTraitProfileRepository profiles;

    @Test
    void traitUpdateKeepsOneProfileAndReplacesPrioritiesUsingCharacterMbti() {
        CharacterRequest create = new CharacterRequest();
        create.setName("update"); create.setSpiceLevel(90); create.setMbti(Mbti.ENFJ);
        create.setSpeechStyle(SpeechStyle.CASUAL); create.setRelationshipStage(RelationshipStage.CRUSH);
        create.setTraits(List.of(new PersonalityTraitSelection(PersonalityKeyword.HUMOROUS, 1)));
        Long id = characterService.create(create).getId();

        CharacterTraitsRequest update = new CharacterTraitsRequest();
        update.setTraits(List.of(
                new PersonalityTraitSelection(PersonalityKeyword.SHY, 2),
                new PersonalityTraitSelection(PersonalityKeyword.EXPRESSIVE, 1)));
        characterService.updateTraits(id, update);

        CharacterTraitProfile profile = profiles.findByCharacterId(id).orElseThrow();
        assertThat(profiles.count()).isEqualTo(1);
        assertThat(profile.getSelectedTraits()).extracting(SelectedPersonalityTrait::getPriority)
                .containsExactly(1, 2);
        CharacterTrait expected = new PersonalityTraitResolver().resolve(update.getTraits().stream()
                .sorted(java.util.Comparator.comparing(PersonalityTraitSelection::getPriority)).toList(), Mbti.ENFJ);
        assertThat(profile.getExpressiveness()).isEqualTo(expected.expressiveness());
    }
}
