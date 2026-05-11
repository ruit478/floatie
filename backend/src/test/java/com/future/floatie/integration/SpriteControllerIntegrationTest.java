package com.future.floatie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.floatie.auth.request.AuthRequest;
import com.future.floatie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SpriteControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String registerAndGetToken(String username, String email) throws Exception {
        var request = new AuthRequest(username, email, "Password123!");
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/sprite/generate
    // -------------------------------------------------------------------------

    @Nested
    class GenerateSprite {

        @Test
        void happyPath_returnsPngImage() throws Exception {
            String token = registerAndGetToken("genuser", "gen@example.com");

            mockMvc.perform(post("/api/v1/pet/sprite/generate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE));
        }

        @Test
        void unauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/pet/sprite/generate"))
                    .andExpect(status().isForbidden());
        }
    }
}