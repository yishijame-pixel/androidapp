package com.example.funlife.social.pacmazews

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

sealed class PacMazeWsEvent {
    data object Disconnected : PacMazeWsEvent()
    data class Joined(
        val entityId: String,
        val isHost: Boolean,
        val tick: Long,
        val state: Map<String, Any?>?,
    ) : PacMazeWsEvent()

    data class State(val payload: Map<String, Any?>) : PacMazeWsEvent()
    data class RoomGo(val startMs: Long) : PacMazeWsEvent()
    data class MatchEnd(
        val winnerEntityId: String?,
        val scoreA: Int,
        val scoreB: Int,
        val reason: String,
    ) : PacMazeWsEvent()

    data class Pong(val clientMs: Long, val serverMs: Long, val rttMs: Long) : PacMazeWsEvent()

    data class PeerInput(
        val entityId: String,
        val tick: Long,
        val dir: String?,
        val release: Boolean,
        val attack: Boolean,
        val seq: Long,
    ) : PacMazeWsEvent()

    data class RoomState(
        val peerCount: Int,
        val readyCount: Int,
        val expectedPeers: Int,
    ) : PacMazeWsEvent()

    data class Error(val code: String, val message: String) : PacMazeWsEvent()
}

/**
 * Pac-Maze 权威服 WebSocket 会话：连接、输入上行、状态下行、断线重连。
 */
