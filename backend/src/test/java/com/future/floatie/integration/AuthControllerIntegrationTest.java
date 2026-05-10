package com.future.floatie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.floatie.auth.request.AuthRequest;
import com.future.floatie.entity.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User savedUser(String username, String email, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private String json(AuthRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    @Nested
    class Register {

        @Test
        void happyPath_returnsCreatedWithToken() throws Exception {
            var request = new AuthRequest("newuser", "new@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andExpect(jsonPath("$.message").value("Registration successful"));
        }

        @Test
        void happyPath_petIsCreatedForNewUser() throws Exception {
            // Registration triggers PetService.createPetForUser — a pet row must exist afterwards
            var request = new AuthRequest("petowner", "petowner@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isCreated());

            var user = userRepository.findByUsername("petowner").orElseThrow();
            org.assertj.core.api.Assertions.assertThat(user.getPet()).isNotNull();
        }

        @Test
        void duplicateUsername_returnsBadRequest() throws Exception {
            savedUser("taken", "taken@example.com", "Password123!");

            var request = new AuthRequest("taken", "other@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Username is already taken"));
        }

        @Test
        void blankUsername_returnsValidationError() throws Exception {
            var request = new AuthRequest("", "valid@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").exists());
        }

        @Test
        void usernameTooShort_returnsValidationError() throws Exception {
            var request = new AuthRequest("ab", "valid@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").exists());
        }

        @Test
        void usernameTooLong_returnsValidationError() throws Exception {
            var request = new AuthRequest("a".repeat(51), "valid@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").exists());
        }

        @Test
        void passwordTooShort_returnsValidationError() throws Exception {
            var request = new AuthRequest("validuser", "valid@example.com", "abc");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }

        @Test
        void blankPassword_returnsValidationError() throws Exception {
            var request = new AuthRequest("validuser", "valid@example.com", "");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.password").exists());
        }

        @Test
        void invalidEmailFormat_returnsValidationError() throws Exception {
            var request = new AuthRequest("validuser", "not-an-email", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        void usernameIsTrimmed_leadingAndTrailingSpacesIgnored() throws Exception {
            // AuthRequest compact constructor trims username — "  alice  " becomes "alice"
            var request = new AuthRequest("  alice  ", "alice@example.com", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        void emailIsLowercased_onRegistration() throws Exception {
            var request = new AuthRequest("caseuser", "UPPER@Example.COM", "Password123!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isCreated());

            var saved = userRepository.findByUsername("caseuser").orElseThrow();
            org.assertj.core.api.Assertions.assertThat(saved.getEmail()).isEqualTo("upper@example.com");
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------

    @Nested
    class Login {

        @Test
        void happyPath_returnsOkWithToken() throws Exception {
            savedUser("loginuser", "login@example.com", "Password123!");

            var request = new AuthRequest("loginuser", null, "Password123!");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.username").value("loginuser"))
                    .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test
        void wrongPassword_returnsUnauthorized() throws Exception {
            savedUser("loginuser", "login@example.com", "Password123!");

            var request = new AuthRequest("loginuser", null, "WrongPassword!");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        void nonexistentUser_returnsUnauthorized() throws Exception {
            var request = new AuthRequest("nobody", null, "Password123!");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        void blankPassword_returnsValidationError() throws Exception {
            var request = new AuthRequest("loginuser", null, "");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void tokenIsUsable_authenticatedRequestSucceeds() throws Exception {
            // Register to get a real token, then use it on a protected endpoint
            var register = new AuthRequest("tokenuser", "token@example.com", "Password123!");

            String body = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(register)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String token = objectMapper.readTree(body).get("token").asText();

            mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        void invalidToken_onProtectedEndpoint_returnsForbidden() throws Exception {
            String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXIifQ.invalidsignature";

            mockMvc.perform(get("/api/v1/pet/sprite/info")
                            .header("Authorization", "Bearer " + fakeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void noToken_onProtectedEndpoint_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/pet/sprite/info"))
                    .andExpect(status().isForbidden());
        }
    }
}