package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.AgentWorldStateResponse;
import com.example.aidatingagentbackend.service.AgentWorldStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-world-states")
public class AgentWorldStateController {

    private final AgentWorldStateService agentWorldStateService;

    public AgentWorldStateController(AgentWorldStateService agentWorldStateService) {
        this.agentWorldStateService = agentWorldStateService;
    }

    @PostMapping("/users/{userId}/refresh")
    public ResponseEntity<AgentWorldStateResponse> refresh(@PathVariable Long userId) {
        return ResponseEntity.ok(AgentWorldStateResponse.from(agentWorldStateService.updateBeforeResponse(userId)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AgentWorldStateResponse> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(agentWorldStateService.findResponseByUserId(userId));
    }
}
