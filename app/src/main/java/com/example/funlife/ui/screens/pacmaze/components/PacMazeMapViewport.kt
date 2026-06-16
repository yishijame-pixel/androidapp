package com.example.funlife.ui.screens.pacmaze.components

import kotlin.math.min

/**
 * 对局地图视口：先等比容纳全图，再由用户分别调节宽/高倍率（仅拉伸地砖层，角色保持等比）。
 */
object PacMazeMapViewport {

    data class Layout(
        /** 等比基准格（实体绘制用） */
        val cell: Float,
        val offsetX: Float,
        val offsetY: Float,
        val baseMapW: Float,
        val baseMapH: Float,
        /** 用户宽/高倍率（默认 1 = 不拉伸） */
        val stretchX: Float,
        val stretchY: Float,
    ) {
        val cellX: Float get() = cell * stretchX
        val cellY: Float get() = cell * stretchY
        val visualMapW: Float get() = baseMapW * stretchX
        val visualMapH: Float get() = baseMapH * stretchY
    }

    fun computeFitMax(
        canvasWidth: Float,
        contentHeight: Float,
        mapGridWidth: Int,
        mapGridHeight: Int,
        insetTopPx: Float = 0f,
        widthScale: Float = 1f,
        heightScale: Float = 1f,
    ): Layout {
        val gw = mapGridWidth.toFloat().coerceAtLeast(1f)
        val gh = mapGridHeight.toFloat().coerceAtLeast(1f)
        val cell = computeUniformCell(canvasWidth, contentHeight, gw, gh)
        val baseMapW = cell * gw
        val baseMapH = cell * gh
        val stretchX = widthScale.coerceIn(PAC_MAZE_MAP_SCALE_MIN, PAC_MAZE_MAP_SCALE_MAX)
        val stretchY = heightScale.coerceIn(PAC_MAZE_MAP_SCALE_MIN, PAC_MAZE_MAP_SCALE_MAX)
        val visualW = baseMapW * stretchX
        val visualH = baseMapH * stretchY
        return Layout(
            cell = cell,
            offsetX = (canvasWidth - visualW) / 2f,
            offsetY = insetTopPx + (contentHeight - visualH) / 2f,
            baseMapW = baseMapW,
            baseMapH = baseMapH,
            stretchX = stretchX,
            stretchY = stretchY,
        )
    }

    fun computeUniformCell(
        canvasWidth: Float,
        contentHeight: Float,
        gridWidth: Float,
        gridHeight: Float,
    ): Float {
        val cellByW = canvasWidth / gridWidth
        val cellByH = contentHeight / gridHeight
        return min(cellByW, cellByH).coerceAtLeast(8f)
    }
}

internal fun computeFitMaxCell(
    canvasWidth: Float,
    contentHeight: Float,
    mapGridWidth: Int,
    mapGridHeight: Int,
): Float = PacMazeMapViewport.computeUniformCell(
    canvasWidth = canvasWidth,
    contentHeight = contentHeight,
    gridWidth = mapGridWidth.toFloat(),
    gridHeight = mapGridHeight.toFloat(),
)
