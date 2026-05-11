package com.future.floatie.pet.service;

import com.future.floatie.pet.dto.PetInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import com.future.floatie.entity.Pet;
import com.future.floatie.entity.User;
import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.PetExpression;
import com.future.floatie.pet.enums.LifeStage;
import com.future.floatie.repository.PetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class PetService {

    private static final Random random = new Random();

    public static final String[] SUBCLASSES = {"cat", "dog", "fox", "rabbit", "axolotl", "frog", "penguin", "parrot"};
    public static final String[] COLORS = {"#FF6B9D", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7"};
    private static final Map<String, String[]> SPECIES_NAMES = Map.of(
            "cat", new String[]{"Whiskers", "Luna", "Simba", "Mittens", "Shadow", "Cleo", "Felix", "Oliver"},
            "dog", new String[]{"Buddy", "Max", "Bella", "Charlie", "Rocky", "Daisy", "Cooper", "Lola"},
            "fox", new String[]{"Fennel", "Rusty", "Vulpes", "Blaze", "Ember", "Zorro", "Foxy", "Cinder"},
            "rabbit", new String[]{"Fluffy", "Thumper", "Hoppy", "Cotton", "Snowball", "Bugs", "Bunny", "Clover"},
            "axolotl", new String[]{"Axel", "Loti", "Salamander", "Mudkip", "Aqua", "Axo", "Lottie", "Mochi"},
            "frog", new String[]{"Kermit", "Croak", "Hopper", "Lily", "Toad", "Frogger", "Sprout", "Puddles"},
            "penguin", new String[]{"Pip", "Skipper", "Pebble", "Ice", "Flake", "Waddle", "Flipper", "Snow"},
            "parrot", new String[]{"Rio", "Sky", "Rainbow", "Kiwi", "Coco", "Phoenix", "Tiki", "Zazu"}
    );

    private final PetRepository petRepository;
    private final PixelArtService pixelArtService;

    @Value("${app.uploads.directory}")
    private String uploadsDirectory;

    public PetService(PetRepository petRepository, PixelArtService pixelArtService) {
        this.petRepository = petRepository;
        this.pixelArtService = pixelArtService;
    }

    @Transactional
    public Optional<Pet> createPetForUser(User user) {
        log.info("Creating pet for user: {}", user.getUsername());

        try {
            // Generate random attributes
            String subclass = getRandomSubclass();
            String colorHex = getRandomColor();
            PetExpression expression = getRandomExpression();

            log.debug("Random attributes for user {}: subclass={}, color={}, expression={}",
                    user.getUsername(), subclass, colorHex, expression);

            // Create pet entity
            Pet pet = new Pet();
            pet.setUser(user);
            pet.setSubclass(subclass);
            pet.setName(getRandomNameForSubclass(subclass));
            pet.setColorHex(colorHex);
            pet.setExpression(expression);
            pet.setLifeStage(LifeStage.EGG);

            // Initialize stats
            pet.setHunger(30 + random.nextInt(60));     // 30-89
            pet.setHappiness(40 + random.nextInt(60));   // 40-99
            pet.setEnergy(50 + random.nextInt(50));      // 50-99
            pet.setHealth(70 + random.nextInt(31));      // 70-100
            pet.setHygiene(40 + random.nextInt(60));     // 40-99
            pet.setWeight(30 + random.nextInt(70));      // 30-99
            pet.setXp(0);
            pet.setLevel(1);

            Pet savedPet = petRepository.save(pet);
            log.info("Pet entity created with ID: {} for user: {}", savedPet.getId(), user.getUsername());

            // Generate and save sprite
            generateAndSaveSprite(savedPet);

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

    private void generateAndSaveSprite(Pet pet) throws IOException {
        log.debug("Generating sprite for petId: {}", pet.getId());

        // Generate sprite image
        java.awt.image.BufferedImage sprite = pixelArtService.generatePetSprite(pet);

        // Save to disk
        String filename = pet.getId().toString() + ".png";
        Path filePath = Paths.get(uploadsDirectory, filename);
        Files.createDirectories(filePath.getParent());
        javax.imageio.ImageIO.write(sprite, "PNG", filePath.toFile());

        log.debug("Sprite saved to disk: {}", filePath);
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
            generateAndSaveSprite(pet);

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

    private String getRandomNameForSubclass(String subclass) {
        String[] names = SPECIES_NAMES.getOrDefault(subclass,
                new String[]{"Buddy", "Lucky", "Sparky", "Rosie", "Max", "Bella", "Charlie", "Daisy"});
        return names[random.nextInt(names.length)];
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

    public Optional<PetInfoResponse> getPetInfo(String username) {
        return petRepository.findByUser_Username(username).map(pet -> {
            String base64 = getSpriteResource(pet.getId() + ".png")
                    .map(resource -> {
                        try {
                            return Base64.getEncoder().encodeToString(resource.getContentAsByteArray());
                        } catch (Exception e) {
                            return null;
                        }
                    }).orElse(null);
            return PetInfoResponse.from(pet, base64);
        });
    }
}