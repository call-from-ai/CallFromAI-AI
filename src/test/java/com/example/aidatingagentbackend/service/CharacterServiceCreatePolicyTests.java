package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.dto.CharacterTraitsRequest;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.Mbti;
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
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

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

    @Test
    void traitUpdatePreservesNewPriorityAndUsesStoredCharacterMbti() {
        CharacterRepository characters = mock(CharacterRepository.class);
        Character character = new Character(); character.setId(7L); character.setMbti(Mbti.ENFJ);
        when(characters.findById(7L)).thenReturn(java.util.Optional.of(character));
        CharacterTraitProfileService profiles = mock(CharacterTraitProfileService.class);
        CharacterTraitProfile profile = new CharacterTraitProfile(); profile.setCharacterId(7L);
        when(profiles.saveForCharacter(eq(7L), any(), eq(Mbti.ENFJ))).thenReturn(profile);
        CharacterService service = new CharacterService(characters, profiles,
                mock(AgentSelfStateRepository.class), mock(RelationshipRepository.class));
        CharacterTraitsRequest request = new CharacterTraitsRequest();
        request.setTraits(List.of(s(PersonalityKeyword.PLAYFUL, 1), s(PersonalityKeyword.HUMOROUS, 2)));

        service.updateTraits(7L, request);

        verify(profiles).saveForCharacter(eq(7L), argThat(items ->
                items.size() == 2
                        && items.get(0).getTrait() == PersonalityKeyword.PLAYFUL && items.get(0).getPriority() == 1
                        && items.get(1).getTrait() == PersonalityKeyword.HUMOROUS && items.get(1).getPriority() == 2),
                eq(Mbti.ENFJ));
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
