package sh.stefan.dragnevar.teamsync

import java.security.SecureRandom

object TeamCode {
    private const val CODE_LENGTH = 16
    private const val CODE_CHARACTERS =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    private val random = SecureRandom()

    fun generate(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) {
            append(CODE_CHARACTERS[random.nextInt(CODE_CHARACTERS.length)])
        }
    }
}
