# vib-MC — Project Knowledge

## Overview

vib-MC is a custom Minecraft 1.12.2 server implementation written from scratch.

The project is experimental, hobbyist, and heavily focused on learning, experimentation, and adding custom functionality. It is not intended to replace mature Minecraft server software such as Paper or Vanilla.

The goal is to make a fun, lightweight, highly customizable Minecraft server implementation while experimenting with networking, world generation, plugins, persistence, and Minecraft protocol behavior.

Repository:

https://github.com/gabytz777/vib-MC

---

# Project Philosophy

vib-MC is intentionally different from production Minecraft server software.

It should prioritize:

- Experimentation
- Simplicity
- Custom functionality
- Learning
- Modularity
- Fun
- Easy plugin development
- Interesting world generation

Do not assume vib-MC needs to behave exactly like Paper, Spigot, or Vanilla internally.

Minecraft compatibility is important, but the implementation is its own server.

---

# Minecraft Version

Target Minecraft version:

**Minecraft Java Edition 1.12.2**

Protocol compatibility should therefore be considered against the Minecraft 1.12.2 protocol.

When implementing networking features, always account for the protocol version rather than assuming modern Minecraft packet behavior.

---

# Core Features

vib-MC currently focuses on implementing the fundamentals required for a playable Minecraft server.

These include:

- Server startup
- Player connections
- Player movement
- Chunk generation
- World generation
- Block interaction
- Chat
- Commands
- Game modes
- Plugin loading
- Player skins
- Tab list
- World persistence
- Chunk persistence
- Seed-based world generation
- Server configuration

---

# World Generation

vib-MC has its own custom world generator.

The world is generated procedurally rather than relying on Vanilla's terrain generator.

Current/planned terrain features include:

- Grass
- Dirt
- Stone
- Water
- Sand
- Bedrock
- Ores
- Trees
- Caves
- Cave entrances
- Hills
- Varied terrain

The generator should remain deterministic for a given world seed.

Different seeds should produce different worlds.

The project may eventually introduce more advanced terrain generation and biome systems.

---

# Seeds

World generation uses a seed.

The project should support randomly generated seeds as well as explicitly specified seeds.

A world should retain its seed once created so that the generated terrain remains consistent across server restarts.

The seed is part of the world's persistent state.

---

# World Persistence

vib-MC supports persistent world storage.

World data is stored in a structure similar to:

```text
world/
├── level.dat
└── region/
    ├── r.<x>.<z>.chunk
    ├── r.<x>.<z>.chunk
    └── ...
```

`level.dat` contains persistent world information such as:

* Seed
* World time
* Time of day
* Weather
* Other world metadata

Chunks are stored individually.
Chunk data is compressed when stored on disk.
Saved chunks should be loaded from disk instead of being regenerated from the seed.

## Saving

The server supports world saving through:

```text
/save-all
```

Saving should be incremental.
Only chunks that have actually changed should need to be rewritten.
The server also supports automatic saving through configuration.
Example:

```properties
autosave-interval-ticks=6000
save-on-stop=true
```

Setting:

```properties
autosave-interval-ticks=0
```

disables automatic saving.

---

# Server Configuration

Server configuration is handled through `server.properties` and related configuration systems.
Configuration should be straightforward and readable.
Avoid introducing configuration options unnecessarily.
When adding new settings:

1. Provide sensible defaults.
2. Document them.
3. Ensure old configurations remain usable.
4. Avoid silently changing existing behavior.

---

# Plugins

vib-MC has plugin support.
The plugin system is intended to allow users to extend the server without modifying the core server source.
Plugins can eventually provide:

* Commands
* Events
* Gameplay features
* Server utilities
* Administrative tools
* Other extensions

Plugins are loaded from:

```text
plugins/
```

The plugin API should remain as simple as possible.

## Plugin Creation

