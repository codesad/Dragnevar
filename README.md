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

for Team Sync, generate a team code and send it to your teammates. they can paste it into the Team Code field and connect.

the included server is used by default. to host your own, run `team-sync-server/server.py`:

```sh
pip install -r team-sync-server/requirements.txt
python team-sync-server/server.py
```

if you self-host, expose it through a `ws://` or `wss://` address your teammates can reach and change the WebSocket URL in the config.
