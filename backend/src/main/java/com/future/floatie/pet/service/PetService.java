package com.future.floatie.pet.service;

import com.future.floatie.pet.PetConstants;
import com.future.floatie.pet.dto.PetInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import com.future.floatie.entity.Pet;
import com.future.floatie.entity.User;
import com.future.floatie.pet.enums.AccessoryType;
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

    // Stat decay rates per hour (configurable)
    @Value("${app.pet.hunger-decay:2.0}")
    private double hungerDecay;
    @Value("${app.pet.happiness-decay:1.5}")
    private double happinessDecay;
    @Value("${app.pet.energy-decay:1.0}")
    private double energyDecay;
    @Value("${app.pet.hygiene-decay:1.0}")
    private double hygieneDecay;
    @Value("${app.pet.health-decay-critical:3.0}")
    private double healthDecayCritical;
    @Value("${app.pet.health-decay-low:1.0}")
    private double healthDecayLow;

    // Time multiplier — crank up for testing (60 = 1 real minute = 1 game hour)
    @Value("${app.pet.time-multiplier:1.0}")
    private double timeMultiplier;

    // XP + Bond rewards per action (configurable)
    @Value("${app.pet.xp-feed:10}")
    private int xpFeed;
    @Value("${app.pet.xp-play:15}")
    private int xpPlay;
    @Value("${app.pet.xp-rest:5}")
    private int xpRest;
    @Value("${app.pet.xp-clean:5}")
    private int xpClean;
    @Value("${app.pet.xp-heal:5}")
    private int xpHeal;
    @Value("${app.pet.bond-feed:1}")
    private int bondFeed;
    @Value("${app.pet.bond-play:2}")
    private int bondPlay;
    @Value("${app.pet.bond-clean:1}")
    private int bondClean;
    @Value("${app.pet.bond-heal:1}")
    private int bondHeal;

    // XP needed per level: level * xpPerLevel
    @Value("${app.pet.xp-per-level:150}")
    private int xpPerLevel;

    // Life-stage age thresholds in days (comma-separated, 6 values for EGG..ELDER)
    @Value("${app.pet.stage-thresholds:0,2,5,11,21,61}")
    private String stageThresholdsStr;
    private int[] stageThresholds;

    // Life-stage level thresholds (comma-separated, 6 values for EGG..ELDER)
    @Value("${app.pet.level-stage-thresholds:1,3,6,10,15,25}")
    private String levelStageThresholdsStr;
    private int[] levelStageThresholds;

    // Levels at which evolution triggers (e.g. 5,10,20)
    @Value("${app.pet.evolution-levels:5,10,20}")
    private String evolutionLevelsStr;
    private java.util.Set<Integer> evolutionLevels = java.util.Set.of();

    @jakarta.annotation.PostConstruct
    private void initProperties() {
        String[] parts = stageThresholdsStr.split(",");
        stageThresholds = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            stageThresholds[i] = Integer.parseInt(parts[i].trim());
        }
        String[] levelParts = levelStageThresholdsStr.split(",");
        levelStageThresholds = new int[levelParts.length];
        for (int i = 0; i < levelParts.length; i++) {
            levelStageThresholds[i] = Integer.parseInt(levelParts[i].trim());
        }
        evolutionLevels = java.util.Arrays.stream(evolutionLevelsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(java.util.stream.Collectors.toSet());
    }

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

            // Random accessory
            AccessoryType[] accessoryTypes = AccessoryType.values();
            pet.setAccessoryType(accessoryTypes[random.nextInt(accessoryTypes.length)]);
            pet.setAccessoryColorHex(PetConstants.ACCENT_COLORS[random.nextInt(PetConstants.ACCENT_COLORS.length)]);

            // Initialize stats
            pet.setHunger(30 + random.nextInt(60));     // 30-89
            pet.setHappiness(40 + random.nextInt(60));   // 40-99
            pet.setEnergy(50 + random.nextInt(50));      // 50-99
            pet.setHealth(70 + random.nextInt(31));      // 70-100
            pet.setHygiene(40 + random.nextInt(60));     // 40-99
            pet.setWeight(30 + random.nextInt(70));      // 30-99
            pet.setXp(0);
            pet.setLevel(1);
            pet.setLastInteractionAt(java.time.LocalDateTime.now());

            Pet savedPet = petRepository.save(pet);
            log.info("Pet entity created with ID: {} for user: {}", savedPet.getId(), user.getUsername());

            // Generate and save sprite
            generateAndSaveSprite(savedPet);

            log.info("Pet creation completed successfully for user: {} -> petId: {}, species: {}",
                    user.getUsername(), savedPet.getId(), subclass);

            return Optional.of(savedPet);

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

        java.awt.image.BufferedImage sprite = pixelArtService.generatePetSprite(pet);

        String filename = pet.getId().toString() + ".png";
        Path filePath = Paths.get(uploadsDirectory, filename);
        Path tmpPath = Paths.get(uploadsDirectory, filename + ".tmp");
        Files.createDirectories(filePath.getParent());
        javax.imageio.ImageIO.write(sprite, "PNG", tmpPath.toFile());
        Files.move(tmpPath, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

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
        String[] names = PetConstants.SPECIES_NAMES.getOrDefault(subclass,
                new String[]{"Buddy", "Lucky", "Sparky", "Rosie", "Max", "Bella", "Charlie", "Daisy"});
        return names[random.nextInt(names.length)];
    }

    private String getRandomSubclass() {
        return PetConstants.SUBCLASSES[random.nextInt(PetConstants.SUBCLASSES.length)];
    }

    private String getRandomColor() {
        return PetConstants.COLORS[random.nextInt(PetConstants.COLORS.length)];
    }

    private PetExpression getRandomExpression() {
        PetExpression[] expressions = PetExpression.values();
        return expressions[random.nextInt(expressions.length)];
    }

    @Transactional
    public Optional<PetInfoResponse> getPetInfo(String username) {
        return petRepository.findByUser_Username(username).map(pet -> {
            applyDecay(pet);
            petRepository.save(pet);
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

    // ═══════════════════════════════════════════════════════════════════
    //  Pet status — returns current state with decay applied
    // ═══════════════════════════════════════════════════════════════════

    public Optional<PetInfoResponse> getPetStatus(String username) {
        return getPetInfo(username);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Interactions — each applies decay first, then the action
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public Optional<PetInfoResponse> feedPet(String username) {
        return interact(username, pet -> {
            pet.setHunger(clamp(pet.getHunger() + 20));
            pet.setWeight(clamp(pet.getWeight() + 5));
            pet.setHappiness(clamp(pet.getHappiness() + 5));
            addXp(pet, xpFeed);
            addBond(pet, bondFeed);
        });
    }

    @Transactional
    public Optional<PetInfoResponse> playWithPet(String username) {
        return interact(username, pet -> {
            pet.setHappiness(clamp(pet.getHappiness() + 20));
            pet.setEnergy(clamp(pet.getEnergy() - 15));
            pet.setHunger(clamp(pet.getHunger() - 5));
            pet.setWeight(clamp(pet.getWeight() - 3));
            addXp(pet, xpPlay);
            addBond(pet, bondPlay);
        });
    }

    @Transactional
    public Optional<PetInfoResponse> restPet(String username) {
        return interact(username, pet -> {
            pet.setEnergy(clamp(pet.getEnergy() + 30));
            pet.setHealth(clamp(pet.getHealth() + 10));
            pet.setIsAsleep(false);
            addXp(pet, xpRest);
        });
    }

    @Transactional
    public Optional<PetInfoResponse> cleanPet(String username) {
        return interact(username, pet -> {
            pet.setHygiene(clamp(pet.getHygiene() + 30));
            pet.setHealth(clamp(pet.getHealth() + 5));
            addXp(pet, xpClean);
            addBond(pet, bondClean);
        });
    }

    @Transactional
    public Optional<PetInfoResponse> healPet(String username) {
        return interact(username, pet -> {
            pet.setHealth(clamp(pet.getHealth() + 30));
            pet.setEnergy(clamp(pet.getEnergy() + 10));
            addXp(pet, xpHeal);
            addBond(pet, bondHeal);
        });
    }

    @Transactional
    public Optional<PetInfoResponse> sleepPet(String username) {
        return interact(username, pet -> {
            pet.setIsAsleep(true);
        });
    }

    @Transactional
    public Optional<PetInfoResponse> wakePet(String username) {
        return interact(username, pet -> {
            pet.setIsAsleep(false);
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Internal mechanics
    // ═══════════════════════════════════════════════════════════════════

    /** Common interaction wrapper: fetch pet, apply decay, run action, save. */
    private Optional<PetInfoResponse> interact(String username, java.util.function.Consumer<Pet> action) {
        return petRepository.findByUser_Username(username).map(pet -> {
            if (pet.getLifeStage() == LifeStage.DEAD) return null;
            applyDecay(pet);
            action.accept(pet);
            petRepository.save(pet);
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

    /**
     * Applies stat decay and age progression based on real time elapsed
     * since the last interaction (or pet creation if never interacted).
     */
    private void applyDecay(Pet pet) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // Sleeping pets are protected from all decay
        if (pet.getIsAsleep()) {
            pet.setLastInteractionAt(now);
            return;
        }

        java.time.LocalDateTime last = pet.getLastInteractionAt() != null
                ? pet.getLastInteractionAt() : pet.getCreatedAt();

        long realSeconds = Math.max(0, java.time.Duration.between(last, now).toSeconds());
        // Convert real seconds → game hours using the configured multiplier
        int h = (int) ((realSeconds / 3600.0) * timeMultiplier);
        if (h == 0) {
            pet.setLastInteractionAt(now);
            return;
        }

        // Apply decay
        pet.setHunger(clamp(pet.getHunger() - (int) (h * hungerDecay)));
        pet.setHappiness(clamp(pet.getHappiness() - (int) (h * happinessDecay)));
        pet.setEnergy(clamp(pet.getEnergy() - (int) (h * energyDecay)));
        pet.setHygiene(clamp(pet.getHygiene() - (int) (h * hygieneDecay)));

        // Health decays when other stats are critically low
        int minStat = Math.min(Math.min(pet.getHunger(), pet.getHappiness()),
                      Math.min(pet.getEnergy(), pet.getHygiene()));
        if (minStat <= 0) {
            pet.setHealth(clamp(pet.getHealth() - (int) (h * healthDecayCritical)));
        } else if (minStat < 20) {
            pet.setHealth(clamp(pet.getHealth() - (int) (h * healthDecayLow)));
        }

        // Age progression — based on total game hours since creation
        long realSecondsSinceCreation = java.time.Duration.between(pet.getCreatedAt(), now).toSeconds();
        int totalGameHours = (int) ((realSecondsSinceCreation / 3600.0) * timeMultiplier);
        pet.setAgeDays(totalGameHours / 24);
        checkLifeStage(pet);

        // If health hits 0, pet dies
        if (pet.getHealth() <= 0) {
            pet.setLifeStage(LifeStage.DEAD);
        }

        pet.setLastInteractionAt(now);
    }

    /** Add XP and handle level-up. Triggers evolution when crossing a configured threshold. */
    private void addXp(Pet pet, int amount) {
        int xp = pet.getXp() + amount;
        int xpToLevel = pet.getLevel() * xpPerLevel;
        boolean leveledUp = false;

        while (xp >= xpToLevel) {
            xp -= xpToLevel;
            pet.setLevel(pet.getLevel() + 1);
            xpToLevel = pet.getLevel() * xpPerLevel;
            leveledUp = true;
        }
        pet.setXp(xp);

        if (leveledUp) {
            checkLifeStageByLevel(pet);
            if (evolutionLevels.contains(pet.getLevel())) {
                triggerEvolution(pet);
            }
        }
    }

    /**
     * Auto-evolve: pick a path based on current stat quality, increment the
     * evolution stage, and regenerate the sprite.
     */
    private void triggerEvolution(Pet pet) {
        EvolutionPath path = determineEvolutionPath(pet);
        pet.setEvolutionPath(path);
        pet.setEvolutionStage(pet.getEvolutionStage() + 1);
        log.info("Pet {} auto-evolved at level {} to stage {} via path {}",
                pet.getId(), pet.getLevel(), pet.getEvolutionStage(), path);
        try {
            generateAndSaveSprite(pet);
        } catch (IOException e) {
            log.error("Failed to regenerate sprite during auto-evolution for pet {}", pet.getId(), e);
        }
    }

    /** Choose evolution path based on current stat quality. */
    private EvolutionPath determineEvolutionPath(Pet pet) {
        int minStat = Math.min(Math.min(pet.getHunger(), pet.getHappiness()),
                      Math.min(pet.getEnergy(), pet.getHygiene()));
        boolean allHigh = minStat >= 70 && pet.getHealth() >= 70;
        boolean allOk   = minStat >= 40 && pet.getHealth() >= 40;

        if (pet.getWeight() > 80) return EvolutionPath.OVERWEIGHT;
        if (allHigh) return EvolutionPath.PERFECT;
        if (allOk) return EvolutionPath.WELL_RAISED;
        if (minStat < 20) return EvolutionPath.NEGLECTED;
        return EvolutionPath.WELL_RAISED;
    }

    /** Add bond, capped at 100. */
    private void addBond(Pet pet, int amount) {
        pet.setBondLevel(Math.min(100, pet.getBondLevel() + amount));
    }

    /** Advance life stage based on ageDays thresholds. */
    private void checkLifeStage(Pet pet) {
        if (pet.getLifeStage() == LifeStage.DEAD) return;

        LifeStage[] stages = LifeStage.values();
        for (int i = stages.length - 1; i >= 0; i--) {
            if (stages[i] == LifeStage.DEAD) continue;
            if (pet.getAgeDays() >= stageThresholds[i]) {
                if (stages[i].ordinal() > pet.getLifeStage().ordinal()) {
                    log.info("Pet {} life stage advanced: {} → {} (age {}d)",
                            pet.getId(), pet.getLifeStage(), stages[i], pet.getAgeDays());
                    pet.setLifeStage(stages[i]);
                    regenerateSprite(pet);
                }
                break;
            }
        }
    }

    /** Advance life stage based on level thresholds. */
    private void checkLifeStageByLevel(Pet pet) {
        if (pet.getLifeStage() == LifeStage.DEAD) return;

        LifeStage[] stages = LifeStage.values();
        for (int i = stages.length - 1; i >= 0; i--) {
            if (stages[i] == LifeStage.DEAD) continue;
            int thresholdIdx = Math.min(i, levelStageThresholds.length - 1);
            if (pet.getLevel() >= levelStageThresholds[thresholdIdx]) {
                if (stages[i].ordinal() > pet.getLifeStage().ordinal()) {
                    log.info("Pet {} life stage advanced: {} → {} (level {})",
                            pet.getId(), pet.getLifeStage(), stages[i], pet.getLevel());
                    pet.setLifeStage(stages[i]);
                    regenerateSprite(pet);
                }
                break;
            }
        }
    }

    private void regenerateSprite(Pet pet) {
        try {
            generateAndSaveSprite(pet);
        } catch (IOException e) {
            log.error("Failed to regenerate sprite for pet {}", pet.getId(), e);
        }
    }

    /** Delete the on-disk sprite PNG for a pet. */
    public void deleteSpriteForPet(UUID petId) {
        Path filePath = Paths.get(uploadsDirectory, petId.toString() + ".png");
        try {
            Files.deleteIfExists(filePath);
            log.debug("Deleted sprite file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete sprite file: {}", filePath, e);
        }
    }

    /** Replace the current pet for a user with a brand-new one. */
    @Transactional
    public Optional<Pet> replacePetForUsername(String username) {
        return petRepository.findByUser_Username(username).map(oldPet -> {
            User user = oldPet.getUser();
            // Create new pet before deleting old one so no 404 window exists
            Optional<Pet> newPetOpt = createPetForUser(user);
            if (newPetOpt.isPresent()) {
                deleteSpriteForPet(oldPet.getId());
                user.setPet(null);
                petRepository.delete(oldPet);
                petRepository.flush();
            }
            return newPetOpt.orElse(null);
        });
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}