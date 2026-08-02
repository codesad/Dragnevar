package sh.stefan.dragnevar.ravengard

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

object RavengardDetector {
    private val RAVENGARD_RESOURCE = Identifier.fromNamespaceAndPath(
        "hypixel_ravengard",
        "textures/ui/ravengard.png"
    )

    @JvmStatic
    fun isOnRavengard(): Boolean = Minecraft.getInstance()
        .resourceManager
        .getResource(RAVENGARD_RESOURCE)
        .isPresent
}
