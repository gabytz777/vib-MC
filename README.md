# vib-MC

<div align="center">

[![Work in Progress](https://img.shields.io/badge/status-WIP-red?style=for-the-badge)](https://github.com/anomalyco/vib-MC)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?style=for-the-badge&logo=java)](https://adoptium.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-blue?style=for-the-badge&logo=minecraft)](https://minecraft.net/)
[![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge)]()

**A modular, extensible Minecraft server implementation written in Java**

> ⚠️ **Work in Progress** — This project is under active development. Many features are incomplete or missing.

</div>

---

## Overview

vib-MC is a from-scratch Minecraft server implementation targeting protocol **340 (1.12.2)**. It features NIO-based networking, procedural terrain generation, a plugin API with event system, mob AI, and full command support — all built without any external dependencies on existing server software.

## Screenshots

*Screenshots coming soon — currently debugging terrain rendering.*

## Badges

[![Java 21](https://img.shields.io/badge/Java-21%2B-orange)](https://adoptium.net/)
[![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-blue)](https://minecraft.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)]()

## Building

```bash
./gradlew build
```

> **Note**: Requires JDK 21+. The built JAR is at `build/libs/vib-mc.jar`.

## Running

```bash
java -jar build/libs/vib-mc.jar
```

Or via Gradle:

```bash
./gradlew run
```

On first launch, the server generates `server.properties` and a `world/` directory.

## Configuration

Edit `server.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `motd` | `A vib-MC server` | Server message of the day |
| `max-players` | `20` | Maximum concurrent players |
| `seed` | `0` | World generation seed |
| `difficulty` | `easy` | peaceful / easy / normal / hard |
| `view-distance` | `8` | Chunk view distance (3–32) |
| `server-port` | `25565` | Server port |
| `online-mode` | `false` | Enable Mojang authentication |
| `level-name` | `world` | World directory name |

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/help` | List commands | `vibmc.command.help` |
| `/tp` | Teleport players | `vibmc.command.tp` |
| `/gamemode` | Change gamemode | `vibmc.command.gamemode` |
| `/time` | Set / query time | `vibmc.command.time` |
| `/weather` | Change weather | `vibmc.command.weather` |
| `/give` | Give items | `vibmc.command.give` |
| `/kill` | Kill player | `vibmc.command.kill` |
| `/say` | Broadcast message | `vibmc.command.say` |
| `/seed` | Show world seed | `vibmc.command.seed` |
| `/save-all` | Save worlds | `vibmc.command.save` |
| `/stop` | Stop server | `vibmc.command.stop` |
| `/list` | List players | `vibmc.command.list` |

## Plugin API

Plugins are loaded from the `plugins/` directory. Create a JAR with:

1. A `plugin.yml`:
   ```yaml
   name: MyPlugin
   version: 1.0
   main: com.example.MyPlugin
   ```
2. Main class extending `VibMCPlugin`
3. Implement `Listener` and use `@EventHandler`

### Available Events

| Event | Description | Cancellable |
|-------|-------------|:-----------:|
| `PlayerJoinEvent` | Player joins | ✗ |
| `PlayerQuitEvent` | Player leaves | ✗ |
| `BlockBreakEvent` | Block is broken | ✓ |
| `BlockPlaceEvent` | Block is placed | ✓ |
| `EntitySpawnEvent` | Entity spawns | ✓ |
| `EntityDeathEvent` | Entity dies | ✗ |
| `PlayerMoveEvent` | Player moves | ✓ |
| `ChatEvent` | Player chats | ✓ |
| `TickEvent.Start` | Every tick start | ✗ |
| `TickEvent.End` | Every tick end | ✗ |

## Architecture

```
net.vibmc.server      — Server core, lifecycle
net.vibmc.config      — Configuration
net.vibmc.network     — Minecraft protocol, NIO networking
net.vibmc.world       — World, chunks, block types
net.vibmc.world.gen   — Terrain generation
net.vibmc.entity      — Entity base, AI, pathfinding
net.vibmc.entity.mob  — Mob implementations
net.vibmc.player      — Player management
net.vibmc.inventory   — Inventory system
net.vibmc.item        — Item registry, ItemStack
net.vibmc.command     — Command framework
net.vibmc.plugin      — Plugin API, event system
net.vibmc.scheduler   — Tick scheduler
net.vibmc.scoreboard  — Scoreboards, teams, boss bars
net.vibmc.advancement — Advancement framework
net.vibmc.permission  — Permission system
net.vibmc.metrics     — TPS monitoring, server metrics
```

## Features

| Status | Feature |
|:------:|---------|
| ✅ | Minecraft protocol (1.12.2 — protocol 340) |
| ✅ | Player join / leave / inventory |
| ✅ | Procedural terrain generation |
| ✅ | Infinite world with seed support |
| ✅ | Day / night cycle |
| ✅ | Weather system |
| ✅ | Passive mobs (Cow, Pig, Sheep, Chicken) |
| ✅ | Hostile mobs (Zombie, Skeleton, Spider, Creeper) |
| ✅ | Mob AI (pathfinding, wandering, combat) |
| ✅ | Full command system |
| ✅ | Plugin API with event system |
| ✅ | Chunk loading / saving (Anvil format) |
| ✅ | Scheduler API |
| ✅ | Scoreboards, teams, boss bars |
| ✅ | Advancements framework |
| ✅ | Permissions |
| ✅ | TPS monitoring |
| ✅ | Crash reports |
| ✅ | Asynchronous chunk I/O |
| ⚠️ | Terrain rendering (in progress) |
| ⚠️ | Redstone framework (extensible) |
| ⚠️ | Crafting recipes (extensible) |
| ❌ | Nether / End dimensions |

## License

[MIT](LICENSE)
