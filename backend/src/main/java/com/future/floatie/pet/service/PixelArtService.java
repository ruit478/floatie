package com.future.floatie.pet.service;

import com.future.floatie.entity.Pet;
import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.PetExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class PixelArtService {

    private static final int SPRITE_SIZE = 128;
    private static final int PIXEL_SCALE = 4;
    private static final int GRID_SIZE = SPRITE_SIZE / PIXEL_SCALE;
    private static final Random random = new Random();

    private static final Map<String, Color> COLOR_PALETTE = Map.of(
            "#FF6B9D", new Color(255, 107, 157),
            "#4ECDC4", new Color(78, 205, 196),
            "#45B7D1", new Color(69, 183, 209),
            "#96CEB4", new Color(150, 206, 180),
            "#FFEAA7", new Color(255, 234, 167),
            "#DDA0DD", new Color(221, 160, 221),
            "#98D8C8", new Color(152, 216, 200),
            "#F7B787", new Color(247, 183, 135),
            "#B5EAD7", new Color(181, 234, 215),
            "#C7CEEA", new Color(199, 206, 234)
    );

    public BufferedImage generateRandomSprite() {
        log.debug("Generating random sprite");

        // Generate random attributes
        String subclass = getRandomSubclass();
        String colorHex = getRandomColor();
        String expression = getRandomExpression();
        int evolutionStage = 1; // Base stage

        log.debug("Random attributes: subclass={}, color={}, expression={}", subclass, colorHex, expression);

        // Generate sprite using existing methods
        int[][] pixelGrid = getSpeciesTemplate(subclass);
        Color petColor = COLOR_PALETTE.getOrDefault(colorHex, Color.GRAY);
        pixelGrid = applyColorOverlay(pixelGrid, petColor);
        pixelGrid = applyExpression(pixelGrid, PetExpression.valueOf(expression));

        return renderPixelGridToImage(pixelGrid);
    }

    private String getRandomSubclass() {
        String[] subclasses = {"cat", "dog", "fox", "rabbit", "axolotl", "frog", "penguin", "parrot"};
        return subclasses[random.nextInt(subclasses.length)];
    }

    private String getRandomColor() {
        String[] colors = {"#FF6B9D", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7"};
        return colors[random.nextInt(colors.length)];
    }

    private String getRandomExpression() {
        String[] expressions = {"HAPPY", "SAD", "MAD", "SLEEPY", "CONFUSED"};
        return expressions[random.nextInt(expressions.length)];
    }

    /**
     * Generate a pet sprite for a new user
     */
    public BufferedImage generatePetSprite(Pet pet) {
        log.debug("Generating sprite for pet: id={}, subclass={}, evolution={}",
                pet.getId(), pet.getSubclass(), pet.getEvolutionPath());

        long startTime = System.currentTimeMillis();

        int[][] pixelGrid = getSpeciesTemplate(pet.getSubclass());
        log.trace("Species template generated for: {}", pet.getSubclass());

        Color petColor = COLOR_PALETTE.getOrDefault(pet.getColorHex(), Color.GRAY);
        pixelGrid = applyColorOverlay(pixelGrid, petColor);

        pixelGrid = applyExpression(pixelGrid, pet.getExpression());

        if (pet.getEvolutionStage() > 1 && pet.getEvolutionPath() != null) {
            log.debug("Applying evolution features: path={}, stage={}",
                    pet.getEvolutionPath(), pet.getEvolutionStage());
            pixelGrid = applyEvolutionFeatures(pixelGrid, pet.getEvolutionPath(), pet.getEvolutionStage());
        }

        BufferedImage result = renderPixelGridToImage(pixelGrid);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Sprite generated in {}ms for pet: {}", duration, pet.getId());

        return result;
    }

    /**
     * Get species template as 32x32 grid of color indices
     */
    private int[][] getSpeciesTemplate(String subclass) {
        switch (subclass.toLowerCase()) {
            case "axolotl":
                return getAxolotlTemplate();
            case "cat":
                return getCatTemplate();
            case "dog":
                return getDogTemplate();
            case "fox":
                return getFoxTemplate();
            case "rabbit":
                return getRabbitTemplate();
            case "frog":
                return getFrogTemplate();
            case "penguin":
                return getPenguinTemplate();
            case "parrot":
                return getParrotTemplate();
            case "goldfish":
                return getGoldfishTemplate();
            case "snail":
                return getSnailTemplate();
            default:
                log.warn("Unknown subclass '{}', using default template", subclass);
                return getDefaultTemplate();
        }
    }

    private int[][] getAxolotlTemplate() {
        int[][] template = new int[GRID_SIZE][GRID_SIZE];

        for (int y = 10; y < 26; y++) {
            for (int x = 8; x < 24; x++) {
                double dx = (x - 16) / 8.0;
                double dy = (y - 18) / 6.0;
                if (dx * dx + dy * dy <= 1.0) {
                    template[y][x] = 1;
                }
            }
        }

        for (int y = 6; y < 16; y++) {
            for (int x = 10; x < 22; x++) {
                double dx = (x - 16) / 7.0;
                double dy = (y - 11) / 5.0;
                if (dx * dx + dy * dy <= 0.9) {
                    template[y][x] = 1;
                }
            }
        }

        int[][] gillPositions = {{12, 6}, {14, 5}, {16, 5}, {18, 5}, {20, 6}};
        for (int[] pos : gillPositions) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (Math.abs(i) + Math.abs(j) <= 1) {
                        int x = pos[0] + i;
                        int y = pos[1] + j;
                        if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
                            template[y][x] = 2;
                        }
                    }
                }
            }
        }

        return template;
    }

    private int[][] getCatTemplate() {
        int[][] template = new int[GRID_SIZE][GRID_SIZE];

        for (int y = 12; y < 28; y++) {
            for (int x = 8; x < 24; x++) {
                double dx = (x - 16) / 8.0;
                double dy = (y - 20) / 8.0;
                if (dx * dx + dy * dy <= 0.9) {
                    template[y][x] = 1;
                }
            }
        }

        for (int y = 4; y < 16; y++) {
            for (int x = 10; x < 22; x++) {
                double dx = (x - 16) / 7.0;
                double dy = (y - 10) / 6.0;
                if (dx * dx + dy * dy <= 0.85) {
                    template[y][x] = 1;
                }
            }
        }

        template[2][12] = 1;
        template[2][13] = 1;
        template[3][11] = 1;
        template[3][12] = 1;
        template[2][19] = 1;
        template[2][20] = 1;
        template[3][20] = 1;
        template[3][21] = 1;

        return template;
    }

    private int[][] getDogTemplate() {
        int[][] template = new int[GRID_SIZE][GRID_SIZE];

        for (int y = 12; y < 26; y++) {
            for (int x = 8; x < 24; x++) {
                double dx = (x - 16) / 9.0;
                double dy = (y - 19) / 7.0;
                if (dx * dx + dy * dy <= 0.9) {
                    template[y][x] = 1;
                }
            }
        }

        for (int y = 4; y < 16; y++) {
            for (int x = 9; x < 23; x++) {
                double dx = (x - 16) / 8.0;
                double dy = (y - 10) / 6.0;
                if (dx * dx + dy * dy <= 0.8) {
                    template[y][x] = 1;
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            template[2 + i][9] = 1;
            template[2 + i][22] = 1;
        }

        return template;
    }

    private int[][] applyColorOverlay(int[][] grid, Color petColor) {
        int[][] coloredGrid = new int[GRID_SIZE][GRID_SIZE];

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int value = grid[y][x];
                if (value == 1) {
                    coloredGrid[y][x] = encodeColor(petColor);
                } else if (value == 2) {
                    Color lighter = new Color(
                            Math.min(255, petColor.getRed() + 30),
                            Math.min(255, petColor.getGreen() + 30),
                            Math.min(255, petColor.getBlue() + 30)
                    );
                    coloredGrid[y][x] = encodeColor(lighter);
                } else {
                    coloredGrid[y][x] = value;
                }
            }
        }

        return coloredGrid;
    }

    private int[][] applyExpression(int[][] grid, PetExpression expression) {
        int[][] result = copyGrid(grid);

        switch (expression) {
            case HAPPY:
                result[8][13] = encodeColor(Color.WHITE);
                result[8][14] = encodeColor(Color.BLACK);
                result[8][18] = encodeColor(Color.WHITE);
                result[8][19] = encodeColor(Color.BLACK);
                for (int x = 14; x <= 18; x++) {
                    result[12][x] = encodeColor(Color.BLACK);
                }
                result[11][15] = encodeColor(Color.BLACK);
                result[11][17] = encodeColor(Color.BLACK);
                break;

            case SAD:
                result[8][13] = encodeColor(Color.WHITE);
                result[9][14] = encodeColor(Color.BLACK);
                result[8][18] = encodeColor(Color.WHITE);
                result[9][19] = encodeColor(Color.BLACK);
                result[12][14] = encodeColor(Color.BLACK);
                result[12][18] = encodeColor(Color.BLACK);
                result[11][15] = encodeColor(Color.BLACK);
                result[11][17] = encodeColor(Color.BLACK);
                break;

            case MAD:
                result[7][13] = encodeColor(Color.BLACK);
                result[7][14] = encodeColor(Color.BLACK);
                result[7][18] = encodeColor(Color.BLACK);
                result[7][19] = encodeColor(Color.BLACK);
                result[8][13] = encodeColor(Color.WHITE);
                result[8][14] = encodeColor(Color.BLACK);
                result[8][18] = encodeColor(Color.WHITE);
                result[8][19] = encodeColor(Color.BLACK);
                result[11][14] = encodeColor(Color.BLACK);
                result[11][16] = encodeColor(Color.BLACK);
                result[11][17] = encodeColor(Color.BLACK);
                break;

            case SLEEPY:
                result[8][13] = encodeColor(Color.BLACK);
                result[8][14] = encodeColor(Color.BLACK);
                result[8][18] = encodeColor(Color.BLACK);
                result[8][19] = encodeColor(Color.BLACK);
                result[11][16] = encodeColor(Color.BLACK);
                break;

            case CONFUSED:
                result[7][13] = encodeColor(Color.WHITE);
                result[7][14] = encodeColor(Color.BLACK);
                result[9][18] = encodeColor(Color.WHITE);
                result[9][19] = encodeColor(Color.BLACK);
                result[11][15] = encodeColor(Color.BLACK);
                result[12][16] = encodeColor(Color.BLACK);
                result[11][17] = encodeColor(Color.BLACK);
                break;
        }

        return result;
    }

    private int[][] applyEvolutionFeatures(int[][] grid, EvolutionPath path, int stage) {
        int[][] result = copyGrid(grid);

        switch (path) {
            case PERFECT:
                for (int y = 0; y < GRID_SIZE; y++) {
                    for (int x = 0; x < GRID_SIZE; x++) {
                        if (grid[y][x] != 0 && grid[y][x] != -1) {
                            Color original = decodeColor(grid[y][x]);
                            Color glowing = new Color(
                                    Math.min(255, original.getRed() + 20 * stage),
                                    Math.min(255, original.getGreen() + 20 * stage),
                                    Math.min(255, original.getBlue() + 10 * stage)
                            );
                            result[y][x] = encodeColor(glowing);
                        }
                    }
                }
                break;

            case OVERWEIGHT:
                if (stage >= 2) {
                    for (int y = 0; y < GRID_SIZE; y++) {
                        for (int x = 0; x < GRID_SIZE; x++) {
                            if (grid[y][x] != 0 && grid[y][x] != -1) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    for (int dx = -1; dx <= 1; dx++) {
                                        int ny = y + dy;
                                        int nx = x + dx;
                                        if (ny >= 0 && ny < GRID_SIZE && nx >= 0 && nx < GRID_SIZE) {
                                            if (result[ny][nx] == 0) {
                                                result[ny][nx] = grid[y][x];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;

            case NEGLECTED:
                int patches = 10 * stage;
                for (int i = 0; i < patches; i++) {
                    int x = 5 + random.nextInt(22);
                    int y = 5 + random.nextInt(22);
                    if (result[y][x] != 0) {
                        result[y][x] = encodeColor(Color.DARK_GRAY);
                    }
                }
                break;

            default:
                log.trace("No evolution features applied for path: {}", path);
                break;
        }

        return result;
    }

    private BufferedImage renderPixelGridToImage(int[][] grid) {
        BufferedImage image = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int value = grid[y][x];
                if (value != 0 && value != -1) {
                    Color color = decodeColor(value);
                    g2d.setColor(color);
                    g2d.fillRect(x * PIXEL_SCALE, y * PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);
                }
            }
        }

        // Retro grid lines
        g2d.setColor(new Color(0, 0, 0, 30));
        for (int i = 0; i <= SPRITE_SIZE; i += PIXEL_SCALE) {
            g2d.drawLine(i, 0, i, SPRITE_SIZE);
            g2d.drawLine(0, i, SPRITE_SIZE, i);
        }

        g2d.dispose();
        return image;
    }

    private int encodeColor(Color color) {
        return (color.getRGB() & 0xFFFFFF) | 0xFF000000;
    }

    private Color decodeColor(int encoded) {
        return new Color(encoded, true);
    }

    private int[][] copyGrid(int[][] original) {
        int[][] copy = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, GRID_SIZE);
        }
        return copy;
    }

    // Additional species templates
    private int[][] getFoxTemplate() { return getCatTemplate(); }
    private int[][] getRabbitTemplate() { return getCatTemplate(); }
    private int[][] getFrogTemplate() { return getAxolotlTemplate(); }
    private int[][] getPenguinTemplate() { return getDefaultTemplate(); }
    private int[][] getParrotTemplate() { return getDefaultTemplate(); }
    private int[][] getGoldfishTemplate() { return getDefaultTemplate(); }
    private int[][] getSnailTemplate() { return getDefaultTemplate(); }

    private int[][] getDefaultTemplate() {
        int[][] template = new int[GRID_SIZE][GRID_SIZE];
        for (int y = 8; y < 24; y++) {
            for (int x = 8; x < 24; x++) {
                double dx = (x - 16) / 8.0;
                double dy = (y - 16) / 8.0;
                if (dx * dx + dy * dy <= 0.9) {
                    template[y][x] = 1;
                }
            }
        }
        return template;
    }
}