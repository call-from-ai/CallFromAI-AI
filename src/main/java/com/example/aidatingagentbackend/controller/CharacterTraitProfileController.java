package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.CharacterTraitProfileRequest;
import com.example.aidatingagentbackend.dto.CharacterTraitProfileResponse;
import com.example.aidatingagentbackend.service.CharacterTraitProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/character-trait-profiles")
public class CharacterTraitProfileController {

    private final CharacterTraitProfileService characterTraitProfileService;

    public CharacterTraitProfileController(CharacterTraitProfileService characterTraitProfileService) {
        this.characterTraitProfileService = characterTraitProfileService;
    }

    @PostMapping
    public ResponseEntity<CharacterTraitProfileResponse> save(@RequestBody CharacterTraitProfileRequest request) {
        return ResponseEntity.ok(characterTraitProfileService.save(request));
    }

    @GetMapping("/characters/{characterId}")
    public ResponseEntity<CharacterTraitProfileResponse> findByCharacterId(@PathVariable Long characterId) {
        return ResponseEntity.ok(characterTraitProfileService.findByCharacterId(characterId));
    }
}
