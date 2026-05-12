package com.future.floatie.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for integration tests.
 *
 * Starts a single PostgreSQL container reused across all subclasses
 * (Testcontainers reuses a static container per JVM), and registers all
 * properties that the application context requires at startup so that
 * individual test classes don't have to repeat them.
 */
@Testcontainers
public abstract class IntegrationTestBase {

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
        registry.add("spring.datasource.hikari.max-lifetime", () -> "60000");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Sprite files are written to a temp dir during tests
        registry.add("app.uploads.directory", () -> System.getProperty("java.io.tmpdir") + "/floatie-test-sprites");
        // CORS — any value is fine for MockMvc tests (no real browser involved)
        registry.add("app.cors.allowed-origins", () -> "http://localhost:4200");
        // JWT
        registry.add("jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hmac-sha");
        registry.add("jwt.expiration", () -> "86400000");
    }
}