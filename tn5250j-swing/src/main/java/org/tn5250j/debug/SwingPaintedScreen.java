package org.tn5250j.debug;

import org.tn5250j.GuiGraphicBuffer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Captures the 5250 screen as it appears in {@link GuiGraphicBuffer}'s off-screen raster,
 * including XOR cursor artifacts and other paint-layer effects not stored in 5250 planes.
 */
public final class SwingPaintedScreen {

    private static final int COLOR_MATCH_TOLERANCE = 24;

    private SwingPaintedScreen() {
    }

    public static Snapshot capture(GuiGraphicBuffer buffer, SwingRenderedScreen.Snapshot logical) {
        return capture(buffer, logical, false);
    }

    public static Snapshot capture(GuiGraphicBuffer buffer, SwingRenderedScreen.Snapshot logical,
                                   boolean includeRaster) {
        BufferedImage raster = buffer.copyTextAreaImage();
        if (raster == null) {
            return empty(logical, includeRaster);
        }

        int rows = logical.rows;
        int cols = logical.cols;
        int size = rows * cols;
        int columnWidth = buffer.getColumnWidth();
        int rowHeight = buffer.getRowHeight();
        Color cursorEffective = logical.cursorPresentation == null
                ? Color.WHITE
                : SwingCursorPresentation.xorColor(
                        parseColorName(logical.cursorPresentation.color, Color.WHITE),
                        parseColorName(logical.cursorPresentation.xorBase, Color.BLACK));

        String[] foreground = new String[size];
        String[] background = new String[size];
        boolean[] cursorPainted = new boolean[size];
        boolean[] differsFromRender = new boolean[size];
        Color[] palette = buffer.getPaletteColors();

        for (int row = 0; row < rows; row++) {
            int backgroundSampleY = row * rowHeight + Math.max(2, rowHeight / 3);
            int characterSampleY = buffer.getCharacterSampleY(row);
            int cursorLineY = buffer.getCursorLineY(row);
            for (int col = 0; col < cols; col++) {
                int pos = row * cols + col;
                int centerX = col * columnWidth + (columnWidth / 2);

                Color sampledBackground = sampleRgb(raster, centerX, backgroundSampleY);
                Color sampledForeground = sampleRgb(raster, centerX, characterSampleY);
                Color sampledCursorLine = sampleRgb(raster, centerX, clampY(cursorLineY, raster.getHeight()));

                background[pos] = nearestColorName(sampledBackground, palette);
                foreground[pos] = nearestColorName(sampledForeground, palette);
                cursorPainted[pos] = isCursorLinePainted(sampledCursorLine, cursorEffective, palette);

                boolean renderCursor = isRenderCursorMarked(logical.render.cursor, row, col);
                differsFromRender[pos] = cursorPainted[pos] != renderCursor;
            }
        }

        String rasterBase64 = null;
        if (includeRaster) {
            rasterBase64 = encodePngBase64(raster);
        }

        return new Snapshot(
                logical.text,
                new PaintedInfo(
                        foreground,
                        background,
                        SwingRenderedScreen.formatBooleanGrid(cursorPainted, rows, cols),
                        SwingRenderedScreen.formatBooleanGrid(differsFromRender, rows, cols)),
                rasterBase64);
    }

    private static Snapshot empty(SwingRenderedScreen.Snapshot logical, boolean includeRaster) {
        int size = logical.rows * logical.cols;
        String[] emptyColors = new String[size];
        for (int i = 0; i < size; i++) {
            emptyColors[i] = "black";
        }
        return new Snapshot(
                logical.text,
                new PaintedInfo(
                        emptyColors,
                        emptyColors,
                        SwingRenderedScreen.formatBooleanGrid(new boolean[size], logical.rows, logical.cols),
                        SwingRenderedScreen.formatBooleanGrid(new boolean[size], logical.rows, logical.cols)),
                includeRaster ? "" : null);
    }

    static boolean isCursorLinePainted(Color sampledCursorLine, Color cursorEffective, Color[] palette) {
        if (colorsNear(sampledCursorLine, cursorEffective)) {
            return true;
        }
        // XOR cursor on green underline still reads as white/magenta, not green.
        return SwingCursorPresentation.colorName(sampledCursorLine).equals(
                SwingCursorPresentation.colorName(cursorEffective));
    }

    static boolean isRenderCursorMarked(String renderCursorGrid, int row, int col) {
        if (renderCursorGrid == null || renderCursorGrid.isEmpty()) {
            return false;
        }
        String[] rows = renderCursorGrid.split("\n", -1);
        if (row < 0 || row >= rows.length) {
            return false;
        }
        String line = rows[row];
        if (col < 0 || col >= line.length()) {
            return false;
        }
        return line.charAt(col) == '1';
    }

    static Color sampleRgb(BufferedImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return Color.BLACK;
        }
        return new Color(image.getRGB(x, y));
    }

    static String nearestColorName(Color sampled, Color[] palette) {
        Color nearest = palette[0];
        int bestDistance = Integer.MAX_VALUE;
        for (Color candidate : palette) {
            int distance = colorDistance(sampled, candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = candidate;
            }
        }
        return SwingCursorPresentation.colorName(nearest);
    }

    static boolean colorNameMatches(String left, String right) {
        return left != null && left.equals(right);
    }

    static boolean colorsNear(Color left, Color right) {
        return colorDistance(left, right) <= COLOR_MATCH_TOLERANCE;
    }

    static int colorDistance(Color left, Color right) {
        return Math.abs(left.getRed() - right.getRed())
                + Math.abs(left.getGreen() - right.getGreen())
                + Math.abs(left.getBlue() - right.getBlue());
    }

    static int clampY(int y, int height) {
        if (height <= 0) {
            return 0;
        }
        if (y < 0) {
            return 0;
        }
        if (y >= height) {
            return height - 1;
        }
        return y;
    }

    static Color parseColorName(String name, Color fallback) {
        if (name == null) {
            return fallback;
        }
        switch (name) {
            case "black":
                return Color.BLACK;
            case "white":
                return Color.WHITE;
            case "red":
                return Color.RED;
            case "green":
                return Color.GREEN;
            case "blue":
                return Color.BLUE;
            case "cyan":
                return Color.CYAN;
            case "yellow":
                return Color.YELLOW;
            case "magenta":
                return Color.MAGENTA;
            default:
                if (name.startsWith("#") && name.length() == 7) {
                    return Color.decode(name);
                }
                return fallback;
        }
    }

    static String encodePngBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException ex) {
            return "";
        }
    }

    public static final class PaintedInfo {
        public final String[] foreground;
        public final String[] background;
        public final String cursor;
        public final String differsFromRender;

        public PaintedInfo(String[] foreground, String[] background, String cursor, String differsFromRender) {
            this.foreground = foreground;
            this.background = background;
            this.cursor = cursor;
            this.differsFromRender = differsFromRender;
        }
    }

    public static final class Snapshot {
        public final String text;
        public final PaintedInfo painted;
        public final String rasterBase64;

        public Snapshot(String text, PaintedInfo painted, String rasterBase64) {
            this.text = text;
            this.painted = painted;
            this.rasterBase64 = rasterBase64;
        }
    }
}