One of vib-MC's goals is making plugin creation accessible to people who don't necessarily know how to program.
The project website includes the idea of a visual/"Scratch-style" plugin creator.
The intended experience is:

```text
Choose actions
      ↓
Configure behavior
      ↓
Generate plugin
      ↓
Put plugin in plugins/
      ↓
Run vib-MC
```

Users who know Java/programming should also be able to create traditional plugins using the documented API.

---

# Commands

vib-MC supports server commands.
Examples include:

```text
/gamemode
/tp
/save-all
```

Commands should be implemented through a centralized command system rather than scattered throughout unrelated server code.
Commands should:

* Validate arguments.
* Provide useful error messages.
* Avoid crashing the server.
* Respect player/server context where appropriate.

---

# Game Modes

vib-MC supports Minecraft game modes.
The implementation must keep the server-side player state and client-side state synchronized.
Minecraft 1.12.2 protocol behavior should be followed when notifying clients of game mode changes.
Supported game modes should eventually include:

* Survival
* Creative
* Adventure
* Spectator

---

# Multiplayer

vib-MC is designed as a multiplayer server.
The architecture should avoid assuming that only one player exists.
When implementing features, consider:

* Multiple players
* Player tracking
* Chunk visibility
* Player movement
* Player state
* Entity visibility
* Disconnects
* Reconnects
* Tab list
* Skins
* Per-player data

Avoid using global state where player-specific state is required.

---

# Skins

Player skin support is part of vib-MC.
The server should correctly handle player skin information and communicate it to other clients where required by the Minecraft 1.12.2 protocol.
Skin behavior should work alongside the tab list/player information system.

---

# Tab List

vib-MC supports a player tab list.
The tab list should represent connected players and their relevant player information.
Changes to:

* Player joining
* Player leaving
* Player names
* Skins
* Player information

should be synchronized appropriately.

---

# Networking

The server communicates with Minecraft clients using the Minecraft 1.12.2 protocol.
Networking code should be:

* Reliable
* Clear
* Version-appropriate
* Efficient
* Defensive against malformed input

When adding protocol features, verify the actual 1.12.2 packet structure rather than copying implementations intended for newer Minecraft versions.

---

# Architecture

Keep the project modular.
Important conceptual areas include:

```text
Server
 ├── Networking
 ├── Players
 ├── Commands
 ├── Plugins
 ├── Worlds
 │    ├── World
 │    ├── Chunks
 │    ├── Blocks
 │    ├── Generation
 │    └── Persistence
 ├── Entities
 └── Configuration
```

Avoid putting unrelated functionality into giant classes.
Prefer clear responsibilities between systems.

---

# World / Chunk Architecture

A world consists of chunks.
Chunks are responsible for storing block data and relevant chunk state.
The world manages:

* Chunk loading
* Chunk generation
* Chunk unloading
* World state
* World time
* Weather
* Persistence

The chunk manager handles chunk lifecycle and storage.
Important distinction:

```text
Chunk generation
        ≠
Chunk persistence
```

Generation creates a chunk that doesn't exist yet.
Persistence stores and reloads an existing chunk.

---

# Performance

vib-MC is not intended to compete with highly optimized production servers, but performance still matters.
Avoid:

* Unnecessary allocations
* Repeated full-world scans
* Rewriting unchanged chunks
* Blocking operations on critical networking paths
* Excessive logging
* Re-generating existing terrain

Prefer incremental operations.

---

# Error Handling

The server should prefer graceful recovery where possible.
For example:

* A corrupt chunk should not necessarily crash the entire server.
* Invalid player commands should return an error.
* Invalid plugin behavior should be isolated where practical.
* Malformed network data should not crash the server.

Errors should be logged clearly enough to diagnose problems.

---

# Testing

Important areas should have automated tests where practical.
Tests should cover:

* World storage
* Chunk storage
* Chunk loading
* Chunk saving
* Seed handling
* Negative chunk coordinates
* Corrupt data
* Dirty chunk tracking
* Configuration
* Command behavior

