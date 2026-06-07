package com.example.funlife.ui.screens.socialgame.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawGuessInkBufferTest {

    @Test
    fun start_add_snapshot() {
        val buf = DrawGuessInkBuffer()
        buf.start("s1", 0.1f, 0.2f)
        buf.add(0.3f, 0.4f)
        val snap = buf.snapshot()
        assertEquals("s1", buf.strokeId)
        assertEquals(2, snap.size)
        assertEquals(0.1f to 0.2f, snap[0])
        assertEquals(0.3f to 0.4f, snap[1])
    }

    @Test
    fun subList_returns_delta_only() {
        val buf = DrawGuessInkBuffer()
        buf.start("s1", 0f, 0f)
        buf.add(1f, 1f)
        buf.add(2f, 2f)
        val delta = buf.subList(1)
        assertEquals(2, delta.size)
        assertEquals(1f to 1f, delta[0])
    }

    @Test
    fun clear_resets() {
        val buf = DrawGuessInkBuffer()
        buf.start("s1", 0.5f, 0.5f)
        buf.clear()
        assertEquals("", buf.strokeId)
        assertTrue(buf.snapshot().isEmpty())
    }
}
