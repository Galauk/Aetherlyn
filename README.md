# Aetherlyn: MMORPG

Protótipo de um MMORPG em Java com LWJGL, inspirado em *Haven & Hearth* e *Overlord*. Transição inicial para gráficos 3D.

## Recursos

- **Protótipo 3D**: Cubo texturizado com câmera controlável (WASD + mouse).
- **Planejado (2D)**: Mapa procedural, coleta de madeira, transformação em lich, invocação de esqueleto.

## Como Rodar

1. Instale Java JDK 21: `sudo apt install openjdk-21-jdk`.

2. Instale IntelliJ IDEA Community (jetbrains.com).

3. Instale dependências do sistema: `sudo apt install libgl1-mesa-dev libglfw3-dev libopenal-dev`.

4. Clone este repositório: `git clone https://github.com/Galauk/Aetherlyn`.

5. Abra no IntelliJ e execute `mvn clean install`.

6. Coloque `grass.png` (32x32, PNG com transparência) em `src/main/resources/assets`.

7. Coloque `vertex.glsl` e `fragment.glsl` em `src/main/resources/shaders`.

8. Rode `Game.java` (clique direito &gt; Run) ou:

   ```bash
   java -jar target/Aetherlyn-1.0-SNAPSHOT.jar
   ```

## Próximos Passos

- Corrigir carregamento de texturas no protótipo 2D.
- Adicionar terreno e modelos 3D (Blender, .obj).
- Implementar mecânicas de sobrevivência e monstros.

Suporte a SQLite e multiplayer (KryoNet).