class PacMazeWsSession(
    private val scope: CoroutineScope,
    private val roomId: String,
    private val token: String,
) {
    companion object {
        private const val TAG = PacMazeOnlineDiagnostics.WS_TAG
        private val client = OkHttpClient.Builder()
            .pingInterval(0, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    private val _events = MutableSharedFlow<PacMazeWsEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<PacMazeWsEvent> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private val stopped = AtomicBoolean(false)
    private var backoffMs = 800L
    private val outboundQueue = ArrayDeque<String>()
    @Volatile
    private var serverJoined = false

    @Volatile
    private var lastPingClientMs: Long = 0L

    val isJoined: Boolean get() = serverJoined && socket != null

    fun start() {
        stopped.set(false)
        serverJoined = false
        connect()
    }

    fun stop() {
        stopped.set(true)
        serverJoined = false
        pingJob?.cancel()
        reconnectJob?.cancel()
        synchronized(outboundQueue) { outboundQueue.clear() }
        socket?.close(1000, "stop")
        socket = null
        scope.launch { _events.emit(PacMazeWsEvent.Disconnected) }
    }

    fun sendInput(tick: Long, dir: String?, attack: Boolean, seq: Long, release: Boolean = false): Boolean {
        val payload = buildString {
            append("{\"t\":\"input\",\"tick\":")
            append(tick)
            append(",\"seq\":")
            append(seq)
            append(",\"attack\":")
            append(attack)
            if (release) {
                append(",\"release\":true")
            } else if (!dir.isNullOrBlank()) {
                append(",\"dir\":\"")
                append(dir)
                append('"')
            }
            append('}')
        }
        return enqueueOrSend(payload)
    }

    fun sendReady(): Boolean = enqueueOrSend("{\"t\":\"ready\"}")

    private fun enqueueOrSend(raw: String): Boolean {
        if (stopped.get()) return false
        if (serverJoined) return sendNow(raw)
        synchronized(outboundQueue) {
            if (outboundQueue.size >= 256) outboundQueue.removeFirst()
            outboundQueue.addLast(raw)
        }
        return true
    }

    private fun sendNow(raw: String): Boolean = socket?.send(raw) == true

    private fun flushOutboundQueue() {
        val batch = synchronized(outboundQueue) {
            if (outboundQueue.isEmpty()) return
            outboundQueue.toList().also { outboundQueue.clear() }
        }
        batch.forEach { sendNow(it) }
    }

    private fun connect() {
        if (stopped.get()) return
        val base = PacMazeWsConfig.url().trimEnd('/')
        val wsUrl = buildString {
            append(base)
            if (!base.endsWith("/pac-maze-ws")) append("/pac-maze-ws")
            append("?token=")
            append(java.net.URLEncoder.encode(token, Charsets.UTF_8.name()))
            append("&roomId=")
            append(java.net.URLEncoder.encode(roomId, Charsets.UTF_8.name()))
        }
        val request = Request.Builder().url(wsUrl).build()
        socket?.cancel()
        serverJoined = false
        socket = client.newWebSocket(request, Listener())
        Log.d(TAG, "connecting room=$roomId")
    }

    private fun scheduleReconnect() {
        if (stopped.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(8_000L)
            connect()
        }
    }

    private fun startPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && !stopped.get()) {
                delay(PacMazeWsConfig.pingIntervalMs())
                lastPingClientMs = System.currentTimeMillis()
                sendNow("{\"t\":\"ping\",\"clientMs\":$lastPingClientMs}")
            }
        }
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "open room=$roomId")
            backoffMs = 800L
            startPing()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            parseMessage(text)?.let { event ->
                if (event is PacMazeWsEvent.State && event.payload["tick"]?.toString()?.toLongOrNull()?.let { it % 60L == 0L } == true) {
                    Log.d(TAG, "state tick=${event.payload["tick"]}")
                }
                if (event is PacMazeWsEvent.Joined) {
                    serverJoined = true
                    flushOutboundQueue()
                }
                if (!_events.tryEmit(event)) {
                    scope.launch { _events.emit(event) }
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "closed room=$roomId code=$code")
            serverJoined = false
            socket = null
            pingJob?.cancel()
            if (!stopped.get()) scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "failure room=$roomId: ${t.message}")
            serverJoined = false
            socket = null
            pingJob?.cancel()
            if (!stopped.get()) scheduleReconnect()
        }
    }

    private fun parseMessage(text: String): PacMazeWsEvent? = runCatching {
        val obj = JsonParser.parseString(text).asJsonObject
        when (obj.get("t")?.asString) {
            "joined" -> PacMazeWsEvent.Joined(
                entityId = obj.get("entityId")?.asString.orEmpty(),
                isHost = obj.get("isHost")?.asBoolean == true,
                tick = obj.get("tick")?.asLong ?: 0L,
                state = obj.get("state")?.asJsonObject?.let { jsonObjectToMap(it) },
            )
            "state" -> PacMazeWsEvent.State(jsonObjectToMap(obj))
            "room_go" -> PacMazeWsEvent.RoomGo(
                startMs = obj.get("startMs")?.asLong ?: System.currentTimeMillis(),
            )
            "room_state" -> PacMazeWsEvent.RoomState(
                peerCount = obj.get("peerCount")?.asInt ?: 0,
                readyCount = obj.get("readyCount")?.asInt ?: 0,
                expectedPeers = obj.get("expectedPeers")?.asInt ?: 2,
            )
            "peer_input" -> PacMazeWsEvent.PeerInput(
                entityId = obj.get("entityId")?.asString.orEmpty(),
                tick = obj.get("tick")?.asLong ?: 0L,
                dir = obj.get("dir")?.asString,
                release = obj.get("release")?.asBoolean == true,
                attack = obj.get("attack")?.asBoolean == true,
                seq = obj.get("seq")?.asLong ?: 0L,
            )
            "pong" -> {
                val clientMs = obj.get("clientMs")?.asLong ?: 0L
                val serverMs = obj.get("serverMs")?.asLong ?: System.currentTimeMillis()
                val rtt = if (lastPingClientMs > 0L) {
                    (System.currentTimeMillis() - lastPingClientMs).coerceAtLeast(0L)
                } else {
                    (System.currentTimeMillis() - clientMs).coerceAtLeast(0L)
                }
                PacMazeWsEvent.Pong(clientMs = clientMs, serverMs = serverMs, rttMs = rtt)
            }
            "match_end" -> PacMazeWsEvent.MatchEnd(
                winnerEntityId = obj.get("winner")?.asString,
                scoreA = obj.get("score_a")?.asInt ?: 0,
                scoreB = obj.get("score_b")?.asInt ?: 0,
                reason = obj.get("reason")?.asString ?: "normal",
            )
            "error" -> PacMazeWsEvent.Error(
                code = obj.get("code")?.asString ?: "unknown",
                message = obj.get("message")?.asString ?: "error",
            )
            else -> null
        }
    }.getOrNull()

    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        obj.entrySet().forEach { (key, value) ->
            result[key] = when {
                value == null || value.isJsonNull -> null
                value.isJsonPrimitive -> {
                    val p = value.asJsonPrimitive
                    when {
                        p.isBoolean -> p.asBoolean
                        p.isNumber -> p.asNumber
                        else -> p.asString
                    }
                }
                value.isJsonArray -> value.asJsonArray.map { el ->
                    when {
                        el.isJsonObject -> jsonObjectToMap(el.asJsonObject)
                        el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asNumber
                        el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asBoolean
                        el.isJsonPrimitive -> el.asString
                        else -> el.toString()
                    }
                }
                value.isJsonObject -> jsonObjectToMap(value.asJsonObject)
                else -> value.toString()
            }
        }
        return result
    }
}
