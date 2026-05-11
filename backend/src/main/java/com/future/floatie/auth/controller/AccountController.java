package com.future.floatie.auth.controller;

import com.future.floatie.entity.User;
import com.future.floatie.pet.service.PetService;
import com.future.floatie.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final UserRepository userRepository;
    private final PetService petService;

    public AccountController(UserRepository userRepository, PetService petService) {
        this.userRepository = userRepository;
        this.petService = petService;
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        String username = authentication.getName();
        log.info("Account deletion requested for user: {}", username);

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (user.getPet() != null) {
            petService.deleteSpriteForPet(user.getPet().getId());
        }

        userRepository.delete(user);
        log.info("Account deleted for user: {}", username);

        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
}
