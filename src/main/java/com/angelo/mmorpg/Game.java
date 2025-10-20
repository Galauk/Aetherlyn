package com.angelo.mmorpg;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.*;
import org.lwjgl.system.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Game {
    private long window;
    private int shaderProgram;
    private int vao, vbo, ebo;
    private int texture;
    private Matrix4f projection, view;
    private Vector3f cameraPos = new Vector3f(0.0f, 1.0f, 3.0f);
    private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
    private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
    private float yaw = -90.0f, pitch = 0.0f;
    private double lastX = 400, lastY = 300;
    private boolean firstMouse = true;

    public void run() throws Exception {
        init();
        loop();
        cleanup();
    }

    private void init() throws Exception {
// Inicializa GLFW
        if (!glfwInit()) throw new IllegalStateException("Falha ao iniciar GLFW");
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        window = glfwCreateWindow(800, 600, "Aetherlyn: MMORPG 3D", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Falha ao criar janela");
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

// Configura callbacks
        glfwSetCursorPosCallback(window, this::mouseCallback);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

// Configura OpenGL
        glEnable(GL_DEPTH_TEST);

// Carrega textura
        texture = loadTexture("assets/grass.png");

// Configura shaders
        shaderProgram = createShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");

// Configura cubo
        float[] vertices = {
// Posições          TexCoords
                -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
                -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,
        };
        int[] indices = {
                0, 1, 2, 2, 3, 0,
                1, 5, 6, 6, 2, 1,
                5, 4, 7, 7, 6, 5,
                4, 0, 3, 3, 7, 4,
                3, 2, 6, 6, 7, 3,
                4, 5, 1, 1, 0, 4
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

// Configura matrizes
        projection = new Matrix4f().perspective((float) Math.toRadians(45.0), 800.0f / 600.0f, 0.1f, 100.0f);
        view = new Matrix4f().lookAt(cameraPos, cameraPos.add(cameraFront, new Vector3f()), cameraUp);
    }

    private int loadTexture(String path) {
        String absolutePath = new java.io.File("src/main/resources/" + path).getAbsolutePath();
        if (!new java.io.File(absolutePath).exists()) {
            throw new RuntimeException("Arquivo não existe: " + absolutePath);
        }
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        ByteBuffer image = stbi_load(absolutePath, width, height, channels, 4);
        if (image == null) {
            throw new RuntimeException("Falha ao carregar textura: " + absolutePath + ". Erro STB: " + stbi_failure_reason());
        }
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        stbi_image_free(image);
        return texture;
    }

    private int createShaderProgram(String vertexPath, String fragmentPath) throws Exception {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        String vertexSource = readFile(vertexPath);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Falha ao compilar vertex shader: " + glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        String fragmentSource = readFile(fragmentPath);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Falha ao compilar fragment shader: " + glGetShaderInfoLog(fragmentShader));
        }

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Falha ao linkar shader program: " + glGetProgramInfoLog(program));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return program;
    }

    private String readFile(String filePath) throws Exception {
         InputStream is = getClass().getResourceAsStream(filePath);  // ex.: "/shaders/vertex.glsl"
        if (is == null) {
            throw new RuntimeException("Arquivo não encontrado: " + filePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler arquivo: " + filePath, e);
        }
    }

    private void mouseCallback(long window, double xpos, double ypos) {
        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
        }
        float xoffset = (float) (xpos - lastX);
        float yoffset = (float) (lastY - ypos);
        lastX = xpos;
        lastY = ypos;

        float sensitivity = 0.1f;
        xoffset *= sensitivity;
        yoffset *= sensitivity;

        yaw += xoffset;
        pitch += yoffset;

        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        Vector3f direction = new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
        ).normalize();
        cameraFront.set(direction);
    }

    private void loop() {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        double lastTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
// Calcula delta time
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

// Movimento da câmera
            float cameraSpeed = 2.5f * deltaTime;
            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
                cameraPos.add(cameraFront.mul(cameraSpeed, new Vector3f()));
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
                cameraPos.sub(cameraFront.mul(cameraSpeed, new Vector3f()));
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
                cameraPos.sub(cameraFront.cross(cameraUp, new Vector3f()).normalize().mul(cameraSpeed));
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
                cameraPos.add(cameraFront.cross(cameraUp, new Vector3f()).normalize().mul(cameraSpeed));

// Renderiza
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glUseProgram(shaderProgram);

// Atualiza matrizes
            view = new Matrix4f().lookAt(cameraPos, cameraPos.add(cameraFront, new Vector3f()), cameraUp);
            Matrix4f model = new Matrix4f().translate(0.0f, 0.0f, 0.0f);

            int modelLoc = glGetUniformLocation(shaderProgram, "model");
            int viewLoc = glGetUniformLocation(shaderProgram, "view");
            int projLoc = glGetUniformLocation(shaderProgram, "projection");
            glUniformMatrix4fv(modelLoc, false, model.get(BufferUtils.createFloatBuffer(16)));
            glUniformMatrix4fv(viewLoc, false, view.get(BufferUtils.createFloatBuffer(16)));
            glUniformMatrix4fv(projLoc, false, projection.get(BufferUtils.createFloatBuffer(16)));

// Desenha cubo
            glBindTexture(GL_TEXTURE_2D, texture);
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteTextures(texture);
        glDeleteProgram(shaderProgram);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) throws Exception {
        new Game().run();
    }
}
