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
import okio.ByteString
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 你画我猜笔画 WebSocket 会话：连接、发送、解析、断线重连。
 * 鉴权完成前出站帧入队，joined 后立刻 flush（避免开局前几笔丢失）。
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    private sealed class Outbound {
        data class Text(val raw: String) : Outbound()
        data class Bin(val payload: ByteArray) : Outbound()
    }

    private val _events = MutableSharedFlow<DrawWsEvent>(extraBufferCapacity = 512)
    val events: SharedFlow<DrawWsEvent> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private val stopped = AtomicBoolean(false)
    private var backoffMs = 800L
    private val outboundQueue = ArrayDeque<Outbound>()
    @Volatile
    private var serverJoined = false

    /** TCP 已建立（未必已完成 joined 握手） */
    val isSocketOpen: Boolean get() = socket != null

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
        scope.launch { _events.emit(DrawWsEvent.Disconnected) }
    }

    fun send(raw: String): Boolean {
        if (stopped.get()) return false
        if (serverJoined) return sendNow(Outbound.Text(raw))
        synchronized(outboundQueue) {
            if (outboundQueue.size >= 512) outboundQueue.removeFirst()
            outboundQueue.addLast(Outbound.Text(raw))
        }
        return true
    }

    fun sendBinary(payload: ByteArray): Boolean {
        if (stopped.get()) return false
        if (serverJoined) return sendNow(Outbound.Bin(payload))
        synchronized(outboundQueue) {
            if (outboundQueue.size >= 512) outboundQueue.removeFirst()
            outboundQueue.addLast(Outbound.Bin(payload))
        }
        return true
    }

    private fun sendNow(msg: Outbound): Boolean {
        val ws = socket ?: return false
        return when (msg) {
            is Outbound.Text -> ws.send(msg.raw)
            is Outbound.Bin -> ws.send(ByteString.of(*msg.payload))
        }
    }

    private fun flushOutboundQueue() {
        val batch = synchronized(outboundQueue) {
            if (outboundQueue.isEmpty()) return
            outboundQueue.toList().also { outboundQueue.clear() }
        }
        batch.forEach { sendNow(it) }
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
                delay(DrawWsConfig.pingIntervalMs())
                if (DrawWsConfig.useBinaryWire()) {
                    sendBinary(DrawWsBinaryCodec.encodePing())
                } else {
                    send(DrawWsProtocol.ping())
                }
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
                if (event is DrawWsEvent.Joined) {
                    serverJoined = true
                    flushOutboundQueue()
                }
                if (!_events.tryEmit(event)) {
                    scope.launch { _events.emit(event) }
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            DrawWsBinaryCodec.decode(bytes.toByteArray())?.let { event ->
                if (!_events.tryEmit(event)) {
                    scope.launch { _events.emit(event) }
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
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

    private fun parseMessage(text: String): DrawWsEvent? = runCatching {
        val obj = JsonParser.parseString(text).asJsonObject
        when (obj.get("t")?.asString) {
            "joined" -> DrawWsEvent.Joined(
                userId = obj.get("userId")?.asString.orEmpty(),
                roomId = obj.get("room")?.asString.orEmpty(),
                peerCount = obj.get("peerCount")?.asInt ?: 1,
                expectedPeers = obj.get("expectedPeers")?.asInt ?: 2,
                readyCount = obj.get("readyCount")?.asInt ?: 0,
            )
            "snapshot", "replay" -> {
                val list = obj.getAsJsonArray("events")?.mapNotNull { el ->
                    parseMessage(el.toString())
                }.orEmpty()
                if (obj.get("t")?.asString == "snapshot") {
                    DrawWsEvent.Snapshot(list)
                } else {
                    DrawWsEvent.Replay(list)
                }
            }
            "room_state" -> DrawWsEvent.RoomState(
                peerCount = obj.get("peerCount")?.asInt ?: 0,
                readyCount = obj.get("readyCount")?.asInt ?: 0,
                expectedPeers = obj.get("expectedPeers")?.asInt ?: 2,
            )
            "room_go" -> DrawWsEvent.RoomGo(
                serverTs = obj.get("serverTs")?.asLong ?: System.currentTimeMillis(),
                round = obj.get("round")?.asInt ?: 1,
            )
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
