package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.MemoryRequest;
import com.example.aidatingagentbackend.dto.MemoryResponse;
import com.example.aidatingagentbackend.memory.MemoryService;
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
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping
    public ResponseEntity<MemoryResponse> create(@RequestBody MemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<MemoryResponse>> findAll() {
        return ResponseEntity.ok(memoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(memoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryResponse> update(@PathVariable Long id, @RequestBody MemoryRequest request) {
        return ResponseEntity.ok(memoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
