package com.future.floatie.pet.service;

import com.future.floatie.entity.Pet;
import com.future.floatie.pet.enums.EvolutionPath;
import com.future.floatie.pet.enums.PetExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates 32×32 native-resolution pixel-art sprites and upscales them
 * to 128×128 with nearest-neighbour interpolation.
 *
 * Every pixel maps to exactly one entry in a strict 7-colour palette.
 * No anti-aliasing, no gradients, no sub-pixel rendering.
 *
 * Sprite sheets use a fixed frame grid (one row, N columns, 32×32 per frame).
 */
@Service
@Slf4j
public class PixelArtService {

    private static final int NATIVE = 32;
    private static final int OUTPUT = 128;
    private static final int SCALE  = OUTPUT / NATIVE; // 4

    // ── Palette indices (5-7 colours total per palette) ──────────────
    private static final byte _T = 0; // transparent
    private static final byte _O = 1; // outline
    private static final byte _B = 2; // body (base colour)
    private static final byte _H = 3; // highlight
    private static final byte _S = 4; // shadow
    private static final byte _W = 5; // white
    private static final byte _E = 6; // eye / pupil
    private static final byte _D = 7; // detail (pink blush / inner ear)

    // Fixed palette entries (shared across all sprites)
    private static final Color C_OUTLINE = new Color(0x2D, 0x1A, 0x0E);
    private static final Color C_WHITE   = new Color(0xFF, 0xFF, 0xFF);
    private static final Color C_EYE     = new Color(0x1A, 0x1A, 0x1A);
    private static final Color C_DETAIL  = new Color(0xFF, 0x99, 0x99);

    private static final Random RNG = new Random();

    // ═══════════════════════════════════════════════════════════════════
    //  Public API
    // ═══════════════════════════════════════════════════════════════════

    /** Single-frame sprite for initial pet creation / evolution. */
    public BufferedImage generatePetSprite(Pet pet) {
        long t0 = System.currentTimeMillis();

        Palette pal = Palette.fromHex(pet.getColorHex());
        PixelCanvas c = buildTemplate(pet.getSubclass());
        applyExpression(c, pet.getExpression());
        if (pet.getEvolutionStage() > 1 && pet.getEvolutionPath() != null) {
            applyEvolution(c, pet.getEvolutionPath(), pet.getEvolutionStage());
        }
        addOutline(c);
        BufferedImage img = render(c, pal);

        log.debug("Sprite generated in {}ms for pet {}", System.currentTimeMillis() - t0, pet.getId());
        return img;
    }

    /** Random single-frame sprite. */
    public BufferedImage generateRandomSprite() {
        String subclass   = PetService.SUBCLASSES[RNG.nextInt(PetService.SUBCLASSES.length)];
        String colorHex   = PetService.COLORS[RNG.nextInt(PetService.COLORS.length)];
        PetExpression expr = PetExpression.values()[RNG.nextInt(PetExpression.values().length)];

        Palette pal = Palette.fromHex(colorHex);
        PixelCanvas c = buildTemplate(subclass);
        applyExpression(c, expr);
        addOutline(c);
        return render(c, pal);
    }

