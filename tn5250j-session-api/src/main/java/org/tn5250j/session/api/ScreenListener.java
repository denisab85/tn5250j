package org.tn5250j.session.api;

public interface ScreenListener {

    void onScreenChanged(int inUpdate, int startRow, int startCol, int endRow, int endCol);

    void onScreenSizeChanged(int rows, int cols);
}
