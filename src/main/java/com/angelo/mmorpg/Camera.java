package com.angelo.mmorpg;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    private Vector3f position  = new Vector3f(0.0f, 1.0f, 3.0f);
    private Vector3f front     = new Vector3f(0.0f, 0.0f, -1.0f);
    private Vector3f up        = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw   = -90.0f;
    private float pitch = 0.0f;
    private float speed = 2.5f;

    private final float fov;
    private final float aspectRatio;

    public Camera(float fov, int screenWidth, int screenHeight) {
        this.fov = fov;
        this.aspectRatio = (float) screenWidth / screenHeight;
    }

    public void processKeyboard(boolean forward, boolean backward, boolean left, boolean right, float deltaTime) {
        float velocity = speed * deltaTime;

        if (forward)   position.add(new Vector3f(front).mul(velocity));
        if (backward)  position.sub(new Vector3f(front).mul(velocity));
        if (left)      position.sub(new Vector3f(front).cross(up, new Vector3f()).normalize().mul(velocity));
        if (right)     position.add(new Vector3f(front).cross(up, new Vector3f()).normalize().mul(velocity));
    }

    public void processMouse(float xOffset, float yOffset) {
        float sensitivity = 0.1f;
        yaw   += xOffset * sensitivity;
        pitch += yOffset * sensitivity;

        if (pitch >  89.0f) pitch =  89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        Vector3f direction = new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float)  Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
        ).normalize();

        front.set(direction);
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.1f, 100.0f);
    }
}