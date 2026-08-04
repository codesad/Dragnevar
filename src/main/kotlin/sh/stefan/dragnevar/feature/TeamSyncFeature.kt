package sh.stefan.dragnevar.feature

import net.fabricmc.loader.api.Version
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import sh.stefan.dragnevar.Dragnevar
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.teamsync.HypixelPartyProvider
import sh.stefan.dragnevar.teamsync.ProfileNames
import sh.stefan.dragnevar.teamsync.TeamSyncConnectionState
import sh.stefan.dragnevar.teamsync.TeamSyncSocket
import sh.stefan.dragnevar.teamsync.protocol.AuthenticatedMessage
import sh.stefan.dragnevar.teamsync.protocol.ClientMessage
import sh.stefan.dragnevar.teamsync.protocol.JoinedMessage
import sh.stefan.dragnevar.teamsync.protocol.MemberJoinedMessage
import sh.stefan.dragnevar.teamsync.protocol.MemberLeftMessage
import sh.stefan.dragnevar.teamsync.protocol.ServerMessage
import sh.stefan.dragnevar.teamsync.protocol.TeamMember
import sh.stefan.dragnevar.utils.Chat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.UUID
import kotlin.reflect.KClass

object TeamSyncFeature : Feature(), WorldConnectionFeature, TickFeature, GameMessageFeature {
    private const val MESSAGE_PREFIX = "&b[TS] &r"
    private const val PARTY_REFRESH_TICKS = 200
    private val partyChangeMessage = Regex(
        "^(?:\\[[A-Z0-9+]+] )?[A-Za-z0-9_]{1,16} " +
            "(?:joined the party|has left the party)\\.$"
    )
    private val messageHandlers =
        ConcurrentHashMap<KClass<out ServerMessage>, (ServerMessage) -> Unit>()
    private val teamMembers = ConcurrentHashMap<String, TeamMember>()
    private val socket = TeamSyncSocket(
        ::receiveMessage,
        ::showStatus
    )
    private val partyProvider = HypixelPartyProvider(::receiveParty)
    private var wasEnabled = DragnevarConfig.values.teamSync.connection.enabled
    private var connectionRequested = false
    private var partyRefreshTicks = 0
    private var partyMembers: Set<UUID>? = null

    @Volatile
    var connectionState: TeamSyncConnectionState = TeamSyncConnectionState.Disconnected
        private set

    val isConnected: Boolean
        get() = socket.isConnected

    val members: List<TeamMember>
        get() = teamMembers.values.sortedBy(TeamMember::playerName)

    internal fun roomSnapshot(): Map<UUID, String?>? {
        val roster = partyMembers ?: return null
        val minecraft = Minecraft.getInstance()
        val ownId = minecraft.user.profileId
        return roster.associateWith { id ->
            when {
                id == ownId && socket.isConnected -> minecraft.user.name
                else -> teamMembers[id.toString()]?.playerName
            }
        }
    }

    override fun onWorldJoin() {
        if (!DragnevarConfig.values.teamSync.connection.enabled) return
        startConnection()
    }

    override fun onWorldLeave() {
        stopConnection()
    }

    override fun onTick() {
        val enabled = DragnevarConfig.values.teamSync.connection.enabled
        if (enabled != wasEnabled) {
            wasEnabled = enabled
            if (!enabled) {
                stopConnection()
            } else if (Minecraft.getInstance().level != null) {
                startConnection()
            }
        }

        if (!enabled || !connectionRequested) return
        if (partyRefreshTicks-- <= 0) requestParty()
    }

    override fun onGameMessage(message: Component, overlay: Boolean) {
        if (!overlay && partyChangeMessage.matches(message.string)) refreshParty()
    }

