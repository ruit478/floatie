package com.future.floatie.entity;

import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.LifeStage;
import com.future.floatie.pet.enums.PetExpression;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pets")
@Getter
@Setter
public class Pet {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 50)
    private String name;

    @Column(name = "class", nullable = false, length = 50)
    private String classType;  // PetClass enum as String

    @Column(nullable = false, length = 50)
    private String subclass;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetExpression expression;

    @Column(name = "sprite_url", nullable = true, length = 512)
    private String spriteUrl;

    // Core Stats (0-100)
    @Column(nullable = false)
    private Integer hunger = 50;

    @Column(nullable = false)
    private Integer happiness = 70;

    @Column(nullable = false)
    private Integer energy = 80;

    @Column(nullable = false)
    private Integer health = 100;

    @Column(nullable = false)
    private Integer hygiene = 80;

    @Column(nullable = false)
    private Integer weight = 50;

    @Enumerated(EnumType.STRING)
    @Column(name = "life_stage", nullable = false, length = 20)
    private LifeStage lifeStage;

    @Column(name = "age_days", nullable = false)
    private Integer ageDays = 0;

    @Column(name = "hatched_at")
    private LocalDateTime hatchedAt;

    // Progression
    @Column(nullable = false)
    private Integer xp = 0;

    @Column(nullable = false)
    private Integer level = 1;

    // Personality
    @Column(length = 20)
    private String personality;

    @Column(name = "personality_locked", nullable = false)
    private Boolean personalityLocked = false;

    // Evolution
    @Enumerated(EnumType.STRING)
    @Column(name = "evolution_path", length = 20)
    private EvolutionPath evolutionPath = EvolutionPath.WELL_RAISED;

    @Column(name = "evolution_stage", nullable = false)
    private Integer evolutionStage = 1;

    // State Flags
    @Column(name = "is_sick", nullable = false)
    private Boolean isSick = false;

    @Column(name = "sick_days", nullable = false)
    private Integer sickDays = 0;

    @Column(name = "is_asleep", nullable = false)
    private Boolean isAsleep = false;

    @Column(name = "bond_level", nullable = false)
    private Integer bondLevel = 0;

    // Timestamps for decay calculations
    @Column(name = "last_fed_at")
    private LocalDateTime lastFedAt;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    @Column(name = "last_cleaned_at")
    private LocalDateTime lastCleanedAt;

    @Column(name = "last_bathed_at")
    private LocalDateTime lastBathedAt;

    @Column(name = "last_slept_at")
    private LocalDateTime lastSleptAt;

    @Column(name = "last_decay_calc")
    private LocalDateTime lastDecayCalc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // PrePersist and PreUpdate hooks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastDecayCalc = LocalDateTime.now();
        lastFedAt = LocalDateTime.now();
        lastPlayedAt = LocalDateTime.now();
        lastCleanedAt = LocalDateTime.now();
        lastBathedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}