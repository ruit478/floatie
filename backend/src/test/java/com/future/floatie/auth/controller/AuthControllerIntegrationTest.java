// backend/src/test/java/com/future/floatie/auth/controller/AuthControllerIntegrationTest.java
package com.future.floatie.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.future.floatie.auth.request.AuthRequest;
import com.future.floatie.entity.User;
import com.future.floatie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnTokenWhenSuccessfulRequest() throws Exception {
        AuthRequest request = new AuthRequest("newuser", "newuser@example.com", "Password123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void shouldReturnErrorWhenExistingUsernameIsProvided() throws Exception {
        // Create existing user
        User existingUser = new User("existinguser", "existing@example.com", passwordEncoder.encode("Password123!"));
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest("existinguser", "new@example.com", "Password123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void shouldReturnErrorWhenExistingEmailIsProvided() throws Exception {
        User existingUser = new User("existinguser", "existing@example.com", passwordEncoder.encode("Password123!"));
        userRepository.save(existingUser);

        AuthRequest request = new AuthRequest("newuser", "existing@example.com", "Password123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void shouldReturnValidationErrorWhenUsernameIsTooShort() throws Exception {
        // Username too short
        AuthRequest request = new AuthRequest("ab", "invalid-email", "123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnTokenOnLoginSuccess() throws Exception {
        // Create user first
        User user = new User("loginuser", "login@example.com", passwordEncoder.encode("Password123!"));
        userRepository.save(user);

        AuthRequest request = new AuthRequest("loginuser", "login@example.com", "Password123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void shouldReturnUnauthorizedWhenInvalidCredentialsAreProvided() throws Exception {
        User user = new User("loginuser", "login@example.com", passwordEncoder.encode("Password123!"));
        userRepository.save(user);

        // Wrong password
        AuthRequest request = new AuthRequest("loginuser", "login@example.com", "WrongPassword!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldReturnUnauthorizedWhenNonexistentUser() throws Exception {
        AuthRequest request = new AuthRequest("nonexistent", "login@example.com", "Password123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}