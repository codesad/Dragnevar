package sh.stefan.dragnevar.feature

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.utils.Chat

object ClassDetector : Feature(), ContainerOpenFeature {
    private const val PROFILE_MENU_SLOT_INDEX = 27

    var currentProfile: Profile? = null
        private set

    override fun onContainerOpen(screen: AbstractContainerScreen<*>) {
        val title = Chat.stripFormatting(screen.title.string)
        if (!title.contains("Main Menu", ignoreCase = true)) return

        currentProfile = screen.menu.slots
            .getOrNull(PROFILE_MENU_SLOT_INDEX)
            ?.item
            ?.takeUnless { it.isEmpty }
            ?.let(::parseProfile)

        currentProfile?.let {
            logger.info("Detected Ravengard profile: ${it.displayName}")
        }
    }

    private fun parseProfile(item: ItemStack): Profile? {
        val name = Chat.stripFormatting(item.hoverName.string)
            .substringAfterLast(" - ", missingDelimiterValue = "")
        return Profile.parse(name)
    }
}
