package sh.stefan.dragnevar.teamsync

import sh.stefan.dragnevar.teamsync.protocol.AuthChallenge
import sh.stefan.dragnevar.teamsync.protocol.AuthenticatedMessage
import sh.stefan.dragnevar.teamsync.protocol.ClientMessage
import sh.stefan.dragnevar.teamsync.protocol.ErrorMessage
import sh.stefan.dragnevar.teamsync.protocol.JoinedMessage
import sh.stefan.dragnevar.teamsync.protocol.SelectPartyRequest
import sh.stefan.dragnevar.teamsync.protocol.ServerMessage
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncProtocol
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncSecurity
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.UUID
import java.util.concurrent.CompletionStage

class TeamSyncSocket(
    private val onMessage: (ServerMessage) -> Unit,
    private val onStatus: (TeamSyncConnectionState) -> Unit
) {
    private val httpClient = HttpClient.newHttpClient()

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var connectionId = 0L

    @Volatile
    private var authenticated = false

    @Volatile
    private var ready = false

    @Volatile
    private var partyMembers = emptyList<String>()

    val isActive: Boolean
        get() = socket != null

    val isConnected: Boolean
        get() = socket != null && ready

    @Synchronized
    fun connect(url: String, members: Set<UUID>) {
        disconnect()
        val uri = URI.create(url)
        val audience = TeamSyncSecurity.normalizeAudience(url)
        partyMembers = canonicalMembers(members)
        onStatus(TeamSyncConnectionState.Connecting)
        val id = ++connectionId

        httpClient.newWebSocketBuilder()
            .buildAsync(uri, ConnectionListener(id, audience))
            .exceptionally { error ->
                if (connectionId == id) {
                    onStatus(
                        TeamSyncConnectionState.Error(
                            error.cause?.message ?: error.message ?: "Connection failed"
                        )
                    )
                }
                null
            }
    }

    @Synchronized
    fun selectParty(members: Set<UUID>) {
        val selected = canonicalMembers(members)
        if (partyMembers == selected) return
        partyMembers = selected
        if (!authenticated) return
        ready = false
        onStatus(TeamSyncConnectionState.Connecting)
        sendControl(SelectPartyRequest(selected))
    }

    @Synchronized
    fun disconnect() {
        connectionId++
        socket?.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect")
        socket = null
        authenticated = false
        ready = false
        partyMembers = emptyList()
        onStatus(TeamSyncConnectionState.Disconnected)
    }

    fun send(message: ClientMessage): Boolean {
        val activeSocket = socket ?: return false
        if (!ready) return false
        activeSocket.sendText(TeamSyncProtocol.encode(message), true)
        return true
    }

    private fun handleMessage(
        id: Long,
        audience: String,
        rawMessage: String
    ) {
        val message = try {
            TeamSyncProtocol.decodeServer(rawMessage)
        } catch (_: Exception) {
            return
        }

        when (message) {
            is AuthChallenge -> authenticate(id, audience, message)
            is AuthenticatedMessage -> {
                authenticated = true
                onMessage(message)
                sendControl(SelectPartyRequest(partyMembers))
            }
            is JoinedMessage -> {
                ready = true
                onStatus(TeamSyncConnectionState.Connected)
                onMessage(message)
            }
            is ErrorMessage -> onStatus(TeamSyncConnectionState.Error(message.message))
            else -> onMessage(message)
        }
    }

    private fun authenticate(
        id: Long,
        audience: String,
        challenge: AuthChallenge
    ) {
        if (authenticated || connectionId != id) return
        MinecraftIdentity.authenticate(challenge, audience).whenComplete { request, error ->
            if (connectionId != id) return@whenComplete
            if (error != null) {
                onStatus(
                    TeamSyncConnectionState.Error(
                        error.cause?.message ?: error.message ?: "Auth failed"
                    )
                )
                socket?.sendClose(WebSocket.NORMAL_CLOSURE, "authentication failed")
                return@whenComplete
            }
            sendControl(request)
        }
    }

    private fun sendControl(message: ClientMessage): Boolean {
        val activeSocket = socket ?: return false
        activeSocket.sendText(TeamSyncProtocol.encode(message), true)
        return true
    }

    private fun canonicalMembers(members: Set<UUID>): List<String> {
        return members.map { it.toString() }.sorted()
    }

    private inner class ConnectionListener(
        private val id: Long,
        private val audience: String
    ) : WebSocket.Listener {
        private val messageBuffer = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            if (connectionId != id) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "cancelled")
                return
            }

            socket = webSocket
            authenticated = false
            ready = false
            webSocket.request(1)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean
        ): CompletionStage<*>? {
            if (connectionId != id) return null

            messageBuffer.append(data)
            if (last) {
                handleMessage(id, audience, messageBuffer.toString())
                messageBuffer.setLength(0)
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(
            webSocket: WebSocket,
            statusCode: Int,
            reason: String
        ): CompletionStage<*>? {
            if (connectionId == id) {
                socket = null
                authenticated = false
                ready = false
                val detail = if (reason.isBlank()) "" else ": $reason"
                onStatus(TeamSyncConnectionState.Error("Connection closed$detail"))
            }
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            if (connectionId == id) {
                socket = null
                authenticated = false
                ready = false
                onStatus(
                    TeamSyncConnectionState.Error(
                        error.message ?: error.javaClass.simpleName
                    )
                )
            }
        }
    }
}
