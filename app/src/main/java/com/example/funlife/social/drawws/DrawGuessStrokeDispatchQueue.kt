package com.example.funlife.social.drawws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 笔画 chunk 网络发送队列：与 UI/渲染线程解耦，在 Default 调度器上串行发送 WS。
 */
object DrawGuessStrokeDispatchQueue {

    data class ChunkJob(
        val roomId: String,
        val strokeId: String,
        val round: Int,
        val color: String,
        val width: Float,
        val points: List<List<Float>>,
        val flushNow: Boolean,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = Channel<ChunkJob>(capacity = 128)
    private var started = false

    fun ensureStarted() {
        if (started) return
        started = true
        scope.launch {
            for (job in channel) {
                if (job.points.isEmpty() || job.strokeId.isBlank()) continue
                if (DrawGuessLiveSync.isStrokeEnded(job.strokeId)) continue
                DrawGuessLiveSync.sendChunk(
                    roomId = job.roomId,
                    strokeId = job.strokeId,
                    round = job.round,
                    color = job.color,
                    width = job.width,
                    points = job.points,
                    flushNow = job.flushNow,
                )
            }
        }
    }

    fun enqueue(job: ChunkJob): Boolean = channel.trySend(job).isSuccess

    fun reset() {
        while (channel.tryReceive().isSuccess) { /* drain */ }
    }

    /** 抬手归档前冲刷待发 chunk，避免 strokeAccumulator 为空 */
    fun flushPending(timeoutMs: Long = 80L) {
        runBlocking {
            withTimeoutOrNull(timeoutMs) {
                while (true) {
                    val job = channel.tryReceive().getOrNull() ?: break
                    if (job.points.isEmpty() || job.strokeId.isBlank()) continue
                    if (DrawGuessLiveSync.isStrokeEnded(job.strokeId)) continue
                    DrawGuessLiveSync.sendChunk(
                        roomId = job.roomId,
                        strokeId = job.strokeId,
                        round = job.round,
                        color = job.color,
                        width = job.width,
                        points = job.points,
                        flushNow = job.flushNow,
                    )
                }
            }
        }
    }
}
