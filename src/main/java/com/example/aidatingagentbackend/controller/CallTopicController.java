package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.CallTopicRequest;
import com.example.aidatingagentbackend.dto.CallTopicResponse;
import com.example.aidatingagentbackend.service.CallTopicService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/calls")
public class CallTopicController {

    private final CallTopicService callTopicService;

    public CallTopicController(CallTopicService callTopicService) {
        this.callTopicService = callTopicService;
    }

    @PostMapping("/topic")
    public CallTopicResponse createTopic(@RequestBody CallTopicRequest request) {
        return callTopicService.createTopic(request);
    }
}
