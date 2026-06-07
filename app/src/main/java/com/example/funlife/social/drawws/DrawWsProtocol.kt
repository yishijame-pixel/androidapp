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
    ): String {
        val pts = JsonArray()
        points.forEach { pair ->
            val arr = JsonArray()
            arr.add(pair.getOrNull(0) ?: 0f)
            arr.add(pair.getOrNull(1) ?: 0f)
            pts.add(arr)
        }
        return JsonObject().apply {
            addProperty("t", "stroke_chunk")
            addProperty("v", VERSION)
            addProperty("room", roomId)
            addProperty("strokeId", strokeId)
            addProperty("chunk", chunk)
            addProperty("round", round)
            addProperty("color", color)
            addProperty("width", width)
            add("points", pts)
        }.toString()
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
}
