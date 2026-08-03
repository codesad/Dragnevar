package sh.stefan.dragnevar.config

enum class WeaponPriority(private val displayName: String) {
    DPS("DPS"),
    DAMAGE("Damage");

    override fun toString(): String = displayName
}
