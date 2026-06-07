package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.funlife.viewmodel.DrawStrokeUi

object DrawGuessCanvasRenderer {

    fun rasterizeStrokes(
        strokes: List<DrawStrokeUi>,
        width: Int,
        height: Int,
        density: Float,
    ): ImageBitmap {
        if (width <= 0 || height <= 0) return ImageBitmap(1, 1)
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            drawRect(color = DrawGuessMatchPalette.canvasBg)
            strokes.forEach { stroke ->
                drawStrokePoints(
                    drawScope = this,
                    points = stroke.points,
                    color = DrawColorPalette.toColor(stroke.color),
                    width = stroke.width,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    smooth = true,
                )
            }
        }
        return bitmap
    }

    fun drawStrokePoints(
        drawScope: DrawScope,
        points: List<Pair<Float, Float>>,
        color: Color,
        width: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        smooth: Boolean = true,
    ) {
        if (points.isEmpty()) return
        val renderPts = if (smooth) DrawStrokeSmoother.smoothForRender(points) else points
        with(drawScope) {
            if (renderPts.size == 1) {
                val (nx, ny) = renderPts.first()
                drawCircle(
                    color = color,
                    radius = width / 2f,
                    center = androidx.compose.ui.geometry.Offset(
                        nx * canvasWidth,
                        ny * canvasHeight,
                    ),
                )
                return
            }
            val path = Path()
            renderPts.forEachIndexed { i, (nx, ny) ->
                val px = nx * canvasWidth
                val py = ny * canvasHeight
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(
                path,
                color = color,
                style = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
