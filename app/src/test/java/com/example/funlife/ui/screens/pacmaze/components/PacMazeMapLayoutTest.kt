package com.example.funlife.ui.screens.pacmaze.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PacMazeMapLayoutTest {

    @Test
    fun uniformCell_allMapsFitInsideCanvas() {
        val canvasW = 800f
        val contentH = 400f
        listOf(40 to 15, 21 to 17, 29 to 27, 17 to 13).forEach { (w, h) ->
            val cell = computeFitMaxCell(canvasW, contentH, w, h)
            assertThat(cell * w).isAtMost(canvasW + 0.01f)
            assertThat(cell * h).isAtMost(contentH + 0.01f)
        }
    }

    @Test
    fun userWidthScale_expandsMapWidth_only() {
        val layout = PacMazeMapViewport.computeFitMax(
            canvasWidth = 800f,
            contentHeight = 400f,
            mapGridWidth = 29,
            mapGridHeight = 27,
            widthScale = 1.2f,
            heightScale = 1f,
        )
        assertThat(layout.stretchX).isWithin(0.01f).of(1.2f)
        assertThat(layout.stretchY).isWithin(0.01f).of(1f)
        assertThat(layout.cell).isWithin(0.01f).of(400f / 27f)
        assertThat(layout.cellX).isGreaterThan(layout.cell)
        assertThat(layout.cellY).isWithin(0.01f).of(layout.cell)
    }

    @Test
    fun defaultScale_noStretch() {
        val layout = PacMazeMapViewport.computeFitMax(800f, 400f, 29, 27)
        assertThat(layout.stretchX).isWithin(0.01f).of(1f)
        assertThat(layout.stretchY).isWithin(0.01f).of(1f)
        assertThat(layout.cellX).isWithin(0.01f).of(layout.cell)
    }
}