Integration testing with a real Minecraft 1.12.2 client is especially valuable for:

* Login
* Chunk streaming
* Movement
* Game modes
* Skins
* Tab list
* Multiplayer

---

# Future Features

Potential future development includes:

## Biomes

Possible biome types:

* Plains
* Desert
* Snow
* Forest
* Other custom biomes

Biomes should affect terrain and potentially vegetation.

## Structures

Potential structures include:

* Villages
* Houses
* Other generated structures

## Nether

A future Nether implementation may include:

* Nether terrain
* Lava
* Nether-specific generation
* Nether portals
* Nether structures

## More terrain

Potential improvements:

* Larger hills
* Mountains
* Better cave systems
* More varied terrain
* More ores
* Better vegetation

---

# Development Style

When modifying vib-MC:

1. Understand the existing implementation first.
2. Prefer small, focused changes.
3. Avoid unnecessary rewrites.
4. Preserve existing functionality.
5. Test changes before committing.
6. Keep Minecraft 1.12.2 compatibility in mind.
7. Document significant new systems.
8. Don't introduce complexity without a reason.

---

# Versioning

vib-MC uses semantic-style versioning with development/hotfix releases.
Examples:

```text
v0.0.3
v0.0.4
v0.0.4-hotfix.1
v0.0.4-hotfix.2
v0.0.4-hotfix.3
```

Hotfix releases should generally contain focused fixes or small improvements to the current release.
Larger feature additions should be grouped into a new development version.

---

# Project Status

vib-MC is a hobbyist/experimental project.
It is not intended to replace:

* Paper
* Spigot
* Vanilla
* Other mature Minecraft server implementations

Those projects will remain substantially more mature and production-ready.
vib-MC exists primarily to experiment with building a Minecraft server from the ground up and to create a fun, customizable alternative.

---

# Agent Instructions

When working on vib-MC:

* Read the relevant source before modifying it.
* Understand how the existing system works.
* Keep changes compatible with Minecraft 1.12.2.
* Avoid assumptions based on modern Minecraft versions.
* Don't remove existing functionality unless explicitly requested.
* Test changes when possible.
* Keep persistence and generation as separate concepts.
* Consider multiplayer even when testing with one player.
* Keep plugins isolated from core server functionality.
* Prefer maintainable code over clever code.
* Do not introduce unnecessary dependencies.
* Do not rewrite large portions of the project unless there is a clear architectural reason.

Most importantly:
vib-MC is an experimental server project. Optimize for functionality, experimentation, maintainability, and fun — not for pretending to be Paper.

---

# Current Handoff Notes — Skins plugin + online mode (14 Aug 2026)

Read this before continuing skins/online-mode work. Written for the next coding session (and future "what do we do next" checks).

## TL;DR — what was happening

User's **own skin doesn't show in-game** in their own client. Everything else (terrain, gamemode, spectator, plugins, blueprint site) is done and shipped.

- Skins for **other** players already work (Player Info packet 0x04 with textures property — commits `fc78966`, `a85e8ee` on `backup`).
- The blocker is the client's launch mode, not the server (see "The root cause").
- A SkinsRestorer-style `/skin` plugin was just built (commits NOT made yet — see "Uncommitted work").
- The proper long-term fix — a real `online-mode=true` in vib-MC — was **deferred by the user** ("too much for now"). Full implementation plan below.

## The root cause (important, explains everything)

The vanilla 1.12.2 client only applies the **local player's** skin from the server's Player Info packet when the UUID in that packet **equals the client's session UUID**.

- User's Prism instance launches **in online mode** (MSA account `_poisoned`, real UUID `f1d38adb-465c-4764-b82a-f29f57a3ff09`, stored in `C:\Users\gab\AppData\Roaming\PrismLauncher\accounts.json`).
- vib-MC is an offline-mode server → it derives UUID as `UUID.nameUUIDFromBytes("OfflinePlayer:" + name)` (see `src/main/java/net/vibmc/network/handler/LoginHandler.java:21`).
- The UUIDs never match → the client keeps Steve for its own player. This is a **client limitation** — SkinsRestorer has the same problem, and no server plugin can fix it for an online-mode client.

