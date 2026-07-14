package com.example.aidatingagentbackend.config;

import com.example.aidatingagentbackend.controller.ChatController;
import com.example.aidatingagentbackend.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalApiAuthFilterTests {

    private static final String TOKEN = "internal-test-token";

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(mock(ChatService.class)))
                .addFilters(new InternalApiAuthFilter(TOKEN))
                .build();
    }

    @Test
    void rejectsRequestWithoutCredentials() throws Exception {
        mvc.perform(post("/chat").contentType(MediaType.APPLICATION_JSON).content(validPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void acceptsBearerToken() throws Exception {
        mvc.perform(post("/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk());
    }

    @Test
    void acceptsInternalApiKey() throws Exception {
        mvc.perform(post("/chat")
                        .header("X-Internal-Api-Key", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk());
    }

    private String validPayload() {
        return "{\"requestId\":\"r1\",\"message\":\"hi\"," +
                "\"character\":{\"characterId\":10,\"name\":\"hana\",\"romanceStyleScore\":50,\"traits\":" + traits() + "}," +
                "\"relationship\":{\"relationshipId\":20,\"relationshipStage\":\"DATING\",\"relationshipTemperatureScore\":35," +
                "\"trust\":50,\"closeness\":50,\"conflictLevel\":20,\"repairProgress\":20,\"breakupRisk\":20,\"daysTogether\":30,\"strategy\":\"NORMAL\"}}";
    }

    private String traits() {
        return "{\"humor\":5,\"playfulness\":5,\"affection\":5,\"empathy\":5,\"attachment\":5,\"jealousy\":5," +
                "\"dominance\":5,\"confidence\":5,\"expressiveness\":5,\"emotionalStability\":5}";
    }
}
