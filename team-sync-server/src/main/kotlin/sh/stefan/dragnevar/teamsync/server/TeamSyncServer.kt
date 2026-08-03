package sh.stefan.dragnevar.teamsync.server

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.stefan.dragnevar.teamsync.protocol.AuthChallenge
import sh.stefan.dragnevar.teamsync.protocol.AuthenticateRequest
import sh.stefan.dragnevar.teamsync.protocol.AuthenticatedMessage
import sh.stefan.dragnevar.teamsync.protocol.ErrorMessage
import sh.stefan.dragnevar.teamsync.protocol.JoinedMessage
import sh.stefan.dragnevar.teamsync.protocol.MemberJoinedMessage
import sh.stefan.dragnevar.teamsync.protocol.MemberLeftMessage
import sh.stefan.dragnevar.teamsync.protocol.SelectPartyRequest
import sh.stefan.dragnevar.teamsync.protocol.ServerMessage
import sh.stefan.dragnevar.teamsync.protocol.TeamMember
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncProtocol
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncSecurity
import sh.stefan.dragnevar.teamsync.protocol.WaypointPingMessage
import sh.stefan.dragnevar.teamsync.protocol.WaypointPingRequest
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.math.abs

private const val CHALLENGE_LIFETIME_MILLIS = 15_000L
private const val MAX_PARTY_MEMBERS = 128
private const val MIN_PING_INTERVAL_NANOS = 250_000_000L
private const val MAX_COORDINATE = 30_000_000

