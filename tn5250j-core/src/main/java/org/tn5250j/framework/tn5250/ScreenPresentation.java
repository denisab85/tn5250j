package org.tn5250j.framework.tn5250;

import org.tn5250j.TN5250jConstants;

/**
 * Shared 5250 presentation rules for translating screen planes into user-visible output.
 */
public final class ScreenPresentation {

    private ScreenPresentation() {
    }

    public static boolean isUnderline5250Attr(int attr) {
        switch (attr & 0xFF) {
            case 36:
            case 37:
            case 38:
            case 44:
            case 45:
            case 46:
            case 52:
            case 53:
            case 54:
            case 60:
            case 61:
            case 62:
                return true;
            default:
                return false;
        }
    }

    public static boolean isCellUnderlined(char attrPlane, char extendedPlane) {
        return (extendedPlane & TN5250jConstants.EXTENDED_5250_UNDERLINE) != 0
                || isUnderline5250Attr(attrPlane);
    }

    public static char underlineExtendedBits(char attrPlane, char extendedPlane) {
        if ((extendedPlane & TN5250jConstants.EXTENDED_5250_UNDERLINE) != 0) {
            return extendedPlane;
        }
        if (isUnderline5250Attr(attrPlane)) {
            return (char) (extendedPlane | TN5250jConstants.EXTENDED_5250_UNDERLINE);
        }
        return extendedPlane;
    }
}
