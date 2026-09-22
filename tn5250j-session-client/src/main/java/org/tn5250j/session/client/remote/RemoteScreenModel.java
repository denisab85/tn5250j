package org.tn5250j.session.client.remote;

import org.tn5250j.session.api.OiaModel;
import org.tn5250j.session.api.ScreenArea;
import org.tn5250j.session.api.ScreenListener;
import org.tn5250j.session.api.ScreenModel;
import org.tn5250j.session.wire.ScreenSnapshotDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class RemoteScreenModel implements ScreenModel {

    private final ScreenFrameBuffer buffer = new ScreenFrameBuffer();
    private final RemoteOiaModel oiaModel = new RemoteOiaModel();
    private final List<ScreenListener> listeners = new ArrayList<>();
    private final Consumer<String> sendKeysFn;
    private final Consumer<Integer> moveCursorFn;
    private final Consumer<Integer> sendAidFn;
    private Runnable repaintFn;

    RemoteScreenModel(Consumer<String> sendKeysFn, Consumer<Integer> moveCursorFn,
                      Consumer<Integer> sendAidFn, Runnable repaintFn) {
        this.sendKeysFn = sendKeysFn;
        this.moveCursorFn = moveCursorFn;
        this.sendAidFn = sendAidFn;
        this.repaintFn = repaintFn;
    }

    void applySnapshot(ScreenSnapshotDto snapshot) {
        buffer.applySnapshot(snapshot);
        oiaModel.apply(snapshot.getOia());
        notifyFullScreen(1);
        repaintFn.run();
    }

    void setRepaintFn(Runnable repaintFn) {
        this.repaintFn = repaintFn != null ? repaintFn : () -> { };
    }

    private void notifyFullScreen(int inUpdate) {
        if (listeners.isEmpty()) {
            return;
        }
        int endRow = Math.max(buffer.getRows() - 1, 0);
        int endCol = Math.max(buffer.getCols() - 1, 0);
        for (ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenChanged(inUpdate, 0, 0, endRow, endCol);
        }
    }

    void applyRegion(int inUpdate, int startRow, int startCol, int endRow, int endCol,
                       int currentRow, int currentCol, boolean cursorActive, Map<String, String> planes) {
        buffer.applyRegion(startRow, startCol, endRow, endCol, currentRow, currentCol, cursorActive, planes);
        for (ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenChanged(inUpdate, startRow, startCol, endRow, endCol);
        }
        repaintFn.run();
    }

    void applySize(int rows, int cols) {
        buffer.resize(rows, cols);
        for (ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenSizeChanged(rows, cols);
        }
    }

    RemoteOiaModel getRemoteOiaModel() {
        return oiaModel;
    }

    @Override
    public int getRows() {
        return buffer.getRows();
    }

    @Override
    public int getColumns() {
        return buffer.getCols();
    }

    @Override
    public int getScreenLength() {
        return buffer.getScreenLength();
    }

    @Override
    public int getCurrentRow() {
        return buffer.getCurrentRow();
    }

    @Override
    public int getCurrentCol() {
        return buffer.getCurrentCol();
    }

    @Override
    public int getCurrentPos() {
        return buffer.getPos(getCurrentRow() - 1, getCurrentCol() - 1);
    }

    @Override
    public int getRow(int pos) {
        return buffer.getRow(pos);
    }

    @Override
    public int getCol(int pos) {
        return buffer.getCol(pos);
    }

    @Override
    public int getPos(int row, int col) {
        return buffer.getPos(row, col);
    }

    @Override
    public boolean isCursorActive() {
        return buffer.isCursorActive();
    }

    @Override
    public void setCursorActive(boolean activate) {
        buffer.setCursorActive(activate);
    }

    @Override
    public boolean isUsingGuiInterface() {
        return buffer.isUsingGuiInterface();
    }

    @Override
    public void setUseGUIInterface(boolean gui) {
        buffer.setUsingGuiInterface(gui);
    }

    @Override
    public void toggleGUIInterface() {
        buffer.setUsingGuiInterface(!buffer.isUsingGuiInterface());
    }

    @Override
    public void setResetRequired(boolean reset) {
    }

    @Override
    public void setBackspaceError(boolean onError) {
    }

    @Override
    public boolean moveCursor(int pos) {
        moveCursorFn.accept(pos);
        return true;
    }

    @Override
    public void setCursor(int row, int col) {
        moveCursorFn.accept(getPos(row - 1, col - 1));
    }

    @Override
    public void sendKeys(String text) {
        sendKeysFn.accept(text);
    }

    @Override
    public void pasteText(String content, boolean special) {
        sendKeysFn.accept(content);
    }

    @Override
    public void sendAid(int aidKey) {
        sendAidFn.accept(aidKey);
    }

    @Override
    public void repaintScreen() {
        repaintFn.run();
    }

    @Override
    public boolean checkHotSpots() {
        return false;
    }

    @Override
    public int GetScreen(char[] bufferArr, int bufferLength, int plane) {
        return buffer.getScreen(bufferArr, bufferLength, plane);
    }

    @Override
    public int GetScreenRect(char[] bufferArr, int bufferLength, int startRow, int startCol,
                             int endRow, int endCol, int plane) {
        // ScreenModel.GetScreenRect uses 1-based row/col (same as Screen5250); buffer is 0-based.
        return buffer.getScreenRect(bufferArr, bufferLength, startRow - 1, startCol - 1,
                endRow - 1, endCol - 1, plane);
    }

    @Override
    public char[] getScreenAsChars() {
        char[] chars = new char[buffer.getScreenLength()];
        buffer.getScreen(chars, chars.length, org.tn5250j.session.api.ScreenPlaneConstants.PLANE_TEXT);
        return chars;
    }

    @Override
    public OiaModel getOia() {
        return oiaModel;
    }

    @Override
    public void addScreenListener(ScreenListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeScreenListener(ScreenListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String copyText(ScreenArea area) {
        return ScreenTextOps.copyText(buffer, area);
    }

    @Override
    public String copyTextField(int position) {
        return "";
    }

    @Override
    public List<Double> sumThem(boolean formatOption, ScreenArea area) {
        return ScreenTextOps.sumThem(buffer, formatOption, area);
    }

    @Override
    public boolean isInField(int pos, boolean chgToField) {
        return ScreenTextOps.isInField(buffer, pos);
    }
}
