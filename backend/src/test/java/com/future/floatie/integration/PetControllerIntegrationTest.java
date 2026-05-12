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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class PetControllerIntegrationTest extends IntegrationTestBase {

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
    // GET /api/v1/pet/info
    // -------------------------------------------------------------------------

    @Nested
    class GetInfo {

        @Test
        void shouldReturnFullPetPayloadWhenAuthenticated() throws Exception {
            String token = registerAndGetToken("infouser", "info@example.com");

            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.name").isNotEmpty())
                    .andExpect(jsonPath("$.subclass").isNotEmpty())
                    .andExpect(jsonPath("$.colorHex").isNotEmpty())
                    .andExpect(jsonPath("$.expression").isNotEmpty())
                    .andExpect(jsonPath("$.lifeStage").value("EGG"))
                    .andExpect(jsonPath("$.level").value(1))
                    .andExpect(jsonPath("$.xp").value(0))
                    .andExpect(jsonPath("$.evolutionStage").value(1))
                    .andExpect(jsonPath("$.isAsleep").value(false))
                    .andExpect(jsonPath("$.bondLevel").value(0));
        }

        @Test
        void shouldIncludeSpriteBase64WhenFetchingPetInfo() throws Exception {
            String token = registerAndGetToken("spriteuser", "sprite@example.com");

            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.spriteBase64").isNotEmpty());
        }

        @Test
        void shouldReturnStatsWithinRangeWhenPetIsNew() throws Exception {
            String token = registerAndGetToken("rangeuser", "range@example.com");

            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hunger").value(allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.happiness").value(allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.energy").value(allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.health").value(allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                    .andExpect(jsonPath("$.hygiene").value(allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/pet/info"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnForbiddenWhenTokenIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer not.a.real.token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnOnlyOwnPetWhenMultipleUsersExist() throws Exception {
            String tokenA = registerAndGetToken("usera", "usera@example.com");
            String tokenB = registerAndGetToken("userb", "userb@example.com");

            String bodyA = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String bodyB = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String idA = objectMapper.readTree(bodyA).get("id").asText();
            String idB = objectMapper.readTree(bodyB).get("id").asText();

            org.assertj.core.api.Assertions.assertThat(idA).isNotEqualTo(idB);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pet/status
    // -------------------------------------------------------------------------

    @Nested
    class GetStatus {

        @Test
        void shouldReturnOkWhenPetExists() throws Exception {
            String token = registerAndGetToken("statususer", "status@example.com");

            mockMvc.perform(get("/api/v1/pet/status")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/pet/status"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/feed
    // -------------------------------------------------------------------------

    @Nested
    class Feed {

        @Test
        void shouldIncreaseHungerWhenFeeding() throws Exception {
            String token = registerAndGetToken("feeduser", "feed@example.com");

            String before = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            int hungerBefore = objectMapper.readTree(before).get("hunger").asInt();

            mockMvc.perform(post("/api/v1/pet/feed")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hunger").value(greaterThanOrEqualTo(Math.min(100, hungerBefore))));
        }

        @Test
        void shouldAwardXpWhenFeeding() throws Exception {
            String token = registerAndGetToken("feedxpuser", "feedxp@example.com");

            mockMvc.perform(post("/api/v1/pet/feed")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.xp").value(greaterThan(0)));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/feed"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/play
    // -------------------------------------------------------------------------

    @Nested
    class Play {

        @Test
        void shouldIncreaseHappinessWhenPlaying() throws Exception {
            String token = registerAndGetToken("playuser", "play@example.com");

            String before = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            int happinessBefore = objectMapper.readTree(before).get("happiness").asInt();

            mockMvc.perform(post("/api/v1/pet/play")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.happiness").value(greaterThanOrEqualTo(Math.min(100, happinessBefore))));
        }

        @Test
        void shouldDecreaseEnergyWhenPlaying() throws Exception {
            String token = registerAndGetToken("playenergy", "playenergy@example.com");

            mockMvc.perform(post("/api/v1/pet/play")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.energy").value(lessThan(100)));
        }

        @Test
        void shouldAwardXpWhenPlaying() throws Exception {
            String token = registerAndGetToken("playxpuser", "playxp@example.com");

            mockMvc.perform(post("/api/v1/pet/play")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.xp").value(greaterThan(0)));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/play"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/rest
    // -------------------------------------------------------------------------

    @Nested
    class Rest {

        @Test
        void shouldIncreaseEnergyWhenResting() throws Exception {
            String token = registerAndGetToken("restuser", "rest@example.com");

            // First drain energy by playing, then rest
            mockMvc.perform(post("/api/v1/pet/play")
                    .header("Authorization", "Bearer " + token));

            String before = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            int energyBefore = objectMapper.readTree(before).get("energy").asInt();

            mockMvc.perform(post("/api/v1/pet/rest")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.energy").value(greaterThan(energyBefore)));
        }

        @Test
        void shouldAwardXpWhenResting() throws Exception {
            String token = registerAndGetToken("restxpuser", "restxp@example.com");

            mockMvc.perform(post("/api/v1/pet/rest")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.xp").value(greaterThan(0)));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/rest"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/clean
    // -------------------------------------------------------------------------

    @Nested
    class Clean {

        @Test
        void shouldIncreaseHygieneWhenCleaning() throws Exception {
            String token = registerAndGetToken("cleanuser", "clean@example.com");

            String before = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            int hygieneBefore = objectMapper.readTree(before).get("hygiene").asInt();

            mockMvc.perform(post("/api/v1/pet/clean")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hygiene").value(greaterThanOrEqualTo(Math.min(100, hygieneBefore))));
        }

        @Test
        void shouldAwardXpWhenCleaning() throws Exception {
            String token = registerAndGetToken("cleanxpuser", "cleanxp@example.com");

            mockMvc.perform(post("/api/v1/pet/clean")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.xp").value(greaterThan(0)));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/clean"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/heal
    // -------------------------------------------------------------------------

    @Nested
    class Heal {

        @Test
        void shouldIncreaseHealthWhenHealing() throws Exception {
            String token = registerAndGetToken("healuser", "heal@example.com");

            String before = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            int healthBefore = objectMapper.readTree(before).get("health").asInt();

            mockMvc.perform(post("/api/v1/pet/heal")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.health").value(greaterThanOrEqualTo(Math.min(100, healthBefore))));
        }

        @Test
        void shouldAwardXpWhenHealing() throws Exception {
            String token = registerAndGetToken("healxpuser", "healxp@example.com");

            mockMvc.perform(post("/api/v1/pet/heal")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.xp").value(greaterThan(0)));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/heal"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/sleep  &  POST /api/v1/pet/wake
    // -------------------------------------------------------------------------

    @Nested
    class SleepWake {

        @Test
        void shouldSetAsleepTrueWhenSleeping() throws Exception {
            String token = registerAndGetToken("sleepuser", "sleep@example.com");

            mockMvc.perform(post("/api/v1/pet/sleep")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAsleep").value(true));
        }

        @Test
        void shouldSetAsleepFalseWhenWaking() throws Exception {
            String token = registerAndGetToken("wakeuser", "wake@example.com");

            mockMvc.perform(post("/api/v1/pet/sleep")
                            .header("Authorization", "Bearer " + token));

            mockMvc.perform(post("/api/v1/pet/wake")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isAsleep").value(false));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/sleep"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pet/replace
    // -------------------------------------------------------------------------

    @Nested
    class Replace {

        @Test
        void shouldCreateNewPetWithDifferentIdWhenReplacing() throws Exception {
            String token = registerAndGetToken("replaceuser", "replace@example.com");

            String before = mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            String oldId = objectMapper.readTree(before).get("id").asText();

            mockMvc.perform(post("/api/v1/pet/replace")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(not(oldId)));
        }

        @Test
        void shouldStartAtEggLifeStageWhenReplacing() throws Exception {
            String token = registerAndGetToken("replacenew", "replacenew@example.com");

            mockMvc.perform(post("/api/v1/pet/replace")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lifeStage").value("EGG"))
                    .andExpect(jsonPath("$.level").value(1))
                    .andExpect(jsonPath("$.xp").value(0));
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/pet/replace"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Cross-action tests
    // -------------------------------------------------------------------------

    @Nested
    class CrossAction {

        @Test
        void shouldIncreaseBondLevelWhenInteractingMultipleTimes() throws Exception {
            String token = registerAndGetToken("bonduser", "bond@example.com");

            mockMvc.perform(post("/api/v1/pet/feed")
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(post("/api/v1/pet/play")
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bondLevel").value(greaterThan(1)));
        }

        @Test
        void shouldAccumulateXpWhenPerformingMultipleActions() throws Exception {
            String token = registerAndGetToken("xpaccuser", "xpacc@example.com");

            mockMvc.perform(post("/api/v1/pet/feed")
                    .header("Authorization", "Bearer " + token));
            mockMvc.perform(post("/api/v1/pet/play")
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    // feed=10 + play=15 = 25 XP minimum
                    .andExpect(jsonPath("$.xp").value(greaterThanOrEqualTo(25)));
        }

        @Test
        void shouldCapStatsAt100WhenExceedingMaximum() throws Exception {
            String token = registerAndGetToken("capuser", "cap@example.com");

            // Spam heal many times
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/v1/pet/heal")
                        .header("Authorization", "Bearer " + token));
            }

            mockMvc.perform(get("/api/v1/pet/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.health").value(lessThanOrEqualTo(100)));
        }
    }
}
