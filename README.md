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

for pings, everyone needs the same websocket url and team name. either use `wss://stephn.codes/dragnevar/` while it's available, or host `waypoint-server/server.py` yourself:

```sh
pip install -r waypoint-server/requirements.txt
python waypoint-server/server.py
```

if you self-host, expose it through a `ws://` or `wss://` address your teammates can reach. use a unique team name to avoid joining someone else's room; it is not a password.
