package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.CharacterResponse;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.dto.PersonalityTraitSelection;
import com.example.aidatingagentbackend.entity.AgentLifeType;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final CharacterTraitProfileService characterTraitProfileService;
    private final AgentSelfStateRepository agentSelfStateRepository;
    private final RelationshipRepository relationshipRepository;

    public CharacterService(CharacterRepository characterRepository,
                            CharacterTraitProfileService characterTraitProfileService,
                            AgentSelfStateRepository agentSelfStateRepository,
                            RelationshipRepository relationshipRepository) {
        this.characterRepository = characterRepository;
        this.characterTraitProfileService = characterTraitProfileService;
        this.agentSelfStateRepository = agentSelfStateRepository;
        this.relationshipRepository = relationshipRepository;
    }

    @Transactional
    public CharacterResponse create(CharacterRequest request) {
        List<PersonalityTraitSelection> selections = validateAndSort(request);
        Character character = new Character();
        applyRequest(character, request);
        character = characterRepository.save(character);
        characterTraitProfileService.saveForCharacter(character.getId(), selections, request.getMbti());
        initializeRelationship(character);
        initializeSelfState(character.getId());
        return CharacterResponse.from(character);
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> findAll() {
        return characterRepository.findAll()
                .stream()
                .map(CharacterResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponse findById(Long id) {
        return CharacterResponse.from(findCharacter(id));
    }

    @Transactional
    public CharacterResponse update(Long id, CharacterRequest request) {
        Character character = findCharacter(id);
        applyRequest(character, request);
        return CharacterResponse.from(characterRepository.save(character));
    }

    @Transactional
    public void delete(Long id) {
        Character character = findCharacter(id);
        characterRepository.delete(character);
    }

    private Character findCharacter(Long id) {
        return characterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found. id=" + id));
    }

    private void applyRequest(Character character, CharacterRequest request) {
        character.setName(request.getName());
        character.setMind(request.getMind());
        character.setValues(request.getValues());
        character.setHabit(request.getHabit());
        character.setResponseStyle(request.getResponseStyle());
        character.setGender(request.getGender());
        character.setAge(request.getAge());
        character.setJob(request.getJob());
        character.setLifeType(request.getLifeType() == null ? inferLifeType(request.getJob()) : request.getLifeType());
        character.setRomanceStyleScore(request.getSpiceLevel());
        character.setMbti(request.getMbti());
        character.setSpeechStyle(request.getSpeechStyle());
        character.setRelationshipStage(request.getRelationshipStage());
    }

    private List<PersonalityTraitSelection> validateAndSort(CharacterRequest request) {
        if (request == null) badRequest("request is required");
        if (request.getSpiceLevel() == null || request.getSpiceLevel() < 0 || request.getSpiceLevel() > 100) {
            badRequest("spiceLevel must be between 0 and 100");
        }
        if (request.getRelationshipStage() == null) badRequest("relationshipStage is required");
        if (request.getSpeechStyle() == null) badRequest("speechStyle is required");
        List<PersonalityTraitSelection> traits = request.getTraits();
        if (traits == null || traits.isEmpty() || traits.size() > 5) badRequest("traits must contain between 1 and 5 items");
        Set<Object> keywords = new HashSet<>();
        Set<Integer> priorities = new HashSet<>();
        for (PersonalityTraitSelection item : traits) {
            if (item == null || item.getTrait() == null || item.getPriority() == null) badRequest("trait and priority are required");
            if (!keywords.add(item.getTrait())) badRequest("duplicate trait is not allowed");
            if (!priorities.add(item.getPriority())) badRequest("duplicate priority is not allowed");
        }
        for (int expected = 1; expected <= traits.size(); expected++) {
            if (!priorities.contains(expected)) badRequest("priorities must be consecutive from 1");
        }
        return traits.stream().sorted(java.util.Comparator.comparing(PersonalityTraitSelection::getPriority)).toList();
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private AgentLifeType inferLifeType(String job) {
        if (job == null) return AgentLifeType.FLEXIBLE;
        if (job.contains("학생")) return AgentLifeType.STUDENT;
        if (job.contains("직장") || job.contains("회사")) return AgentLifeType.WORKER;
        return AgentLifeType.FLEXIBLE;
    }

    private void initializeRelationship(Character character) {
        Relationship relationship = new Relationship();
        relationship.setCharacterId(character.getId());
        relationship.setRelationshipStage(character.getRelationshipStage().name());
        relationship.setTrust(50); relationship.setCloseness(30); relationship.setConflictLevel(0);
        relationship.setRepairProgress(0); relationship.setBreakupRisk(0); relationship.setDaysTogether(0);
        relationshipRepository.save(relationship);
    }

    private void initializeSelfState(Long characterId) {
        AgentSelfState state = new AgentSelfState();
        state.setCharacterId(characterId); state.setAffection(.55); state.setTrust(.6);
        state.setHurt(0.0); state.setAnger(0.0); state.setInsecurity(.15);
        state.setDisappointment(0.0); state.setEmotionalDistance(.15);
        state.setLastEmotion("calm"); state.setLastSignificantEvent("none"); state.setUpdatedAt(LocalDateTime.now());
        agentSelfStateRepository.save(state);
    }
}
