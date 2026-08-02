package sh.stefan.dragnevar.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import sh.stefan.dragnevar.mixin.ContainerScreenAccessor

object Gui {
    fun hoveredItemName(client: Minecraft): Component? {
        val screen = client.gui.screen() as? AbstractContainerScreen<*> ?: return null
        val stack = (screen as ContainerScreenAccessor).hoveredSlot?.item ?: return null
        return stack.takeUnless { it.isEmpty }?.styledHoverName
    }
}
