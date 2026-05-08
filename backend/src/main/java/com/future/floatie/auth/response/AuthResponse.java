package com.future.floatie.auth.response;

public record AuthResponse(
        String token,
        String username,
        String message
) {}