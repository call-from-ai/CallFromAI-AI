package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.config.InternalApiAuthFilter;
import com.example.aidatingagentbackend.dto.ConversationSummaryResponse;
import com.example.aidatingagentbackend.service.ConversationSummaryService;
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

class ConversationSummaryControllerTests {

    private ConversationSummaryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ConversationSummaryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ConversationSummaryController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new InternalApiAuthFilter("secret", new ObjectMapper().findAndRegisterModules()))
                .build();
    }

    @Test
    void returnsSummaryForAuthenticatedInternalRequest() throws Exception {
        when(service.summarize(any())).thenReturn(new ConversationSummaryResponse("요약 결과"));

        mvc.perform(post("/internal/conversations/summary")
                        .header("X-Internal-Api-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("요약 결과"));
    }

    @Test
    void rejectsRequestWithoutInternalToken() throws Exception {
        mvc.perform(post("/internal/conversations/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized());
    }

    private String validPayload() {
        return """
                {
                  "relationshipId": 1,
                  "previousSummary": null,
                  "messages": [
                    {"role": "user", "content": "나는 커피보다 아이스티가 좋아"},
                    {"role": "assistant", "content": "저녁에도 아이스티를 자주 마셔?"}
                  ],
                  "maxCharacters": 200
                }
                """;
    }
}
