package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostSilhouette
import kotlin.math.cos
import kotlin.math.sin

/** 赛博主题：按轮廓绘制几何体，含经典圆顶幽灵。 */
internal object PacMazeGhostCyberDraw {

    fun drawBody(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        color: Color,
        rotation: Float,
    ) {
        when (kind.silhouette) {
            GhostSilhouette.CLASSIC_ARCADE -> drawClassicGhost(scope, center, radius, color, rotation)
            GhostSilhouette.PENDULUM -> drawWedge(scope, center, radius, color, rotation)
            GhostSilhouette.TALISMAN -> drawHex(scope, center, radius, color, rotation)
            GhostSilhouette.TWIN_ECHO -> drawBracket(scope, center, radius, color, rotation)
            GhostSilhouette.WISP -> drawFlameDiamond(scope, center, radius, color, rotation)
            GhostSilhouette.ORIGAMI -> drawTriangle(scope, center, radius, color, rotation)
            GhostSilhouette.ERROR_PANEL -> drawBrokenRect(scope, center, radius, color, rotation)
            GhostSilhouette.HOURGLASS -> drawHourglassPoly(scope, center, radius, color, rotation)
            GhostSilhouette.GLITCH_BLOCKS -> drawGlitchBlocks(scope, center, radius, color, rotation)
            GhostSilhouette.ABACUS -> drawColumn(scope, center, radius, color, rotation)
            GhostSilhouette.SPIDER_NODE -> drawStar(scope, center, radius, color, rotation)
            GhostSilhouette.GATE_STATUE -> drawTrapezoid(scope, center, radius, color, rotation)
            GhostSilhouette.CACHE_STACK -> drawStack(scope, center, radius, color, rotation)
        }
    }

    fun outlinePath(center: Offset, radius: Float, kind: GhostKind, rotation: Float): Path {
        val path = Path()
        scopeRotatePath(path, center, radius, kind, rotation)
        return path
    }

    private fun drawClassicGhost(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        val pts = classicGhostPoints(c, r)
        drawPoly(scope, c, r, color, rot, pts)
    }

    private fun classicGhostPoints(c: Offset, r: Float): List<Offset> {
        val top = c.y - r * 0.95f
        val left = c.x - r
        val right = c.x + r
        val skirtY = c.y + r * 0.55f
        return listOf(
            Offset(left, skirtY),
            Offset(left + r * 0.5f, skirtY + r * 0.18f),
            Offset(left + r, skirtY),
            Offset(left + r * 1.5f, skirtY + r * 0.18f),
            Offset(right, skirtY),
            Offset(right, top + r * 0.9f),
            Offset(c.x, top),
            Offset(left, top + r * 0.9f),
        )
    }

    private fun scopeRotatePath(path: Path, c: Offset, r: Float, kind: GhostKind, rot: Float) {
        // build unrotated then caller rotates — simplified: use polygon points
        val pts = polygonPoints(c, r, kind)
        if (pts.isEmpty()) return
        val rad = Math.toRadians(rot.toDouble())
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()
        fun rotPt(p: Offset): Offset {
            val dx = p.x - c.x
            val dy = p.y - c.y
            return Offset(c.x + dx * cosR - dy * sinR, c.y + dx * sinR + dy * cosR)
        }
        val rp = pts.map { rotPt(it) }
        path.moveTo(rp[0].x, rp[0].y)
        rp.drop(1).forEach { path.lineTo(it.x, it.y) }
        path.close()
    }

    private fun polygonPoints(c: Offset, r: Float, kind: GhostKind): List<Offset> = when (kind.silhouette) {
        GhostSilhouette.CLASSIC_ARCADE -> classicGhostPoints(c, r)
        GhostSilhouette.PENDULUM -> listOf(
            Offset(c.x, c.y - r), Offset(c.x + r * 0.85f, c.y + r * 0.35f),
            Offset(c.x, c.y + r), Offset(c.x - r * 0.85f, c.y + r * 0.35f),
        )
        GhostSilhouette.TALISMAN -> hexPoints(c, r)
        GhostSilhouette.TWIN_ECHO -> listOf(
            Offset(c.x - r, c.y - r * 0.5f), Offset(c.x - r * 0.3f, c.y - r),
            Offset(c.x + r * 0.3f, c.y - r), Offset(c.x + r, c.y - r * 0.5f),
            Offset(c.x + r, c.y + r * 0.5f), Offset(c.x - r, c.y + r * 0.5f),
        )
        else -> diamondPoints(c, r)
    }

    private fun hexPoints(c: Offset, r: Float): List<Offset> =
        (0 until 6).map { i ->
            val a = Math.toRadians((60.0 * i - 30.0))
            Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
        }

