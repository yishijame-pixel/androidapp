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

data class DrawWsRoomSync(
    val peerCount: Int = 0,
    val readyCount: Int = 0,
    val expectedPeers: Int = 2,
    val goAtMs: Long = 0L,
    val goRound: Int = 0,
)

sealed class DrawWsEvent {
    data class Joined(
        val userId: String,
        val roomId: String,
        val peerCount: Int = 1,
        val expectedPeers: Int = 2,
        val readyCount: Int = 0,
    ) : DrawWsEvent()

    data class StrokeChunk(val data: DrawWsStrokeChunk) : DrawWsEvent()
    data class StrokeEnd(val data: DrawWsStrokeChunk) : DrawWsEvent()
    data object Clear : DrawWsEvent()
    data class Replay(val events: List<DrawWsEvent>) : DrawWsEvent()
    data class Snapshot(val events: List<DrawWsEvent>) : DrawWsEvent()
    data class RoomState(
        val peerCount: Int,
        val readyCount: Int,
        val expectedPeers: Int = 2,
    ) : DrawWsEvent()

    data class RoomGo(val serverTs: Long, val round: Int) : DrawWsEvent()
    data class Error(val code: String) : DrawWsEvent()
    data object Disconnected : DrawWsEvent()
}
