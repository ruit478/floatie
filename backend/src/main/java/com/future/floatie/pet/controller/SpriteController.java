package com.future.floatie.pet.controller;

import com.future.floatie.pet.service.PixelArtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/pet/sprite")
public class SpriteController {

    private final PixelArtService pixelArtService;

    public SpriteController(PixelArtService pixelArtService) {
        this.pixelArtService = pixelArtService;
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