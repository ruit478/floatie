package com.future.floatie.pet.enums;

/**
 * Ordered life stages. Progression is monotonic — a pet never regresses to
 * an earlier stage. Advancement is driven by age (game days since creation)
 * or level (XP level-ups), whichever reaches the threshold first.
 * {@code DEAD} is terminal; no interactions are allowed once reached.
 */
public enum LifeStage {
    EGG,
    BABY,
    CHILD,
    TEEN,
    ADULT,
    ELDER,
    DEAD
}
