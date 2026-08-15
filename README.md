# vib-MC

<div align="center">

[![Work in Progress](https://img.shields.io/badge/status-WIP-red?style=for-the-badge)]()
[![AI Generated](https://img.shields.io/badge/AI-Generated-9cf?style=for-the-badge)]()
[![Vibecoded](https://img.shields.io/badge/Vibecoded-ff69b4?style=for-the-badge)]()
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-blue?style=for-the-badge&logo=minecraft)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)]()
[![Release](https://img.shields.io/badge/Release-v0.0.5-blue?style=for-the-badge)](https://github.com/gabytz777/vib-MC/releases/tag/v0.0.5)

**vibed into existence** — a Minecraft server made entirely by AI, one prompt at a time.

> ⚠️ **Latest release: v0.0.5** — Nether and End are real, portals travel, and online mode verifies you with Mojang. This whole thing is still being coded by an AI. YMMV.

</div>

![vib-MC screenshot](https://slopmadebymestudios.lol/files/vibmc-screenshot.png)

---

`vib-MC` is a Minecraft server (protocol 340 / 1.12.2) that was **entirely vibecoded by AI**. No human wrote any of this. It connects, generates terrain, and occasionally works. The latest release runs on Java 8 or newer, and so does building from source.

## What's new in v0.0.5 (changes since v0.0.4-hotfix.3)

- **The Nether is real** — a netherrack world with caverns, lava seas, soul sand and glowstone, bedrock floor and roof, no sky light
- **The End is real** — end-stone islands floating in the void with a guaranteed solid arrival platform
- **Portals travel** — stand in a Nether or End portal to cross over (Nether coordinates are the overworld's ÷ 8, the End has a fixed arrival platform); a portal is built for you on the far side if there isn't one, and existing worlds get a spawn portal + End exit portal on startup
- **Online mode works** — `online-mode=true` by default: logins are verified against Mojang's session servers with the real RSA + AES-128/CFB8 handshake, so players get real UUIDs and their real skins
- **BungeeCord / Velocity legacy forwarding** — `proxy-mode=legacy` + `proxy-trusted-address` for proxied setups; the server refuses to start on an insecure proxy+offline combo
- **Trees** — biome-aware oak trees (dense forests, sparse plains, none in deserts)
- **Seeds** — blank `seed=` rolls a random seed for new worlds, numbers are used as-is, text seeds are hashed deterministically; the saved world's level.dat is the authority
- **Anti-flight check** — vanilla-style: only sustained unsupported hovering gets kicked, never normal play
- **Generator refactor** — Overworld / Nether / End behind one `ChunkGenerator` interface
- **Protocol fixes** — two wrong protocol-340 packet IDs that caused real-client disconnects; villages no longer place chest blocks with no tile entity (crashed clients)

## What's new in v0.0.4 (changes since v0.0.3)

- **Terrain renders on the vanilla 1.12.2 client** — chunk data now uses the canonical 13-bit block encoding (the previous 12-bit format was being misinterpreted as garbage by the notchian client)
- **New terrain profile** — grass (2 layers) on top, stone mixed with andesite and diorite (7 layers), water up to 4 deep, bedrock at the bottom
- **Spawn on dry land** — the game finds the nearest dry column instead of dropping you in the sea
- **Chunk streaming fixes** — partial non-blocking writes are flushed properly so terrain streams in as you move
- **Protocol fixes** — client status (respawn), client settings, teleport confirm, plugin messages, keep-alive, and position packets handled at the correct 1.12.2 IDs
- **Andesite/diorite wired up** — stone metadata states (stone:3 diorite, stone:5 andesite) now map correctly on the wire

## Hotfixes — apply to ALL versions

### v0.0.4-hotfix.2
- **The client actually switches game mode** — `/gamemode` and plugin commands that call `setGameMode` now send the Change Game State packet (0x1E, reason 3) so the creative/spectator UI switch happens in-game instead of only changing server state. Applies to every release: v0.0.1 through v0.0.4.

### v0.0.4-hotfix.1
- **Slash commands actually execute** — chat messages starting with `/` are now routed to the command system instead of being broadcast as chat, so `/gamemode`, `/tp`, `/time`, `/weather`, `/give` and plugin commands work in-game for the first time. This fix applies to every release: v0.0.1 through v0.0.4.

## Features

| Status | Feature |
|:------:|---------|
| ✅ | Actually starts |
| ✅ | People can join |
| ✅ | Terrain generates (somewhere) |
| ✅ | Terrain renders (13-bit chunk data, vanilla client sees it) |
| ✅ | Camera moves (chunk streaming works) |
| ✅ | Plugin API |
| ✅ | Mobs that exist |
| ✅ | Nether (lava seas, caverns, glowstone, the works) |
| ✅ | End (floating islands, arrival platform) |
| ✅ | Portals (travel both ways) |
| ✅ | Online mode (Mojang-verified logins, real skins) |
| ❌ | Working game (soon™) |

## Requirements

- **Java 8 or newer** to run v0.0.5 (and every release from v0.0.3 up)
- **Java 8 or newer** to build from source

## Quick Start

```bash
gradle build
java -jar build/libs/vib-mc.jar
```

Server starts on port 25565 by default. Edit `server.properties` after first run.

## Commands

`/help`, `/tp`, `/gamemode`, `/time`, `/weather`, `/give`, `/kill`, `/say`, `/seed`, `/save-all`, `/stop`, `/list`

## Architecture

```
net.vibmc.server      — makes it go
net.vibmc.network     — talks to Minecraft
net.vibmc.world       — blocks and stuff
net.vibmc.world.gen   — makes the ground
net.vibmc.entity      — things that move
net.vibmc.player      — the people
net.vibmc.plugin      — mod support
net.vibmc.command     — slash commands
net.vibmc.permission  — who can do what
```

## Version Requirements

| Status | Version | Requires |
|:------:|---------|----------|
| ✅ | v0.0.1 — alpha | Java 21 or newer |
| ✅ | v0.0.2 — stable | Java 11 or newer |
| ✅ | v0.0.3 — stable | Java 8 or newer |
| ✅ | v0.0.4 — stable | Java 8 or newer |
| ✅ | v0.0.4-hotfix.1 — stable (hotfix applies to all versions) | Java 8 or newer |
| ✅ | v0.0.4-hotfix.2 — stable (hotfix applies to all versions) | Java 8 or newer |
| ✅ | v0.0.4-hotfix.3 — stable (hotfix applies to all versions) | Java 8 or newer |
| ✅ | v0.0.5 — latest, stable | Java 8 or newer |

Building from source requires **Java 8 or newer** (`options.release = 8`).

## License

MIT — do whatever you want with this AI-generated mess.

```
MIT License

Copyright (c) 2026 vib-MC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
