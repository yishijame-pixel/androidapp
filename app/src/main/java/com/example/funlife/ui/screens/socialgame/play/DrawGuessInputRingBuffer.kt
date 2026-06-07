package com.example.funlife.ui.screens.socialgame.play

import java.util.concurrent.atomic.AtomicInteger

/**
 * 单生产者（pointer）/ 单消费者（VSYNC 帧循环）环形缓冲。
 * 避免 pointer 路径分配 List、避免 Compose State 触发 recomposition。
 */
class DrawGuessInputRingBuffer(
    bufferCapacity: Int = 4096,
) {
    private val xs = FloatArray(bufferCapacity)
    private val ys = FloatArray(bufferCapacity)
    private val writeIdx = AtomicInteger(0)
    private val readIdx = AtomicInteger(0)
    private val cap = bufferCapacity

    @Volatile
    var strokeId: String = ""
        private set

    val capacity: Int get() = cap

    fun beginStroke(strokeId: String, x: Float, y: Float) {
        writeIdx.set(0)
        readIdx.set(0)
        this.strokeId = strokeId
        appendInternal(x, y)
    }

    fun append(x: Float, y: Float) {
        appendInternal(x, y)
    }

    private fun appendInternal(x: Float, y: Float) {
        val w = writeIdx.get()
        if (w >= cap) return
        xs[w] = x
        ys[w] = y
        writeIdx.set(w + 1)
    }

    fun clear() {
        writeIdx.set(0)
        readIdx.set(0)
        strokeId = ""
    }

    fun size(): Int = writeIdx.get()

    /** 消费者读取全量快照（帧渲染用，每帧一次） */
    fun snapshotInto(out: MutableList<Pair<Float, Float>>) {
        out.clear()
        val n = writeIdx.get()
        for (i in 0 until n) {
            out.add(xs[i] to ys[i])
        }
    }

    /**
     * 网络发送：自 [fromIndex] 起的增量点（不分配中间 List 时由调用方映射）。
     */
    fun forEachFrom(fromIndex: Int, block: (Float, Float) -> Unit) {
        val n = writeIdx.get()
        for (i in fromIndex until n) {
            block(xs[i], ys[i])
        }
    }

    fun drainCount(): Int = writeIdx.get() - readIdx.get()

    fun markReadThrough(index: Int) {
        readIdx.set(index.coerceIn(0, writeIdx.get()))
    }
}
