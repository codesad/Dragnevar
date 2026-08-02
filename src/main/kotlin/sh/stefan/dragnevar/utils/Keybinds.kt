package sh.stefan.dragnevar.utils

import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import sh.stefan.dragnevar.Dragnevar

object Keybinds {
    val category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(Dragnevar.MOD_ID, "main")
    )
}
