package org.tn5250j.debug;

import java.awt.Color;

/**
 * Describes how Swing paints the 5250 cursor ({@link org.tn5250j.GuiGraphicBuffer#drawCursor}).
 * The cursor is drawn in XOR mode: {@code colorCursor} XOR {@code colorBg}.
 */
public final class SwingCursorPresentation {

    public final String style;
    public final String color;
    public final String xorBase;
    public final String effectiveColor;

    public SwingCursorPresentation(String style, Color cursorColor, Color xorBaseColor) {
        this.style = style;
        this.color = colorName(cursorColor);
        this.xorBase = colorName(xorBaseColor);
        this.effectiveColor = colorName(xorColor(cursorColor, xorBaseColor));
    }

    static Color xorColor(Color cursorColor, Color xorBaseColor) {
        return new Color(
                cursorColor.getRed() ^ xorBaseColor.getRed(),
                cursorColor.getGreen() ^ xorBaseColor.getGreen(),
                cursorColor.getBlue() ^ xorBaseColor.getBlue());
    }

    static String colorName(Color color) {
        int rgb = color.getRGB() & 0xFFFFFF;
        switch (rgb) {
            case 0x000000:
                return "black";
            case 0xFFFFFF:
                return "white";
            case 0xFF0000:
                return "red";
            case 0x00FF00:
                return "green";
            case 0x0000FF:
                return "blue";
            case 0x00FFFF:
                return "cyan";
            case 0xFFFF00:
                return "yellow";
            case 0xFF00FF:
                return "magenta";
            default:
                return String.format("#%06x", rgb);
        }
    }

    public static String cursorStyle(int cursorSize) {
        switch (cursorSize) {
            case 0:
                return "line";
            case 1:
                return "half";
            case 2:
                return "block";
            default:
                return "line";
        }
    }
}
