package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.AgentSelfStateLogResponse;
import com.example.aidatingagentbackend.service.AgentSelfStateLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-self-state-logs")
public class AgentSelfStateLogController {

    private final AgentSelfStateLogService agentSelfStateLogService;

    public AgentSelfStateLogController(AgentSelfStateLogService agentSelfStateLogService) {
        this.agentSelfStateLogService = agentSelfStateLogService;
    }

    @GetMapping
    public ResponseEntity<List<AgentSelfStateLogResponse>> findAll() {
        return ResponseEntity.ok(agentSelfStateLogService.findAll());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AgentSelfStateLogResponse>> findRecentByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(agentSelfStateLogService.findRecentByUserId(userId));
    }
}
