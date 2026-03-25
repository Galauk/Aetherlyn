# Aetherlyn — TODO

## Estado atual (já implementado)
- [x] Janela LWJGL/OpenGL 3.3
- [x] Cubo 3D texturizado (grass.png)
- [x] Câmera FPS livre (WASD + mouse)
- [x] Shaders GLSL customizados (vertex + fragment)
- [x] Pipeline VAO/VBO/EBO
- [x] Assets base: player.png, lich.png, skeleton.png, terrain.png, tree.png, stone.png
- [x] Build Maven

---

## Fase 1 — Fundação: Arquitetura & Câmera

- [ ] Refatorar Game.java em classes separadas (Camera, Renderer, InputHandler, ResourceLoader)
- [ ] Implementar câmera isométrica fixa (substituir câmera FPS atual)
- [ ] Corrigir loadTexture() para usar getResourceAsStream (atualmente usa caminho absoluto em disco)
- [ ] Implementar game loop com tick fixo (60 TPS lógico separado do render)

---

## Fase 2 — Mundo: Terreno & Tiles

- [ ] Renderizar grid NxN de cubos usando terrain.png como spritesheet (3D)
- [ ] Sistema de chunks (carregar/descarregar ao redor da câmera)
- [ ] Geração procedural com Perlin/Simplex Noise (grama, pedra, água)
- [ ] Objetos estáticos no mundo: árvores (tree.png) e pedras (stone.png) com colisão simples
- [ ] Setup do cliente 2D: módulo Maven separado com LibGDX

---

## Fase 3 — Entidades: Player & Mobs

- [ ] Entidade Player no mundo (sprite, movimento, colisão com tiles)
- [ ] Sistema de inventário básico (slots, coleta de recursos, persistência local)
- [ ] Mobs: Skeleton e Lich com IA simples (patrulha + aggro)
- [ ] Sistema de combate básico (HP, dano, morte, drop de itens)
- [ ] HUD básico (barra de HP, minimap, slots de inventário)

---

## Fase 4 — Servidor: Multiplayer & Persistência

- [ ] Módulo server standalone (Maven, sem rendering)
- [ ] Protocolo de rede via KryoNet (PlayerMove, EntitySpawn, WorldChunk, etc.)
- [ ] Persistência SQLite (mundo, players, inventários)
- [ ] Clientes 2D e 3D conectando ao mesmo servidor

---

## Fase 5 — Dark Path: Lich & Undead

- [ ] Mecânica de transformação em Lich (ritual com requisitos)
- [ ] Invocar e comandar esqueletos (ressuscitar mobs mortos como minions)
- [ ] Phylactery: item físico no mundo, destruível por outros players