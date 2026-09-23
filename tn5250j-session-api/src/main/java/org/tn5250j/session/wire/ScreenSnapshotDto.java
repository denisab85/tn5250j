package org.tn5250j.session.wire;

import java.util.Map;

public final class ScreenSnapshotDto {

    private int rows;
    private int cols;
    private int currentRow;
    private int currentCol;
    private boolean cursorActive;
    private boolean usingGuiInterface;
    private String allocatedDeviceName;
    private OiaStateDto oia;
    private Map<String, String> planes;

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    public int getCurrentCol() {
        return currentCol;
    }

    public void setCurrentCol(int currentCol) {
        this.currentCol = currentCol;
    }

    public boolean isCursorActive() {
        return cursorActive;
    }

    public void setCursorActive(boolean cursorActive) {
        this.cursorActive = cursorActive;
    }

    public boolean isUsingGuiInterface() {
        return usingGuiInterface;
    }

    public void setUsingGuiInterface(boolean usingGuiInterface) {
        this.usingGuiInterface = usingGuiInterface;
    }

    public String getAllocatedDeviceName() {
        return allocatedDeviceName;
    }

    public void setAllocatedDeviceName(String allocatedDeviceName) {
        this.allocatedDeviceName = allocatedDeviceName;
    }

    public OiaStateDto getOia() {
        return oia;
    }

    public void setOia(OiaStateDto oia) {
        this.oia = oia;
    }

    public Map<String, String> getPlanes() {
        return planes;
    }

    public void setPlanes(Map<String, String> planes) {
        this.planes = planes;
    }
}
