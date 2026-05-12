package com.future.floatie.pet.controller;

import com.future.floatie.pet.dto.PetInfoResponse;
import com.future.floatie.pet.service.PetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for pet interactions. All endpoints require authentication
 * (the username is extracted from the JWT via {@code Authentication}).
 * Every endpoint delegates to {@link PetService}, which applies decay before
 * each action and returns the updated pet state as a {@link PetInfoResponse}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pet")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/status")
    public ResponseEntity<PetInfoResponse> getStatus(Authentication authentication) {
        log.debug("Status check for user: {}", authentication.getName());
        return petService.getPetStatus(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/info")
    public ResponseEntity<PetInfoResponse> getPetInfo(Authentication authentication) {
        return petService.getPetInfo(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/feed")
    public ResponseEntity<PetInfoResponse> feed(Authentication authentication) {
        log.info("Feed action for user: {}", authentication.getName());
        return petService.feedPet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/play")
    public ResponseEntity<PetInfoResponse> play(Authentication authentication) {
        log.info("Play action for user: {}", authentication.getName());
        return petService.playWithPet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rest")
    public ResponseEntity<PetInfoResponse> rest(Authentication authentication) {
        log.info("Rest action for user: {}", authentication.getName());
        return petService.restPet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/clean")
    public ResponseEntity<PetInfoResponse> clean(Authentication authentication) {
        log.info("Clean action for user: {}", authentication.getName());
        return petService.cleanPet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/heal")
    public ResponseEntity<PetInfoResponse> heal(Authentication authentication) {
        log.info("Heal action for user: {}", authentication.getName());
        return petService.healPet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sleep")
    public ResponseEntity<PetInfoResponse> sleep(Authentication authentication) {
        log.info("Sleep action for user: {}", authentication.getName());
        return petService.sleepPet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/wake")
    public ResponseEntity<PetInfoResponse> wake(Authentication authentication) {
        log.info("Wake action for user: {}", authentication.getName());
        return petService.wakePet(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/replace")
    public ResponseEntity<PetInfoResponse> replace(Authentication authentication) {
        log.info("Replace pet for user: {}", authentication.getName());
        return petService.replacePetForUsername(authentication.getName())
                .flatMap(pet -> petService.getPetInfo(authentication.getName()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
