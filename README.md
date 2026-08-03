# vib-MC

<div align="center">

[![Work in Progress](https://img.shields.io/badge/status-WIP-red?style=for-the-badge)]()
[![AI Generated](https://img.shields.io/badge/AI-Generated-9cf?style=for-the-badge)]()
[![Vibecoded](https://img.shields.io/badge/Vibecoded-ff69b4?style=for-the-badge)]()
[![Java](https://img.shields.io/badge/Java-11-green?style=for-the-badge&logo=java)]()
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-blue?style=for-the-badge&logo=minecraft)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)]()
[![Release](https://img.shields.io/badge/Release-v0.0.2-blue?style=for-the-badge)](https://github.com/gabytz777/vib-MC/releases/tag/v0.0.2)

**vibed into existence** — a Minecraft server made entirely by AI, one prompt at a time.

> ⚠️ **Latest release: v0.0.2** — This whole thing is being coded by an AI. It connects sometimes. YMMV.

</div>

---

`vib-MC` is a Minecraft server (protocol 340 / 1.12.2) that was **entirely vibecoded by AI**. No human wrote any of this. It connects, generates terrain, and occasionally works. Built with Java 11 because that's what vibes with the project.

## Features

| Status | Feature |
|:------:|---------|
| ✅ | Actually starts |
| ✅ | People can join |
| ✅ | Terrain generates (somewhere) |
| ⚠️ | Terrain renders (chunk format patched, client still sees void) |
| ⚠️ | Camera moves (chunk streaming added, still broken) |
| ✅ | Plugin API |
| ✅ | Mobs that exist |
| ❌ | Nether (who needs it) |
| ❌ | Working game (soon™) |

## Requirements

- **Java 11 or newer**

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
```

## Version Requirements

| Version | Status | Java | Notes |
|---------|--------|------|-------|
| v0.0.2 | ✅ Latest · Stable | 11 or newer | Recommended release; matches this README |
| v0.0.3 | ⚠️ Pre-release | 8 or newer | Java 8 compatibility build |

Building from source requires **Java 11 or newer** (`options.release = 11`).

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
