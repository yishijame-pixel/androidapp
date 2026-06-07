package com.example.funlife.social.drawws

data class DrawWsStrokeChunk(
    val strokeId: String,
    val chunk: Int,
    val round: Int,
    val fromPbId: String,
    val color: String,
    val width: Float,
    val points: List<Pair<Float, Float>>,
    val seq: Int? = null,
)

sealed class DrawWsEvent {
    data class Joined(val userId: String, val roomId: String) : DrawWsEvent()
    data class StrokeChunk(val data: DrawWsStrokeChunk) : DrawWsEvent()
    data class StrokeEnd(val data: DrawWsStrokeChunk) : DrawWsEvent()
    data object Clear : DrawWsEvent()
    data class Replay(val events: List<DrawWsEvent>) : DrawWsEvent()
    data class Error(val code: String) : DrawWsEvent()
    data object Disconnected : DrawWsEvent()
}
