package sh.stefan.dragnevar.teamsync

import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.resolver.ServerAddress
import java.util.Locale
import java.util.UUID

class HypixelPartyProvider(
    private val onPartyChanged: (Set<UUID>?) -> Unit
) {
    init {
        HypixelModAPI.getInstance().createHandler(ClientboundPartyInfoPacket::class.java) { packet ->
            val members = if (packet.isInParty) packet.members.toSet() else null
            Minecraft.getInstance().execute {
                onPartyChanged(members.withFakeMembers())
            }
        }
    }

    private fun Set<UUID>?.withFakeMembers(): Set<UUID>? =
        (this.orEmpty() + fakeMembers).takeIf { it.isNotEmpty() }

    fun request(): Boolean {
        if (!isConnectedToHypixel()) {
            onPartyChanged(null)
            return false
        }
        return try {
            HypixelModAPI.getInstance().sendPacket(ServerboundPartyInfoPacket())
        } catch (_: Exception) {
            false
        }
    }

    private fun isConnectedToHypixel(): Boolean {
        val address = Minecraft.getInstance().currentServer?.ip ?: return false
        val host = ServerAddress.parseString(address)
            .host
            .trimEnd('.')
            .lowercase(Locale.ROOT)
        return host == "hypixel.net" || host.endsWith(".hypixel.net")
    }

    companion object {
        // for solo dev testing
        private val fakeMembers = mutableSetOf<UUID>()

        fun addFakeMember(uuid: UUID) {
            fakeMembers.add(uuid)
        }

        fun removeFakeMember(uuid: UUID) {
            fakeMembers.remove(uuid)
        }

        fun clearFakeMembers() {
            fakeMembers.clear()
        }
    }
}
