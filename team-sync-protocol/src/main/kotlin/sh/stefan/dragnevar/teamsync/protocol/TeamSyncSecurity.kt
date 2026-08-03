package sh.stefan.dragnevar.teamsync.protocol

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.URI
import java.util.Base64
import java.util.UUID

object TeamSyncSecurity {
    private const val AUTH_DOMAIN = "dragnevar-team-sync-auth-v1"
    const val NONCE_SIZE = 32

    fun authenticationPayload(
        challenge: AuthChallenge,
        audience: String,
        playerId: UUID
    ): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeByteArray(AUTH_DOMAIN.toByteArray(Charsets.UTF_8))
            data.writeByteArray(normalizeAudience(audience).toByteArray(Charsets.UTF_8))
            data.writeUuid(UUID.fromString(challenge.challengeId))
            data.writeByteArray(Base64.getUrlDecoder().decode(challenge.nonce))
            data.writeLong(challenge.expiresAt)
            data.writeUuid(playerId)
        }
        return output.toByteArray()
    }

    fun normalizeAudience(value: String): String {
        val uri = URI(value).normalize()
        val scheme = uri.scheme?.lowercase() ?: error("Missing WebSocket scheme")
        require(scheme == "ws" || scheme == "wss") { "Expected ws:// or wss://" }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Invalid WebSocket URL"
        }
        val host = uri.host?.lowercase() ?: error("Missing WebSocket host")
        val port = when (scheme) {
            "ws" if uri.port == 80 -> -1
            "wss" if uri.port == 443 -> -1
            else -> uri.port
        }
        val path = if (uri.path.isNullOrBlank()) "/" else uri.path
        return URI(scheme, null, host, port, path, null, null).toASCIIString()
    }

    private fun DataOutputStream.writeByteArray(bytes: ByteArray) {
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeUuid(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }
}
