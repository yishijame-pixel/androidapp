package com.example.funlife.ui.screens.socialgame.play

import com.example.funlife.viewmodel.DrawStrokeUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawGuessEnterpriseCanvasTest {

    @Test
    fun ringBuffer_begin_append_snapshot() {
        val ring = DrawGuessInputRingBuffer(bufferCapacity = 64)
        ring.beginStroke("s1", 0.1f, 0.2f)
        ring.append(0.3f, 0.4f)
        val out = mutableListOf<Pair<Float, Float>>()
        ring.snapshotInto(out)
        assertEquals("s1", ring.strokeId)
        assertEquals(2, out.size)
        assertEquals(0.1f to 0.2f, out[0])
        assertEquals(0.3f to 0.4f, out[1])
    }

    @Test
    fun ringBuffer_forEachFrom_delta_only() {
        val ring = DrawGuessInputRingBuffer(bufferCapacity = 64)
        ring.beginStroke("s1", 0f, 0f)
        ring.append(1f, 1f)
        ring.append(2f, 2f)
        val delta = mutableListOf<Pair<Float, Float>>()
        ring.forEachFrom(1) { x, y -> delta.add(x to y) }
        assertEquals(2, delta.size)
        assertEquals(1f to 1f, delta[0])
        assertEquals(2f to 2f, delta[1])
    }

    @Test
    fun smoother_preserves_endpoints() {
        val pts = listOf(
            0f to 0f,
            0.25f to 0.1f,
            0.5f to 0.5f,
            0.75f to 0.9f,
            1f to 1f,
        )
        val smooth = DrawStrokeSmoother.smoothForRender(pts)
        assertTrue(smooth.size >= pts.size)
        assertEquals(pts.first(), smooth.first())
        assertEquals(pts.last().first, smooth.last().first, 0.02f)
        assertEquals(pts.last().second, smooth.last().second, 0.02f)
    }

    @Test
    fun layerBitmapPolicy_keeps_contiguous_seq_prefix_only() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f), color = "#222", width = 4f, strokeId = "a")
        val s3 = DrawStrokeUi(seq = 3, points = listOf(0.3f to 0.3f), color = "#222", width = 4f, strokeId = "c")
        val s4 = DrawStrokeUi(seq = 4, points = listOf(0.4f to 0.4f), color = "#222", width = 4f, strokeId = "d")
        assertEquals(listOf("a"), DrawGuessLayerBitmapPolicy.contiguousSeqPrefix(listOf(s1, s3, s4)).map { it.strokeId })
        val s2 = DrawStrokeUi(seq = 2, points = listOf(0.2f to 0.2f), color = "#222", width = 4f, strokeId = "b")
        assertEquals(
            listOf("a", "b", "c", "d"),
            DrawGuessLayerBitmapPolicy.contiguousSeqPrefix(listOf(s1, s3, s4, s2)).map { it.strokeId },
        )
    }

    @Test
    fun layerAppendPolicy_accepts_sequential_stroke() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0.1f to 0.1f), color = "#222", width = 4f, strokeId = "a")
        val s2 = DrawStrokeUi(seq = 2, points = listOf(0.3f to 0.3f), color = "#222", width = 4f, strokeId = "b")
        assertTrue(DrawGuessLayerAppendPolicy.canAppend(listOf("a"), listOf(s1, s2)))
        assertTrue(DrawGuessLayerAppendPolicy.canAppend(emptyList(), listOf(s1)))
        assertFalse(DrawGuessLayerAppendPolicy.canAppend(emptyList(), listOf(s1, s2)))
        assertFalse(DrawGuessLayerAppendPolicy.canAppend(listOf("a"), listOf(s2)))
    }

    @Test
    fun layerFingerprint_changes_when_points_grow() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f), color = "#222", width = 4f, strokeId = "a")
        val s1More = s1.copy(points = listOf(0f to 0f, 0.5f to 0.5f, 1f to 1f))
        val fpA = DrawGuessLayerFingerprint.fromStrokes(listOf(s1))
        val fpB = DrawGuessLayerFingerprint.fromStrokes(listOf(s1More))
        assertFalse("同 stroke 补点应触发 bitmap 层同步", fpA == fpB)
    }

    @Test
    fun coalesceStrokes_sorted_by_seq() {
        val s3 = DrawStrokeUi(seq = 3, points = listOf(0.3f to 0.3f), color = "#222", width = 4f, strokeId = "c")
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0.1f to 0.1f), color = "#222", width = 4f, strokeId = "a")
        val s2 = DrawStrokeUi(seq = 2, points = listOf(0.2f to 0.2f), color = "#222", width = 4f, strokeId = "b")
        val out = com.example.funlife.social.game.engine.DrawGuessSync.coalesceStrokes(listOf(s3, s1, s2))
        assertEquals(listOf("a", "b", "c"), out.map { it.strokeId })
    }

    @Test
    fun layerFingerprint_stable_when_stroke_order_changes() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f), color = "#222", width = 4f, strokeId = "a")
        val s2 = DrawStrokeUi(seq = 2, points = listOf(1f to 1f), color = "#222", width = 4f, strokeId = "b")
        val s3 = DrawStrokeUi(seq = 3, points = listOf(0.5f to 0.5f), color = "#222", width = 4f, strokeId = "c")
        val ordered = com.example.funlife.social.game.engine.DrawGuessSync.coalesceStrokes(listOf(s1, s2, s3))
        val shuffled = com.example.funlife.social.game.engine.DrawGuessSync.coalesceStrokes(listOf(s3, s1, s2))
        assertEquals(
            DrawGuessLayerFingerprint.fromStrokes(ordered),
            DrawGuessLayerFingerprint.fromStrokes(shuffled),
        )
    }

    @Test
    fun layerFingerprint_changes_when_stroke_added() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f), color = "#222", width = 4f, strokeId = "a")
        val s2 = DrawStrokeUi(seq = 2, points = listOf(1f to 1f), color = "#222", width = 4f, strokeId = "b")
        assertFalse(
            DrawGuessLayerFingerprint.fromStrokes(listOf(s1)) ==
                DrawGuessLayerFingerprint.fromStrokes(listOf(s1, s2)),
        )
    }

    @Test
    fun publishPolicy_skips_when_same_richness() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f, 1f to 1f), color = "#222", width = 4f, strokeId = "a")
        val s1Ledger = s1.copy(points = listOf(0f to 0f, 0.9f to 0.9f))
        assertTrue(
            DrawGuessCanvasPublishPolicy.shouldSkipRepublish(
                candidate = listOf(s1Ledger),
                current = listOf(s1),
                clearToken = 0,
                currentClearToken = 0,
            ),
        )
    }

    @Test
    fun publishPolicy_publishes_when_stroke_count_grows() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f), color = "#222", width = 4f, strokeId = "a")
        val s2 = DrawStrokeUi(seq = 2, points = listOf(1f to 1f), color = "#222", width = 4f, strokeId = "b")
        assertFalse(
            DrawGuessCanvasPublishPolicy.shouldSkipRepublish(
                candidate = listOf(s1, s2),
                current = listOf(s1),
                clearToken = 0,
                currentClearToken = 0,
            ),
        )
    }

    @Test
    fun publishPolicy_publishes_when_points_grow() {
        val cur = DrawStrokeUi(seq = 1, points = listOf(0f to 0f), color = "#222", width = 4f, strokeId = "a")
        val richer = cur.copy(points = listOf(0f to 0f, 0.5f to 0.5f, 1f to 1f))
        assertFalse(
            DrawGuessCanvasPublishPolicy.shouldSkipRepublish(
                candidate = listOf(richer),
                current = listOf(cur),
                clearToken = 0,
                currentClearToken = 0,
            ),
        )
    }
}
