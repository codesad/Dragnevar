package sh.stefan.dragnevar.feature

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.loader.api.Version
import net.minecraft.client.DeltaTracker
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import sh.stefan.dragnevar.Dragnevar
import sh.stefan.dragnevar.config.DragnevarConfig
import sh.stefan.dragnevar.utils.Chat
import sh.stefan.dragnevar.utils.Gui
import sh.stefan.dragnevar.utils.Keybinds
import sh.stefan.dragnevar.utils.Render
import sh.stefan.dragnevar.utils.Render.ScreenPosition
import sh.stefan.dragnevar.waypoint.Waypoint
import sh.stefan.dragnevar.waypoint.WaypointSocket
import sh.stefan.dragnevar.teamsync.TeamSyncConnectionState
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object WaypointFeature :
    Feature(),
    TickFeature,
    KeybindFeature,
    HudRenderFeature,
    WorldConnectionFeature {
    private const val RAYCAST_DISTANCE = 128.0
    private const val ICON_BACKGROUND_COLOR = 0xC0000000.toInt()
    private const val LABEL_BACKGROUND_COLOR = 0x90000000.toInt()
    private const val LABEL_COLOR = 0xFFFFFFFF.toInt()

    private val waypoints = ConcurrentHashMap<String, Waypoint>()
    private val socket = WaypointSocket(
        ::receiveWaypoint,
        ::showStatus,
        ::checkServerVersion
    )

    @Volatile
    var connectionState: TeamSyncConnectionState = TeamSyncConnectionState.Disconnected
        private set

    val isConnected: Boolean
        get() = socket.isConnected

    override val keyMapping = KeyMapping(
        "key.dragnevar.ping",
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_MIDDLE,
        Keybinds.category
    )

    override val hudElementId = Identifier.fromNamespaceAndPath(
        Dragnevar.MOD_ID,
        "waypoints"
    )

    override fun onKeybind() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        pingTarget(player, level, Gui.hoveredItemName(client))
    }

    override fun onTick() {
        val now = System.currentTimeMillis()
        waypoints.entries.removeIf { it.value.expiresAt <= now }
    }

    override fun renderHud(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val camera = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
        if (!camera.initialized) return

        val dimension = level.dimension().identifier().toString()
        waypoints.values
            .filter { it.dimension == dimension }
            .forEach { waypoint ->
                val target = Vec3.atCenterOf(waypoint.position).add(0.0, 0.85, 0.0)
                val position = Render.projectToScreen(
                    graphics,
                    camera,
                    target,
                    horizontalMargin = 12,
                    topMargin = 12,
                    bottomMargin = 20
                ) ?: return@forEach
                drawWaypoint(graphics, camera.pos, waypoint, position)
            }
    }

    override fun onWorldJoin() {
        val config = DragnevarConfig.values.teamSync.connection
        if (config.websocketUrl.isBlank() || config.teamCode.isBlank()) return

        connectConfigured().onFailure(::showConnectionError)
    }

    override fun onWorldLeave() {
        disconnect()
    }

    fun toggleConnection() {
        if (isConnected || connectionState is TeamSyncConnectionState.Connecting) {
            disconnect()
            return
        }

        DragnevarConfig.save()
        connectConfigured().onFailure(::showConnectionError)
    }

    private fun connectConfigured(): Result<Unit> {
        val config = DragnevarConfig.values.teamSync.connection
        return connect(config.websocketUrl, config.teamCode)
    }

    private fun connect(url: String, room: String): Result<Unit> = runCatching {
        require(url.isNotBlank()) { "WebSocket URL cannot be empty." }
        require(room.isNotBlank()) { "Team code cannot be empty." }
        val player = Minecraft.getInstance().player
            ?: error("Join a world before connecting.")
        socket.connect(url, room, player.stringUUID, player.name.string)
    }

    fun disconnect() {
        socket.disconnect()
        waypoints.clear()
    }

    private fun pingTarget(
        player: LocalPlayer,
        level: ClientLevel,
        itemName: Component?
    ) {
        if (!socket.isConnected) {
            Chat.showError(player, "Open /rgconfig and connect first.")
            return
        }

        val hit = player.pick(RAYCAST_DISTANCE, 1.0f, false)
        if (hit.type != HitResult.Type.BLOCK || hit !is BlockHitResult) {
            Chat.showError(player, "You aren't looking at a block.")
            return
        }

        val position = hit.blockPos
        val dimension = level.dimension().identifier().toString()
        if (!socket.sendPing(position, dimension, itemName)) {
            Chat.showError(player, "The waypoint connection isn't ready.")
            return
        }

        addWaypoint(player.stringUUID, player.name.string, dimension, position, itemName)
        playPingSound(player)
    }

    private fun receiveWaypoint(waypoint: Waypoint) {
        val client = Minecraft.getInstance()
        client.execute {
            addWaypoint(
                waypoint.senderId,
                waypoint.senderName,
                waypoint.dimension,
                waypoint.position,
                waypoint.itemName
            )
            client.player?.let(::playPingSound)
        }
    }

    private fun playPingSound(player: LocalPlayer) {
        if (!DragnevarConfig.values.teamSync.waypoints.playPingSound) return
        player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.7f, 3.5f)
    }

    private fun addWaypoint(
        senderId: String,
        senderName: String,
        dimension: String,
        position: BlockPos,
        itemName: Component?
    ) {
        waypoints[senderId] = Waypoint(
            senderId,
            senderName,
            dimension,
            position,
            itemName,
            System.currentTimeMillis() +
                DragnevarConfig.values.teamSync.waypoints.waypointTimeoutSeconds * 1_000L
        )
    }

    private fun drawWaypoint(
        graphics: GuiGraphicsExtractor,
        cameraPosition: Vec3,
        waypoint: Waypoint,
        position: ScreenPosition
    ) {
        val playerColor = Render.locatorBarColor(waypoint.senderId)
        Render.drawDiamond(graphics, position.x, position.y, 5, ICON_BACKGROUND_COLOR)
        Render.drawDiamond(graphics, position.x, position.y, 4, playerColor)
        graphics.fill(position.x - 1, position.y - 1, position.x + 2, position.y + 2, LABEL_COLOR)

        val client = Minecraft.getInstance()
        val playerLabel = Component.literal(waypoint.senderName)
            .withColor(playerColor and 0xFFFFFF)
        if (DragnevarConfig.values.teamSync.waypoints.showWaypointDistance) {
            val distance = cameraPosition
                .distanceTo(Vec3.atCenterOf(waypoint.position))
                .roundToInt()
            playerLabel.append(
                Component.literal(" • ${distance}m").withColor(LABEL_COLOR and 0xFFFFFF)
            )
        }
        val labels = listOfNotNull(playerLabel, waypoint.itemName)
        val textWidth = labels.maxOf(client.font::width)
        val textY = (position.y + 8)
            .coerceAtMost(graphics.guiHeight() - client.font.lineHeight * labels.size - 2)
        val backgroundX = (position.x - textWidth / 2)
            .coerceIn(2, graphics.guiWidth() - textWidth - 2)

        graphics.fill(
            backgroundX - 2,
            textY - 1,
            backgroundX + textWidth + 2,
            textY + client.font.lineHeight * labels.size,
            LABEL_BACKGROUND_COLOR
        )
        labels.forEachIndexed { index, label ->
            val labelWidth = client.font.width(label)
            val labelX = (position.x - labelWidth / 2)
                .coerceIn(2, graphics.guiWidth() - labelWidth - 2)
            graphics.text(
                client.font,
                label,
                labelX,
                textY + client.font.lineHeight * index,
                LABEL_COLOR,
                true
            )
        }
    }

    private fun showStatus(state: TeamSyncConnectionState) {
        connectionState = state
        if (state !is TeamSyncConnectionState.Error) return

        val client = Minecraft.getInstance()
        client.execute {
            client.player?.let { player ->
                Chat.showError(player, state.message)
            }
        }
    }

    private fun checkServerVersion(serverVersion: String) {
        val requiredVersion = runCatching { Version.parse(serverVersion) }
            .getOrElse {
                logger.warning("Team Sync returned an invalid version: $serverVersion")
                return
            }
        if (Dragnevar.VERSION >= requiredVersion) return

        val client = Minecraft.getInstance()
        client.execute {
            client.player?.let { player ->
                Chat.showError(
                    player,
                    "Dragnevar ${Dragnevar.VERSION.friendlyString} is outdated for Team Sync. " +
                        "Some features may be broken or missing. " +
                        "Update to $serverVersion or newer."
                )
            }
        }
    }

    private fun showConnectionError(error: Throwable) {
        val message = error.message ?: "Could not connect."
        connectionState = TeamSyncConnectionState.Error(message)
        val client = Minecraft.getInstance()
        client.execute {
            client.player?.let { player ->
                Chat.showError(player, message)
            }
        }
    }
}
