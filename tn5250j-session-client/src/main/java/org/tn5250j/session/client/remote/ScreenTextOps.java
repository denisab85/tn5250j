package org.tn5250j.session.client.remote;

import org.tn5250j.session.api.ScreenArea;
import org.tn5250j.session.api.ScreenPlaneConstants;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

final class ScreenTextOps {

    private static final char EXTENDED_5250_NON_DSP = 0x01;

    private ScreenTextOps() {
    }

    static String copyText(ScreenFrameBuffer buffer, ScreenArea area) {
        StringBuilder sb = new StringBuilder();
        int row = area.getStartRow();
        while (row <= area.getEndRow()) {
            int col = area.getStartCol();
            while (col <= area.getEndCol()) {
                int pos = buffer.getPos(row - 1, col - 1);
                char c = charAt(buffer, ScreenPlaneConstants.PLANE_TEXT, pos);
                char extended = charAt(buffer, ScreenPlaneConstants.PLANE_EXTENDED, pos);
                if (c >= ' ' && (extended & EXTENDED_5250_NON_DSP) == 0) {
                    sb.append(c);
                } else {
                    sb.append(' ');
                }
                col++;
            }
            sb.append('\n');
            row++;
        }
        return sb.toString();
    }

    static List<Double> sumThem(ScreenFrameBuffer buffer, boolean formatOption, ScreenArea area) {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        if (formatOption) {
            dfs.setDecimalSeparator('.');
            dfs.setGroupingSeparator(',');
        } else {
            dfs.setDecimalSeparator(',');
            dfs.setGroupingSeparator('.');
        }
        df.setDecimalFormatSymbols(dfs);

        List<Double> sums = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int row = area.getStartRow();
        while (row <= area.getEndRow()) {
            int col = area.getStartCol();
            while (col <= area.getEndCol()) {
                int pos = buffer.getPos(row - 1, col - 1);
                char c = charAt(buffer, ScreenPlaneConstants.PLANE_TEXT, pos);
                if ((c >= '0' && c <= '9') || c == '.' || c == ',' || c == '-') {
                    sb.append(c);
                }
                col++;
            }
            if (sb.length() > 0) {
                if (sb.charAt(sb.length() - 1) == '-') {
                    sb.insert(0, '-');
                    sb.deleteCharAt(sb.length() - 1);
                }
                try {
                    sums.add(df.parse(sb.toString()).doubleValue());
                } catch (ParseException ignored) {
                }
            }
            sb.setLength(0);
            row++;
        }
        return sums;
    }

    static boolean isInField(ScreenFrameBuffer buffer, int pos) {
        if (pos < 0 || pos >= buffer.getScreenLength()) {
            return false;
        }
        char field = charAt(buffer, ScreenPlaneConstants.PLANE_FIELD, pos);
        return field != 0 && field != ' ';
    }

    private static char charAt(ScreenFrameBuffer buffer, int plane, int pos) {
        char[] scratch = new char[buffer.getScreenLength()];
        buffer.getScreen(scratch, scratch.length, plane);
        if (pos >= scratch.length) {
            return 0;
        }
        return scratch[pos];
    }
}
