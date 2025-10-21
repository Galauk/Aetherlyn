# Aetherlyn

> An open-world MMORPG prototype built in Java, inspired by *Haven & Hearth* and *Overlord*.

![Aetherlyn 3D Client - Isometric prototype](docs/screenshot.png)

---

## What is Aetherlyn?

Aetherlyn is a sandbox MMORPG where players explore a persistent open world, gather resources, survive, and build power — including dark paths like becoming a lich and commanding undead minions.

The project is built around a **dual-client architecture**: both clients connect to the same game world and server, but offer different visual experiences:

| Client | Renderer | Perspective |
|--------|----------|-------------|
| **2D Client** | LibGDX (sprite-based) | Isometric |
| **3D Client** | LWJGL + OpenGL + GLSL | Isometric |

This design allows players to choose their preferred visual style while sharing the same persistent world, similar to how *Haven & Hearth* balances simplicity with depth.

---

## Current State

The project is in early prototype stage. The **3D client** currently features:

- ✅ Isometric camera with WASD + mouse control
- ✅ Textured 3D cube rendered via custom GLSL shaders
- ✅ OpenGL pipeline via LWJGL
- 🔲 Terrain chunk loading (in progress)
- 🔲 Player character instance
- 🔲 2D client (planned)

---

## Planned Features

### World
- Procedural map generation with biomes
- Resource gathering (wood, stone, etc.)
- SQLite-based persistence

### Characters & Combat
- Player character with inventory system
- Survival mechanics (hunger, shelter)
- Monster spawning and AI

### Dark Path
- Lich transformation mechanic
- Skeleton summoning and undead army management

### Multiplayer
- Client-server architecture via KryoNet
- Persistent shared world for both 2D and 3D clients

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| 3D Rendering | LWJGL 3, OpenGL, GLSL |
| 2D Rendering | LibGDX (planned) |
| Networking | KryoNet (planned) |
| Database | SQLite (planned) |
| Build | Maven |

---

## How to Run

### Requirements

- Java JDK 21
- IntelliJ IDEA Community (recommended)
- Linux system dependencies:

```bash
sudo apt install libgl1-mesa-dev libglfw3-dev libopenal-dev
```

### Setup

1. Clone the repository:
```bash
git clone https://github.com/Galauk/Aetherlyn
```

2. Open in IntelliJ IDEA and run:
```bash
mvn clean install
```

3. Place `grass.png` (32x32 PNG) in `src/main/resources/assets/`

4. Place `vertex.glsl` and `fragment.glsl` in `src/main/resources/shaders/`

5. Run `Game.java` (right-click → Run) or:
```bash
java -jar target/Aetherlyn-1.0-SNAPSHOT.jar
```

---

## Inspiration

- [Haven & Hearth](https://www.havenandhearth.com/) — persistent open world, survival and crafting depth
- *Overlord* — dark fantasy theme, commanding minions, morality through power

---
