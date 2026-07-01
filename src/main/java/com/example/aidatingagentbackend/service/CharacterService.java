package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.CharacterResponse;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;

    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    @Transactional
    public CharacterResponse create(CharacterRequest request) {
        Character character = new Character();
        applyRequest(character, request);
        return CharacterResponse.from(characterRepository.save(character));
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
    }
}
