package com.future.floatie.pet;

import java.util.Map;

/**
 * Shared pet data — species, colours, and names.
 * Used by both {@code PetService} and {@code PixelArtService}.
 */
public final class PetConstants {

    private PetConstants() {}

    public static final String[] SUBCLASSES = {
        "cat", "dog", "fox", "rabbit", "axolotl", "frog", "penguin", "parrot"
    };

    public static final String[] COLORS = {
        "#FF8C69", "#D4A574", "#E87A6B", "#6BB56B", "#D4B84A",
        "#F0C48C", "#B08A5C", "#E88080", "#7AB880", "#D4C08C"
    };

    public static final String[] ACCENT_COLORS = {
        "#FF6B8A", "#4ECDC4", "#FFE66D", "#FF8C42", "#A855F7",
        "#38BDF8", "#34D399", "#F472B6", "#FB923C", "#818CF8"
    };

    public static final Map<String, String[]> SPECIES_NAMES = Map.of(
        "cat",     new String[]{"Whiskers", "Luna", "Simba", "Mittens", "Shadow", "Cleo", "Felix", "Oliver"},
        "dog",     new String[]{"Buddy", "Max", "Bella", "Charlie", "Rocky", "Daisy", "Cooper", "Lola"},
        "fox",     new String[]{"Fennel", "Rusty", "Vulpes", "Blaze", "Ember", "Zorro", "Foxy", "Cinder"},
        "rabbit",  new String[]{"Fluffy", "Thumper", "Hoppy", "Cotton", "Snowball", "Bugs", "Bunny", "Clover"},
        "axolotl", new String[]{"Axel", "Loti", "Salamander", "Mudkip", "Aqua", "Axo", "Lottie", "Mochi"},
        "frog",    new String[]{"Kermit", "Croak", "Hopper", "Lily", "Toad", "Frogger", "Sprout", "Puddles"},
        "penguin", new String[]{"Pip", "Skipper", "Pebble", "Ice", "Flake", "Waddle", "Flipper", "Snow"},
        "parrot",  new String[]{"Rio", "Sky", "Rainbow", "Kiwi", "Coco", "Phoenix", "Tiki", "Zazu"}
    );
}
