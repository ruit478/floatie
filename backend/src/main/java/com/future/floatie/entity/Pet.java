package com.future.floatie.entity;

import com.future.floatie.pet.enums.AccessoryType;
import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.LifeStage;
import com.future.floatie.pet.enums.PetExpression;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Virtual pet entity with a 1:1 relationship to {@link User}.
 *
 * <h3>Stat system</h3>
 * Six core stats (hunger, happiness, energy, health, hygiene, weight) are
 * clamped to 0–100. All decay over time; health decays as a consequence of
 * other stats dropping too low. When health hits 0 the pet dies.
 *
 * <h3>Progression</h3>
 * XP from interactions drives level-ups. Each level requires
 * {@code level * xpPerLevel} XP. Level-ups can advance the life stage and
 * trigger evolution at configured thresholds.
 *
 * <h3>Timestamps</h3>
 * {@code lastInteractionAt} drives the decay calculation — it is updated on
 * every interaction and on poll (info/status). {@code createdAt} seeds the
 * age calculation. Both work with the configurable time multiplier so decay
 * can be accelerated for testing.
 */
@Entity
@Table(name = "pets")
@Getter
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @Setter
    private User user;

    @Column(length = 50)
    @Setter
    private String name;

    @Column(nullable = false, length = 50)
    @Setter
    private String subclass;

    @Column(name = "color_hex", nullable = false, length = 7)
    @Setter
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private PetExpression expression;

    @Enumerated(EnumType.STRING)
    @Column(name = "accessory_type", length = 20)
    @Setter
    private AccessoryType accessoryType = AccessoryType.NONE;

    @Column(name = "accessory_color_hex", length = 7)
    @Setter
    private String accessoryColorHex;

    // Core Stats (0-100)
    @Column(nullable = false)
    @Setter
    private Integer hunger = 50;

    @Column(nullable = false)
    @Setter
    private Integer happiness = 70;

    @Column(nullable = false)
    @Setter
    private Integer energy = 80;

    @Column(nullable = false)
    @Setter
    private Integer health = 100;

    @Column(nullable = false)
    @Setter
    private Integer hygiene = 80;

    @Column(nullable = false)
    @Setter
    private Integer weight = 50;

    @Enumerated(EnumType.STRING)
    @Column(name = "life_stage", nullable = false, length = 20)
    @Setter
    private LifeStage lifeStage;

    @Column(name = "age_days", nullable = false)
    @Setter
    private Integer ageDays = 0;

    // Progression
    @Column(nullable = false)
    @Setter
    private Integer xp = 0;

    @Column(nullable = false)
    @Setter
    private Integer level = 1;

    // Evolution
    @Enumerated(EnumType.STRING)
    @Column(name = "evolution_path", length = 20)
    @Setter
    private EvolutionPath evolutionPath = EvolutionPath.WELL_RAISED;

    @Column(name = "evolution_stage", nullable = false)
    @Setter
    private Integer evolutionStage = 1;

    @Column(name = "is_asleep", nullable = false)
    @Setter
    private Boolean isAsleep = false;

    @Column(name = "bond_level", nullable = false)
    @Setter
    private Integer bondLevel = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_interaction_at")
    @Setter
    private LocalDateTime lastInteractionAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}