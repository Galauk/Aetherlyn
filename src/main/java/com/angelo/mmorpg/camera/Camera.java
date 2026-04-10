package com.angelo.mmorpg.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Camera {

    private Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);

    private static final float DISTANCE   = 10.0f;
    private static final float PITCH      = 45.0f;
    private static final float YAW_STEP   = 45.0f; // graus por rotação

    // YAW agora é mutável — começa no ângulo clássico isométrico
    private float yaw = -135.0f;

    private final float aspectRatio;
    private float zoom = 5.0f;

    public Camera(int screenWidth, int screenHeight) {
        this.aspectRatio = (float) screenWidth / screenHeight;
    }

    public void setTarget(Vector3f target) {
        this.target.set(target);
    }

    private Vector3f getCameraPosition() {
        float pitchRad = (float) Math.toRadians(PITCH);
        float yawRad   = (float) Math.toRadians(yaw);

        return new Vector3f(
                target.x + DISTANCE * (float) (Math.cos(pitchRad) * Math.cos(yawRad)),
                target.y + DISTANCE * (float)  Math.sin(pitchRad),
                target.z + DISTANCE * (float) (Math.cos(pitchRad) * Math.sin(yawRad))
        );
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(
                getCameraPosition(),
                target,
                new Vector3f(0.0f, 1.0f, 0.0f)
        );
    }

    public Matrix4f getProjectionMatrix() {
        float h = zoom;
        float w = zoom * aspectRatio;
        return new Matrix4f().ortho(-w, w, -h, h, 0.1f, 100.0f);
    }

    public Vector3f screenToWorld(float screenX, float screenY, int screenW, int screenH) {
        float ndcX =  (2.0f * screenX / screenW) - 1.0f;
        float ndcY = -(2.0f * screenY / screenH) + 1.0f;

        Matrix4f invPV = new Matrix4f(getProjectionMatrix()).mul(getViewMatrix()).invert();

        Vector4f nearClip = invPV.transform(new Vector4f(ndcX, ndcY, -1.0f, 1.0f));
        Vector4f farClip  = invPV.transform(new Vector4f(ndcX, ndcY,  1.0f, 1.0f));

        nearClip.div(nearClip.w);
        farClip.div(farClip.w);

        Vector3f rayOrigin = new Vector3f(nearClip.x, nearClip.y, nearClip.z);
        Vector3f rayDir    = new Vector3f(farClip.x - nearClip.x,
                farClip.y - nearClip.y,
                farClip.z - nearClip.z).normalize();

        if (Math.abs(rayDir.y) < 1e-6f) return null;
        float t = -rayOrigin.y / rayDir.y;
        if (t < 0) return null;

        return new Vector3f(
                rayOrigin.x + t * rayDir.x,
                0.0f,
                rayOrigin.z + t * rayDir.z
        );
    }

    public Vector3f getTarget()         { return target; }
    public float    getZoom()           { return zoom; }
    public void     setZoom(float zoom) { this.zoom = Math.max(1.0f, Math.min(zoom, 20.0f)); }

    /** Rotaciona a câmera 45° no sentido horário (tecla E). */
    public void rotateRight() { yaw = (yaw + YAW_STEP) % 360f; }

    /** Rotaciona a câmera 45° no sentido anti-horário (tecla Q). */
    public void rotateLeft()  { yaw = (yaw - YAW_STEP + 360f) % 360f; }

    public float getYaw() { return yaw; }
}