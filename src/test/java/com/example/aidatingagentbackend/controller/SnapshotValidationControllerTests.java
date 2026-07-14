package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.service.ChatService;
import com.example.aidatingagentbackend.service.ProactiveChatService;
import com.example.aidatingagentbackend.config.RequestIdCaptureFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SnapshotValidationControllerTests {
    private MockMvc mvc;
    private MockMvc proactiveMvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(mock(ChatService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdCaptureFilter(new ObjectMapper()))
                .build();
        proactiveMvc = MockMvcBuilders.standaloneSetup(new ProactiveChatController(mock(ProactiveChatService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidRelationshipStageReturnsExistingErrorResponseShape() throws Exception {
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(payload("UNKNOWN", traits())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/chat"))
                .andExpect(jsonPath("$.requestId").value("r1"));
    }

    @Test
    void missingTraitReturns400() throws Exception {
        String traits = "{\"playfulness\":5,\"affection\":5,\"empathy\":5,\"attachment\":5,\"jealousy\":5,\"dominance\":5,\"confidence\":5,\"expressiveness\":5,\"emotionalStability\":5}";
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(payload("DATING", traits)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingChatMessageReturns400() throws Exception {
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(snapshotPayload(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("message is required"));
    }

    @Test
    void proactiveAcceptsUnifiedRequestWithoutMessage() throws Exception {
        proactiveMvc.perform(post("/api/chat/proactive/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(snapshotPayload(null)))
                .andExpect(status().isOk());
    }

    @Test
    void chatAcceptsRelationshipWithoutClosenessAndConflictLevel() throws Exception {
        String payload = snapshotPayload("hi")
                .replace("\"closeness\":50,", "")
                .replace("\"conflictLevel\":20,", "");
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }

    private String payload(String stage, String traits) {
        return "{\"requestId\":\"r1\",\"message\":\"hi\"," +
                "\"character\":{\"characterId\":10,\"name\":\"hana\",\"romanceStyleScore\":90,\"traits\":" + traits + "}," +
                "\"relationship\":{\"relationshipId\":20,\"relationshipStage\":\"" + stage + "\",\"relationshipTemperatureScore\":35," +
                "\"trust\":50,\"closeness\":50,\"conflictLevel\":20,\"repairProgress\":20,\"breakupRisk\":20,\"daysTogether\":30,\"strategy\":\"NORMAL\"}}";
    }

    private String traits() {
        return "{\"humor\":5,\"playfulness\":5,\"affection\":5,\"empathy\":5,\"attachment\":5,\"jealousy\":5,\"dominance\":5,\"confidence\":5,\"expressiveness\":5,\"emotionalStability\":5}";
    }

    private String snapshotPayload(String message) {
        String messageJson = message == null ? "" : ",\"message\":\"" + message + "\"";
        return "{\"requestId\":\"r1\"" + messageJson + "," +
                "\"character\":{\"characterId\":10,\"name\":\"hana\",\"romanceStyleScore\":90,\"traits\":" + traits() + "}," +
                "\"relationship\":{\"relationshipId\":20,\"relationshipStage\":\"DATING\",\"relationshipTemperatureScore\":35," +
                "\"trust\":50,\"closeness\":50,\"conflictLevel\":20,\"repairProgress\":20,\"breakupRisk\":20,\"daysTogether\":30,\"strategy\":\"NORMAL\"}}";
    }
}
