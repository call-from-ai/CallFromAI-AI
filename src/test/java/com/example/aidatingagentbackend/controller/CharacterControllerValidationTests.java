package com.example.aidatingagentbackend.controller;

import com.example.aidatingagentbackend.repository.AgentSelfStateRepository;
import com.example.aidatingagentbackend.repository.CharacterRepository;
import com.example.aidatingagentbackend.repository.RelationshipRepository;
import com.example.aidatingagentbackend.service.CharacterService;
import com.example.aidatingagentbackend.service.CharacterTraitProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CharacterControllerValidationTests {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CharacterTraitProfileService profiles = mock(CharacterTraitProfileService.class);
        CharacterService service = new CharacterService(mock(CharacterRepository.class), profiles,
                mock(AgentSelfStateRepository.class), mock(RelationshipRepository.class));
        mvc = MockMvcBuilders.standaloneSetup(new CharacterController(service, profiles))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void invalidRelationshipStageAndSpeechStyleReturnCommon400() throws Exception {
        mvc.perform(post("/api/characters").contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("INVALID", "반말")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/characters").contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("CRUSH", "INVALID")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void priorityGapAndDuplicateReturnCommon400() throws Exception {
        mvc.perform(put("/api/characters/1/traits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"traits\":[{\"trait\":\"HUMOROUS\",\"priority\":1},{\"trait\":\"PLAYFUL\",\"priority\":3}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(put("/api/characters/1/traits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"traits\":[{\"trait\":\"HUMOROUS\",\"priority\":1},{\"trait\":\"PLAYFUL\",\"priority\":1}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    private String createJson(String stage, String speech) {
        return "{\"name\":\"test\",\"spiceLevel\":90,\"relationshipStage\":\"" + stage
                + "\",\"speechStyle\":\"" + speech
                + "\",\"traits\":[{\"trait\":\"HUMOROUS\",\"priority\":1}]}";
    }
}
