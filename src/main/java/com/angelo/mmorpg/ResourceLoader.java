package com.angelo.mmorpg;

import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.stb.STBImage.*;

public class ResourceLoader {

    /**
     * Carrega uma textura a partir do classpath.
     * Exemplo: loadTexture("/assets/grass.png")
     */
    public static int loadTexture(String classpathPath) {
        IntBuffer width    = BufferUtils.createIntBuffer(1);
        IntBuffer height   = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Lê o arquivo como stream do classpath (funciona dentro do jar)
        byte[] bytes;
        try (InputStream is = ResourceLoader.class.getResourceAsStream(classpathPath)) {
            if (is == null) throw new RuntimeException("Textura não encontrada no classpath: " + classpathPath);
            bytes = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler textura: " + classpathPath, e);
        }

        ByteBuffer imageBuffer = BufferUtils.createByteBuffer(bytes.length);
        imageBuffer.put(bytes).flip();

        ByteBuffer image = stbi_load_from_memory(imageBuffer, width, height, channels, 4);
        if (image == null) {
            throw new RuntimeException("Falha ao decodificar textura: " + classpathPath + " — " + stbi_failure_reason());
        }

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        stbi_image_free(image);
        return textureId;
    }

    /**
     * Cria e linka um shader program a partir de dois arquivos no classpath.
     * Exemplo: createShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl")
     */
    public static int createShaderProgram(String vertexPath, String fragmentPath) {
        int vertexShader   = compileShader(GL_VERTEX_SHADER, vertexPath);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentPath);

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

    private static int compileShader(int type, String classpathPath) {
        String source = readClasspathFile(classpathPath);

        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String typeName = (type == GL_VERTEX_SHADER) ? "vertex" : "fragment";
            throw new RuntimeException("Falha ao compilar " + typeName + " shader: " + glGetShaderInfoLog(shader));
        }

        return shader;
    }

    private static String readClasspathFile(String path) {
        try (InputStream is = ResourceLoader.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Arquivo não encontrado no classpath: " + path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler arquivo: " + path, e);
        }
    }
}