package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ResponseQualityEvaluationResponse;
import com.example.aidatingagentbackend.service.ResponseQualityEvaluatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/response-quality-evaluations")
public class ResponseQualityEvaluationController {

    private final ResponseQualityEvaluatorService responseQualityEvaluatorService;

    public ResponseQualityEvaluationController(ResponseQualityEvaluatorService responseQualityEvaluatorService) {
        this.responseQualityEvaluatorService = responseQualityEvaluatorService;
    }

    @GetMapping
    public ResponseEntity<List<ResponseQualityEvaluationResponse>> findAll() {
        return ResponseEntity.ok(responseQualityEvaluatorService.findAll()
                .stream()
                .map(ResponseQualityEvaluationResponse::from)
                .toList());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ResponseQualityEvaluationResponse>> findRecentByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(responseQualityEvaluatorService.findRecentByUserId(userId)
                .stream()
                .map(ResponseQualityEvaluationResponse::from)
                .toList());
    }
}
