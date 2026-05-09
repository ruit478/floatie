package com.future.floatie.pet.controller;

import com.future.floatie.entity.Pet;
import com.future.floatie.pet.dto.PetInfoResponse;
import com.future.floatie.pet.service.PetService;
import com.future.floatie.pet.service.PixelArtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/pet/sprite")
public class SpriteController {

    @Autowired
    private PetService petService;

    @Autowired
    private PixelArtService pixelArtService;

    @GetMapping("/info")
    public ResponseEntity<PetInfoResponse> getPetInfo(Authentication authentication) {

        return petService.getPetInfo(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<ByteArrayResource> generateRandomSprite() {
        log.info("Generating random sprite");

        try {
            // Generate random sprite directly from PixelArtService
            BufferedImage sprite = pixelArtService.generateRandomSprite();

            // Convert to PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(sprite, "PNG", baos);

            log.info("Random sprite generated successfully");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .body(new ByteArrayResource(baos.toByteArray()));

        } catch (Exception e) {
            log.error("Failed to generate random sprite", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}