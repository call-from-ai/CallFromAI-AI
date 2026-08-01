package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.dto.ConversationSummaryRequest;
import com.example.aidatingagentbackend.dto.ConversationSummaryResponse;
import com.example.aidatingagentbackend.service.ConversationSummaryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/conversations")
public class ConversationSummaryController {

    private final ConversationSummaryService conversationSummaryService;

    public ConversationSummaryController(ConversationSummaryService conversationSummaryService) {
        this.conversationSummaryService = conversationSummaryService;
    }

    @PostMapping("/summary")
    public ConversationSummaryResponse summarize(@RequestBody ConversationSummaryRequest request) {
        return conversationSummaryService.summarize(request);
    }
}