class TeamSyncServer(
    private val version: String,
    private val identityVerifier: MinecraftIdentityVerifier
) {
    private val random = SecureRandom()
    private val stateMutex = Mutex()
    private val rooms = mutableMapOf<String, MutableSet<Client>>()
    private val clientsByPlayerId = mutableMapOf<UUID, Client>()

    suspend fun handle(session: DefaultWebSocketServerSession) {
        val challenge = createChallenge()
        var client: Client? = null
        session.sendMessage(challenge)
        val authenticationTimeout = session.launch {
            delay(CHALLENGE_LIFETIME_MILLIS)
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Auth timeout"))
        }

        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) {
                    session.sendMessage(ErrorMessage("Text messages only"))
                    continue
                }

                val message = try {
                    TeamSyncProtocol.decodeClient(frame.readText())
                } catch (_: Exception) {
                    session.sendMessage(ErrorMessage("Invalid message"))
                    continue
                }

                if (client == null) {
                    if (message !is AuthenticateRequest) {
                        session.sendMessage(ErrorMessage("Authenticate first"))
                        continue
                    }
                    client = authenticate(session, challenge, message) ?: return
                    authenticationTimeout.cancel()
                    continue
                }

                when (message) {
                    is SelectPartyRequest -> selectParty(client, message)
                    is WaypointPingRequest -> relayWaypoint(client, message)
                    is AuthenticateRequest -> client.send(ErrorMessage("Already signed in"))
                }
            }
        } finally {
            authenticationTimeout.cancel()
            client?.let { leave(it) }
        }
    }

    private suspend fun authenticate(
        session: DefaultWebSocketServerSession,
        challenge: AuthChallenge,
        request: AuthenticateRequest
    ): Client? {
        val identity = try {
            identityVerifier.verify(request, challenge)
        } catch (_: Exception) {
            session.sendMessage(ErrorMessage("Auth failed"))
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Auth failed"))
            return null
        }

        val client = Client(
            session,
            TeamMember(identity.playerId.toString(), identity.playerName),
            identity.playerId
        )
        val registered = stateMutex.withLock {
            if (clientsByPlayerId.containsKey(identity.playerId)) {
                false
            } else {
                clientsByPlayerId[identity.playerId] = client
                true
            }
        }
        if (!registered) {
            session.sendMessage(ErrorMessage("Already connected"))
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Already connected"))
            return null
        }

        return try {
            client.send(AuthenticatedMessage(version))
            client
        } catch (error: Throwable) {
            remove(client)
            throw error
        }
    }

    private suspend fun selectParty(client: Client, request: SelectPartyRequest) {
        val members = parsePartyMembers(request.memberIds)
        if (members == null || client.playerId !in members) {
            client.send(ErrorMessage("Invalid party"))
            return
        }

        val roomId = partyId(members)
        val move = stateMutex.withLock {
            val oldRoomId = client.roomId
            if (oldRoomId == roomId) {
                val existing = rooms[roomId]
                    .orEmpty()
                    .filterNot { it === client }
                    .map(Client::member)
                return@withLock RoomMove(emptyList(), emptyList(), existing, false)
            }

            val oldRecipients = removeFromRoom(client)
            val newRoom = rooms.getOrPut(roomId, ::linkedSetOf)
            val newRecipients = newRoom.toList()
            val existing = newRecipients.map(Client::member)
            newRoom += client
            client.roomId = roomId
            RoomMove(oldRecipients, newRecipients, existing, true)
        }

        sendToClients(move.oldRecipients, MemberLeftMessage(client.member.playerId))
        client.send(JoinedMessage(move.existingMembers))
        if (move.changedRooms) {
            sendToClients(
                move.newRecipients,
                MemberJoinedMessage(client.member.playerId, client.member.playerName),
            )
        }
    }

    private fun parsePartyMembers(values: List<String>): Set<UUID>? {
        if (values.isEmpty() || values.size > MAX_PARTY_MEMBERS) return null
        val members = linkedSetOf<UUID>()
        try {
            for (value in values) members.add(UUID.fromString(value))
        } catch (_: IllegalArgumentException) {
            return null
        }
        return if (members.size == values.size) members else null
    }

    private suspend fun relayWaypoint(
        client: Client,
        request: WaypointPingRequest
    ) {
        val roomId = client.roomId
        if (roomId == null) {
            client.send(ErrorMessage("Join a party first"))
            return
        }

        val now = System.nanoTime()
        if (now - client.lastPingAt < MIN_PING_INTERVAL_NANOS) return
        client.lastPingAt = now

        if (
            request.dimension.isBlank() ||
            request.dimension.length > 128 ||
            listOf(request.x, request.y, request.z).any { abs(it.toLong()) > MAX_COORDINATE }
        ) {
            client.send(ErrorMessage("Invalid ping"))
            return
        }

        sendToRoom(
            roomId,
            WaypointPingMessage(
                senderId = client.member.playerId,
                senderName = client.member.playerName,
                dimension = request.dimension,
                x = request.x,
                y = request.y,
                z = request.z,
                itemName = request.itemName
            ),
            excludedClient = client
        )
    }

    private suspend fun leave(client: Client) {
        val recipients = remove(client)
        sendToClients(recipients, MemberLeftMessage(client.member.playerId))
    }

    private suspend fun remove(client: Client): List<Client> =
        stateMutex.withLock {
            clientsByPlayerId.remove(client.playerId, client)
            removeFromRoom(client)
        }

    private fun removeFromRoom(client: Client): List<Client> {
        val roomId = client.roomId ?: return emptyList()
        client.roomId = null
        val room = rooms[roomId] ?: return emptyList()
        if (!room.remove(client)) return emptyList()
        if (room.isEmpty()) {
            rooms.remove(roomId)
            return emptyList()
        }
        return room.toList()
    }

    private suspend fun sendToRoom(
        roomId: String,
        message: ServerMessage,
        excludedClient: Client? = null
    ) {
        val recipients = stateMutex.withLock {
            rooms[roomId]
                .orEmpty()
                .filterNot { it === excludedClient }
        }
        sendToClients(recipients, message)
    }

    private suspend fun sendToClients(
        recipients: List<Client>,
        message: ServerMessage
    ) {
        coroutineScope {
            recipients.map { client ->
                async {
                    try {
                        client.send(message)
                    } catch (_: Throwable) {
                    }
                }
            }.awaitAll()
        }
    }

    private fun createChallenge(): AuthChallenge {
        val nonce = ByteArray(TeamSyncSecurity.NONCE_SIZE)
        random.nextBytes(nonce)
        return AuthChallenge(
            challengeId = UUID.randomUUID().toString(),
            nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce),
            expiresAt = System.currentTimeMillis() + CHALLENGE_LIFETIME_MILLIS
        )
    }

    private fun partyId(members: Set<UUID>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = ByteBuffer.allocate(members.size * 16)
        for (member in members.sortedBy { it.toString() }) {
            bytes.putLong(member.mostSignificantBits)
            bytes.putLong(member.leastSignificantBits)
        }
        val hash = digest.digest(bytes.array())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    private suspend fun DefaultWebSocketServerSession.sendMessage(message: ServerMessage) {
        send(Frame.Text(TeamSyncProtocol.encode(message)))
    }

    private data class RoomMove(
        val oldRecipients: List<Client>,
        val newRecipients: List<Client>,
        val existingMembers: List<TeamMember>,
        val changedRooms: Boolean
    )

    private class Client(
        val session: DefaultWebSocketServerSession,
        val member: TeamMember,
        val playerId: UUID
    ) {
        private val sendMutex = Mutex()
        var roomId: String? = null
        var lastPingAt: Long = 0

        suspend fun send(message: ServerMessage) {
            sendMutex.withLock {
                session.send(Frame.Text(TeamSyncProtocol.encode(message)))
            }
        }
    }
}
