package com.example.funlife.social.drawws

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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 你画我猜笔画 WebSocket 会话：连接、发送、解析、断线重连。
 */
class DrawWsSession(
    private val scope: CoroutineScope,
    private val roomId: String,
    private val token: String,
) {
    companion object {
        private const val TAG = "DrawWs"
        private val client = OkHttpClient.Builder()
            .pingInterval(0, TimeUnit.SECONDS)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    private val _events = MutableSharedFlow<DrawWsEvent>(extraBufferCapacity = 512)
    val events: SharedFlow<DrawWsEvent> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private val stopped = AtomicBoolean(false)
    private var backoffMs = 1_500L

    val isConnected: Boolean get() = socket != null

    fun start() {
        stopped.set(false)
        connect()
    }

    fun stop() {
        stopped.set(true)
        pingJob?.cancel()
        reconnectJob?.cancel()
        socket?.close(1000, "stop")
        socket = null
        scope.launch { _events.emit(DrawWsEvent.Disconnected) }
    }

    fun send(raw: String): Boolean {
        val ws = socket ?: return false
        return ws.send(raw)
    }

    private fun connect() {
        if (stopped.get()) return
        val base = DrawWsConfig.url()
        val wsUrl = buildString {
            append(base)
            if (!base.endsWith("/ws")) append("/ws")
            append("?token=")
            append(java.net.URLEncoder.encode(token, Charsets.UTF_8.name()))
            append("&room=")
            append(java.net.URLEncoder.encode(roomId, Charsets.UTF_8.name()))
        }
        val request = Request.Builder().url(wsUrl).build()
        socket?.cancel()
        socket = client.newWebSocket(request, Listener())
        Log.d(TAG, "connecting room=$roomId")
    }

    private fun scheduleReconnect() {
        if (stopped.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(20_000L)
            connect()
        }
    }

    private fun startPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && !stopped.get()) {
                delay(DrawWsConfig.pingIntervalMs())
                send(DrawWsProtocol.ping())
            }
        }
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "open room=$roomId")
            backoffMs = 1_500L
            startPing()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            parseMessage(text)?.let { event ->
                scope.launch { _events.emit(event) }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "closed room=$roomId code=$code")
            socket = null
            pingJob?.cancel()
            if (!stopped.get()) scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "failure room=$roomId: ${t.message}")
            socket = null
            pingJob?.cancel()
            if (!stopped.get()) scheduleReconnect()
        }
    }

    private fun parseMessage(text: String): DrawWsEvent? = runCatching {
        val obj = JsonParser.parseString(text).asJsonObject
        when (obj.get("t")?.asString) {
            "joined" -> DrawWsEvent.Joined(
                userId = obj.get("userId")?.asString.orEmpty(),
                roomId = obj.get("room")?.asString.orEmpty(),
            )
            "replay" -> {
                val list = obj.getAsJsonArray("events")?.mapNotNull { el ->
                    parseMessage(el.toString())
                }.orEmpty()
                DrawWsEvent.Replay(list)
            }
            "stroke_chunk", "stroke_end" -> {
                val chunk = parseStrokePayload(obj) ?: return@runCatching null
                if (obj.get("t")?.asString == "stroke_end") {
                    DrawWsEvent.StrokeEnd(chunk)
                } else {
                    DrawWsEvent.StrokeChunk(chunk)
                }
            }
            "clear" -> DrawWsEvent.Clear
            "error" -> DrawWsEvent.Error(obj.get("code")?.asString ?: "unknown")
            else -> null
        }
    }.getOrNull()

    private fun parseStrokePayload(obj: JsonObject): DrawWsStrokeChunk? {
        val strokeId = obj.get("strokeId")?.asString ?: return null
        val from = obj.get("from")?.asString.orEmpty()
        val round = obj.get("round")?.asInt ?: 1
        val chunk = obj.get("chunk")?.asInt ?: 0
        val color = obj.get("color")?.asString ?: "#222222"
        val width = obj.get("width")?.asFloat ?: 4f
        val seq = obj.get("seq")?.asInt
        val points = parsePoints(obj.getAsJsonArray("points"))
        return DrawWsStrokeChunk(
            strokeId = strokeId,
            chunk = chunk,
            round = round,
            fromPbId = from,
            color = color,
            width = width,
            points = points,
            seq = seq,
        )
    }

    private fun parsePoints(arr: JsonArray?): List<Pair<Float, Float>> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val a = el.asJsonArray
            if (a.size() < 2) return@mapNotNull null
            a[0].asFloat to a[1].asFloat
        }
    }
}
