package com.angelo.mmorpg.debug;

import org.joml.Vector3f;

public class DebugInfo {

    public float fps     = 0;
    public float tps     = 0;
    public float playerX = 0;
    public float playerZ = 0;
    public float targetX = Float.NaN;
    public float targetZ = Float.NaN;
    public float zoom    = 0;

    public void update(float fps, float tps, Vector3f playerPos, Vector3f clickTarget, float zoom) {
        this.fps     = fps;
        this.tps     = tps;
        this.playerX = playerPos.x;
        this.playerZ = playerPos.z;
        this.zoom    = zoom;

        if (clickTarget != null) {
            this.targetX = clickTarget.x;
            this.targetZ = clickTarget.z;
        }
    }

    public String[] lines() {
        String target = Float.isNaN(targetX)
                ? "Destino: ---"
                : String.format("Destino:  X=%.1f  Z=%.1f", targetX, targetZ);

        return new String[]{
                String.format("FPS:      %.0f  |  TPS: %.0f",  fps, tps),
                String.format("Player:   X=%.2f  Z=%.2f",      playerX, playerZ),
                target,
                String.format("Zoom:     %.1f",                zoom),
        };
    }
}