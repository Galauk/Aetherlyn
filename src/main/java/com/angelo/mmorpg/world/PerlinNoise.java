package com.angelo.mmorpg.world;

import java.util.Random;

/**
 * Implementação de Perlin Noise 2D sem dependências externas.
 * Usado para geração procedural do terreno.
 */
public class PerlinNoise {

    private final int[]   perm;   // tabela de permutação
    private final float[] gradX;  // componente X dos gradientes
    private final float[] gradY;  // componente Y dos gradientes

    private static final int TABLE_SIZE = 256;

    public PerlinNoise(long seed) {
        Random rng = new Random(seed);
        perm  = new int[TABLE_SIZE * 2];
        gradX = new float[TABLE_SIZE];
        gradY = new float[TABLE_SIZE];

        // Gera gradientes aleatórios unitários
        for (int i = 0; i < TABLE_SIZE; i++) {
            float angle = rng.nextFloat() * (float) (Math.PI * 2);
            gradX[i] = (float) Math.cos(angle);
            gradY[i] = (float) Math.sin(angle);
            perm[i]  = i;
        }

        // Embaralha a tabela de permutação
        for (int i = TABLE_SIZE - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
        }

        // Duplica para evitar overflow
        for (int i = 0; i < TABLE_SIZE; i++) {
            perm[TABLE_SIZE + i] = perm[i];
        }
    }

    /** Retorna valor de ruído em [-1, 1] para a posição (x, y). */
    public float noise(float x, float y) {
        int xi = (int) Math.floor(x) & (TABLE_SIZE - 1);
        int yi = (int) Math.floor(y) & (TABLE_SIZE - 1);

        float xf = x - (float) Math.floor(x);
        float yf = y - (float) Math.floor(y);

        // Curva de suavização (fade)
        float u = fade(xf);
        float v = fade(yf);

        // Índices dos 4 cantos da célula
        int aa = perm[perm[xi    ] + yi    ];
        int ab = perm[perm[xi    ] + yi + 1];
        int ba = perm[perm[xi + 1] + yi    ];
        int bb = perm[perm[xi + 1] + yi + 1];

        // Interpola
        float x1 = lerp(u, dot(gradX[aa], gradY[aa], xf,       yf      ),
                dot(gradX[ba], gradY[ba], xf - 1f,  yf      ));
        float x2 = lerp(u, dot(gradX[ab], gradY[ab], xf,       yf - 1f ),
                dot(gradX[bb], gradY[bb], xf - 1f,  yf - 1f ));
        return lerp(v, x1, x2);
    }

    /**
     * Fractional Brownian Motion — soma múltiplas oitavas de ruído
     * para criar detalhes em várias escalas.
     *
     * @param octaves    número de camadas (mais = mais detalhe)
     * @param persistence quanto cada oitava contribui (0-1)
     * @param scale      escala base do ruído
     */
    public float fbm(float x, float y, int octaves, float persistence, float scale) {
        float value     = 0f;
        float amplitude = 1f;
        float frequency = scale;
        float maxValue  = 0f;

        for (int i = 0; i < octaves; i++) {
            value    += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2f;
        }

        return value / maxValue; // normaliza para [-1, 1]
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float dot(float gx, float gy, float dx, float dy) {
        return gx * dx + gy * dy;
    }
}