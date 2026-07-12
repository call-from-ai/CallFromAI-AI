package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.entity.*;
import com.example.aidatingagentbackend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@Import(CharacterService.class)
class CharacterPolicyPersistenceTests {
    @Autowired CharacterService characterService;
    @Autowired CharacterRepository characters;
    @Autowired CharacterTraitProfileRepository profiles;
    @Autowired RelationshipRepository relationships;
    @Autowired AgentSelfStateRepository selfStates;
    @MockBean CharacterTraitProfileService traitProfileService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void traitProfileFailureRollsBackAllCharacterCreationRows() {
        when(traitProfileService.saveForCharacter(anyLong(), anyList(), any()))
                .thenThrow(new DataIntegrityViolationException("forced"));
        assertThatThrownBy(() -> characterService.create(validRequest()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(characters.count()).isZero();
        assertThat(profiles.count()).isZero();
        assertThat(relationships.count()).isZero();
        assertThat(selfStates.count()).isZero();
    }

    private CharacterRequest validRequest() {
        CharacterRequest request = new CharacterRequest();
        request.setName("rollback"); request.setSpiceLevel(90); request.setMbti(Mbti.ENFJ);
        request.setSpeechStyle(SpeechStyle.CASUAL); request.setRelationshipStage(RelationshipStage.CRUSH);
        request.setTraits(List.of(new PersonalityTraitSelection(PersonalityKeyword.HUMOROUS, 1)));
        return request;
    }
}
