package sh.stefan.dragnevar.ravengard.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import sh.stefan.dragnevar.ravengard.Profile
import sh.stefan.dragnevar.ravengard.ProfileLabel
import sh.stefan.dragnevar.ravengard.Rarity
import sh.stefan.dragnevar.ravengard.findAllIn
import sh.stefan.dragnevar.ravengard.findByName
import sh.stefan.dragnevar.ravengard.findIn
import sh.stefan.dragnevar.ravengard.item.type.AccessoryType
import sh.stefan.dragnevar.ravengard.item.type.ArmorType
import sh.stefan.dragnevar.ravengard.item.type.ConsumableType
import sh.stefan.dragnevar.ravengard.item.type.WeaponType

class RavengardItemData(val stack: ItemStack) {
    private val name = stack.hoverName.string
    private val lore = stack.get(DataComponents.LORE)?.lines()?.map { it.string }.orEmpty()

    val rarity: Rarity? = Rarity.entries.findIn(lore)
    val profiles: Set<Profile> = ProfileLabel.entries
        .findAllIn(lore)
        .mapTo(mutableSetOf(), ProfileLabel::profile)
    val armorType: ArmorType? = ArmorType.entries.findIn(lore)
    val accessoryType: AccessoryType? = AccessoryType.entries.findIn(lore)
    val weaponType: WeaponType? = WeaponType.entries.findIn(lore)
    val consumableType: ConsumableType? = ConsumableType.entries.findByName(name)
    val defense: Double? = findNumber(DEFENSE_PATTERN)
    val damage: Double? = findNumber(DAMAGE_PATTERN)
    val attackSpeed: Double? = findNumber(ATTACK_SPEED_PATTERN)
    val healing: Double? = consumableType?.let { findNumber(HEALING_PATTERN) }
    val healingDurationSeconds: Double? = healing?.let {
        findNumber(HEALING_DURATION_PATTERN)
    }
    val price: Int? = lore.firstNotNullOfOrNull { line ->
        PRICE_PATTERN.find(line)
            ?.groupValues
            ?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull()
    }

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
        val HEALING_PATTERN = Regex(
            """Heals:?\s*\+(\d+(?:\.\d+)?)\s*HP\b""",
            RegexOption.IGNORE_CASE
        )
        val HEALING_DURATION_PATTERN = Regex(
            """\bover\s+(\d+(?:\.\d+)?)\s+seconds?\b""",
            RegexOption.IGNORE_CASE
        )
        val PRICE_PATTERN = Regex(
            """\b(\d[\d,]*)\s+Crowns?\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
