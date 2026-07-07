package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.AgentGoalResponse;
import com.example.aidatingagentbackend.service.AgentGoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-goals")
public class AgentGoalController {

    private final AgentGoalService agentGoalService;

    public AgentGoalController(AgentGoalService agentGoalService) {
        this.agentGoalService = agentGoalService;
    }

    @PostMapping("/users/{userId}/select")
    public ResponseEntity<AgentGoalResponse> select(@PathVariable Long userId) {
        return ResponseEntity.ok(AgentGoalResponse.from(agentGoalService.selectCurrentGoal(userId)));
    }

    @GetMapping("/users/{userId}/current")
    public ResponseEntity<AgentGoalResponse> findCurrent(@PathVariable Long userId) {
        AgentGoalResponse response = agentGoalService.findCurrentGoalResponse(userId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }
}
