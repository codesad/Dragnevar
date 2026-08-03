package sh.stefan.dragnevar.teamsync.server

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import sh.stefan.dragnevar.teamsync.protocol.TeamSyncSecurity
import kotlin.time.Duration.Companion.seconds

private const val MAX_MESSAGE_SIZE = 16_384L

fun main() {
    val host = System.getenv("TEAM_SYNC_HOST") ?: "0.0.0.0"
    val port = System.getenv("TEAM_SYNC_PORT")?.toIntOrNull() ?: 8765
    val version = System.getenv("TEAM_SYNC_VERSION")
        ?: TeamSyncServer::class.java.`package`.implementationVersion
        ?: "dev"
    val audience = TeamSyncSecurity.normalizeAudience(
        System.getenv("TEAM_SYNC_AUDIENCE") ?: "ws://localhost:$port/"
    )
    val server = TeamSyncServer(version, MinecraftIdentityVerifier(audience))

    println("Team Sync $version on $host:$port")
    embeddedServer(Netty, host = host, port = port) {
        install(WebSockets) {
            pingPeriod = 20.seconds
            timeout = 20.seconds
            maxFrameSize = MAX_MESSAGE_SIZE
        }
        routing {
            webSocket("/") {
                server.handle(this)
            }
            webSocket("/{path...}") {
                server.handle(this)
            }
        }
    }.start(wait = true)
}
