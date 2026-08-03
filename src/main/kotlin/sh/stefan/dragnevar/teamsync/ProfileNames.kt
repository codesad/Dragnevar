package sh.stefan.dragnevar.teamsync

import net.minecraft.client.Minecraft
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ProfileNames {
    private val names = ConcurrentHashMap<UUID, String>()

    fun resolve(id: UUID): String {
        val cached = names[id]
        if (cached != null) return cached

        val name = try {
            Minecraft.getInstance().services().sessionService().fetchProfile(id, false)
                ?.profile()
                ?.name()
        } catch (_: Exception) {
            null
        }
        if (name == null) return id.toString()

        names[id] = name
        return name
    }
}
