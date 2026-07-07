package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ReflectionResponse;
import com.example.aidatingagentbackend.service.ReflectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reflections")
public class ReflectionController {

    private final ReflectionService reflectionService;

    public ReflectionController(ReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    @GetMapping
    public ResponseEntity<List<ReflectionResponse>> findAll() {
        return ResponseEntity.ok(reflectionService.findAll()
                .stream()
                .map(ReflectionResponse::from)
                .toList());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ReflectionResponse>> findRelevantByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(reflectionService.findRelevantForPrompt(userId)
                .stream()
                .map(ReflectionResponse::from)
                .toList());
    }
}
