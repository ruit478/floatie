package com.future.floatie.auth.controller;

import com.future.floatie.auth.request.AuthRequest;
import com.future.floatie.auth.response.AuthResponse;
import com.future.floatie.config.JwtUtil;
import com.future.floatie.entity.User;
import com.future.floatie.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // REGISTER - uses AuthRequest
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        // Check if user exists
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity
                    .badRequest()
                    .body(new AuthResponse(null, null, "Username is already taken"));
        }

        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity
                    .badRequest()
                    .body(new AuthResponse(null, null, "Email is already registered"));
        }

        // Create new user
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password())  // Hash password
        );

        userRepository.save(user);

        // Generate token
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        // Return AuthResponse with token
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getUsername(), "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            // Authenticate using username and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            // Generate JWT
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            // Return AuthResponse with token
            return ResponseEntity.ok(
                    new AuthResponse(token, userDetails.getUsername(), "Login successful")
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid username or password"));
        }
    }
}