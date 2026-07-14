package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.service.CharacterDerivedDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/characters")
public class InternalCharacterDataController {

    private final CharacterDerivedDataService characterDerivedDataService;

    public InternalCharacterDataController(CharacterDerivedDataService characterDerivedDataService) {
        this.characterDerivedDataService = characterDerivedDataService;
    }

    @DeleteMapping("/{characterId}/data")
    public ResponseEntity<Void> deleteCharacterData(@PathVariable Long characterId) {
        characterDerivedDataService.deleteAllForCharacter(characterId);
        return ResponseEntity.noContent().build();
    }
}
