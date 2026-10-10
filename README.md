# PackKeep (multi-version)

A Fabric client mod that skips the resource-pack reload screen when a server hands
you a pack you already have applied. This is the Stonecutter multi-version source
tree covering Minecraft 1.21 through 1.21.11.

Lots of servers mint a fresh pack UUID every time you join, so the vanilla client
rebuilds an identical pack on every reconnect. PackKeep matches on the pack's content
hash and skips the rebuild.

## What it does

- Skips the reload when a reconnecting server sends a pack that is already applied.
- Keeps the pack applied through a disconnect, so rejoining the same server does
  nothing.

Identity is the pack's content hash, not the server-sent UUID.

## Config

Open the config screen with the PackKeep keybind (unbound by default, set it under
Options then Controls then Key Binds then PackKeep), or from the Mods list if Mod
Menu is installed. You can also edit `config/packkeep.json`.

The screen holds the three switches: `enabled`, `keepResidentAcrossDisconnect`,
`skipReloadOnHashMatch`.

## Dependencies

- Fabric Loader
- Fabric API
- Mod Menu (optional, only for the Mods-list config button)

The config screen uses vanilla widgets, so there is no YACL dependency.

## Versions

One source tree builds 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6,
1.21.7, 1.21.8, 1.21.9, 1.21.10, and 1.21.11 through Stonecutter. The only
version-conditional code is the keybind category, which changed in 1.21.9.

## Build

```
JAVA_HOME=<jdk-25> ./gradlew "1.21.11:build"
```

Build one version with its `<version>:build` task, or all of them by listing every
task with `--no-parallel` (the loom remapper races under parallel execution). The
Gradle daemon runs on JDK 25; compilation targets release 21.
