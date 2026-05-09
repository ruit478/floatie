package com.future.floatie.pet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import com.future.floatie.entity.Pet;
import com.future.floatie.entity.User;
import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.PetExpression;
import com.future.floatie.pet.enums.LifeStage;
import com.future.floatie.repository.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class PetService {

    private static final Random random = new Random();

    private static final String[] PET_CLASSES = {"MAMMALS", "BIRDS", "REPTILES", "AMPHIBIANS", "FISH", "INVERTEBRATES"};
    private static final String[] SUBCLASSES = {"cat", "dog", "fox", "rabbit", "axolotl", "frog", "penguin", "parrot"};
    private static final String[] COLORS = {"#FF6B9D", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7"};

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PixelArtService pixelArtService;

    @Value("${app.uploads.directory}")
    private String uploadsDirectory;

    @Transactional
    public Optional<Pet> createPetForUser(User user) {
        log.info("Creating pet for user: {}", user.getUsername());

        try {
            // Generate random attributes
            String petClass = getRandomPetClass();
            String subclass = getRandomSubclass();
            String colorHex = getRandomColor();
            PetExpression expression = getRandomExpression();

            log.debug("Random attributes for user {}: class={}, subclass={}, color={}, expression={}",
                    user.getUsername(), petClass, subclass, colorHex, expression);

            // Create pet entity
            Pet pet = new Pet();
            pet.setUser(user);
            pet.setClassType(petClass);
            pet.setSubclass(subclass);
            pet.setColorHex(colorHex);
            pet.setExpression(expression);
            pet.setLifeStage(LifeStage.EGG);

            // Initialize stats
            pet.setHunger(50);
            pet.setHappiness(70);
            pet.setEnergy(80);
            pet.setHealth(100);
            pet.setHygiene(80);
            pet.setWeight(50);
            pet.setXp(0);
            pet.setLevel(1);

            Pet savedPet = petRepository.save(pet);
            log.info("Pet entity created with ID: {} for user: {}", savedPet.getId(), user.getUsername());

            // Generate and save sprite
            String spriteUrl = generateAndSaveSprite(savedPet);
            savedPet.setSpriteUrl(spriteUrl);

            Pet finalPet = petRepository.save(savedPet);
            log.info("Pet creation completed successfully for user: {} -> petId: {}, species: {}",
                    user.getUsername(), finalPet.getId(), subclass);

            return Optional.of(finalPet);

        } catch (IOException e) {
            log.error("Failed to create pet for user: {} - IO error: {}", user.getUsername(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error creating pet for user: {}", user.getUsername(), e);
            return Optional.empty();
        }
    }

    public Optional<Resource> getPetSprite(UUID petId) {
        return petRepository.findById(petId)
                .flatMap(pet -> {
                    log.debug("Loading sprite for petId: {}", petId);
                    return getSpriteResource(pet.getId().toString() + ".png");
                });
    }

    private String generateAndSaveSprite(Pet pet) throws IOException {
        log.debug("Generating sprite for petId: {}", pet.getId());

        // Generate sprite image
        java.awt.image.BufferedImage sprite = pixelArtService.generatePetSprite(pet);

        // Save to disk
        String filename = pet.getId().toString() + ".png";
        Path filePath = Paths.get(uploadsDirectory, filename);
        Files.createDirectories(filePath.getParent());
        javax.imageio.ImageIO.write(sprite, "PNG", filePath.toFile());

        log.debug("Sprite saved to disk: {}", filePath);

        // Return URL
        String spriteUrl = "/api/v1/pet/sprite/" + pet.getId();
        return spriteUrl;
    }

    public Optional<Resource> getPetSpriteByFilename(String filename) {
        log.debug("Loading sprite by filename: {}", filename);
        return getSpriteResource(filename);
    }

    private Optional<Resource> getSpriteResource(String filename) {
        try {
            Path filePath = Paths.get(uploadsDirectory, filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("Sprite found: {}", filename);
                return Optional.of(resource);
            } else {
                log.warn("Sprite not found or not readable: {}", filename);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error loading sprite resource: {}", filename, e);
            return Optional.empty();
        }
    }

    @Transactional
    public Optional<Pet> evolvePet(UUID petId, EvolutionPath evolutionPath) {
        log.info("Evolving pet: {} to path: {}", petId, evolutionPath);

        try {
            Pet pet = petRepository.findById(petId)
                    .orElseThrow(() -> new RuntimeException("Pet not found: " + petId));

            int oldStage = pet.getEvolutionStage();
            pet.setEvolutionPath(evolutionPath);
            pet.setEvolutionStage(oldStage + 1);

            log.info("Pet {} evolving from stage {} to {} via path {}",
                    petId, oldStage, oldStage + 1, evolutionPath);

            // Regenerate sprite
            String newSpriteUrl = generateAndSaveSprite(pet);
            pet.setSpriteUrl(newSpriteUrl);

            Pet evolvedPet = petRepository.save(pet);
            log.info("Pet {} evolution completed successfully", petId);

            return Optional.of(evolvedPet);

        } catch (IOException e) {
            log.error("Failed to generate sprite during evolution for pet: {}", petId, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during pet evolution: {}", petId, e);
            return Optional.empty();
        }
    }

    // Helper methods for random generation
    private String getRandomPetClass() {
        return PET_CLASSES[random.nextInt(PET_CLASSES.length)];
    }

    private String getRandomSubclass() {
        return SUBCLASSES[random.nextInt(SUBCLASSES.length)];
    }

    private String getRandomColor() {
        return COLORS[random.nextInt(COLORS.length)];
    }

    private PetExpression getRandomExpression() {
        PetExpression[] expressions = PetExpression.values();
        return expressions[random.nextInt(expressions.length)];
    }
}