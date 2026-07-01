package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.RelationshipRequest;
import com.example.aidatingagentbackend.dto.RelationshipResponse;
import com.example.aidatingagentbackend.service.RelationshipService;
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
@RequestMapping("/api/relationships")
public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @PostMapping
    public ResponseEntity<RelationshipResponse> create(@RequestBody RelationshipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(relationshipService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<RelationshipResponse>> findAll() {
        return ResponseEntity.ok(relationshipService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RelationshipResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(relationshipService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RelationshipResponse> update(@PathVariable Long id, @RequestBody RelationshipRequest request) {
        return ResponseEntity.ok(relationshipService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        relationshipService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
