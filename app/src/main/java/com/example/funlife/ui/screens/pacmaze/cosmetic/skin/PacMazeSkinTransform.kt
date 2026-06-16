package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.funlife.social.game.engine.pacmaze.Direction

/** 横版位图四向变换：横走绕脚点；竖走绕精灵中心（走廊中心对齐）。 */
internal object PacMazeSkinTransform {

    /** 竖向移动时绕精灵几何中心旋转，避免脚点 pivot 导致整体偏到一侧。 */
    fun usesCenterPivot(facing: Direction): Boolean =
        facing == Direction.UP || facing == Direction.DOWN

    /** 目标朝向与素材默认朝向不一致时水平镜像（XOR，非相等）。 */
    fun horizontalMirror(facing: Direction, facesRight: Boolean): Boolean =
        (facing == Direction.RIGHT) != facesRight

    fun DrawScope.drawOrientedBitmap(
        image: ImageBitmap,
        layout: PacMazeSkinLayoutEngine.Layout,
        facing: Direction,
        profile: PacMazeSkinRenderProfile?,
        skinId: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId?,
    ) {
        val w = layout.width
        val h = layout.height
        val feetFrac = layout.feetFrac
        val feetFracX = layout.feetFracX
        val pivot = layout.feetCenter
        val dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
        val resolvedProfile = profile ?: skinId?.let { PacMazeSkinRenderProfileCatalog.profile(it) }
        val facesRight = resolvedProfile?.defaultFacesRight
            ?: PacMazeSkinRenderProfileCatalog.defaultFacesRight(skinId)
        // 原图朝左：+90 朝上、-90 朝下；原图朝右则取反（与矢量手绘默认朝右不同）。
        val upDeg = resolvedProfile?.upRotationDeg ?: if (facesRight) -90f else 90f
        val downDeg = resolvedProfile?.downRotationDeg ?: if (facesRight) 90f else -90f
        val mirror = (facing == Direction.LEFT || facing == Direction.RIGHT) &&
            horizontalMirror(facing, facesRight)

        withTransform({
            translate(pivot.x, pivot.y)
            when (facing) {
                Direction.UP -> rotate(degrees = upDeg, pivot = Offset.Zero)
                Direction.DOWN -> rotate(degrees = downDeg, pivot = Offset.Zero)
                else -> if (mirror) scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
            }
            translate(-w * feetFracX, -h * feetFrac)
        }) {
            drawImage(
                image = image,
                dstOffset = IntOffset.Zero,
                dstSize = dstSize,
            )
        }
    }
}
