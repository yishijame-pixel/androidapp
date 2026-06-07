package com.example.funlife.social.drawws

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * 你画我猜 WS 二进制帧（v1 无 color / v2 含 color）。
 * 控制消息（joined/replay/error）仍走 JSON 文本。
 */
object DrawWsBinaryCodec {

    private const val MAGIC_0 = 0xFD.toByte()
    private const val MAGIC_1 = 0x47.toByte()
    private const val VERSION_V1: Byte = 1
    private const val VERSION_V2: Byte = 2

    const val TYPE_CHUNK: Byte = 1
    const val TYPE_END: Byte = 2
    const val TYPE_CLEAR: Byte = 3
    const val TYPE_PING: Byte = 4

    fun isBinaryPayload(data: ByteArray): Boolean =
        data.size >= 3 && data[0] == MAGIC_0 && data[1] == MAGIC_1

    fun encodePing(): ByteArray = byteArrayOf(MAGIC_0, MAGIC_1, VERSION_V2, TYPE_PING)

    fun encodeClear(round: Int): ByteArray =
        byteArrayOf(MAGIC_0, MAGIC_1, VERSION_V2, TYPE_CLEAR, round.coerceIn(0, 255).toByte())

    fun encodeStrokeEnd(
        strokeId: String,
        round: Int,
        width: Float,
        color: String,
        seq: Int,
    ): ByteArray = encodeStrokeChunk(
        strokeId = strokeId,
        chunk = 0,
        round = round,
        width = width,
        color = color,
        points = emptyList(),
        seq = seq,
        type = TYPE_END,
    )

    fun encodeStrokeChunk(
        strokeId: String,
        chunk: Int,
        round: Int,
        width: Float,
        color: String,
        points: List<List<Float>>,
        seq: Int? = null,
        type: Byte = TYPE_CHUNK,
    ): ByteArray {
        val sid = strokeId.toByteArray(StandardCharsets.UTF_8).take(255).toByteArray()
        val colorBytes = normalizeColor(color).toByteArray(StandardCharsets.UTF_8).take(32).toByteArray()
        val n = points.size
        val extraSeq = if (type == TYPE_END) 2 else 0
        val size = 4 + 1 + sid.size + 2 + 1 + 4 + extraSeq + 2 + n * 8 + 1 + colorBytes.size
        val buf = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        buf.put(MAGIC_0).put(MAGIC_1).put(VERSION_V2).put(type)
        buf.put(sid.size.toByte()).put(sid)
        buf.putShort(chunk.coerceIn(0, 65535).toShort())
        buf.put(round.coerceIn(0, 255).toByte())
        buf.putFloat(width)
        if (type == TYPE_END) {
            buf.putShort((seq ?: 0).coerceIn(0, 65535).toShort())
        }
        buf.putShort(n.coerceIn(0, 65535).toShort())
        points.forEach { pair ->
            buf.putFloat(pair.getOrNull(0) ?: 0f)
            buf.putFloat(pair.getOrNull(1) ?: 0f)
        }
        buf.put(colorBytes.size.toByte()).put(colorBytes)
        return buf.array()
    }

    fun appendRelayFrom(payload: ByteArray, fromPbId: String): ByteArray {
        val from = fromPbId.toByteArray(StandardCharsets.UTF_8).take(255).toByteArray()
        return payload + byteArrayOf(from.size.toByte()) + from
    }

    fun decode(data: ByteArray): DrawWsEvent? {
        if (!isBinaryPayload(data)) return null
        if (data.size < 4) return null
        val version = data[2]
        val type = data[3]
        return when (type) {
            TYPE_PING -> null
            TYPE_CLEAR -> DrawWsEvent.Clear
            TYPE_CHUNK, TYPE_END -> decodeStroke(data, type, version)
            else -> null
        }
    }

    private fun decodeStroke(data: ByteArray, type: Byte, version: Byte): DrawWsEvent? {
        var off = 4
        if (off >= data.size) return null
        val sidLen = data[off].toInt() and 0xFF
        off += 1
        if (off + sidLen > data.size) return null
        val strokeId = String(data, off, sidLen, StandardCharsets.UTF_8)
        off += sidLen
        if (off + 2 + 1 + 4 > data.size) return null
        val buf = ByteBuffer.wrap(data, off, data.size - off).order(ByteOrder.BIG_ENDIAN)
        val chunk = buf.short.toInt() and 0xFFFF
        val round = buf.get().toInt() and 0xFF
        val width = buf.float
        val seq = if (type == TYPE_END) {
            if (buf.remaining() < 2) return null
            buf.short.toInt() and 0xFFFF
        } else {
            null
        }
        if (buf.remaining() < 2) return null
        val pointCount = buf.short.toInt() and 0xFFFF
        val points = ArrayList<Pair<Float, Float>>(pointCount)
        repeat(pointCount) {
            if (buf.remaining() < 8) return null
            points.add(buf.float to buf.float)
        }
        var color = "#222222"
        if (version >= VERSION_V2 && buf.remaining() >= 1) {
            val colorLen = buf.get().toInt() and 0xFF
            if (colorLen > 0 && buf.remaining() >= colorLen) {
                val bytes = ByteArray(colorLen)
                buf.get(bytes)
                color = normalizeColor(String(bytes, StandardCharsets.UTF_8))
            }
        }
        var fromPbId = ""
        if (buf.remaining() >= 1) {
            val fromLen = buf.get().toInt() and 0xFF
            if (fromLen > 0 && buf.remaining() >= fromLen) {
                val bytes = ByteArray(fromLen)
                buf.get(bytes)
                fromPbId = String(bytes, StandardCharsets.UTF_8)
            }
        }
        val chunkData = DrawWsStrokeChunk(
            strokeId = strokeId,
            chunk = chunk,
            round = round,
            fromPbId = fromPbId,
            color = color,
            width = width,
            points = points,
            seq = seq,
        )
        return if (type == TYPE_END) DrawWsEvent.StrokeEnd(chunkData) else DrawWsEvent.StrokeChunk(chunkData)
    }

    private fun normalizeColor(raw: String): String {
        val c = raw.trim()
        if (c.isBlank()) return "#222222"
        return if (c.startsWith("#")) c else "#$c"
    }
}
