package com.angelo.mmorpg.debug;

public class DebugState {

    private boolean debugPanelVisible = false;
    private boolean gridVisible       = false;

    public void toggleDebugPanel() { debugPanelVisible = !debugPanelVisible; }
    public void toggleGrid()       { gridVisible       = !gridVisible; }

    public boolean isDebugPanelVisible() { return debugPanelVisible; }
    public boolean isGridVisible()       { return gridVisible; }
}