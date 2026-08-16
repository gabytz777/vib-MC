# vib-MC

<div align="center">

[![Work in Progress](https://img.shields.io/badge/status-WIP-red?style=for-the-badge)]()
[![AI Generated](https://img.shields.io/badge/AI-Generated-9cf?style=for-the-badge)]()
[![Vibecoded](https://img.shields.io/badge/Vibecoded-ff69b4?style=for-the-badge)]()
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-blue?style=for-the-badge&logo=minecraft)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)]()
[![Release](https://img.shields.io/badge/Release-v0.0.5--hotfix.1-blue?style=for-the-badge)](https://github.com/gabytz777/vib-MC/releases/tag/v0.0.5-hotfix.1)

**vibed into existence** — a Minecraft server made entirely by AI, one prompt at a time.

> ⚠️ **Latest release: v0.0.5-hotfix.1** — the Nether and the End actually work now: portals you build yourself, real lighting, and dimension travel that arrives. This whole thing is still being coded by an AI. YMMV.

</div>
---

`vib-MC` is a Minecraft server (protocol 340 / 1.12.2) that was **entirely vibecoded by AI**. No human wrote any of this. It connects, generates terrain, and occasionally works. The latest release runs on Java 8 or newer, and so does building from source.

## What's new in v0.0.5-hotfix.1 (changes since v0.0.5)

The dimensions shipped in v0.0.5 generated but were not really playable. This fixes that,
and the way you get to them changed: nothing is handed to you at spawn any more.

- **Portals actually appear** — portal blocks were sent with a block state the 1.12.2 client does not know, so it drew every portal as air. You could stand in one and never see it
- **Dimension travel arrives** — a dimension change sent a Respawn packet and never told the client where it had landed, so it sat on "Downloading terrain" forever. Everything that packet throws away is now re-sent behind it
- **You build your own way in** — no free obsidian portal at spawn. Rare lava pools sitting in a bed of stone are the obsidian supply: pour water on lava, mine it, build a frame, light it with flint and steel. Twelve eyes of ender in a frame ring open an End portal
- **Break and place blocks** — this did not exist at all before, which is why the above was impossible. Digging, placing, drops into your inventory, and inventory sync to the client
- **The Nether is a place** — it was solid netherrack with the occasional bubble. Now it is an open cavern between a rolling floor and a hanging ceiling, over a lava sea, with soul sand, glowstone and quartz
- **The End has towers** — ten obsidian pillars ring the central island, and the arrival platform sits in a patch of void the generator keeps clear, so you land on the platform instead of inside an island
- **Real lighting** — sky light was worked out per column and block light was hardcoded to zero, which is why trees rendered as black lumps and glowstone gave off nothing. Both are now flooded properly
- **Fall damage, and the void kills you** — falling used to trip the anti-flight check and kick you off the server. Now you take fall damage, dying puts up a death screen, and you respawn in the overworld
- **A usable console** — chunk and packet logging moved behind `log-level=debug`, so the console is something you can type commands into
- **EULA** — `eula.txt` is written on first start and must be accepted, as with vanilla
- **Fixed a disconnect under load** — packets were encrypted and queued as two separate steps, so two threads sending at once could put the encrypted stream out of order and drop the client with "Bad packet id"

## Features

| Status | Feature |
|:------:|---------|
| ✅ | Actually starts |
| ✅ | People can join |
| ✅ | Terrain generates (somewhere) |
| ✅ | Terrain renders (13-bit chunk data, vanilla client sees it) |
| ✅ | Camera moves (chunk streaming works) |
| ✅ | Plugin API |
| ❌ | Mobs (no entities are sent to clients yet — players only) |
| ✅ | Nether (open caverns, lava sea, glowstone, quartz) |
| ✅ | End (islands, obsidian towers, arrival platform, exit portal) |
| ✅ | Portals (player-built: flint and steel, or 12 eyes of ender) |
| ✅ | Lighting (sky and block light are flooded, not guessed per column) |
| ⚠️ | Building (break/place works; no crafting, no tool tiers) |
| ❌ | The dragon fight (no mob entities yet — towers are just scenery) |
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
