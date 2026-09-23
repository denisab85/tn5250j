package org.tn5250j.session.client.remote;

import org.tn5250j.session.api.OiaModel;
import org.tn5250j.session.api.ScreenArea;
import org.tn5250j.session.api.ScreenListener;
import org.tn5250j.session.api.ScreenModel;
import org.tn5250j.session.wire.ScreenSnapshotDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;

final class RemoteScreenModel implements ScreenModel {

    private final ScreenFrameBuffer buffer = new ScreenFrameBuffer();
    private final RemoteOiaModel oiaModel = new RemoteOiaModel();
    private final List<ScreenListener> listeners = new ArrayList<>();
    private final Consumer<String> sendKeysFn;
    private final Consumer<Integer> moveCursorFn;
    private final Consumer<Integer> sendAidFn;
    private final BiConsumer<String, Boolean> screenOptionFn;
    private final BiConsumer<String, Boolean> pasteTextFn;
    private final IntFunction<String> copyTextFieldFn;
    private final BooleanSupplier checkHotSpotsFn;
    private Runnable repaintFn;

    RemoteScreenModel(Consumer<String> sendKeysFn, Consumer<Integer> moveCursorFn,
                      Consumer<Integer> sendAidFn, Runnable repaintFn) {
        this(sendKeysFn, moveCursorFn, sendAidFn, repaintFn, null, null, null, null);
    }

    RemoteScreenModel(Consumer<String> sendKeysFn, Consumer<Integer> moveCursorFn,
                      Consumer<Integer> sendAidFn, Runnable repaintFn,
                      BiConsumer<String, Boolean> screenOptionFn) {
        this(sendKeysFn, moveCursorFn, sendAidFn, repaintFn, screenOptionFn, null, null, null);
    }

    RemoteScreenModel(Consumer<String> sendKeysFn, Consumer<Integer> moveCursorFn,
                      Consumer<Integer> sendAidFn, Runnable repaintFn,
                      BiConsumer<String, Boolean> screenOptionFn,
                      BiConsumer<String, Boolean> pasteTextFn,
                      IntFunction<String> copyTextFieldFn,
                      BooleanSupplier checkHotSpotsFn) {
        this.sendKeysFn = sendKeysFn;
        this.moveCursorFn = moveCursorFn;
        this.sendAidFn = sendAidFn;
        this.repaintFn = repaintFn;
        this.screenOptionFn = screenOptionFn;
        this.pasteTextFn = pasteTextFn;
        this.copyTextFieldFn = copyTextFieldFn;
        this.checkHotSpotsFn = checkHotSpotsFn;
    }

    void applySnapshot(ScreenSnapshotDto snapshot) {
        boolean keepCursorActive = buffer.isCursorActive() && !snapshot.isCursorActive();
        buffer.applySnapshot(snapshot);
        if (keepCursorActive) {
            buffer.setCursorActive(true);
        }
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
        int prevRow = buffer.getCurrentRow();
        int prevCol = buffer.getCurrentCol();
        boolean prevActive = buffer.isCursorActive();
        boolean keepCursorActive = prevActive && !cursorActive;
        buffer.applyRegion(inUpdate, startRow, startCol, endRow, endCol,
                currentRow, currentCol, cursorActive, planes);
        if (keepCursorActive) {
            buffer.setCursorActive(true);
        }
        for (ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenChanged(inUpdate, startRow, startCol, endRow, endCol);
        }
        if (inUpdate != 3 && inUpdate != 4) {
            relayCursorEventsIfNeeded(prevRow, prevCol, prevActive);
        }
        repaintFn.run();
    }

    /**
     * Region updates carry cursor metadata but not {@code inUpdate=3} paint events.
     * Mirror {@code Screen5250.goto_XY()} by relaying cursor events at the old and new
     * cells when position changes, so {@code GuiGraphicBuffer} can XOR-clear the prior cell.
     */
    private void relayCursorEventsIfNeeded(int prevRow, int prevCol, boolean prevActive) {
        int newRow = buffer.getCurrentRow();
        int newCol = buffer.getCurrentCol();
        boolean newActive = buffer.isCursorActive();
        if (!newActive && prevActive) {
            fireCursorEvent(prevRow - 1, prevCol - 1);
            return;
        }
        if (!newActive) {
            return;
        }
        if (prevActive && (prevRow != newRow || prevCol != newCol)) {
            fireCursorEvent(prevRow - 1, prevCol - 1);
        }
        if (!prevActive || prevRow != newRow || prevCol != newCol) {
            fireCursorEvent(newRow - 1, newCol - 1);
        }
    }

    private void fireCursorEvent(int row, int col) {
        for (ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenChanged(3, row, col, row, col);
        }
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
        sendScreenOption("setResetRequired", reset);
    }

    @Override
    public void setBackspaceError(boolean onError) {
        sendScreenOption("setBackspaceError", onError);
    }

    private void sendScreenOption(String option, boolean value) {
        if (screenOptionFn != null) {
            screenOptionFn.accept(option, value);
        }
    }

    @Override
    public boolean moveCursor(int pos) {
        moveCursorFn.accept(pos);
        return true;
    }

    @Override
    public void setCursor(int row, int col) {
        buffer.setCurrentPosition(row, col);
        moveCursorFn.accept(getPos(row - 1, col - 1));
    }

    @Override
    public void sendKeys(String text) {
        sendKeysFn.accept(text);
    }

    @Override
    public void pasteText(String content, boolean special) {
        if (pasteTextFn != null) {
            pasteTextFn.accept(content, special);
            return;
        }
        ScreenBufferOps.pasteText(buffer, content, special);
        repaintFn.run();
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
        if (checkHotSpotsFn != null) {
            return checkHotSpotsFn.getAsBoolean();
        }
        return ScreenBufferOps.checkHotSpots(buffer);
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
        if (copyTextFieldFn != null) {
            return copyTextFieldFn.apply(position);
        }
        return ScreenBufferOps.copyTextField(buffer, position);
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
