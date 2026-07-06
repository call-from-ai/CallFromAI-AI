package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.TurningPointResponse;
import com.example.aidatingagentbackend.service.TurningPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/turning-points")
public class TurningPointController {

    private final TurningPointService turningPointService;

    public TurningPointController(TurningPointService turningPointService) {
        this.turningPointService = turningPointService;
    }

    @GetMapping
    public ResponseEntity<List<TurningPointResponse>> findAll() {
        return ResponseEntity.ok(turningPointService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurningPointResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(turningPointService.findById(id));
    }
}