    /**
     * Generates a sprite sheet with idle + walk animation frames laid out
     * in a single row: [idle0 idle1 idle2 idle3 walk0 walk1 walk2 walk3].
     * Each frame is 32×32 native, output at 128×128 per cell.
     * Total sheet: 8 frames × 128px = 1024×128.
     */
    public BufferedImage generateSpriteSheet(Pet pet) {
        Palette pal = Palette.fromHex(pet.getColorHex());
        PixelCanvas base = buildTemplate(pet.getSubclass());
        applyExpression(base, pet.getExpression());
        if (pet.getEvolutionStage() > 1 && pet.getEvolutionPath() != null) {
            applyEvolution(base, pet.getEvolutionPath(), pet.getEvolutionStage());
        }

        List<PixelCanvas> frames = new ArrayList<>(8);
        frames.addAll(generateIdleFrames(base));
        frames.addAll(generateWalkFrames(base));

        int cols = frames.size();
        BufferedImage sheet = new BufferedImage(OUTPUT * cols, OUTPUT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = sheet.createGraphics();
        disableSmoothing(g2);
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(0, 0, OUTPUT * cols, OUTPUT);

        for (int i = 0; i < frames.size(); i++) {
            PixelCanvas frame = frames.get(i);
            addOutline(frame);
            BufferedImage img = render(frame, pal);
            g2.drawImage(img, i * OUTPUT, 0, null);
        }
        g2.dispose();
        return sheet;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Palette
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Immutable 7-colour palette. Index 0 is always transparent.
     * Every pixel in the sprite maps to exactly one of these entries.
     */
    private record Palette(Color outline, Color body, Color highlight,
                           Color shadow, Color white, Color eye, Color detail) {

        Color get(byte idx) {
            return switch (idx) {
                case _O -> outline;
                case _B -> body;
                case _H -> highlight;
                case _S -> shadow;
                case _W -> white;
                case _E -> eye;
                case _D -> detail;
                default -> null;
            };
        }

        static Palette fromHex(String hex) {
            Color base = parseHex(hex);
            return new Palette(
                C_OUTLINE,
                base,
                lighter(base),
                darker(base),
                C_WHITE,
                C_EYE,
                C_DETAIL
            );
        }

        private static Color lighter(Color c) {
            return new Color(Math.min(255, c.getRed() + 40), Math.min(255, c.getGreen() + 40), Math.min(255, c.getBlue() + 40));
        }

        private static Color darker(Color c) {
            return new Color(Math.max(0, c.getRed() - 45), Math.max(0, c.getGreen() - 45), Math.max(0, c.getBlue() - 45));
        }

        private static Color parseHex(String hex) {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return new Color(Integer.parseInt(h, 16));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PixelCanvas — pixel-snapped drawing surface
    // ═══════════════════════════════════════════════════════════════════

    private static class PixelCanvas {
        final byte[][] g = new byte[NATIVE][NATIVE];

        PixelCanvas() {}

        PixelCanvas(PixelCanvas src) {
            for (int y = 0; y < NATIVE; y++) System.arraycopy(src.g[y], 0, g[y], 0, NATIVE);
        }

        void set(int x, int y, byte v) {
            if (x >= 0 && x < NATIVE && y >= 0 && y < NATIVE) g[y][x] = v;
        }

        byte get(int x, int y) {
            return (x >= 0 && x < NATIVE && y >= 0 && y < NATIVE) ? g[y][x] : _T;
        }

        // ── pixel-snapped primitives ──

        void fillRect(int x, int y, int w, int h, byte v) {
            for (int py = y; py < y + h; py++)
                for (int px = x; px < x + w; px++)
                    set(px, py, v);
        }

        /** Pixel-snapped filled ellipse with integer radii. */
        void fillEllipse(int cx, int cy, int rx, int ry, byte v) {
            if (rx <= 0 || ry <= 0) return;
            int rx2 = rx * rx, ry2 = ry * ry;
            for (int py = cy - ry; py <= cy + ry; py++)
                for (int px = cx - rx; px <= cx + rx; px++) {
                    int dx = px - cx, dy = py - cy;
                    if ((long) dx * dx * ry2 + (long) dy * dy * rx2 <= (long) rx2 * ry2)
                        set(px, py, v);
                }
        }

        /** Shift all non-transparent pixels by (dx, dy). Pixels moved off-canvas are lost. */
        void shift(int dx, int dy) {
            byte[][] copy = new byte[NATIVE][NATIVE];
            for (int y = 0; y < NATIVE; y++)
                for (int x = 0; x < NATIVE; x++)
                    if (g[y][x] != _T) {
                        int ny = y + dy, nx = x + dx;
                        if (ny >= 0 && ny < NATIVE && nx >= 0 && nx < NATIVE)
                            copy[ny][nx] = g[y][x];
                    }
            for (int y = 0; y < NATIVE; y++) System.arraycopy(copy[y], 0, g[y], 0, NATIVE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Outline pass
    // ═══════════════════════════════════════════════════════════════════

    /** Solid 1px dark outline around every non-transparent cluster (8-directional). */
    private static void addOutline(PixelCanvas c) {
        // First pass: record which cells are body (pre-outline)
        boolean[][] body = new boolean[NATIVE][NATIVE];
        for (int y = 0; y < NATIVE; y++)
            for (int x = 0; x < NATIVE; x++)
                body[y][x] = c.g[y][x] != _T && c.g[y][x] != _O;

        // Second pass: place outline pixels in empty cells adjacent to body
        for (int y = 0; y < NATIVE; y++)
            for (int x = 0; x < NATIVE; x++) {
                if (!body[y][x]) continue;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < NATIVE && ny >= 0 && ny < NATIVE && c.g[ny][nx] == _T)
                            c.g[ny][nx] = _O;
                    }
            }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Render
    // ═══════════════════════════════════════════════════════════════════

    private static BufferedImage render(PixelCanvas c, Palette pal) {
        BufferedImage img = new BufferedImage(OUTPUT, OUTPUT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        disableSmoothing(g2);
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(0, 0, OUTPUT, OUTPUT);

        for (int y = 0; y < NATIVE; y++)
            for (int x = 0; x < NATIVE; x++) {
                Color color = pal.get(c.g[y][x]);
                if (color != null) {
                    g2.setColor(color);
                    g2.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
                }
            }

        g2.dispose();
        return img;
    }

    private static void disableSmoothing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Animation frames
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 4-frame idle animation: subtle body bob (1-2px vertical shift).
     * Frame 0: neutral, Frame 1: up 1px, Frame 2: neutral, Frame 3: down 1px.
     */
    static List<PixelCanvas> generateIdleFrames(PixelCanvas base) {
        List<PixelCanvas> frames = new ArrayList<>(4);
        int[] offsets = {0, -1, 0, 1};
        for (int off : offsets) {
            PixelCanvas f = new PixelCanvas(base);
            f.shift(0, off);
            frames.add(f);
        }
        return frames;
    }

    /**
     * 4-frame walk cycle with alternating leg positions.
     * Leg area (rows 23-29) is shifted left/right in opposite phases
     * to simulate a walk. Body bounces slightly.
     */
    static List<PixelCanvas> generateWalkFrames(PixelCanvas base) {
        List<PixelCanvas> frames = new ArrayList<>(4);

        // Frame 0: neutral stance
        frames.add(new PixelCanvas(base));

        // Frame 1: left leg forward, slight body rise
        PixelCanvas f1 = new PixelCanvas(base);
        shiftRegion(f1, 23, 29, true, 1);   // right side down → shift right leg
        f1.shift(0, -1);                     // body bounce up
        frames.add(f1);

        // Frame 2: neutral stance
        frames.add(new PixelCanvas(base));

        // Frame 3: right leg forward, slight body rise
        PixelCanvas f3 = new PixelCanvas(base);
        shiftRegion(f3, 23, 29, false, 1);  // left side down → shift left leg
        f3.shift(0, -1);                     // body bounce up
        frames.add(f3);

        return frames;
    }

    /**
     * Shifts pixels in a horizontal row range. When {@code rightSide} is true,
     * only pixels at x >= 16 are shifted; otherwise pixels at x < 16 are shifted.
     */
    private static void shiftRegion(PixelCanvas c, int yStart, int yEnd, boolean rightSide, int dx) {
        byte[][] orig = new byte[NATIVE][NATIVE];
        for (int y = yStart; y <= yEnd; y++) System.arraycopy(c.g[y], 0, orig[y], 0, NATIVE);

        for (int y = yStart; y <= yEnd; y++) {
            for (int x = 0; x < NATIVE; x++) {
                boolean match = rightSide ? (x >= 16) : (x < 16);
                if (match && orig[y][x] != _T) {
                    c.g[y][x] = _T;
                    int nx = x + dx;
                    if (nx >= 0 && nx < NATIVE) c.g[y][nx] = orig[y][x];
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Expression overlays
    // ═══════════════════════════════════════════════════════════════════

    private static void applyExpression(PixelCanvas c, PetExpression expr) {
        switch (expr) {
            case HAPPY -> {
                // Curved-up dot eyes
                c.set(12, 8, _W); c.set(13, 8, _E); c.set(14, 8, _W);
                c.set(18, 8, _W); c.set(19, 8, _E); c.set(20, 8, _W);
                // Smile — 1px line
                for (int x = 14; x <= 18; x++) c.set(x, 13, _E);
                // Blush
                c.set(11, 10, _D); c.set(21, 10, _D);
            }
            case SAD -> {
                c.set(12, 8, _W); c.set(13, 8, _E); c.set(13, 9, _E);
                c.set(19, 8, _W); c.set(20, 8, _E); c.set(20, 9, _E);
                // Frown
                c.set(14, 13, _E); c.set(15, 12, _E); c.set(16, 12, _E); c.set(17, 12, _E); c.set(18, 13, _E);
            }
            case MAD -> {
                // Angry brows
                c.set(12, 7, _E); c.set(13, 7, _E);
                c.set(19, 7, _E); c.set(20, 7, _E);
                // Eyes
                c.set(12, 8, _W); c.set(13, 8, _E);
                c.set(19, 8, _W); c.set(20, 8, _E);
                // Mouth
                c.set(14, 12, _E); c.set(15, 12, _E); c.set(16, 12, _E); c.set(17, 12, _E); c.set(18, 12, _E);
                c.set(15, 11, _E); c.set(17, 11, _E);
            }
            case SLEEPY -> {
                // Closed/half-closed eyes
                c.set(12, 8, _E); c.set(13, 8, _E); c.set(14, 8, _E);
                c.set(18, 8, _E); c.set(19, 8, _E); c.set(20, 8, _E);
                // Small mouth
                c.set(15, 13, _E); c.set(16, 13, _E); c.set(17, 13, _E);
                // Blush
                c.set(11, 10, _D); c.set(21, 10, _D);
            }
            case CONFUSED -> {
                // Uneven eyes
                c.set(12, 8, _W); c.set(13, 8, _E);
                c.set(19, 9, _W); c.set(20, 9, _E);
                // Wobbly mouth
                c.set(14, 12, _E); c.set(15, 13, _E); c.set(16, 12, _E); c.set(17, 13, _E);
                // Question-like dot
                c.set(19, 6, _E);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Evolution effects
    // ═══════════════════════════════════════════════════════════════════

    private static void applyEvolution(PixelCanvas c, EvolutionPath path, int stage) {
        switch (path) {
            case PERFECT -> {
                float chance = 0.07f * stage;
                for (int y = 0; y < NATIVE; y++)
                    for (int x = 0; x < NATIVE; x++) {
                        byte v = c.g[y][x];
                        if (v == _B && RNG.nextFloat() < chance) c.g[y][x] = _H;
                        else if (v == _S && RNG.nextFloat() < chance) c.g[y][x] = _B;
                    }
            }
            case WELL_RAISED -> {
                for (int y = 0; y < NATIVE / 3; y++)
                    for (int x = 0; x < NATIVE; x++)
                        if (c.g[y][x] == _B) c.g[y][x] = _H;
            }
            case OVERWEIGHT -> {
                if (stage >= 2) {
                    // Expand body horizontally by 1px on each side
                    byte[][] orig = new byte[NATIVE][NATIVE];
                    for (int y = 0; y < NATIVE; y++) System.arraycopy(c.g[y], 0, orig[y], 0, NATIVE);
                    for (int y = 0; y < NATIVE; y++)
                        for (int x = 0; x < NATIVE; x++) {
                            byte v = orig[y][x];
                            if (v == _B || v == _S || v == _H || v == _W) {
                                c.set(x - 1, y, v);
                                c.set(x + 1, y, v);
                            }
                        }
                }
            }
            case NEGLECTED -> {
                int patches = 6 + stage * 5;
                for (int i = 0; i < patches; i++) {
                    int px = 4 + RNG.nextInt(NATIVE - 8);
                    int py = 4 + RNG.nextInt(NATIVE - 8);
                    if (c.g[py][px] == _B || c.g[py][px] == _H) {
                        c.g[py][px] = _S;
                        if (px + 1 < NATIVE && c.g[py][px + 1] == _B) c.g[py][px + 1] = _S;
                        if (py + 1 < NATIVE && c.g[py + 1][px] == _B) c.g[py + 1][px] = _S;
                    }
                }
            }
            default -> {}
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Species templates — procedurally built with pixel-snapped primitives
    // ═══════════════════════════════════════════════════════════════════

    private static PixelCanvas buildTemplate(String subclass) {
        return switch (subclass.toLowerCase()) {
            case "cat"     -> buildCat();
            case "dog"     -> buildDog();
            case "fox"     -> buildFox();
            case "rabbit"  -> buildRabbit();
            case "axolotl" -> buildAxolotl();
            case "frog"    -> buildFrog();
            case "penguin" -> buildPenguin();
            case "parrot"  -> buildParrot();
            default -> {
                log.warn("Unknown subclass '{}', using cat template", subclass);
                yield buildCat();
            }
        };
    }

    // ── Cat: chunky body, large round head (~40% height), stubby legs, upright tail ──
    private static PixelCanvas buildCat() {
        PixelCanvas c = new PixelCanvas();

        // Body — rounded rectangle
        c.fillEllipse(16, 20, 7, 6, _B);   // main body
        c.fillEllipse(16, 20, 6, 5, _B);   // fill centre
        // Head — large circle, ~12px tall
        c.fillEllipse(16, 10, 7, 6, _B);
        c.fillEllipse(16, 10, 7, 5, _B);
        // Ears — small triangles on top
        c.fillRect(11, 3, 3, 3, _B);  // left ear
        c.fillRect(18, 3, 3, 3, _B);  // right ear
        c.set(12, 2, _B); c.set(19, 2, _B); // ear tips
        // Inner ears
        c.set(12, 4, _D); c.set(19, 4, _D);
        // Head highlight
        for (int x = 12; x <= 20; x++) { c.set(x, 6, _H); c.set(x, 7, _H); }
        // White muzzle
        c.fillEllipse(16, 12, 3, 2, _W);
        // White belly/chest
        c.fillEllipse(16, 20, 4, 4, _W);
        // Eyes (placed by expression overlay)
        // Legs — stubby, 2px wide
        c.fillRect(12, 25, 2, 5, _B);  // left leg
        c.fillRect(18, 25, 2, 5, _B);  // right leg
        // Feet — slightly wider
        c.set(11, 28, _B); c.set(13, 28, _B); c.set(12, 29, _B);
        c.set(17, 28, _B); c.set(19, 28, _B); c.set(18, 29, _B);
        // Tail — upright, slight curve
        c.fillRect(24, 22, 2, 8, _B);
        c.set(25, 22, _B); c.set(25, 21, _B); // curve tip

        return c;
    }

    // ── Dog: broader body, floppy ears, wagging tail ──
    private static PixelCanvas buildDog() {
        PixelCanvas c = new PixelCanvas();
        // Body — wider than cat
        c.fillEllipse(16, 19, 8, 7, _B);
        // Head
        c.fillEllipse(16, 9, 7, 6, _B);
        // Floppy ears (hanging down on sides)
        c.fillRect(9, 6, 2, 6, _B);  // left ear
        c.fillRect(21, 6, 2, 6, _B);  // right ear
        // Head highlight
        for (int x = 13; x <= 19; x++) c.set(x, 5, _H);
        // Muzzle
        c.fillEllipse(16, 11, 3, 2, _W);
        // Chest/belly
        c.fillEllipse(16, 19, 4, 5, _W);
        // Legs — a bit longer than cat
        c.fillRect(12, 25, 2, 5, _B);
        c.fillRect(18, 25, 2, 5, _B);
        c.fillRect(11, 29, 3, 1, _B); // paws
        c.fillRect(18, 29, 3, 1, _B);
        // Tail — wagging up-right
        c.fillRect(24, 16, 2, 6, _B);
        c.set(24, 15, _B);

        return c;
    }

    // ── Fox: pointed ears, slender face, bushy tail ──
    private static PixelCanvas buildFox() {
        PixelCanvas c = new PixelCanvas();
        // Body — slightly slender
        c.fillEllipse(16, 19, 6, 7, _B);
        // Head — wider at cheeks
        c.fillEllipse(16, 9, 7, 5, _B);
        // Pointed ears (taller triangles)
        c.fillRect(11, 2, 2, 4, _B);  // left ear
        c.fillRect(19, 2, 2, 4, _B);  // right ear
        c.set(12, 1, _B); c.set(19, 1, _B); // tips
        // Inner ears
        c.set(12, 3, _D); c.set(19, 3, _D);
        // Head highlight
        for (int x = 13; x <= 19; x++) c.set(x, 5, _H);
        // White cheeks/muzzle
        c.fillEllipse(16, 11, 3, 2, _W);
        // White chest
        c.fillEllipse(16, 18, 3, 4, _W);
        // Legs — slim
        c.fillRect(12, 25, 2, 4, _B);
        c.fillRect(18, 25, 2, 4, _B);
        c.set(11, 29, _B); c.set(13, 29, _B);
        c.set(18, 29, _B); c.set(20, 29, _B);
        // Bushy tail — thick, sweeping right
        c.fillEllipse(25, 24, 4, 3, _B);
        c.fillRect(21, 22, 5, 4, _B);
        // White tail tip
        c.set(28, 24, _W); c.set(27, 23, _W); c.set(27, 25, _W);

        return c;
    }

    // ── Rabbit: long ears, round body, fluffy tail ──
    private static PixelCanvas buildRabbit() {
        PixelCanvas c = new PixelCanvas();
        // Body — round
        c.fillEllipse(16, 19, 6, 6, _B);
        // Head — round
        c.fillEllipse(16, 10, 5, 5, _B);
        // Long ears — 6px tall
        c.fillRect(11, 0, 3, 7, _B);  // left ear
        c.fillRect(18, 0, 3, 7, _B);  // right ear
        // Inner ears
        c.set(12, 1, _D); c.set(12, 2, _D); c.set(12, 3, _D);
        c.set(19, 1, _D); c.set(19, 2, _D); c.set(19, 3, _D);
        // Head highlight
        for (int x = 12; x <= 20; x++) c.set(x, 6, _H);
        // Muzzle / nose
        c.fillEllipse(16, 11, 2, 2, _W);
        c.set(16, 11, _D); // pink nose
        // White belly
        c.fillEllipse(16, 19, 4, 4, _W);
        // Legs
        c.fillRect(12, 24, 2, 5, _B);
        c.fillRect(18, 24, 2, 5, _B);
        c.fillRect(11, 28, 4, 1, _B); // big feet
        c.fillRect(17, 28, 4, 1, _B);
        // Fluffy tail — small white puff
        c.fillEllipse(23, 25, 2, 2, _W);

        return c;
    }

    // ── Axolotl: wide head, external gills, finned tail ──
    private static PixelCanvas buildAxolotl() {
        PixelCanvas c = new PixelCanvas();
        // Body — elongated
        c.fillEllipse(16, 19, 6, 7, _B);
        // Head — wide
        c.fillEllipse(16, 10, 8, 5, _B);
        // External gills (side branches)
        for (int y = 7; y <= 11; y++) { c.set(7, y, _D); c.set(25, y, _D); }
        c.set(6, 8, _D); c.set(6, 10, _D); c.set(26, 8, _D); c.set(26, 10, _D);
        // Head highlight
        for (int x = 11; x <= 21; x++) c.set(x, 7, _H);
        // White belly
        c.fillEllipse(16, 19, 3, 5, _W);
        // Tiny legs
        c.fillRect(10, 24, 2, 3, _B);
        c.fillRect(20, 24, 2, 3, _B);
        c.set(10, 27, _B); c.set(11, 27, _B); c.set(20, 27, _B); c.set(21, 27, _B);
        // Finned tail
        c.fillEllipse(16, 27, 4, 3, _B);
        c.fillEllipse(16, 29, 3, 2, _B);

        return c;
    }

    // ── Frog: wide body, bulging eyes on top, no tail ──
    private static PixelCanvas buildFrog() {
        PixelCanvas c = new PixelCanvas();
        // Body — wide oval
        c.fillEllipse(16, 18, 9, 6, _B);
        // Head — wide and flat
        c.fillEllipse(16, 11, 8, 4, _B);
        // Bulging eyes on top of head
        c.fillEllipse(11, 7, 3, 3, _B);
        c.fillEllipse(21, 7, 3, 3, _B);
        // Pupils
        c.fillRect(10, 7, 2, 2, _E);
        c.fillRect(20, 7, 2, 2, _E);
        // Highlight on top
        for (int x = 11; x <= 21; x++) c.set(x, 9, _H);
        // White belly
        c.fillEllipse(16, 18, 4, 4, _W);
        // Wide-set legs
        c.fillRect(7, 22, 3, 3, _B);
        c.fillRect(22, 22, 3, 3, _B);
        c.fillRect(5, 24, 3, 2, _B);  // splayed feet
        c.fillRect(24, 24, 3, 2, _B);

        return c;
    }

    // ── Penguin: tall oval body, flippers, beak, white belly ──
    private static PixelCanvas buildPenguin() {
        PixelCanvas c = new PixelCanvas();
        // Body — tall oval
        c.fillEllipse(16, 16, 7, 10, _B);
        // Head — continuous with body, slightly narrower
        c.fillEllipse(16, 7, 6, 5, _B);
        // White face/belly patch
        c.fillEllipse(16, 5, 4, 3, _W);
        c.fillEllipse(16, 14, 5, 8, _W);
        // Beak
        c.set(14, 7, _D); c.set(15, 7, _D); c.set(16, 7, _D); c.set(17, 7, _D);
        c.set(15, 8, _D); c.set(16, 8, _D);
        // Eyes
        c.set(13, 5, _E); c.set(14, 5, _E); c.set(18, 5, _E); c.set(19, 5, _E);
        // Flippers (side wings)
        c.fillRect(8, 14, 2, 6, _B);
        c.fillRect(22, 14, 2, 6, _B);
        // Feet
        c.fillRect(13, 27, 3, 2, _B);
        c.fillRect(17, 27, 3, 2, _B);

        return c;
    }

    // ── Parrot: crest, curved beak, wing detail, tail feathers ──
    private static PixelCanvas buildParrot() {
        PixelCanvas c = new PixelCanvas();
        // Body
        c.fillEllipse(16, 18, 6, 6, _B);
        // Head
        c.fillEllipse(16, 9, 5, 5, _B);
        // Crest (top feathers)
        c.fillRect(13, 2, 2, 4, _B);
        c.set(14, 1, _B); c.set(15, 1, _B);
        c.fillRect(17, 3, 2, 3, _B);
        // Head highlight
        for (int x = 13; x <= 19; x++) c.set(x, 6, _H);
        // Curved beak
        c.set(12, 8, _D); c.set(11, 9, _D); c.set(12, 9, _D);
        // Belly
        c.fillEllipse(16, 18, 3, 4, _W);
        // Wing detail (right side)
        c.fillEllipse(22, 17, 3, 4, _H);
        // Legs
        c.fillRect(14, 24, 2, 4, _B);
        c.fillRect(18, 24, 2, 4, _B);
        c.set(13, 28, _B); c.set(15, 28, _B); c.set(17, 28, _B); c.set(19, 28, _B);
        // Tail feathers
        c.fillRect(13, 26, 2, 5, _B);
        c.fillRect(16, 25, 2, 6, _B);
        c.fillRect(19, 26, 2, 5, _B);

        return c;
    }
}
