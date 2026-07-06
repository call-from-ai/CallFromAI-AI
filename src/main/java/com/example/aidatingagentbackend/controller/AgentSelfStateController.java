package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.AgentSelfStateResponse;
import com.example.aidatingagentbackend.service.AgentSelfStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-self-states")
public class AgentSelfStateController {

    private final AgentSelfStateService agentSelfStateService;

    public AgentSelfStateController(AgentSelfStateService agentSelfStateService) {
        this.agentSelfStateService = agentSelfStateService;
    }

    @GetMapping
    public ResponseEntity<List<AgentSelfStateResponse>> findAll() {
        return ResponseEntity.ok(agentSelfStateService.findAll());
    }

    @GetMapping("/characters/{characterId}")
    public ResponseEntity<AgentSelfStateResponse> findByCharacterId(@PathVariable Long characterId) {
        return ResponseEntity.ok(agentSelfStateService.findByCharacterId(characterId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agentSelfStateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
