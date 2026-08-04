package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.config.InternalApiAuthFilter;
import com.example.aidatingagentbackend.dto.CallTopicResponse;
import com.example.aidatingagentbackend.service.CallTopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CallTopicControllerTests {

    private CallTopicService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CallTopicService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CallTopicController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new InternalApiAuthFilter("secret", new ObjectMapper().findAndRegisterModules()))
                .build();
    }

    @Test
    void returnsTopicForAuthenticatedInternalRequest() throws Exception {
        when(service.createTopic(any())).thenReturn(new CallTopicResponse("퇴근 후 일상 이야기"));

        mvc.perform(post("/internal/calls/topic")
                        .header("X-Internal-Api-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("퇴근 후 일상 이야기"));
    }

    @Test
    void rejectsRequestWithoutInternalToken() throws Exception {
        mvc.perform(post("/internal/calls/topic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized());
    }

    private String validPayload() {
        return """
                {
                  "callId": 123,
                  "messages": [
                    {"role": "user", "content": "오늘 퇴근하고 뭐 했어?"},
                    {"role": "assistant", "content": "집에서 좀 쉬었어. 너는?"}
                  ],
                  "maxCharacters": 20
                }
                """;
    }
}