## Quick fix (works today, no code) — Prism offline account

Prism's offline accounts use the exact same `OfflinePlayer:` UUID convention (verified: `poisw1`'s stored id `ab81a882ac593c70921be9f351bee188` == Java UUID of md5("OfflinePlayer:poisw1")). So:

1. Prism Launcher → top-right **account dropdown** → **Add Offline** → username `_poisoned`.
2. Select the new offline `_poisoned` account for the vib-MC instance (the MSA `_poisoned` account is currently the active one — `"active": true` in accounts.json).
3. Launch, join, check **F5 / tab list** (first person never shows your own skin).

Result: UUIDs match → the server's `skin-url` config applies → skin shows. Do NOT edit `accounts.json` while Prism is running (it overwrites on exit).

## The proper fix (deferred) — real online mode

`server.properties` already has `online-mode=false` (line 14) and `ServerConfig.onlineMode()` exists — but **nothing reads it**; it's a dead switch. Implementing it makes the server verify the client with Mojang, learn the real UUID, and send matching Player Info → own skin works with zero client changes, and everyone's skin becomes their real Mojang skin.

Implementation plan (1.12.2 protocol):

1. **NetworkServer** (`src/main/java/net/vibmc/network/NetworkServer.java`): generate an RSA keypair at startup (`KeyPairGenerator.getInstance("RSA")`); expose the public key as `KeyFactory`/`X509EncodedKeySpec` DER bytes (client hashes exactly these bytes).
2. **LoginHandler** (`src/main/java/net/vibmc/network/handler/LoginHandler.java`): when `config.onlineMode()`:
   - On Login Start (login 0x00) → send **Encryption Request** (login 0x01): `serverId=""`, public key bytes (length-prefixed), verify token (4 random bytes).
   - On **Encryption Response** (login 0x02): two length-prefixed byte arrays (RSA-encrypted 16-byte shared secret, RSA-encrypted verify token). Decrypt with the private key, compare the verify token.
   - Server hash (must match vanilla): `sha1(secretBytes + publicKeyDerBytes)` → `new BigInteger(digest).toString(16)` (keep the negative sign; strip leading zeros if longer than 40 chars).
   - Verify: `GET https://sessionserver.mojang.com/session/minecraft/hasJoined?username=<name>&serverId=<hash>` (HttpURLConnection, ~3s timeouts). 204 → kick "Failed to verify username!". 200 → JSON: `"id"` (real UUID, no dashes), `"name"`, `properties[].textures` (base64 of the textures JSON — pass it through verbatim in Player Info).
   - Enable **AES-128/CFB8 NoPadding** both directions with the shared secret — outbound cipher BEFORE sending Login Success (it's the first encrypted packet), inbound cipher BEFORE processing any later client packets.
   - Login Success (0x02) with the **real UUID** (dashed) + name. Use the real UUID for the `PlayerEntity` from then on.
3. **ClientConnection** (`src/main/java/net/vibmc/network/ClientConnection.java`) — the hard part. It's non-blocking NIO: inbound arrives via `feed(byte[])` (partial frames OK), outbound is queued byte arrays. Approach: keep a persistent `Cipher` per direction; in `feed()`, decrypt the incoming chunk **before** appending to the framing buffer; in `sendPacket()`/`disconnect()`, `cipher.update()` the frame bytes before queueing. CFB8 is a stream cipher so chunked operation with a persistent Cipher is correct. Add `enableEncryption(SecretKey)` that switches both paths atomically.
4. **PlayerManager.texturesProperty** (`src/main/java/net/vibmc/player/PlayerManager.java`): priority for the textures property = per-player `skin-url.<name>` override (from `/skin set`) → online mode: the player's real Mojang textures (store the base64 on `PlayerEntity` at login) → global `skin-url`.
5. **server.properties**: document the `online-mode` switch with comments.

Testing: offline flow regression first; then a raw Node protocol client (pattern: `C:\Users\gab\AppData\Local\Temp\opencode\skin-test.js` from the skins work) implementing RSA/AES handshake with a fake session — Mojang will 204 at `hasJoined` → proves the handshake bytes, encryption, and kick path; final confirmation needs the user's real MSA account.

## Uncommitted work (in working tree as of 14 Aug 2026, build passes)

`git status` shows (all in `C:\Users\gab\Documents\drumskicodes\vib-MC`):

- `M src/main/java/net/vibmc/server/ServerConfig.java` — `skin-plugin-enabled` flag, `setSkinUrlFor`/`removeSkinUrlFor`/`enableSkinPlugin` + **line-preserving, BOM/CRLF-safe `persist()`** (rewrites only the target key lines, keeps comments; strips UTF-8 BOM on load so text-editor BOMs can't corrupt keys — bug found & fixed during testing).
- `M src/main/java/net/vibmc/player/PlayerManager.java` — textures gated on `skinPluginEnabled()`; new `refreshSkin(PlayerEntity)` (re-sends Player Info remove+add to all online players, live skin swap).
- `?? src/main/java/net/vibmc/command/commands/SkinCommand.java` — `/skin set <url> [player]`, `/skin remove [player]`, `/skin info [player]`; works for **offline** players too (config-only); validates http(s).
- `M src/main/java/net/vibmc/command/CommandManager.java` — registers `SkinCommand`.
- `M src/main/java/net/vibmc/server/VibMC.java` — first-run prompt: on first launch, console asks "Would you like to add this plugin? (y/n)" with a description; `System.console()==null` (e.g. hidden/pipe) → enables by default; writes `skin-plugin-enabled` to server.properties.

Suggested commit (to `backup` ONLY — v0.0.5 rule, origin stays untouched at `669a441`):
`git add -A && git commit -m "Skins plugin: /skin set/remove/info with per-player overrides, skin-plugin-enabled flag, first-run add-plugin prompt, BOM-safe config persistence" && git push backup main`

Testing done so far: build green (`gradle jar`, ~4s warm daemon); console-driven test (Start-Process + RedirectStandardInput on port 25566 in `C:\Users\gab\AppData\Local\Temp\opencode\vibmc-skin-test`) verified: BOM/comment/CRLF preservation, `skin-plugin-enabled=true` written, `/skin set/remove/info` execute, `stop` works. The final test run after the offline-target fix was aborted by the user mid-run — rerun it before trusting the offline-target path (previous run only failed with "Player not found" for offline names, which the fix addresses).

## Other things to remember

- User's server is run **manually** by the user (`java -jar build/libs/vib-mc.jar` in their own console). The instance running during that session (PID 6200) used the **old jar** — needs restart with the new `build/libs/vib-mc.jar` to get `/skin` and the prompt.
- `server.properties` (live): `online-mode=false`, `seed=0`, `skin-url=https://textures.minecraft.net/texture/59f3329902e438a47ee59f1042668a0f1ae4b4f7045dc320e523d3458504109d` (a resolved mineskin texture — mineskin/minesk.in links are HTML pages, NOT PNGs; the client loads the URL as an image directly).
- v0.0.5 rule: **never push to origin** (`github.com/gabytz777/vib-MC.git`, public, at `669a441`); all v0.0.5 work goes to `backup` (`github.com/gabytz777/vib-MC-private.git`, currently at `a85e8ee`).
- Hotfixes still apply to all versions; bot notifications via `POST http://127.0.0.1:9177/notify` (LazyVibeBot must be running).
- Blueprint builder site (`slopmadebymestudios.lol` repo, main `b6bd475`) is done — unrelated to this.