    private fun diamondPoints(c: Offset, r: Float) = listOf(
        Offset(c.x, c.y - r), Offset(c.x + r, c.y), Offset(c.x, c.y + r), Offset(c.x - r, c.y),
    )

    private fun drawWedge(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) =
        drawPoly(scope, c, r, color, rot, polygonPoints(c, r, GhostKind.STRIKER))

    private fun drawHex(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) =
        drawPoly(scope, c, r, color, rot, hexPoints(c, r))

    private fun drawBracket(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) =
        drawPoly(scope, c, r, color, rot, polygonPoints(c, r, GhostKind.FLANKER))

    private fun drawFlameDiamond(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        val path = Path().apply {
            moveTo(c.x, c.y - r * 1.1f)
            lineTo(c.x + r * 0.55f, c.y)
            lineTo(c.x, c.y + r * 1.05f)
            lineTo(c.x - r * 0.55f, c.y)
            close()
        }
        scope.rotate(rot, c) { scope.drawPath(path, color, style = Fill) }
    }

    private fun drawTriangle(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) =
        drawPoly(scope, c, r, color, rot, listOf(Offset(c.x, c.y - r), Offset(c.x + r, c.y + r * 0.8f), Offset(c.x - r, c.y + r * 0.8f)))

    private fun drawBrokenRect(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        scope.rotate(rot, c) {
            scope.drawRect(color, topLeft = Offset(c.x - r * 0.85f, c.y - r * 0.65f), size = androidx.compose.ui.geometry.Size(r * 1.7f, r * 1.3f))
            scope.drawRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(c.x - r * 0.5f, c.y - r * 0.2f), size = androidx.compose.ui.geometry.Size(r * 0.4f, r * 0.35f))
        }
    }

    private fun drawHourglassPoly(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) =
        drawPoly(scope, c, r, color, rot, listOf(
            Offset(c.x - r * 0.55f, c.y - r), Offset(c.x + r * 0.55f, c.y - r),
            Offset(c.x + r * 0.15f, c.y), Offset(c.x + r * 0.55f, c.y + r),
            Offset(c.x - r * 0.55f, c.y + r), Offset(c.x - r * 0.15f, c.y),
        ))

    private fun drawGlitchBlocks(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        scope.rotate(rot, c) {
            scope.drawRect(color, Offset(c.x - r * 0.7f, c.y - r * 0.75f), androidx.compose.ui.geometry.Size(r * 1.1f, r * 0.55f))
            scope.drawRect(color.copy(alpha = 0.75f), Offset(c.x - r * 0.35f, c.y + r * 0.05f), androidx.compose.ui.geometry.Size(r * 0.95f, r * 0.65f))
        }
    }

    private fun drawColumn(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        scope.rotate(rot, c) {
            scope.drawRect(color, Offset(c.x - r * 0.25f, c.y - r * 0.85f), androidx.compose.ui.geometry.Size(r * 0.5f, r * 1.7f))
            repeat(4) { i ->
                scope.drawCircle(color.copy(alpha = 0.8f), r * 0.12f, Offset(c.x, c.y - r * 0.65f + i * r * 0.38f))
            }
        }
    }

    private fun drawStar(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        val pts = (0 until 8).map { i ->
            val rad = r * if (i % 2 == 0) 1f else 0.45f
            val a = Math.toRadians((45.0 * i) + rot)
            Offset(c.x + cos(a).toFloat() * rad, c.y + sin(a).toFloat() * rad)
        }
        drawPoly(scope, c, r, color, 0f, pts)
    }

    private fun drawTrapezoid(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) =
        drawPoly(scope, c, r, color, rot, listOf(
            Offset(c.x - r * 0.65f, c.y - r * 0.85f), Offset(c.x + r * 0.65f, c.y - r * 0.85f),
            Offset(c.x + r * 0.85f, c.y + r), Offset(c.x - r * 0.85f, c.y + r),
        ))

    private fun drawStack(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float) {
        scope.rotate(rot, c) {
            repeat(3) { i ->
                scope.drawRect(
                    color.copy(alpha = 1f - i * 0.12f),
                    Offset(c.x - r * 0.75f + i * r * 0.08f, c.y - r * 0.35f + i * r * 0.22f),
                    androidx.compose.ui.geometry.Size(r * 1.5f - i * r * 0.16f, r * 0.45f),
                )
            }
        }
    }

    private fun drawPoly(scope: DrawScope, c: Offset, r: Float, color: Color, rot: Float, pts: List<Offset>) {
        scope.rotate(rot, c) {
            val path = Path().apply {
                if (pts.isEmpty()) return@rotate
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            scope.drawPath(path, color, style = Fill)
        }
    }
}
