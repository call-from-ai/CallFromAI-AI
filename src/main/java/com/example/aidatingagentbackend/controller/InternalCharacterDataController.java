package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.service.CharacterDerivedDataService;
import com.example.aidatingagentbackend.service.CharacterSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/characters")
public class InternalCharacterDataController {

    private final CharacterDerivedDataService characterDerivedDataService;
    private final CharacterSnapshotService characterSnapshotService;

    public InternalCharacterDataController(CharacterDerivedDataService characterDerivedDataService,
                                           CharacterSnapshotService characterSnapshotService) {
        this.characterDerivedDataService = characterDerivedDataService;
        this.characterSnapshotService = characterSnapshotService;
    }

    @PutMapping("/{characterId}/snapshot")
    public ResponseEntity<Void> upsertSnapshot(@PathVariable Long characterId, @RequestBody CharacterSnapshot snapshot) {
        characterSnapshotService.upsert(characterId, snapshot);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{characterId}/data")
    public ResponseEntity<Void> deleteCharacterData(@PathVariable Long characterId) {
        characterDerivedDataService.deleteAllForCharacter(characterId);
        return ResponseEntity.noContent().build();
    }
}
