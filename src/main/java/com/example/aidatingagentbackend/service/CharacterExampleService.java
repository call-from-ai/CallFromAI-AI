package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.CharacterExampleRequest;
import com.example.aidatingagentbackend.dto.CharacterExampleResponse;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.repository.CharacterExampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CharacterExampleService {

    private final CharacterExampleRepository characterExampleRepository;

    public CharacterExampleService(CharacterExampleRepository characterExampleRepository) {
        this.characterExampleRepository = characterExampleRepository;
    }

    @Transactional
    public CharacterExampleResponse create(CharacterExampleRequest request) {
        CharacterExample example = new CharacterExample();
        example.setCharacterId(request.getCharacterId());
        example.setUserExample(request.getUserExample());
        example.setAssistantExample(request.getAssistantExample());
        example.setToneTag(request.getToneTag());
        example.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        return CharacterExampleResponse.from(characterExampleRepository.save(example));
    }

    @Transactional(readOnly = true)
    public List<CharacterExampleResponse> findByCharacterId(Long characterId) {
        return characterExampleRepository.findTop5ByCharacterIdOrderByPriorityDescIdAsc(characterId)
                .stream()
                .map(CharacterExampleResponse::from)
                .toList();
    }
}
