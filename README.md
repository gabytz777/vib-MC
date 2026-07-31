# vib-MC

vib-MC is a lightweight, modular Java Minecraft server foundation built with Gradle.

## Features

- Standalone executable JAR via `java -jar vib-mc.jar`
- Basic networking with handshake/status/ping/keepalive/chat/move handling
- Procedural world generation with chunk loading/saving
- Simple mob AI, player state, inventory, and commands
- Event-driven plugin API and scheduler
- Configurable server.properties

## Build

```bash
gradle build
```

## Run

```bash
java -jar build/libs/vib-mc.jar
```
