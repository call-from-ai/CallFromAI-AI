package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.PersonalityKeyword;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.SpeechStyle;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterServiceCreatePolicyTests {

    @Test
    void rejectsInvalidTraitCountsDuplicatesAndPrioritySequences() {
        CharacterService service = service();
        CharacterRequest empty = valid(); empty.setTraits(List.of());
        CharacterRequest six = valid(); six.setTraits(List.of(s(PersonalityKeyword.HUMOROUS,1),s(PersonalityKeyword.PLAYFUL,2),s(PersonalityKeyword.CUTE,3),s(PersonalityKeyword.SHY,4),s(PersonalityKeyword.SMOOTH,5),s(PersonalityKeyword.CLINGY,6)));
        CharacterRequest duplicateKeyword = valid(); duplicateKeyword.setTraits(List.of(s(PersonalityKeyword.HUMOROUS,1),s(PersonalityKeyword.HUMOROUS,2)));
        CharacterRequest duplicatePriority = valid(); duplicatePriority.setTraits(List.of(s(PersonalityKeyword.HUMOROUS,1),s(PersonalityKeyword.PLAYFUL,1)));
        CharacterRequest gap = valid(); gap.setTraits(List.of(s(PersonalityKeyword.HUMOROUS,1),s(PersonalityKeyword.PLAYFUL,3)));
        CharacterRequest startsAtTwo = valid(); startsAtTwo.setTraits(List.of(s(PersonalityKeyword.HUMOROUS,2)));
        CharacterRequest nullKeyword = valid(); nullKeyword.setTraits(List.of(s(null,1)));
        CharacterRequest nullPriority = valid(); nullPriority.setTraits(List.of(s(PersonalityKeyword.HUMOROUS,null)));
        for (CharacterRequest request : List.of(empty, six, duplicateKeyword, duplicatePriority, gap, startsAtTwo, nullKeyword, nullPriority)) {
            assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResponseStatusException.class)
                    .extracting(error -> ((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400);
        }
    }

    @Test
    void acceptsRomanceStyleBoundariesAndStoresSpeechIndependently() {
        CharacterService service = service();
        CharacterRequest zero = valid(); zero.setSpiceLevel(0);
        CharacterRequest hundred = valid(); hundred.setSpiceLevel(100); hundred.setSpeechStyle(SpeechStyle.FORMAL);
        assertThat(service.create(zero).getRomanceStyleScore()).isZero();
        assertThat(service.create(hundred).getRomanceStyleScore()).isEqualTo(100);
        assertThat(service.create(hundred).getSpeechStyle()).isEqualTo(SpeechStyle.FORMAL);
        CharacterRequest below = valid(); below.setSpiceLevel(-1);
        CharacterRequest above = valid(); above.setSpiceLevel(101);
        assertThatThrownBy(() -> service.create(below)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create(above)).isInstanceOf(ResponseStatusException.class);
    }

    private CharacterService service() {
        CharacterRepository characters = mock(CharacterRepository.class);
        when(characters.save(any(Character.class))).thenAnswer(invocation -> { Character c = invocation.getArgument(0); c.setId(1L); return c; });
        return new CharacterService(characters, mock(CharacterTraitProfileService.class),
                mock(AgentSelfStateRepository.class), mock(RelationshipRepository.class));
    }

    private CharacterRequest valid() {
        CharacterRequest request = new CharacterRequest();
        request.setName("민준"); request.setSpiceLevel(50); request.setSpeechStyle(SpeechStyle.CASUAL);
        request.setRelationshipStage(RelationshipStage.CRUSH);
        request.setTraits(new ArrayList<>(List.of(s(PersonalityKeyword.HUMOROUS, 1))));
        return request;
    }

    private PersonalityTraitSelection s(PersonalityKeyword keyword, Integer priority) {
        return new PersonalityTraitSelection(keyword, priority);
    }
}
