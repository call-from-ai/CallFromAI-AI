package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.CharacterExampleRequest;
import com.example.aidatingagentbackend.dto.CharacterExampleResponse;
import com.example.aidatingagentbackend.engine.AgentEventType;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.service.CharacterExampleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/character-examples")
public class CharacterExampleController {

    private final CharacterExampleService characterExampleService;

    public CharacterExampleController(CharacterExampleService characterExampleService) {
        this.characterExampleService = characterExampleService;
    }

    @PostMapping
    public ResponseEntity<CharacterExampleResponse> create(@RequestBody CharacterExampleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(characterExampleService.create(request));
    }

    @GetMapping("/characters/{characterId}")
    public ResponseEntity<List<CharacterExampleResponse>> findByCharacterId(@PathVariable Long characterId) {
        return ResponseEntity.ok(characterExampleService.findByCharacterId(characterId));
    }

    @GetMapping("/characters/{characterId}/relevant")
    public ResponseEntity<List<CharacterExampleResponse>> findRelevant(
            @PathVariable Long characterId,
            @RequestParam(defaultValue = "NORMAL") AgentEventType eventType,
            @RequestParam(defaultValue = "NEUTRAL") RelationshipTemperature relationshipTemperature
    ) {
        return ResponseEntity.ok(characterExampleService.findRelevant(characterId, eventType, relationshipTemperature));
    }

    @PostMapping("/characters/{characterId}/samples")
    public ResponseEntity<List<CharacterExampleResponse>> createSamples(@PathVariable Long characterId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(characterExampleService.createStyleSamples(characterId));
    }
}
