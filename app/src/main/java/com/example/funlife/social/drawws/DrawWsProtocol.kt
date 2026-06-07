package com.example.funlife.social.drawws

import com.google.gson.JsonArray
import com.google.gson.JsonObject

object DrawWsProtocol {
    const val VERSION = 1

    fun strokeChunk(
        roomId: String,
        strokeId: String,
        chunk: Int,
        round: Int,
        color: String,
        width: Float,
        points: List<List<Float>>,
    ): String = buildString {
        append("{\"t\":\"stroke_chunk\",\"v\":")
        append(VERSION)
        append(",\"room\":\"")
        append(roomId)
        append("\",\"strokeId\":\"")
        append(strokeId)
        append("\",\"chunk\":")
        append(chunk)
        append(",\"round\":")
        append(round)
        append(",\"color\":\"")
        append(color)
        append("\",\"width\":")
        append(width)
        append(",\"points\":[")
        points.forEachIndexed { i, pair ->
            if (i > 0) append(',')
            append('[')
            append(pair.getOrNull(0) ?: 0f)
            append(',')
            append(pair.getOrNull(1) ?: 0f)
            append(']')
        }
        append("]}")
    }

    fun strokeEnd(
        roomId: String,
        strokeId: String,
        round: Int,
        seq: Int,
        color: String,
        width: Float,
        points: List<List<Float>>,
    ): String {
        val pts = JsonArray()
        points.forEach { pair ->
            val arr = JsonArray()
            arr.add(pair.getOrNull(0) ?: 0f)
            arr.add(pair.getOrNull(1) ?: 0f)
            pts.add(arr)
        }
        return JsonObject().apply {
            addProperty("t", "stroke_end")
            addProperty("v", VERSION)
            addProperty("room", roomId)
            addProperty("strokeId", strokeId)
            addProperty("round", round)
            addProperty("seq", seq)
            addProperty("color", color)
            addProperty("width", width)
            add("points", pts)
        }.toString()
    }

    fun clear(roomId: String, round: Int): String =
        """{"t":"clear","v":$VERSION,"room":"$roomId","round":$round}"""

    fun ping(): String = """{"t":"ping","v":$VERSION}"""

    fun ready(round: Int): String = """{"t":"ready","v":$VERSION,"round":$round}"""
}
