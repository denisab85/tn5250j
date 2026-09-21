package org.tn5250j.session.client.local;

import org.tn5250j.event.ScreenListener;
import org.tn5250j.framework.tn5250.Rect;
import org.tn5250j.framework.tn5250.Screen5250;
import org.tn5250j.session.api.OiaModel;
import org.tn5250j.session.api.ScreenArea;
import org.tn5250j.session.api.ScreenModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
public final class LocalScreenModel implements ScreenModel, ScreenListener {

    private final Screen5250 screen;
    private final LocalOiaModel oiaModel;
    private final List<org.tn5250j.session.api.ScreenListener> listeners = new ArrayList<>();

    public LocalScreenModel(Screen5250 screen) {
        this.screen = screen;
        this.oiaModel = new LocalOiaModel(screen.getOIA());
        screen.addScreenListener(this);
    }

    public Screen5250 getScreen5250() {
        return screen;
    }

    @Override
    public int getRows() {
        return screen.getRows();
    }

    @Override
    public int getColumns() {
        return screen.getColumns();
    }

    @Override
    public int getScreenLength() {
        return screen.getScreenLength();
    }

    @Override
    public int getCurrentRow() {
        return screen.getCurrentRow();
    }

    @Override
    public int getCurrentCol() {
        return screen.getCurrentCol();
    }

    @Override
    public int getCurrentPos() {
        return screen.getCurrentPos();
    }

    @Override
    public int getRow(int pos) {
        return screen.getRow(pos);
    }

    @Override
    public int getCol(int pos) {
        return screen.getCol(pos);
    }

    @Override
    public int getPos(int row, int col) {
        return screen.getPos(row, col);
    }

    @Override
    public boolean isCursorActive() {
        return screen.isCursorActive();
    }

    @Override
    public void setCursorActive(boolean activate) {
        screen.setCursorActive(activate);
    }

    @Override
    public boolean isUsingGuiInterface() {
        return screen.isUsingGuiInterface();
    }

    @Override
    public void setUseGUIInterface(boolean gui) {
        screen.setUseGUIInterface(gui);
    }

    @Override
    public void toggleGUIInterface() {
        screen.toggleGUIInterface();
    }

    @Override
    public void setResetRequired(boolean reset) {
        screen.setResetRequired(reset);
    }

    @Override
    public void setBackspaceError(boolean onError) {
        screen.setBackspaceError(onError);
    }

    @Override
    public boolean moveCursor(int pos) {
        return screen.moveCursor(pos);
    }

    @Override
    public void setCursor(int row, int col) {
        screen.setCursor(row, col);
    }

    @Override
    public void sendKeys(String text) {
        screen.sendKeys(text);
    }

    @Override
    public void pasteText(String content, boolean special) {
        screen.pasteText(content, special);
    }

    @Override
    public void sendAid(int aidKey) {
        screen.sendAid(aidKey);
    }

    @Override
    public void repaintScreen() {
        screen.repaintScreen();
    }

    @Override
    public boolean checkHotSpots() {
        return screen.checkHotSpots();
    }

    @Override
    public int GetScreen(char[] buffer, int bufferLength, int plane) {
        return screen.GetScreen(buffer, bufferLength, plane);
    }

    @Override
    public int GetScreenRect(char[] buffer, int bufferLength, int startRow, int startCol,
                            int endRow, int endCol, int plane) {
        return screen.GetScreenRect(buffer, bufferLength, startRow, startCol, endRow, endCol, plane);
    }

    @Override
    public char[] getScreenAsChars() {
        return screen.getScreenAsChars();
    }

    @Override
    public OiaModel getOia() {
        return oiaModel;
    }

    @Override
    public void addScreenListener(org.tn5250j.session.api.ScreenListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeScreenListener(org.tn5250j.session.api.ScreenListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onScreenChanged(int inUpdate, int startRow, int startCol, int endRow, int endCol) {
        for (org.tn5250j.session.api.ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenChanged(inUpdate, startRow, startCol, endRow, endCol);
        }
    }

    @Override
    public void onScreenSizeChanged(int rows, int cols) {
        for (org.tn5250j.session.api.ScreenListener listener : new ArrayList<>(listeners)) {
            listener.onScreenSizeChanged(rows, cols);
        }
    }

    @Override
    public String copyText(ScreenArea area) {
        return screen.copyText(toRect(area));
    }

    @Override
    public String copyTextField(int position) {
        return screen.copyTextField(position);
    }

    @Override
    public List<Double> sumThem(boolean formatOption, ScreenArea area) {
        Vector<Double> sums = screen.sumThem(formatOption, toRect(area));
        return new ArrayList<>(sums);
    }

    @Override
    public boolean isInField(int pos, boolean chgToField) {
        return screen.isInField(pos, chgToField);
    }

    private static Rect toRect(ScreenArea area) {
        Rect rect = new Rect();
        rect.setBounds(area.getStartRow(), area.getStartCol(),
                area.getEndCol() - area.getStartCol() + 1,
                area.getEndRow() - area.getStartRow() + 1);
        return rect;
    }
}
