package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ReflectionCandidateResponse;
import com.example.aidatingagentbackend.service.ReflectionCandidateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reflection-candidates")
public class ReflectionCandidateController {

    private final ReflectionCandidateService reflectionCandidateService;

    public ReflectionCandidateController(ReflectionCandidateService reflectionCandidateService) {
        this.reflectionCandidateService = reflectionCandidateService;
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ReflectionCandidateResponse>> findRecentByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(reflectionCandidateService.findRecentByUserId(userId)
                .stream()
                .map(ReflectionCandidateResponse::from)
                .toList());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ReflectionCandidateResponse>> findPending() {
        return ResponseEntity.ok(reflectionCandidateService.findPending()
                .stream()
                .map(ReflectionCandidateResponse::from)
                .toList());
    }
}
