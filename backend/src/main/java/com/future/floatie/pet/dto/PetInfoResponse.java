package com.future.floatie.pet.dto;

import com.future.floatie.pet.enums.AccessoryType;
import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.LifeStage;
import com.future.floatie.pet.enums.PetExpression;
import com.future.floatie.entity.Pet;

import java.util.UUID;

/**
 * Full pet state sent to the frontend. The {@code spriteBase64} field
 * contains the PNG encoded as a Base64 data URI string — decoded and
 * displayed directly in the game component.
 *
 * <p>{@link #from(Pet, String)} maps every entity field explicitly so
 * the response shape is stable even if the entity gains internal columns.
 */
public record PetInfoResponse(
        UUID id,
        String name,
        String subclass,
        String colorHex,
        PetExpression expression,
        AccessoryType accessoryType,
        String accessoryColorHex,
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
        boolean isAsleep,
        int bondLevel
) {
    public static PetInfoResponse from(Pet pet, String spriteBase64) {
        return new PetInfoResponse(
                pet.getId(),
                pet.getName(),
                pet.getSubclass(),
                pet.getColorHex(),
                pet.getExpression(),
                pet.getAccessoryType(),
                pet.getAccessoryColorHex(),
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
                pet.getIsAsleep(),
                pet.getBondLevel()

        );
    }
}