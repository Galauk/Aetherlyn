# Aetherlyn

> An open-world MMORPG prototype built in Java, inspired by *Haven & Hearth* and *Overlord*.

---

## What is Aetherlyn?

Aetherlyn is a sandbox MMORPG where players explore a persistent open world, gather resources, survive, and build power — including dark paths like becoming a lich and commanding undead minions.

---

## Current State

### ✅ Phase 1 — Foundation (complete)
- Isometric camera with PgUp/PgDown rotation (smooth ease-out interpolation)
- Point-and-click movement with raycasting
- Scroll zoom
- Fixed timestep game loop (60 TPS logic / uncapped render)
- Organized package structure (`core`, `camera`, `input`, `rendering`, `debug`, `world`, `entity`)
- F3 debug panel (FPS, TPS, position, zoom, camera angle, seed)
- Ctrl+G debug grid overlay

### ✅ Phase 2 — World (complete)
- Terrain grid rendered from `terrain.png` spritesheet (32×32px tiles)
- Procedural map generation via Perlin Noise + FBM (4 tile types: grass, dirt, stone, water)
- Island-shaped maps with natural water borders (64×64 tiles)
- Static objects (stone, bush) with billboard rendering and circular collision
- Player cannot walk on water or through objects
- Camera rotation with smooth ease-out (PgUp/PgDown, 45° steps)

### ✅ Phase 3 — Entities (complete)
- **Player entity** with position, stats (STR, DEF, HP), inventory (UO-style free placement)
- **Inventory** — weight limit based on strength, drag items, toggle with `I`
- **Resource collection** — right-click stone/bush to collect (Stone, Wood items)
- **Experience system** — XP from kills and collecting resources, level up with stat bonuses
- **Creature AI** — state machine (IDLE → PATROL → CHASE → ATTACK → FLEE → DEAD)
- **3 behaviors** — PASSIVE (Deer), NEUTRAL (Villager), HOSTILE (Skeleton, Lich)
- **Combat** — right-click creature to attack, HP + defense + variable damage
- **Out-of-combat regeneration** — HP regens after 5s without taking damage
- **Death & respawn** — 5s respawn timer at map center
- **Push mechanic** — player pushes creatures instead of getting blocked
- **Complete HUD:**
    - HP / Weight / XP bars with numeric values (bottom-left)
    - Level display
    - Creature panel showing up to 3 nearby enemies with HP bars + numeric HP (top-right, below minimap)
    - Minimap 150×150px with tiles, player dot (white), creatures by behavior (red/yellow/green)
    - Death screen with respawn progress bar

### 🔲 Phase 4 — Server
- KryoNet multiplayer
- SQLite persistence
- Shared world for 2D and 3D clients

### 🔲 Phase 5 — Dark Path
- Lich transformation
- Undead army management
- Phylactery mechanic

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| 3D Rendering | LWJGL 3, OpenGL 3.3, GLSL |
| Math | JOML |
| Font Rendering | STB TrueType |
| Procedural Gen | Perlin Noise (custom) |
| 2D Rendering | LibGDX (planned) |
| Networking | KryoNet (planned) |
| Database | SQLite (planned) |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/angelo/mmorpg/
├── Game.java
├── camera/
│   └── Camera.java
├── core/
│   ├── Window.java
│   └── ResourceLoader.java
├── debug/
│   ├── DebugState.java
│   ├── DebugInfo.java
│   └── DebugRenderer.java
├── entity/
│   ├── Player.java
│   ├── PlayerStats.java
│   ├── ExperienceSystem.java
│   ├── Inventory.java
│   ├── Item.java
│   ├── ItemType.java
│   ├── Creature.java
│   ├── CreatureType.java
│   ├── CreatureBehavior.java
│   ├── CreatureState.java
│   ├── CreatureManager.java
│   └── CombatSystem.java
├── input/
│   └── InputHandler.java
├── rendering/
│   ├── Renderer.java
│   ├── TerrainRenderer.java
│   ├── ObjectRenderer.java
│   ├── CreatureRenderer.java
│   ├── GridRenderer.java
│   ├── HudRenderer.java
│   └── InventoryRenderer.java
└── world/
    ├── WorldMap.java
    ├── PerlinNoise.java
    └── StaticObject.java
```

---

## Controls

| Input | Action |
|-------|--------|
| Left click | Move player |
| Right click | Attack creature / Collect resource |
| PgUp / PgDown | Rotate camera 45° |
| Scroll | Zoom in/out |
| I | Open/close inventory |
| F3 | Toggle debug panel |
| Ctrl+G | Toggle debug grid |
| ESC | Quit |

---

## How to Run

```bash
git clone https://github.com/Galauk/Aetherlyn
cd Aetherlyn
mvn clean install
java -jar target/Aetherlyn-1.0-SNAPSHOT.jar
```

Linux dependencies:
```bash
sudo apt install libgl1-mesa-dev libglfw3-dev libopenal-dev
```

---

## Inspiration

- [Haven & Hearth](https://www.havenandhearth.com/) — persistent open world, survival and crafting depth
- *Overlord* — dark fantasy theme, commanding minions, morality through power