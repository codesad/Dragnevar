from __future__ import annotations

import asyncio
import json
import math
import os
import time
from collections import defaultdict
from dataclasses import dataclass

from websockets.asyncio.server import ServerConnection, serve


MAX_MESSAGE_SIZE = 4_096
MIN_PING_INTERVAL = 0.25
TEAM_SYNC_VERSION = os.getenv("TEAM_SYNC_VERSION", "1.3.0")


@dataclass
class Client:
    socket: ServerConnection
    team_code: str
    player_id: str
    player_name: str
    last_ping: float = 0.0


teams: dict[str, set[ServerConnection]] = defaultdict(set)
clients: dict[ServerConnection, Client] = {}


async def send_error(socket: ServerConnection, message: str) -> None:
    await socket.send(json.dumps({"type": "error", "message": message}))


async def send_to_team(
    team_code: str,
    message: dict,
    excluded_socket: ServerConnection | None = None,
) -> None:
    recipients = [
        socket for socket in teams[team_code] if socket is not excluded_socket
    ]
    if recipients:
        payload = json.dumps(message)
        await asyncio.gather(
            *(socket.send(payload) for socket in recipients),
            return_exceptions=True,
        )


async def join_team(socket: ServerConnection, message: dict) -> Client | None:
    team_code = str(message.get("teamCode", message.get("room", ""))).strip()
    player_id = str(message.get("playerId", "")).strip()
    player_name = str(message.get("playerName", "")).strip()

    if not team_code or len(team_code) > 64:
        await send_error(socket, "Invalid team code")
        return None
    if not player_id or len(player_id) > 64:
        await send_error(socket, "Invalid player ID")
        return None
    if not player_name or len(player_name) > 32:
        await send_error(socket, "Invalid player name")
        return None

    existing_members = [clients[member] for member in teams[team_code]]
    client = Client(socket, team_code, player_id, player_name)
    clients[socket] = client
    teams[team_code].add(socket)
    await socket.send(
        json.dumps(
            {
                "type": "joined",
                "teamCode": team_code,
                "version": TEAM_SYNC_VERSION,
                "members": [
                    {
                        "playerId": member.player_id,
                        "playerName": member.player_name,
                    }
                    for member in existing_members
                ],
            }
        )
    )
    await send_to_team(
        team_code,
        {
            "type": "member_joined",
            "playerId": player_id,
            "playerName": player_name,
        },
        excluded_socket=socket,
    )
    return client


def parse_coordinate(message: dict, name: str) -> int:
    value = message[name]
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError
    if not math.isfinite(value) or abs(value) > 30_000_000:
        raise ValueError
    return int(value)


async def relay_ping(client: Client, message: dict) -> None:
    now = time.monotonic()
    if now - client.last_ping < MIN_PING_INTERVAL:
        return
    client.last_ping = now

    try:
        dimension = str(message["dimension"])
        x = parse_coordinate(message, "x")
        y = parse_coordinate(message, "y")
        z = parse_coordinate(message, "z")
    except (KeyError, TypeError, ValueError):
        await send_error(client.socket, "Invalid ping")
        return

    if not dimension or len(dimension) > 128:
        await send_error(client.socket, "Invalid dimension")
        return

    await send_to_team(
        client.team_code,
        {
            "type": "ping",
            "senderId": client.player_id,
            "senderName": client.player_name,
            "dimension": dimension,
            "x": x,
            "y": y,
            "z": z,
            **({"itemName": message["itemName"]} if "itemName" in message else {}),
        },
        excluded_socket=client.socket,
    )


async def handler(socket: ServerConnection) -> None:
    client = None
    try:
        async for raw_message in socket:
            if not isinstance(raw_message, str):
                await send_error(socket, "Binary messages aren't supported")
                continue

            try:
                message = json.loads(raw_message)
            except json.JSONDecodeError:
                await send_error(socket, "Invalid JSON")
                continue

            if not isinstance(message, dict):
                await send_error(socket, "Messages must be JSON objects")
                continue

            message_type = message.get("type")
            if client is None:
                if message_type != "join":
                    await send_error(socket, "Join a team first")
                    continue
                client = await join_team(socket, message)
            elif message_type == "ping":
                await relay_ping(client, message)
            else:
                await send_error(socket, "Unknown message type")
    finally:
        client = clients.pop(socket, None)
        if client is not None:
            team = teams[client.team_code]
            team.discard(socket)
            await send_to_team(
                client.team_code,
                {
                    "type": "member_left",
                    "playerId": client.player_id,
                },
            )
            if not team:
                teams.pop(client.team_code, None)


async def main() -> None:
    host = os.getenv("TEAM_SYNC_HOST", "0.0.0.0")
    port = int(os.getenv("TEAM_SYNC_PORT", "8765"))
    async with serve(handler, host, port, max_size=MAX_MESSAGE_SIZE):
        print(
            f"Team Sync server {TEAM_SYNC_VERSION} listening on ws://{host}:{port}"
        )
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
