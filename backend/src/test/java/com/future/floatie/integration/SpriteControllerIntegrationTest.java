package com.future.floatie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.floatie.auth.request.AuthRequest;
import com.future.floatie.entity.User;
import com.future.floatie.integration.IntegrationTestBase;
import com.future.floatie.pet.service.PetService;
import com.future.floatie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SpriteControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PetService petService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Registers a user via the API and returns their JWT. */
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
    // GET /api/v1/pet/sprite/info
    // -------------------------------------------------------------------------

    @Nested
    class PetInfo {

        @Test
        void happyPath_returnsFullPetPayload() throws Exception {
            String token = registerAndGetToken("spriteuser", "sprite@example.com");

            mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").isNotEmpty())
                    .andExpect(jsonPath("$.subclass").isNotEmpty())
                    .andExpect(jsonPath("$.colorHex").isNotEmpty())
                    .andExpect(jsonPath("$.expression").isNotEmpty())
                    .andExpect(jsonPath("$.lifeStage").value("EGG"))
                    .andExpect(jsonPath("$.level").value(1))
                    .andExpect(jsonPath("$.xp").value(0));
        }

        @Test
        void happyPath_spriteBase64IsPresent() throws Exception {
            String token = registerAndGetToken("base64user", "base64@example.com");

            mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    // Base64 payload must be a non-empty string — PNG is always > 0 bytes
                    .andExpect(jsonPath("$.spriteBase64").isNotEmpty());
        }

        @Test
        void statsAreWithinExpectedRange() throws Exception {
            String token = registerAndGetToken("rangeuser", "range@example.com");

            // All core stats must be between 0 and 100 inclusive
            mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hunger").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                            org.hamcrest.Matchers.lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.happiness").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                            org.hamcrest.Matchers.lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.energy").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                            org.hamcrest.Matchers.lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.health").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                            org.hamcrest.Matchers.lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.hygiene").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                            org.hamcrest.Matchers.lessThanOrEqualTo(100))));
        }

        @Test
        void unauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/pet/sprite/info"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void invalidToken_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer not.a.real.token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void eachUserSeesOnlyTheirOwnPet() throws Exception {
            String tokenA = registerAndGetToken("usera", "usera@example.com");
            String tokenB = registerAndGetToken("userb", "userb@example.com");

            String bodyA = mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String bodyB = mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String idA = objectMapper.readTree(bodyA).get("id").asText();
            String idB = objectMapper.readTree(bodyB).get("id").asText();

            org.assertj.core.api.Assertions.assertThat(idA).isNotEqualTo(idB);
        }
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