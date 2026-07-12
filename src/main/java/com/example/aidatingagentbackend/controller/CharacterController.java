package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.CharacterRequest;
import com.example.aidatingagentbackend.dto.CharacterResponse;
import com.example.aidatingagentbackend.dto.CharacterTraitsRequest;
import com.example.aidatingagentbackend.dto.CharacterTraitsResponse;
import com.example.aidatingagentbackend.service.CharacterService;
import com.example.aidatingagentbackend.service.CharacterTraitProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;
    private final CharacterTraitProfileService characterTraitProfileService;

    public CharacterController(
            CharacterService characterService,
            CharacterTraitProfileService characterTraitProfileService
    ) {
        this.characterService = characterService;
        this.characterTraitProfileService = characterTraitProfileService;
    }

    @PostMapping
    public ResponseEntity<CharacterResponse> create(@RequestBody CharacterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(characterService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CharacterResponse>> findAll() {
        return ResponseEntity.ok(characterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(characterService.findById(id));
    }

    @PutMapping("/{id}/traits")
    public ResponseEntity<CharacterTraitsResponse> updateTraits(
            @PathVariable Long id,
            @RequestBody CharacterTraitsRequest request
    ) {
        return ResponseEntity.ok(characterService.updateTraits(id, request));
    }

    @GetMapping("/{id}/traits")
    public ResponseEntity<CharacterTraitsResponse> findTraits(@PathVariable Long id) {
        return ResponseEntity.ok(characterTraitProfileService.findTraitsByCharacterId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CharacterResponse> update(@PathVariable Long id, @RequestBody CharacterRequest request) {
        return ResponseEntity.ok(characterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        characterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
