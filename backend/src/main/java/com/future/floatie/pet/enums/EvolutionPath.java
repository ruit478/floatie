package com.future.floatie.pet.enums;

/**
 * Evolution path determined by stat quality at evolution-trigger levels.
 * Set by {@code PetService#determineEvolutionPath} based on stat thresholds.
 * Path affects the sprite visually (glow, widen, patches).
 */
public enum EvolutionPath {
    PERFECT,
    WELL_RAISED,
    NEGLECTED,
    OVERWEIGHT,
}