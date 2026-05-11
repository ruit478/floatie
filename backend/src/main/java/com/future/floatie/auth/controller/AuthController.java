package com.future.floatie.auth.controller;

import com.future.floatie.auth.request.AuthRequest;
import com.future.floatie.auth.response.AuthResponse;
import com.future.floatie.config.JwtUtil;
import com.future.floatie.entity.User;
import com.future.floatie.pet.service.PetService;
import com.future.floatie.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PetService petService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          PetService petService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.petService = petService;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        log.info("Registration attempt for username='{}'", request.username());
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed - username already taken: '{}'", request.username());
            return ResponseEntity
                    .badRequest()
                    .body(new AuthResponse(null, null, "Username is already taken"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        petService.createPetForUser(user)
                .ifPresentOrElse(
                        pet -> log.info("Pet created: {}", pet.getId()),
                        () -> { throw new RuntimeException("Failed to create pet"); }
                );

        // Generate token
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        log.info("User registered successfully: username='{}', id='{}'", user.getUsername(), user.getId());
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