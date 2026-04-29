package com.angelo.mmorpg.debug;

/**
 * Flags do modo debug.
 * Grid e vision radius só ficam disponíveis quando debugMode estiver ativo.
 */
public class DebugState {

    private boolean debugMode    = false; // F3
    private boolean gridVisible  = false; // Ctrl+G (só quando debugMode)
    private boolean visionVisible = false; // Ctrl+F (só quando debugMode)

    public void toggleDebugMode()  {
        debugMode = !debugMode;
        // Desativa sub-flags ao sair do debug mode
        if (!debugMode) { gridVisible = false; visionVisible = false; }
    }

    public void toggleGrid() {
        if (debugMode) gridVisible = !gridVisible;
    }

    public void toggleVision() {
        if (debugMode) visionVisible = !visionVisible;
    }

    public boolean isDebugMode()     { return debugMode; }
    public boolean isGridVisible()   { return gridVisible; }
    public boolean isVisionVisible() { return visionVisible; }

    // Retrocompatibilidade
    public boolean isDebugPanelVisible() { return debugMode; }
}