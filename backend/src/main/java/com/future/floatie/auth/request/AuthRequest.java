package com.future.floatie.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,


        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password
) {
    // Compact constructor for additional validation if needed
    public AuthRequest {
        // You can add custom validation here if needed
        // For example, trim whitespace
        if (username != null) {
            username = username.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    // Factory method for login (email not required)
    public static AuthRequest forLogin(String username, String password) {
        return new AuthRequest(username, null, password);
    }

    // Factory method for registration
    public static AuthRequest forRegistration(String username, String email, String password) {
        return new AuthRequest(username, email, password);
    }
}