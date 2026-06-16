package com.example.funlife.pacmaze.server

import com.example.funlife.pacmaze.server.auth.PacMazePbAuth
import com.example.funlife.pacmaze.server.room.PacMazeRoomRegistry
import com.example.funlife.social.game.engine.pacmaze.PacMazeWsProtocol
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.http.HttpMethod
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PAC_MAZE_WS_PORT")?.toIntOrNull() ?: 8791
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    val pbBase = System.getenv("PB_BASE_URL") ?: "http://127.0.0.1:8090"
    val auth = PacMazePbAuth(pbBase)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val registry = PacMazeRoomRegistry(scope)

    install(CallLogging) { level = Level.INFO }
    install(CORS) { anyHost() }
    install(
        createApplicationPlugin(name = "PacMazeHealth") {
            onCall { call ->
                val path = call.request.path().removeSuffix("/")
                val isHealth = path == "/health" || path == "/pac-maze-ws/health"
                if (isHealth && call.request.httpMethod == HttpMethod.Get) {
                    call.respondText(
                        JsonUtil.toJson(
                            mapOf(
                                "ok" to true,
                                "service" to "funlife-pac-maze-ws",
                                "version" to 2,
                                "pathPrefix" to "/pac-maze-ws",
                                "rooms" to registry.activeRoomCount(),
                            ),
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.OK,
                    )
                }
            }
        },
    )
    install(WebSockets) {
        pingPeriodMillis = 15_000L
        timeoutMillis = 30_000L
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/pac-maze-ws") {
            val roomId = call.request.queryParameters["roomId"]
                ?: call.request.queryParameters["room"]
                ?: run {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "missing roomId"))
                    return@webSocket
                }
            val token = call.request.queryParameters["token"]?.trim().orEmpty()
            if (token.isBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "missing token"))
                return@webSocket
            }
            val authResult = runCatching { auth.authenticateJoin(token, roomId) }.getOrElse { err ->
                send(
                    Frame.Text(
                        JsonUtil.toJson(
                            PacMazeWsProtocol.buildError("auth_failed", err.message ?: "auth_failed"),
                        ),
                    ),
                )
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "auth_failed"))
                return@webSocket
            }
            val session = registry.getOrCreate(roomId, authResult.room)
            session.attachPeer(authResult, this)
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val payload = runCatching { JsonUtil.parseMessage(frame.readText()) }.getOrNull()
                        ?: continue
                    session.handleMessage(authResult.userId, payload)
                }
            } finally {
                session.detachPeer(authResult.userId)
            }
        }
    }
}
