package sh.stefan.dragnevar.ravengard.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.ravengard.label.AccessoryType
import sh.stefan.dragnevar.ravengard.label.ArmorType
import sh.stefan.dragnevar.ravengard.label.ProfileLabel
import sh.stefan.dragnevar.ravengard.label.Rarity
import sh.stefan.dragnevar.ravengard.label.WeaponType
import sh.stefan.dragnevar.ravengard.label.findAllIn
import sh.stefan.dragnevar.ravengard.label.findIn

class RavengardItemData(val stack: ItemStack) {
    private val lore = stack.get(DataComponents.LORE)?.lines()?.map { it.string }.orEmpty()

    // ravengard labels can show up in either the item's name or its lore
    private val text = listOf(stack.hoverName.string) + lore

    val rarity: Rarity? = Rarity.entries.findIn(text)
    val profiles: Set<Profile> = ProfileLabel.entries
        .findAllIn(text)
        .mapTo(mutableSetOf(), ProfileLabel::profile)
    val armorType: ArmorType? = ArmorType.entries.findIn(text)
    val accessoryType: AccessoryType? = AccessoryType.entries.findIn(text)
    val weaponType: WeaponType? = WeaponType.entries.findIn(text)
    val defense: Double? = findNumber(DEFENSE_PATTERN)
    val damage: Double? = findNumber(DAMAGE_PATTERN)
    val attackSpeed: Double? = findNumber(ATTACK_SPEED_PATTERN)

    // no profile labels means the item has no class requirement
    fun isCompatibleWith(profile: Profile): Boolean =
        profiles.isEmpty() || profile in profiles

    private fun findNumber(pattern: Regex): Double? {
        return lore.firstNotNullOfOrNull { line ->
            pattern.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
        }
    }

    private companion object {
        val DAMAGE_PATTERN = Regex(
            """([+-]?\d+(?:\.\d+)?)\s*Damage\b""",
            RegexOption.IGNORE_CASE
        )
        val ATTACK_SPEED_PATTERN = Regex(
            """([+-]?\d+(?:\.\d+)?)\s*Attack Speed\b""",
            RegexOption.IGNORE_CASE
        )
        val DEFENSE_PATTERN = Regex(
            """([+-]?\d+(?:\.\d+)?)\s*Defen[cs]e\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
