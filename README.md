# PackKeep

A Fabric client mod for Minecraft 26.2. It skips the resource-pack reload screen
when a server hands you a pack you already have applied.

Lots of servers mint a fresh pack UUID every time you join, which makes the vanilla
client rebuild an identical pack from scratch on every reconnect. PackKeep notices
the bytes are the same and skips the rebuild.

## What it does

- Skips the reload when a reconnecting server sends a pack whose content is already
  applied.
- Keeps the pack applied through a disconnect, so rejoining the same server does
  nothing.
- Loads the last server's packs during the startup splash, so the first join after
  launching shows no pack screen.
- Accepts server-required packs without the Yes/No prompt.

A pack's identity is the SHA-1 that Minecraft names the downloaded file with, so two
packs with the same contents are treated as the same pack even under different server
UUIDs. Packs without a hashed filename (local or dev packs) are never skipped.

## Config

Open the config screen with the PackKeep keybind (unbound by default, set it under
Options then Controls then Key Binds then PackKeep), or from the Mods list if you
have Mod Menu installed. You can also edit `config/packkeep.json` directly.

The screen exposes the three main switches:

- `enabled`
- `keepResidentAcrossDisconnect`
- `skipReloadOnHashMatch`

The config file also holds `preWarmPinnedAtLaunch`, `autoAcceptRequired`, and
`verboseLog`.

## Dependencies

- Fabric Loader
- Fabric API
- Mod Menu (optional, only for the Mods-list config button)

The config screen is drawn with vanilla widgets, so there is no YACL dependency.

## Build

```
JAVA_HOME=<jdk-25> ./gradlew build
```

Minecraft 26.2 ships Mojang-mapped (unobfuscated), so the build uses
`net.fabricmc.fabric-loom` with no `mappings` line and compiles on JDK 25.