    fun <T : ServerMessage> registerMessageHandler(
        messageClass: KClass<T>,
        handler: (T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val erasedHandler: (ServerMessage) -> Unit = { handler(it as T) }
        check(messageHandlers.putIfAbsent(messageClass, erasedHandler) == null) {
            "A Team Sync handler is already registered for ${messageClass.simpleName}"
        }
    }

    fun send(message: ClientMessage): Boolean =
        DragnevarConfig.values.teamSync.connection.enabled && socket.send(message)

    fun sendPrefixMessage(message: String) {
        Chat.sendPrefixMessage(MESSAGE_PREFIX + message)
    }

    private fun startConnection() {
        val url = DragnevarConfig.values.teamSync.connection.websocketUrl
        if (url.isBlank()) {
            showConnectionError(IllegalArgumentException("WebSocket URL cannot be empty."))
            return
        }
        connectionRequested = true
        partyRefreshTicks = 0
        showStatus(TeamSyncConnectionState.WaitingForParty)
        partyMembers?.let(::connectToParty) ?: requestParty()
    }

    private fun stopConnection() {
        connectionRequested = false
        partyMembers = null
        partyRefreshTicks = 0
        disconnect()
    }

    private fun disconnect() {
        val wasActive = socket.isActive
        socket.disconnect()
        teamMembers.clear()
        if (wasActive) showTeamMessage("&7Disconnected.")
    }

    private fun requestParty() {
        partyRefreshTicks = PARTY_REFRESH_TICKS
        partyProvider.request()
    }

    private fun refreshParty() {
        if (!connectionRequested || !DragnevarConfig.values.teamSync.connection.enabled) return
        requestParty()
    }

    private fun receiveParty(members: Set<UUID>?) {
        if (!connectionRequested || !DragnevarConfig.values.teamSync.connection.enabled) return
        if (members == null) {
            partyMembers = null
            if (socket.isActive) disconnect()
            showStatus(TeamSyncConnectionState.WaitingForParty)
            return
        }

        val playerId = Minecraft.getInstance().user.profileId
        if (playerId !in members) {
            showConnectionError(IllegalStateException("Hypixel returned an invalid party."))
            return
        }

        if (partyMembers == members && socket.isActive) return
        partyMembers = members
        teamMembers.clear()
        connectToParty(members)
    }

    private fun connectToParty(members: Set<UUID>) {
        val url = DragnevarConfig.values.teamSync.connection.websocketUrl
        runCatching {
            if (socket.isActive) {
                socket.selectParty(members)
            } else {
                socket.connect(url, members)
            }
        }.onFailure(::showConnectionError)
    }

    private fun receiveMessage(message: ServerMessage) {
        if (!DragnevarConfig.values.teamSync.connection.enabled) return

        when (message) {
            is AuthenticatedMessage -> checkServerVersion(message.version)
            is JoinedMessage -> {
                receiveTeamMembers(message)
            }
            is MemberJoinedMessage -> message.member.let { member ->
                teamMembers[member.playerId] = member
                showTeamMessage("&a${member.playerName} joined.")
            }
            is MemberLeftMessage -> {
                teamMembers.remove(message.playerId)?.let { member ->
                    showTeamMessage("&c${member.playerName} left.")
                }
            }
            else -> messageHandlers[message::class]?.invoke(message)
        }
    }

    private fun receiveTeamMembers(message: JoinedMessage) {
        val existingMembers = message.members.sortedBy(TeamMember::playerName)
        teamMembers.clear()
        existingMembers.associateByTo(teamMembers, TeamMember::playerId)

        val roster = partyMembers
        val ownId = Minecraft.getInstance().user.profileId
        val connectedIds = existingMembers.mapNotNullTo(mutableSetOf()) {
            runCatching { UUID.fromString(it.playerId) }.getOrNull()
        }
        val missingIds = roster.orEmpty() - connectedIds - ownId
        if (missingIds.isEmpty()) {
            showRoomStatus(existingMembers, emptyList())
            return
        }

        CompletableFuture.supplyAsync {
            missingIds.associateWith(ProfileNames::resolve)
        }.thenAccept { names ->
            Minecraft.getInstance().execute {
                if (partyMembers != roster) return@execute
                val connected = teamMembers.values.sortedBy(TeamMember::playerName)
                val currentConnectedIds = connected.mapNotNullTo(mutableSetOf()) {
                    runCatching { UUID.fromString(it.playerId) }.getOrNull()
                }
                val waiting = (roster.orEmpty() - currentConnectedIds - ownId)
                    .mapNotNull(names::get)
                    .sortedBy(String::lowercase)
                showRoomStatus(connected, waiting)
            }
        }
    }

    private fun showRoomStatus(connected: List<TeamMember>, waiting: List<String>) {
        val connectedText = if (connected.isEmpty()) {
            "&7No teammates are connected yet."
        } else {
            "&fConnected: &a${connected.joinToString { it.playerName }}"
        }
        val waitingText = if (waiting.isEmpty()) {
            ""
        } else {
            "\n&eWaiting for: &6${waiting.joinToString()}"
        }
        showTeamMessage(connectedText + waitingText)
    }

    private fun showTeamMessage(message: String) {
        sendPrefixMessage(message)
    }

    private fun showStatus(state: TeamSyncConnectionState) {
        connectionState = state
        if (state !is TeamSyncConnectionState.Error) return

        sendPrefixMessage("&c${state.message}")
    }

    private fun checkServerVersion(serverVersion: String) {
        val requiredVersion = runCatching { Version.parse(serverVersion) }
            .getOrElse {
                logger.warning("Team Sync returned an invalid version: $serverVersion")
                return
            }
        if (Dragnevar.VERSION >= requiredVersion) return

        sendPrefixMessage(
            "&eDragnevar ${Dragnevar.VERSION.friendlyString} is outdated for Team Sync. " +
                "Some features may be broken or missing. " +
                "Update to $serverVersion or newer."
        )
    }

    private fun showConnectionError(error: Throwable) {
        val message = error.message ?: "Could not connect."
        connectionState = TeamSyncConnectionState.Error(message)
        sendPrefixMessage("&c$message")
    }
}
