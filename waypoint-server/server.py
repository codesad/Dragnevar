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


@dataclass
class Client:
    socket: ServerConnection
    room: str
    player_id: str
    player_name: str
    last_ping: float = 0.0


rooms: dict[str, set[ServerConnection]] = defaultdict(set)
clients: dict[ServerConnection, Client] = {}


async def send_error(socket: ServerConnection, message: str) -> None:
    await socket.send(json.dumps({"type": "error", "message": message}))


async def join(socket: ServerConnection, message: dict) -> Client | None:
    room = str(message.get("room", "")).strip()
    player_id = str(message.get("playerId", "")).strip()
    player_name = str(message.get("playerName", "")).strip()

    if not room or len(room) > 64:
        await send_error(socket, "Invalid room")
        return None
    if not player_id or len(player_id) > 64:
        await send_error(socket, "Invalid player ID")
        return None
    if not player_name or len(player_name) > 32:
        await send_error(socket, "Invalid player name")
        return None

    client = Client(socket, room, player_id, player_name)
    clients[socket] = client
    rooms[room].add(socket)
    await socket.send(json.dumps({"type": "joined", "room": room}))
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

    payload = json.dumps(
        {
            "type": "ping",
            "senderId": client.player_id,
            "senderName": client.player_name,
            "dimension": dimension,
            "x": x,
            "y": y,
            "z": z,
            **({"itemName": message["itemName"]} if "itemName" in message else {}),
        }
    )
    teammates = [socket for socket in rooms[client.room] if socket is not client.socket]
    if teammates:
        await asyncio.gather(
            *(socket.send(payload) for socket in teammates),
            return_exceptions=True,
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
                    await send_error(socket, "Join a room first")
                    continue
                client = await join(socket, message)
            elif message_type == "ping":
                await relay_ping(client, message)
            else:
                await send_error(socket, "Unknown message type")
    finally:
        client = clients.pop(socket, None)
        if client is not None:
            room = rooms[client.room]
            room.discard(socket)
            if not room:
                rooms.pop(client.room, None)


async def main() -> None:
    host = os.getenv("WAYPOINT_HOST", "0.0.0.0")
    port = int(os.getenv("WAYPOINT_PORT", "8765"))
    async with serve(handler, host, port, max_size=MAX_MESSAGE_SIZE):
        print(f"Waypoint server listening on ws://{host}:{port}")
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
