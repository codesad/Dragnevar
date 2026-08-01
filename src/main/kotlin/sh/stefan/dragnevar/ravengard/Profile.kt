package sh.stefan.dragnevar.ravengard

enum class Profile(val displayName: String) {
    WARRIOR("Warrior"),
    HUNTER("Hunter"),
    KNIGHT("Knight"),
    RANGER("Ranger");

    companion object {
        fun parse(value: String): Profile? {
            val name = value.trim()
            return entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true)
            }
        }
    }
}
