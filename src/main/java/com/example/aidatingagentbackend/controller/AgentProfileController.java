package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.AgentProfileRequest;
import com.example.aidatingagentbackend.dto.AgentProfileResponse;
import com.example.aidatingagentbackend.service.AgentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-profiles")
public class AgentProfileController {

    private final AgentProfileService agentProfileService;

    public AgentProfileController(AgentProfileService agentProfileService) {
        this.agentProfileService = agentProfileService;
    }

    @PostMapping
    public ResponseEntity<AgentProfileResponse> save(@RequestBody AgentProfileRequest request) {
        return ResponseEntity.ok(agentProfileService.save(request));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AgentProfileResponse> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(agentProfileService.findByUserId(userId));
    }
}
