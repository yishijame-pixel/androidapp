package com.example.funlife.social.game.engine

import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.viewmodel.DrawStrokeUi
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawGuessSyncTest {

    @Test
    fun parseStrokes_duplicateDrawPhase_doesNotClearStrokes() {
        val moves = listOf(
            move(1, """{"kind":"draw_phase","phase":"drawing","round":1}"""),
            move(2, strokePayload("a", 1)),
            move(3, strokePayload("b", 2)),
            move(4, """{"kind":"draw_phase","phase":"drawing","round":1}"""),
        )
        val strokes = DrawGuessSync.parseStrokes(moves, currentRound = 1)
        assertEquals(listOf("a", "b"), strokes.map { it.strokeId })
    }

    @Test
    fun parseStrokes_drawClear_clearsOnlyAfterClearMove() {
        val moves = listOf(
            move(1, strokePayload("a", 1)),
            move(2, strokePayload("b", 2)),
            move(3, """{"kind":"draw_clear","round":1}"""),
            move(4, strokePayload("c", 3)),
        )
        val strokes = DrawGuessSync.parseStrokes(moves, currentRound = 1)
        assertEquals(listOf("c"), strokes.map { it.strokeId })
    }

    @Test
    fun coalesceStrokes_keepsLongerPointsForSameId() {
        val short = DrawStrokeUi(
            seq = 1,
            points = listOf(0f to 0f, 0.1f to 0.1f),
            color = "#222",
            width = 4f,
            strokeId = "a",
        )
        val long = short.copy(
            points = listOf(0f to 0f, 0.1f to 0.1f, 0.5f to 0.5f, 1f to 1f),
        )
        val merged = DrawGuessSync.coalesceStrokes(listOf(short, long))
        assertEquals(4, merged.single().points.size)
    }

    @Test
    fun coalesceStrokes_monotonicUnion_preservesExtraId() {
        val s1 = DrawStrokeUi(seq = 1, points = listOf(0f to 0f, 1f to 1f), color = "#222", width = 4f, strokeId = "a")
        val s2 = DrawStrokeUi(seq = 2, points = listOf(0.5f to 0.5f, 0.6f to 0.6f), color = "#222", width = 4f, strokeId = "b")
        val s3 = DrawStrokeUi(seq = 3, points = listOf(0.2f to 0.2f, 0.3f to 0.3f), color = "#222", width = 4f, strokeId = "c")
        val current = listOf(s1, s2)
        val computed = listOf(s1, s3)
        val union = DrawGuessSync.coalesceStrokes(current + computed)
        assertEquals(setOf("a", "b", "c"), union.map { it.strokeId }.toSet())
    }

    @Test
    fun clearToken_countsDrawClearOnly() {
        val moves = listOf(
            move(1, """{"kind":"draw_phase","phase":"drawing","round":1}"""),
            move(2, """{"kind":"draw_clear","round":1}"""),
            move(3, """{"kind":"draw_phase","phase":"drawing","round":1}"""),
        )
        assertEquals(1, DrawGuessSync.clearToken(moves, currentRound = 1))
    }

    private fun move(index: Int, payloadJson: String): GameMoveDto =
        GameMoveDto(
            id = "m$index",
            roomId = "room",
            playerPbId = "p1",
            moveIndex = index,
            payload = JsonParser.parseString(payloadJson),
            createdAtMs = index.toLong(),
        )

    private fun strokePayload(strokeId: String, seq: Int): String =
        """{"kind":"draw_stroke","round":1,"seq":$seq,"stroke_id":"$strokeId",""" +
            """"color":"#222222","width":4,"points":[[0.1,0.1],[0.2,0.2]]}"""
}
