package sh.stefan.dragnevar.ravengard.item.type

import sh.stefan.dragnevar.ravengard.RavengardName

enum class ConsumableType(override val displayName: String) : RavengardName {
    APPLE("Apple"),
    HEALTH_POTION("Health Potion"),
    BASIC_BANDAGE("Basic Bandage"),
    BANDAGE("Bandage")
}
