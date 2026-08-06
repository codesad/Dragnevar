# Dragnevar

A state-of-the-art Ravengard mod!

[Download the latest release](https://github.com/codesad/Dragnevar/releases/latest)

## Important

Open the Ravengard Main Menu once after joining so the mod can detect your class. Item highlights mean it's working.

## Features

- Highlights the best gear for your current class.
- Highlights every healing consumable.
- Shows which cheaper inventory items to replace with more valuable loot.
- Lets you ping locations and hovered items to teammates with middle-click.

## Setup

Install the following dependencies:

- Fabric Language Kotlin
- Fabric API

Run `/rgconfig` in-game to open the configuration screen.

## Team Sync

Team Sync automatically connects players using Dragnevar who are in the same Hypixel party.

The included server is used by default. To host your own, build and run the Team Sync server:

```sh
./gradlew :team-sync-server:shadowJar
TEAM_SYNC_AUDIENCE="wss://your-public-host/path/" \
  java -jar team-sync-server/build/libs/team-sync-server.jar
```

If you self-host, use the same `ws://` or `wss://` URL for both `TEAM_SYNC_AUDIENCE` and the mod configuration.
