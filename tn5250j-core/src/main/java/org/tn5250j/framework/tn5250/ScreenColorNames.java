package org.tn5250j.framework.tn5250;

/**
 * Human-readable names for 5250 color indices stored in the color plane.
 */
public final class ScreenColorNames {

    private ScreenColorNames() {
    }

    public static String foregroundName(int index) {
        switch (index) {
            case 0:
                return "black";
            case 1:
                return "blue";
            case 2:
                return "green";
            case 3:
                return "cyan";
            case 4:
                return "red";
            case 5:
                return "magenta";
            case 6:
                return "yellow";
            case 7:
                return "white";
            case 8:
                return "gray";
            case 9:
                return "lightBlue";
            case 0xA:
                return "lightGreen";
            case 0xB:
                return "lightCyan";
            case 0xC:
                return "lightRed";
            case 0xD:
                return "lightMagenta";
            case 0xE:
                return "brown";
            case 0xF:
                return "whiteHigh";
            default:
                return "unknown(" + index + ")";
        }
    }

    public static String backgroundName(int index) {
        switch (index) {
            case 0:
                return "black";
            case 1:
                return "blue";
            case 2:
                return "green";
            case 3:
                return "cyan";
            case 4:
                return "red";
            case 5:
                return "magenta";
            case 6:
                return "yellow";
            case 7:
                return "white";
            default:
                return "unknown(" + index + ")";
        }
    }

    public static int foregroundIndex(char colorPlane) {
        return colorPlane & 0xFF;
    }

    public static int backgroundIndex(char colorPlane) {
        return (colorPlane & 0xFF00) >> 8;
    }
}
