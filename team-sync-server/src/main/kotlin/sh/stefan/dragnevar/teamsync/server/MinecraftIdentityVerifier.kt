package sh.stefan.dragnevar.teamsync.server

import com.mojang.authlib.yggdrasil.ServicesKeyType
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.stefan.dragnevar.teamsync.protocol.AuthChallenge
import sh.stefan.dragnevar.teamsync.protocol.AuthenticateRequest
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncSecurity
import java.net.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val MAX_PUBLIC_KEY_SIZE = 1_024
private const val MAX_SIGNATURE_SIZE = 1_024
private const val CERTIFICATE_CLOCK_SKEW_MILLIS = 120_000L

data class VerifiedIdentity(
    val playerId: UUID,
    val playerName: String
)

class MinecraftIdentityVerifier(
    private val expectedAudience: String
) {
    private val authenticationService = YggdrasilAuthenticationService(Proxy.NO_PROXY)
    private val servicesKeySet = authenticationService.servicesKeySet
    private val sessionService = authenticationService.createMinecraftSessionService()
    private val playerNames = ConcurrentHashMap<UUID, String>()

    suspend fun verify(
        request: AuthenticateRequest,
        challenge: AuthChallenge
    ): VerifiedIdentity = withContext(Dispatchers.IO) {
        verifyBlocking(request, challenge)
    }

    private fun verifyBlocking(
        request: AuthenticateRequest,
        challenge: AuthChallenge
    ): VerifiedIdentity {
        require(challenge.expiresAt >= System.currentTimeMillis()) { "Expired challenge" }
        require(TeamSyncSecurity.normalizeAudience(request.audience) == expectedAudience) {
            "Wrong audience"
        }

        val playerId = UUID.fromString(request.playerId)
        val publicKeyBytes = decode(request.publicKey, MAX_PUBLIC_KEY_SIZE)
        val certificateSignature = decode(request.certificateSignature, MAX_SIGNATURE_SIZE)
        val challengeSignature = decode(request.challengeSignature, MAX_SIGNATURE_SIZE)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(publicKeyBytes)) as RSAPublicKey

        require(publicKey.modulus.bitLength() in 2_048..4_096) { "Bad key size" }
        require(
            request.certificateExpiresAt >=
                Instant.now().toEpochMilli() - CERTIFICATE_CLOCK_SKEW_MILLIS
        ) { "Expired key" }

        val certificatePayload = ByteBuffer
            .allocate(24 + publicKey.encoded.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(playerId.mostSignificantBits)
            .putLong(playerId.leastSignificantBits)
            .putLong(request.certificateExpiresAt)
            .put(publicKey.encoded)
            .array()
        val certificateValid = servicesKeySet
            .keys(ServicesKeyType.PROFILE_KEY)
            .any { key ->
                val verifier = key.signature()
                verifier.update(certificatePayload)
                verifier.verify(certificateSignature)
            }
        require(certificateValid) { "Bad certificate" }

        val proof = Signature.getInstance("SHA256withRSA")
        proof.initVerify(publicKey)
        proof.update(TeamSyncSecurity.authenticationPayload(challenge, request.audience, playerId))
        require(proof.verify(challengeSignature)) { "Bad proof" }

        var playerName = playerNames[playerId]
        if (playerName == null) {
            playerName = sessionService.fetchProfile(playerId, false)
                ?.profile()
                ?.name()
                ?: error("Profile not found")
            playerNames[playerId] = playerName
        }
        return VerifiedIdentity(playerId, playerName)
    }

    private fun decode(value: String, maximumSize: Int): ByteArray {
        val decoded = Base64.getDecoder().decode(value)
        require(decoded.size <= maximumSize) { "Value too large" }
        return decoded
    }
}
