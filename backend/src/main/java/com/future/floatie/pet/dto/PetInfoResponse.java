package com.future.floatie.pet.dto;

import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.LifeStage;
import com.future.floatie.pet.enums.PetExpression;
import com.future.floatie.entity.Pet;

import java.util.UUID;

public record PetInfoResponse(
        UUID id,
        String name,
        String classType,
        String subclass,
        String colorHex,
        PetExpression expression,
        String spriteBase64,

        // Stats
        int hunger,
        int happiness,
        int energy,
        int health,
        int hygiene,
        int weight,

        // Progression
        int xp,
        int level,
        LifeStage lifeStage,
        int ageDays,
        int evolutionStage,
        EvolutionPath evolutionPath,

        // State
        boolean isSick,
        boolean isAsleep,
        int bondLevel,
        String personality
) {
    public static PetInfoResponse from(Pet pet, String spriteBase64) {
        return new PetInfoResponse(
                pet.getId(),
                pet.getName(),
                pet.getClassType(),
                pet.getSubclass(),
                pet.getColorHex(),
                pet.getExpression(),
                spriteBase64,
                pet.getHunger(),
                pet.getHappiness(),
                pet.getEnergy(),
                pet.getHealth(),
                pet.getHygiene(),
                pet.getWeight(),
                pet.getXp(),
                pet.getLevel(),
                pet.getLifeStage(),
                pet.getAgeDays(),
                pet.getEvolutionStage(),
                pet.getEvolutionPath(),
                pet.getIsSick(),
                pet.getIsAsleep(),
                pet.getBondLevel(),
                pet.getPersonality()

        );
    }
}