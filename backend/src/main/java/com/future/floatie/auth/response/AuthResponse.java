package com.future.floatie.auth.response;

/**
 * JWT authentication response. The frontend stores {@code token} and
 * {@code username} in localStorage and attaches the token as a Bearer
 * header on subsequent requests.
 */
public record AuthResponse(
        String token,
        String username,
        String message
) {}