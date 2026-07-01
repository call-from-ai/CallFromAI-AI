package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.StateRequest;
import com.example.aidatingagentbackend.dto.StateResponse;
import com.example.aidatingagentbackend.service.StateService;
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
@RequestMapping("/api/states")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping
    public ResponseEntity<StateResponse> create(@RequestBody StateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stateService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<StateResponse>> findAll() {
        return ResponseEntity.ok(stateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StateResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(stateService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StateResponse> update(@PathVariable Long id, @RequestBody StateRequest request) {
        return ResponseEntity.ok(stateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
