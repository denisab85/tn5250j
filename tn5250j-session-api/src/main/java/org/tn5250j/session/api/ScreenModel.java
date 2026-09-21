package org.tn5250j.session.api;

import java.util.List;

/**
 * Read and input surface for a 5250 screen, usable locally or over the wire.
 */
public interface ScreenModel {
    int getRows();

    int getColumns();

    int getScreenLength();

    int getCurrentRow();

    int getCurrentCol();

    int getCurrentPos();

    int getRow(int pos);

    int getCol(int pos);

    int getPos(int row, int col);

    boolean isCursorActive();

    void setCursorActive(boolean activate);

    boolean isUsingGuiInterface();

    void setUseGUIInterface(boolean gui);

    void toggleGUIInterface();

    void setResetRequired(boolean reset);

    void setBackspaceError(boolean onError);

    boolean moveCursor(int pos);

    void setCursor(int row, int col);

    void sendKeys(String text);

    void pasteText(String content, boolean special);

    void sendAid(int aidKey);

    void repaintScreen();

    boolean checkHotSpots();

    int GetScreen(char[] buffer, int bufferLength, int plane);

    int GetScreenRect(char[] buffer, int bufferLength, int startRow, int startCol,
                      int endRow, int endCol, int plane);

    char[] getScreenAsChars();

    OiaModel getOia();

    void addScreenListener(ScreenListener listener);

    void removeScreenListener(ScreenListener listener);

    String copyText(ScreenArea area);

    String copyTextField(int position);

    List<Double> sumThem(boolean formatOption, ScreenArea area);

    boolean isInField(int pos, boolean chgToField);
}