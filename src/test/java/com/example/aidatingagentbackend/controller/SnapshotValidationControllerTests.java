package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.service.ChatService;
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

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(mock(ChatService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidRelationshipStageReturnsExistingErrorResponseShape() throws Exception {
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(payload("UNKNOWN", traits())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/chat"));
    }

    @Test
    void missingTraitReturns400() throws Exception {
        String traits = "{\"playfulness\":5,\"affection\":5,\"empathy\":5,\"attachment\":5,\"jealousy\":5,\"dominance\":5,\"confidence\":5,\"expressiveness\":5,\"emotionalStability\":5}";
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(payload("DATING", traits)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String payload(String stage, String traits) {
        return "{\"requestId\":\"r1\",\"memberId\":1,\"message\":\"hi\"," +
                "\"character\":{\"characterId\":10,\"name\":\"hana\",\"romanceStyleScore\":90,\"traits\":" + traits + "}," +
                "\"relationship\":{\"relationshipId\":20,\"relationshipStage\":\"" + stage + "\",\"relationshipTemperatureScore\":35," +
                "\"trust\":50,\"closeness\":50,\"conflictLevel\":20,\"repairProgress\":20,\"breakupRisk\":20,\"daysTogether\":30,\"strategy\":\"NORMAL\"}}";
    }

    private String traits() {
        return "{\"humor\":5,\"playfulness\":5,\"affection\":5,\"empathy\":5,\"attachment\":5,\"jealousy\":5,\"dominance\":5,\"confidence\":5,\"expressiveness\":5,\"emotionalStability\":5}";
    }
}
