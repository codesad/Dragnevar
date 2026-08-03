# Dragnevar

a state-of-the-art ravengard mod! (because there isn't another one)

[download the latest release](https://github.com/codesad/Dragnevar/releases/latest)

# important

open the ravengard main menu once after joining so the mod can detect your class. item highlights mean it's working.

# features

- highlights the best gear for your current class
- highlights every healing consumable
- lets you ping locations and hovered items to teammates with middle click

# setup

install:

- fabric language kotlin
- fabric api

run `/rgconfig` in-game to open the config.

Team Sync automatically connects players using Dragnevar who are in the same Hypixel party. the official Hypixel Mod API mod is required.

the included server is used by default. to host your own, build and run the Team Sync server:

```sh
./gradlew :team-sync-server:shadowJar
TEAM_SYNC_AUDIENCE=wss://your-public-host/path/ java -jar team-sync-server/build/libs/team-sync-server.jar
```

if you self-host, use the same `ws://` or `wss://` URL for both `TEAM_SYNC_AUDIENCE` and the mod config.
