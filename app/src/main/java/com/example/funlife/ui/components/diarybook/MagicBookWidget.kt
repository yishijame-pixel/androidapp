// ═══════════════════════════════════════════════════════════════════════════
// MagicBookWidget · 真 3D 立方体魔法书（drawBitmapMesh 方案）
// ───────────────────────────────────────────────────────────────────────────
// 之前用 graphicsLayer 嵌套做不出真 3D：每个 layer 先渲染成 2D 位图再合成,
// 子 layer 的 rotationY=90 会被压成零宽度位图后才被外层旋转 → 永远是一条线。
//
// 真 3D 做法：
//   1. 6 个面的内容一次性渲染成 6 张 Bitmap (随 skin 缓存)
//   2. 在 Canvas 中:
//      a. 把立方体 8 个顶点 (±w/2, ±h/2, ±t/2) 应用 Y/X 旋转 → 3D 坐标
//      b. 透视投影 → 8 个 2D 屏幕坐标
//      c. 6 个面按平均 z 排序, 远到近绘制
//      d. 多边形有符号面积 < 0 → 背面剔除
//      e. drawBitmapMesh 把矩形位图扭曲贴到投影后的四边形
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.funlife.domain.skin.BookSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

// ────────────────────────────────────────────────────────────────────────
// 公共组件
// ────────────────────────────────────────────────────────────────────────

@Composable
fun MagicBookWidget(
    skin: BookSkin,
    pageCount: Int = 1000,
    widthDp: Dp = 200.dp,
    heightDp: Dp = 280.dp,
    breathing: Boolean = true,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val palette = skin.palette

    // ── 厚度（可视）─────────────────────────────────────
    val thicknessDp = remember(pageCount) {
        when {
            pageCount <= 1000  -> 14.dp
            pageCount <= 5000  -> 28.dp
            pageCount <= 10000 -> 44.dp
            else               -> 64.dp
        }
    }

    val widthPx  = with(density) { widthDp.toPx() }
    val heightPx = with(density) { heightDp.toPx() }
    val thickPx  = with(density) { thicknessDp.toPx() }

    // ── 6 个面位图（按 skin + 尺寸缓存）─────────────────
    val faces = remember(skin, widthPx, heightPx, thickPx) {
        buildFaceBitmaps(skin, widthPx.toInt(), heightPx.toInt(), thickPx.toInt())
    }

    // ── 呼吸 / 浮动 ────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "magic_book")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )
    val floatYpx    = if (breathing) (-6f + phase * 12f) * density.density else 0f
    val baseRotY    = if (breathing) (-14f + phase * 6f) else 0f
    val auraAlpha   = 0.28f + phase * 0.20f

    // ── 拖拽旋转 ───────────────────────────────────────
    val dragRotY = remember { Animatable(0f) }
    val dragRotX = remember { Animatable(0f) }
    val coScope  = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    val totalRotY = (if (isDragging) 0f else baseRotY) + dragRotY.value
    val totalRotX = -3f + dragRotX.value

    // ── 容器尺寸（书最大对角线 + 厚度 + 留白）──────────
    val maxDim = maxOf(widthDp, heightDp) + thicknessDp * 2 + 80.dp
    val containerW = maxDim
    val containerH = maxDim

    Box(
        modifier = Modifier
            .width(containerW)
            .height(containerH),
        contentAlignment = Alignment.Center
    ) {
        // 后方光晕
        Canvas(
            modifier = Modifier
                .size(maxDim)
                .graphicsLayer {
                    alpha = auraAlpha
                    translationY = floatYpx
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.foil.base.copy(alpha = 0.55f),
                        palette.foil.base.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        // 地面阴影
        Box(
            modifier = Modifier
                .width(widthDp * 0.85f)
                .height(20.dp)
                .graphicsLayer {
                    translationY = (heightDp / 2 + 24.dp).toPx()
                    scaleX = 1f - (floatYpx / 60f).coerceIn(-0.15f, 0.15f)
                    scaleY = (1f - (floatYpx / 60f).coerceIn(-0.15f, 0.15f)) * 0.6f
                    alpha = 0.45f
                }
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // ── 真 3D 立方体（手动投影 + drawBitmapMesh）──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = floatYpx }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            coScope.launch {
                                dragRotY.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
                            }
                            coScope.launch {
                                dragRotX.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
                            }
                        },
                        onDragCancel = { isDragging = false }
                    ) { change, drag ->
                        change.consume()
                        coScope.launch {
                            dragRotY.snapTo(
                                (dragRotY.value + drag.x * 0.5f).coerceIn(-180f, 180f)
                            )
                            dragRotX.snapTo(
                                (dragRotX.value - drag.y * 0.35f).coerceIn(-60f, 60f)
                            )
                        }
                    }
                }
                .pointerInput(onClick) {
                    detectTapGestures(onTap = { onClick() })
                }
        ) {
            drawCube3D(
                widthPx, heightPx, thickPx,
                rotXdeg = totalRotX,
                rotYdeg = totalRotY,
                centerX = size.width / 2f,
                centerY = size.height / 2f,
                faces = faces
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// 3D 数学
// ────────────────────────────────────────────────────────────────────────

/** 立方体顶点索引：8 个角 */
private object V {
    // 前面 (Z = +t/2)，从左上顺时针：TL, TR, BR, BL
    const val FTL = 0; const val FTR = 1; const val FBR = 2; const val FBL = 3
    // 后面 (Z = -t/2)
    const val BTL = 4; const val BTR = 5; const val BBR = 6; const val BBL = 7
}

private data class FaceDef(val a: Int, val b: Int, val c: Int, val d: Int, val bitmap: Bitmap)

internal data class FaceBitmaps(
    val front: Bitmap,
    val back: Bitmap,
    val right: Bitmap,
    val left: Bitmap,
    val top: Bitmap,
    val bottom: Bitmap,
)

private fun DrawScope.drawCube3D(
    widthPx: Float,
    heightPx: Float,
    thickPx: Float,
    rotXdeg: Float,
    rotYdeg: Float,
    centerX: Float,
    centerY: Float,
    faces: FaceBitmaps,
) {
    val w2 = widthPx / 2f
    val h2 = heightPx / 2f
    val t2 = thickPx / 2f

    // 8 个顶点（局部坐标，立方体居中于原点）
    val local = arrayOf(
        floatArrayOf(-w2, -h2, +t2),  // 0 FTL 前-左上
        floatArrayOf(+w2, -h2, +t2),  // 1 FTR 前-右上
        floatArrayOf(+w2, +h2, +t2),  // 2 FBR 前-右下
        floatArrayOf(-w2, +h2, +t2),  // 3 FBL 前-左下
        floatArrayOf(-w2, -h2, -t2),  // 4 BTL 后-左上
        floatArrayOf(+w2, -h2, -t2),  // 5 BTR 后-右上
        floatArrayOf(+w2, +h2, -t2),  // 6 BBR 后-右下
        floatArrayOf(-w2, +h2, -t2),  // 7 BBL 后-左下
    )

    val rx = rotXdeg * PI.toFloat() / 180f
    val ry = rotYdeg * PI.toFloat() / 180f
    val cx = cos(rx); val sx = sin(rx)
    val cy = cos(ry); val sy = sin(ry)

    val cameraDist = maxOf(widthPx, heightPx) * 3.5f

    val rotated = Array(8) { FloatArray(3) }
    val proj    = Array(8) { Offset.Zero }

    for (i in 0..7) {
        val (x0, y0, z0) = Triple(local[i][0], local[i][1], local[i][2])
        // Y 轴旋转
        val x1 =  x0 * cy + z0 * sy
        val z1 = -x0 * sy + z0 * cy
        // X 轴旋转
        val y2 = y0 * cx - z1 * sx
        val z2 = y0 * sx + z1 * cx
        rotated[i][0] = x1
        rotated[i][1] = y2
        rotated[i][2] = z2
        // 透视投影：z 越小 (越远) 缩得越小；这里相机在 +Z 方向
        val depth = (cameraDist - z2).coerceAtLeast(1f)
        val scale = cameraDist / depth
        proj[i] = Offset(centerX + x1 * scale, centerY + y2 * scale)
    }

    // 6 个面 (顶点顺序：从外侧看 CCW，便于背面剔除)
    val faceList = listOf(
        FaceDef(V.FTL, V.FTR, V.FBR, V.FBL, faces.front),    // 前 +Z
        FaceDef(V.BTR, V.BTL, V.BBL, V.BBR, faces.back),     // 后 -Z
        FaceDef(V.FTR, V.BTR, V.BBR, V.FBR, faces.right),    // 右 +X
        FaceDef(V.BTL, V.FTL, V.FBL, V.BBL, faces.left),     // 左 -X
        FaceDef(V.BTL, V.BTR, V.FTR, V.FTL, faces.top),      // 顶 -Y
        FaceDef(V.FBL, V.FBR, V.BBR, V.BBL, faces.bottom),   // 底 +Y
    )

    // 计算各面平均深度，远 → 近排序（画家算法）
    val sorted = faceList.sortedBy { f ->
        (rotated[f.a][2] + rotated[f.b][2] + rotated[f.c][2] + rotated[f.d][2]) / 4f
    }

    val nc = drawContext.canvas.nativeCanvas

    for (f in sorted) {
        val pa = proj[f.a]; val pb = proj[f.b]; val pc = proj[f.c]; val pd = proj[f.d]

        // 屏幕空间有符号面积 → 背面剔除
        // (CCW 在屏幕坐标系 (Y 向下) 中表现为 area < 0)
        val area = (pb.x - pa.x) * (pc.y - pa.y) - (pb.y - pa.y) * (pc.x - pa.x) +
                   (pc.x - pa.x) * (pd.y - pa.y) - (pc.y - pa.y) * (pd.x - pa.x)
        if (area > 0f) continue  // 背面 (Y 向下时 CCW = 负面积)

        // drawBitmapMesh：mesh 1×1 (4 个顶点，按 row-major 排列)
        // 顺序：TL, TR, BL, BR  ←  对应  pa, pb, pd, pc
        val verts = floatArrayOf(
            pa.x, pa.y,
            pb.x, pb.y,
            pd.x, pd.y,
            pc.x, pc.y,
        )
        nc.drawBitmapMesh(f.bitmap, 1, 1, verts, 0, null, 0, null)
    }
}

// ────────────────────────────────────────────────────────────────────────
// 6 张面位图（按 skin + 尺寸缓存）
// ────────────────────────────────────────────────────────────────────────

// LRU 缓存：相同 (skinId + 尺寸) 不再重渲染，反复切皮零开销
private data class FaceBitmapsKey(val skinId: String, val wPx: Int, val hPx: Int, val tPx: Int)
private val faceBitmapsCache: android.util.LruCache<FaceBitmapsKey, FaceBitmaps> =
    android.util.LruCache(12)   // 至多缓存 12 套（6 皮肤 × ~2 种尺寸）

internal fun buildFaceBitmaps(skin: BookSkin, wPx: Int, hPx: Int, tPx: Int): FaceBitmaps {
    val key = FaceBitmapsKey(skin.id.raw, wPx, hPx, tPx)
    faceBitmapsCache.get(key)?.let { return it }

    val raw = FaceBitmaps(
        front  = renderToBitmap(wPx, hPx) { drawMiniCover(skin) },
        back   = renderToBitmap(wPx, hPx) { drawBackCover(skin) },
        right  = renderToBitmap(maxOf(tPx, 4), hPx) { drawPageEdgeVertical(skin) },
        left   = renderToBitmap(maxOf(tPx, 4), hPx) { drawSpineVertical(skin) },
        top    = renderToBitmap(wPx, maxOf(tPx, 4)) { drawPageEdgeHorizontal(skin) },
        bottom = renderToBitmap(wPx, maxOf(tPx, 4)) { drawPageEdgeHorizontal(skin) },
    )
    // ChiYan / JiYue 后处理：把"被烧/被劈"烘焙到纹理里
    val result = when (skin.id.raw) {
        "builtin::chiyan" -> applyScorchEffect(raw)
        "builtin::jiyue"  -> applyLightningStrikeEffect(raw)
        else              -> raw
    }
    faceBitmapsCache.put(key, result)
    return result
}

/** 赤焰天书：在每个面上叠加灼烧（底部火光照亮 + 顶部焦痕 + 随机暗斑 + 边缘火舌余晖）。 */
private fun applyScorchEffect(faces: FaceBitmaps): FaceBitmaps {
    return FaceBitmaps(
        front  = scorchBitmap(faces.front, sideMode = false, seed = 1),
        back   = scorchBitmap(faces.back,  sideMode = false, seed = 2),
        right  = scorchBitmap(faces.right, sideMode = true,  seed = 3),
        left   = scorchBitmap(faces.left,  sideMode = true,  seed = 4),
        top    = scorchTopBitmap(faces.top,    seed = 5),
        bottom = scorchTopBitmap(faces.bottom, seed = 6),
    )
}

private fun scorchBitmap(src: Bitmap, sideMode: Boolean, seed: Int): Bitmap {
    val w = src.width.toFloat()
    val h = src.height.toFloat()
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }

    // 1. 底部 45% 区域：橙红色光照渐变（火光从下方照亮书面）
    paint.shader = android.graphics.LinearGradient(
        0f, h * 0.55f, 0f, h,
        intArrayOf(0x00000000, 0x66FF6020.toInt(), 0xBBFF3010.toInt()),
        floatArrayOf(0f, 0.55f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
    canvas.drawRect(0f, h * 0.55f, w, h, paint)
    paint.shader = null
    paint.xfermode = null

    // 2. 顶部 35%：黑色焦痕渐变（书顶被烧焦）
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, h * 0.40f,
        intArrayOf(0xDD0F0808.toInt(), 0x880F0808.toInt(), 0x00000000),
        floatArrayOf(0f, 0.5f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, h * 0.40f, paint)
    paint.shader = null

    // 3. 随机焦痕暗斑（集中在上半部，模拟烧黑的痕迹）
    val rng = java.util.Random(seed.toLong() * 977)
    val spotCount = (if (sideMode) 6 else 14)
    paint.color = 0xAA0F0606.toInt()
    for (i in 0 until spotCount) {
        val px = rng.nextFloat() * w
        val py = rng.nextFloat() * h * 0.65f
        val r = (5f + rng.nextFloat() * 14f) * (if (sideMode) 0.5f else 1f)
        paint.maskFilter = android.graphics.BlurMaskFilter(r * 0.7f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(px, py, r, paint)
    }
    paint.maskFilter = null

    // 4. 底边火光余晖（书底有正在烧的余烬）
    paint.color = 0xDDFF5018.toInt()
    paint.maskFilter = android.graphics.BlurMaskFilter(12f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    canvas.drawRect(0f, h - 8f, w, h, paint)
    paint.color = 0xCCFFAA40.toInt()
    paint.maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    canvas.drawRect(0f, h - 4f, w, h, paint)
    paint.maskFilter = null

    return out
}

/** 顶切口/底切口的灼烧（横向窄条，重点是焦痕，不画大的光照渐变）。 */
private fun scorchTopBitmap(src: Bitmap, seed: Int): Bitmap {
    val w = src.width.toFloat()
    val h = src.height.toFloat()
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    // 整体加深 + 几处焦黑点
    paint.color = 0x550F0808.toInt()
    canvas.drawRect(0f, 0f, w, h, paint)
    val rng = java.util.Random(seed.toLong() * 977)
    paint.color = 0xAA0F0606.toInt()
    for (i in 0..10) {
        val px = rng.nextFloat() * w
        val py = rng.nextFloat() * h
        val r = 3f + rng.nextFloat() * 6f
        paint.maskFilter = android.graphics.BlurMaskFilter(r * 0.7f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(px, py, r, paint)
    }
    paint.maskFilter = null
    return out
}

/** 霁月长明：前后封被天雷劈中——画一道发光紫白色闪电折线 + 劈中点焦痕。 */
private fun applyLightningStrikeEffect(faces: FaceBitmaps): FaceBitmaps {
    return FaceBitmaps(
        front  = lightningStrikeBitmap(faces.front, seed = 11),
        back   = lightningStrikeBitmap(faces.back,  seed = 12),
        right  = faces.right,
        left   = faces.left,
        top    = faces.top,
        bottom = faces.bottom,
    )
}

private fun lightningStrikeBitmap(src: Bitmap, seed: Int): Bitmap {
    val w = src.width.toFloat()
    val h = src.height.toFloat()
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }

    val rng = java.util.Random(seed.toLong() * 73)
    // 闪电折线 8 段，从顶部某点斜劈到底部偏中
    val sx = w * (0.30f + rng.nextFloat() * 0.40f)
    val ex = w * (0.35f + rng.nextFloat() * 0.30f)
    val segs = 9
    val pts = mutableListOf<android.graphics.PointF>()
    pts += android.graphics.PointF(sx, 0f)
    for (i in 1..segs) {
        val tt = i / segs.toFloat()
        val baseX = sx + (ex - sx) * tt
        val baseY = h * tt
        val jitter = (rng.nextFloat() - 0.5f) * w * 0.16f * (1f - kotlin.math.abs(tt - 0.5f))
        pts += android.graphics.PointF(baseX + jitter, baseY)
    }

    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeCap = android.graphics.Paint.Cap.ROUND

    // 外层深紫光晕
    paint.color = 0xAA9C5BFF.toInt()
    paint.strokeWidth = (w * 0.06f).coerceAtLeast(8f)
    paint.maskFilter = android.graphics.BlurMaskFilter(paint.strokeWidth * 0.9f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    drawPolyline(canvas, pts, paint)
    // 中层淡紫白光
    paint.color = 0xFFE6D9FF.toInt()
    paint.strokeWidth = (w * 0.022f).coerceAtLeast(3f)
    paint.maskFilter = android.graphics.BlurMaskFilter(paint.strokeWidth * 0.9f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    drawPolyline(canvas, pts, paint)
    // 内核纯白
    paint.color = 0xFFFFFFFF.toInt()
    paint.strokeWidth = (w * 0.008f).coerceAtLeast(1.4f)
    paint.maskFilter = null
    drawPolyline(canvas, pts, paint)

    // 分叉
    paint.strokeWidth = (w * 0.005f).coerceAtLeast(1f)
    val rngBranch = java.util.Random(seed.toLong() * 31)
    for (i in 2 until pts.size - 2) {
        if (rngBranch.nextFloat() < 0.45f) {
            val from = pts[i]
            val len = w * (0.10f + rngBranch.nextFloat() * 0.18f)
            val ang = (40f + rngBranch.nextFloat() * 90f) * (Math.PI / 180.0)
            val sgn = if (rngBranch.nextBoolean()) 1f else -1f
            val ex2 = from.x + (kotlin.math.cos(ang).toFloat()) * len * sgn
            val ey2 = from.y + (kotlin.math.sin(ang).toFloat()) * len * 0.7f
            paint.color = 0xCCE6D9FF.toInt()
            paint.maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            canvas.drawLine(from.x, from.y, ex2, ey2, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.maskFilter = null
            canvas.drawLine(from.x, from.y, ex2, ey2, paint)
        }
    }

    // 劈中点焦痕：闪电终点处一片烧黑 + 几道炭裂纹
    paint.style = android.graphics.Paint.Style.FILL
    val strike = pts.last()
    paint.color = 0xCC0A0408.toInt()
    paint.maskFilter = android.graphics.BlurMaskFilter(w * 0.07f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    canvas.drawCircle(strike.x, strike.y - h * 0.08f, w * 0.12f, paint)
    paint.maskFilter = null

    // 整体顶部加点焦痕余晖
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, h * 0.25f,
        intArrayOf(0x66050310, 0x00000000),
        floatArrayOf(0f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, h * 0.25f, paint)
    paint.shader = null

    return out
}

private fun drawPolyline(canvas: android.graphics.Canvas, pts: List<android.graphics.PointF>, paint: android.graphics.Paint) {
    for (i in 0 until pts.size - 1) {
        canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, paint)
    }
}

private fun renderToBitmap(wPx: Int, hPx: Int, block: DrawScope.() -> Unit): Bitmap {
    val w = wPx.coerceAtLeast(1)
    val h = hPx.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val androidCanvas = android.graphics.Canvas(bmp)
    val composeCanvas = androidx.compose.ui.graphics.Canvas(androidCanvas)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(w.toFloat(), h.toFloat()),
        block = block
    )
    return bmp
}

// ────────────────────────────────────────────────────────────────────────
// 立面绘制
// ────────────────────────────────────────────────────────────────────────

/**
 * 后封：魔法书形态
 *   1. 封面渐变 + 暗角晕
 *   2. 双层烫金边框 + 四角符文
 *   3. 中央巨型法阵（三重同心圆 + 八卦方位刻线 + 内外旋符号）
 *   4. 法阵中心朱砂"啟"印章（带留白边、立体阴影）
 *   5. 顶/底古文标语 + 装饰横线
 *   6. 散布星点（星座氛围）
 */
private fun DrawScope.drawBackCover(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val foil = palette.foil.base
    val nc = drawContext.canvas.nativeCanvas

    // 1. 封面渐变
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(palette.cover.base, palette.cover.accent, palette.coverShadow),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
    )

    // 1b. 四角暗角晕（vignette）
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.30f)),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.75f
        )
    )

    // 2. 双层烫金边框
    val pad1 = w * 0.04f
    drawRect(
        color = foil.copy(alpha = 0.85f),
        topLeft = Offset(pad1, pad1),
        size = Size(w - pad1 * 2, h - pad1 * 2),
        style = Stroke(width = (w * 0.006f).coerceAtLeast(1f))
    )
    val pad2 = pad1 + w * 0.02f
    drawRect(
        color = foil.copy(alpha = 0.45f),
        topLeft = Offset(pad2, pad2),
        size = Size(w - pad2 * 2, h - pad2 * 2),
        style = Stroke(width = (w * 0.003f).coerceAtLeast(0.6f))
    )

    // 3. 四角符文（菱形 + 小十字）
    val cornerCol = foil.copy(alpha = 0.85f)
    val cornerR = w * 0.022f
    val cornerStroke = (w * 0.0035f).coerceAtLeast(0.7f)
    listOf(
        Offset(pad1 + w * 0.05f, pad1 + w * 0.05f),
        Offset(w - pad1 - w * 0.05f, pad1 + w * 0.05f),
        Offset(pad1 + w * 0.05f, h - pad1 - w * 0.05f),
        Offset(w - pad1 - w * 0.05f, h - pad1 - w * 0.05f),
    ).forEach { c ->
        // 菱形
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(c.x, c.y - cornerR)
            lineTo(c.x + cornerR, c.y)
            lineTo(c.x, c.y + cornerR)
            lineTo(c.x - cornerR, c.y)
            close()
        }
        drawPath(p, cornerCol, style = Stroke(width = cornerStroke))
        // 内部十字
        drawLine(cornerCol, Offset(c.x - cornerR * 0.5f, c.y),
            Offset(c.x + cornerR * 0.5f, c.y), strokeWidth = cornerStroke * 0.7f)
        drawLine(cornerCol, Offset(c.x, c.y - cornerR * 0.5f),
            Offset(c.x, c.y + cornerR * 0.5f), strokeWidth = cornerStroke * 0.7f)
    }

    // 4. 中央巨型法阵
    val centerX = w / 2f
    val centerY = h / 2f
    val sigilStroke = (w * 0.0035f).coerceAtLeast(0.6f)
    val r1 = w * 0.36f   // 外圈
    val r2 = w * 0.30f   // 中外
    val r3 = w * 0.18f   // 内圈
    drawCircle(foil.copy(alpha = 0.55f), r1, Offset(centerX, centerY),
        style = Stroke(width = sigilStroke))
    drawCircle(foil.copy(alpha = 0.40f), r2, Offset(centerX, centerY),
        style = Stroke(width = sigilStroke * 0.8f))
    drawCircle(foil.copy(alpha = 0.60f), r3, Offset(centerX, centerY),
        style = Stroke(width = sigilStroke))

    // 八方位刻线（连接 r1 与 r3）
    for (i in 0 until 8) {
        val ang = i * (Math.PI * 2 / 8) - Math.PI / 2
        val cs = kotlin.math.cos(ang).toFloat()
        val sn = kotlin.math.sin(ang).toFloat()
        drawLine(
            foil.copy(alpha = 0.6f),
            Offset(centerX + cs * r3, centerY + sn * r3),
            Offset(centerX + cs * r1, centerY + sn * r1),
            strokeWidth = sigilStroke
        )
        // 端点小圆点
        drawCircle(foil.copy(alpha = 0.85f), sigilStroke * 1.4f,
            Offset(centerX + cs * r1, centerY + sn * r1))
    }

    // r1 和 r2 之间的星座连线（间隔顶点连接）
    for (i in 0 until 8) {
        val a1 = i * (Math.PI * 2 / 8) - Math.PI / 2
        val a2 = ((i + 3) % 8) * (Math.PI * 2 / 8) - Math.PI / 2  // 跳 3 个
        val rMid = (r1 + r2) / 2f
        drawLine(
            foil.copy(alpha = 0.25f),
            Offset(centerX + kotlin.math.cos(a1).toFloat() * rMid,
                   centerY + kotlin.math.sin(a1).toFloat() * rMid),
            Offset(centerX + kotlin.math.cos(a2).toFloat() * rMid,
                   centerY + kotlin.math.sin(a2).toFloat() * rMid),
            strokeWidth = sigilStroke * 0.6f
        )
    }

    // r3 内的旋转符号刻度（24 等分）
    val r3In = r3 * 0.85f
    for (i in 0 until 24) {
        val ang = i * (Math.PI * 2 / 24)
        val cs = kotlin.math.cos(ang).toFloat()
        val sn = kotlin.math.sin(ang).toFloat()
        drawLine(
            foil.copy(alpha = 0.35f),
            Offset(centerX + cs * r3In, centerY + sn * r3In),
            Offset(centerX + cs * r3, centerY + sn * r3),
            strokeWidth = sigilStroke * 0.5f
        )
    }

    // 5. 中央朱砂"啟"印章（带留白外圈，立体感）
    val stampSize = w * 0.16f
    val stampHalf = stampSize / 2f
    // 印章的"留白底"：稍微比印章大，浅色，模拟印泥晕染
    drawRoundRect(
        color = palette.cover.base.copy(alpha = 0.6f),
        topLeft = Offset(centerX - stampHalf - 4f, centerY - stampHalf - 4f),
        size = Size(stampSize + 8f, stampSize + 8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    // 印章主体
    drawRoundRect(
        color = palette.seal.copy(alpha = 0.92f),
        topLeft = Offset(centerX - stampHalf, centerY - stampHalf),
        size = Size(stampSize, stampSize),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
    )
    // 印章内白边
    drawRoundRect(
        color = Color.White.copy(alpha = 0.4f),
        topLeft = Offset(centerX - stampHalf + 3f, centerY - stampHalf + 3f),
        size = Size(stampSize - 6f, stampSize - 6f),
        style = Stroke(width = 1.2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
    )
    // 印章内"啟"字
    val sealPaint = android.graphics.Paint().apply {
        color = Color.White.copy(alpha = 0.95f).toArgb()
        textSize = stampSize * 0.62f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.BOLD
        )
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val sealFm = sealPaint.fontMetrics
    nc.drawText(
        "啟",
        centerX,
        centerY - (sealFm.ascent + sealFm.descent) / 2f,
        sealPaint
    )

    // 6. 顶部标语
    val topLabelPaint = android.graphics.Paint().apply {
        color = foil.copy(alpha = 0.75f).toArgb()
        textSize = w * 0.038f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.NORMAL
        )
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.35f
    }
    nc.drawText("光  阴  之  录", w / 2f, h * 0.12f, topLabelPaint)
    // 顶部装饰横线
    drawLine(
        foil.copy(alpha = 0.5f),
        Offset(w * 0.30f, h * 0.145f),
        Offset(w * 0.70f, h * 0.145f),
        strokeWidth = (w * 0.0025f).coerceAtLeast(0.5f)
    )

    // 底部标语
    val botLabelPaint = android.graphics.Paint().apply {
        color = foil.copy(alpha = 0.65f).toArgb()
        textSize = w * 0.032f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.NORMAL
        )
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.25f
    }
    drawLine(
        foil.copy(alpha = 0.5f),
        Offset(w * 0.30f, h * 0.88f),
        Offset(w * 0.70f, h * 0.88f),
        strokeWidth = (w * 0.0025f).coerceAtLeast(0.5f)
    )
    nc.drawText("惟  此  册  以  记  岁  时", w / 2f, h * 0.93f, botLabelPaint)

    // 7. 散布星点（避开法阵中心区域）
    for (i in 0 until 18) {
        val sx = ((i * 7919 + 31337) % 233280) / 233280f
        val sy = ((i * 6133 + 49297) % 233280) / 233280f
        val px = w * (0.08f + sx * 0.84f)
        val py = h * (0.16f + sy * 0.68f)
        // 法阵中心区域跳过
        val dx = px - centerX
        val dy = py - centerY
        if (dx * dx + dy * dy < r1 * r1) continue
        val a = 0.3f + ((i % 5) / 5f) * 0.5f
        drawCircle(foil.copy(alpha = a), radius = (w * 0.004f).coerceAtLeast(0.7f),
            center = Offset(px, py))
    }
}

/**
 * 右页摞（垂直立面）· 魔法古籍形态
 *   1. 多色纸张老化批次（横向 5 段微差色，旧书风）
 *   2. 高密度水平页线 + 强对比章节粗分割线
 *   3. 上下大幅烫金切口（10%）+ 划痕高光
 *   4. 朱砂丝绸书签（中央，颜色取自皮肤 seal）
 *   5. 三枚章节金标签突起（沿页摞外缘）
 *   6. 顶 / 底阳阴边线 + 顶口烫金圆点
 */
private fun DrawScope.drawPageEdgeVertical(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val foil = palette.foil.base

    // 1. 多色纸张老化批次（5 段，沿 X 微差色 = 内深外浅，类似真书）
    val ageColors = listOf(
        palette.pageEdge.copy(alpha = 1f),
        palette.pageEdge.copy(alpha = 0.97f),
        palette.pageEdge.copy(alpha = 0.93f),
        palette.pageEdge.copy(alpha = 0.88f),
        palette.pageEdgeDark
    )
    drawRect(brush = Brush.horizontalGradient(ageColors))

    // 2. 上下大幅烫金切口（各 10%，带强渐变）
    val gildH = h * 0.10f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                foil.copy(alpha = 1.0f),
                foil.copy(alpha = 0.85f),
                foil.copy(alpha = 0.55f)
            )
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, gildH)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                foil.copy(alpha = 0.55f),
                foil.copy(alpha = 0.85f),
                foil.copy(alpha = 1.0f)
            )
        ),
        topLeft = Offset(0f, h - gildH),
        size = Size(w, gildH)
    )
    // 烫金切口和页体的分界线
    drawLine(
        color = palette.pageEdgeDark.copy(alpha = 0.7f),
        start = Offset(0f, gildH), end = Offset(w, gildH), strokeWidth = 0.8f
    )
    drawLine(
        color = palette.pageEdgeDark.copy(alpha = 0.7f),
        start = Offset(0f, h - gildH), end = Offset(w, h - gildH), strokeWidth = 0.8f
    )

    // 烫金切口上的高光划痕（金质感）
    for (i in 0 until 18) {
        val sx = ((i * 9301 + 49297) % 233280) / 233280f
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(w * sx, gildH * 0.15f),
            end = Offset(w * sx, gildH * 0.9f),
            strokeWidth = 0.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(w * sx, h - gildH * 0.9f),
            end = Offset(w * sx, h - gildH * 0.15f),
            strokeWidth = 0.5f
        )
    }

    // 3. 中间页体区域的页线（更强对比）
    val pagesTop = gildH
    val pagesBot = h - gildH
    val pagesH = pagesBot - pagesTop
    val lines = (pagesH / 1.3f).toInt().coerceAtMost(skin.geometry.pageStackCountHigh)
    val gap = pagesH / lines.coerceAtLeast(1)
    val aMin = (skin.geometry.pageStackLineAlphaMin * 1.5f).coerceAtMost(0.35f)
    val aMax = (skin.geometry.pageStackLineAlphaMax * 1.5f).coerceAtMost(0.55f)
    for (i in 0 until lines) {
        val a = aMin + ((i * 7) % 11 / 11f) * (aMax - aMin)
        drawLine(
            color = palette.pageEdgeDark.copy(alpha = a),
            start = Offset(0f, pagesTop + i * gap),
            end = Offset(w, pagesTop + i * gap),
            strokeWidth = 0.8f
        )
    }

    // 4. 章节粗分割线（5 道，明显加粗深色）
    val chapterYs = listOf(0.20f, 0.36f, 0.50f, 0.66f, 0.82f)
    chapterYs.forEach { p ->
        val y = h * p
        // 主深线
        drawLine(
            color = palette.pageEdgeDark.copy(alpha = 0.85f),
            start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.6f
        )
        // 上下各一根次浅线（厚度感）
        drawLine(
            color = palette.pageEdgeDark.copy(alpha = 0.45f),
            start = Offset(0f, y - 1.5f), end = Offset(w, y - 1.5f), strokeWidth = 0.8f
        )
        drawLine(
            color = palette.pageEdgeDark.copy(alpha = 0.45f),
            start = Offset(0f, y + 1.5f), end = Offset(w, y + 1.5f), strokeWidth = 0.8f
        )
    }

    // 5. 朱砂丝绸书签（中央，从页摞向外延伸的丝带感）
    val ribbonY = h * 0.50f
    val ribbonH = h * 0.035f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.seal.copy(alpha = 0.5f),
                palette.seal.copy(alpha = 0.95f),
                palette.seal.copy(alpha = 0.5f)
            )
        ),
        topLeft = Offset(0f, ribbonY - ribbonH / 2f),
        size = Size(w, ribbonH)
    )
    // 书签中央亮线（丝绸光泽）
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(0f, ribbonY),
        end = Offset(w, ribbonY),
        strokeWidth = 0.6f
    )
    // 书签上下暗边
    drawLine(
        color = Color.Black.copy(alpha = 0.45f),
        start = Offset(0f, ribbonY - ribbonH / 2f),
        end = Offset(w, ribbonY - ribbonH / 2f),
        strokeWidth = 0.5f
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.45f),
        start = Offset(0f, ribbonY + ribbonH / 2f),
        end = Offset(w, ribbonY + ribbonH / 2f),
        strokeWidth = 0.5f
    )

    // 6. 三枚章节金标签突起（沿外缘 +X 方向，模拟 thumb-index tabs）
    val tabYs = listOf(0.13f, 0.28f, 0.71f)
    val tabH = h * 0.05f
    val tabExtend = w * 0.6f  // 延伸到 page edge 的 60%
    tabYs.forEach { p ->
        val cy = h * p
        // 标签底色（深朱砂或暗色）
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(foil.copy(alpha = 0.95f), foil.copy(alpha = 0.6f))
            ),
            topLeft = Offset(w - tabExtend, cy - tabH / 2f),
            size = Size(tabExtend, tabH)
        )
        // 标签上下暗边
        drawLine(
            color = Color.Black.copy(alpha = 0.55f),
            start = Offset(w - tabExtend, cy - tabH / 2f),
            end = Offset(w, cy - tabH / 2f),
            strokeWidth = 0.8f
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.55f),
            start = Offset(w - tabExtend, cy + tabH / 2f),
            end = Offset(w, cy + tabH / 2f),
            strokeWidth = 0.8f
        )
        // 标签左侧深色斜阴影（厚度感）
        drawLine(
            color = Color.Black.copy(alpha = 0.6f),
            start = Offset(w - tabExtend, cy - tabH / 2f),
            end = Offset(w - tabExtend, cy + tabH / 2f),
            strokeWidth = 1.0f
        )
    }

    // 7. 顶口烫金圆点（左右各 3，类似古书装订珠）
    val beadR = (w * 0.06f).coerceAtLeast(1.2f)
    listOf(0.20f, 0.50f, 0.80f).forEach { px ->
        drawCircle(
            color = foil.copy(alpha = 0.9f),
            radius = beadR,
            center = Offset(w * px, gildH * 0.5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = beadR * 0.5f,
            center = Offset(w * px - beadR * 0.2f, gildH * 0.5f - beadR * 0.2f)
        )
        drawCircle(
            color = foil.copy(alpha = 0.9f),
            radius = beadR,
            center = Offset(w * px, h - gildH * 0.5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = beadR * 0.5f,
            center = Offset(w * px - beadR * 0.2f, h - gildH * 0.5f - beadR * 0.2f)
        )
    }

    // 8. 顶 / 底阳阴细线
    drawLine(
        color = palette.pageEdgeDark.copy(alpha = 0.9f),
        start = Offset(0f, 0.5f), end = Offset(w, 0.5f), strokeWidth = 1.2f
    )
    drawLine(
        color = palette.pageEdgeDark.copy(alpha = 0.9f),
        start = Offset(0f, h - 0.5f), end = Offset(w, h - 0.5f), strokeWidth = 1.2f
    )
}

/**
 * 左书脊（垂直立面）：古书奏生装访
 *   1. 多层渐变 + 腰部高光
 *   2. 五道烫金分段 ridge（价起位依据黄金分割）
 *   3. 中央竖排烫金标题 “岁·时·录”
 *   4. 顶/底 重装访记：烫金双线 + 菱形
 *   5. 上下阳阴边线
 */
private fun DrawScope.drawSpineVertical(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val foil = palette.foil.base
    val nc = drawContext.canvas.nativeCanvas

    // 1. 多层渐变（中间亮，两侧暗，模拟书脊肩）
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                palette.coverShadow,
                palette.spine.base,
                palette.spine.accent,
                palette.spine.base,
                palette.coverShadow
            )
        )
    )
    // 1.5 纵向高光带：模拟书脊圆柱面正中的反光（窄白色亮条 + 两侧渐隐）
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.18f),
                Color.Transparent,
                Color.Transparent,
            ),
            startX = w * 0.30f,
            endX = w * 0.70f,
        ),
        topLeft = Offset(w * 0.30f, h * 0.08f),
        size = Size(w * 0.40f, h * 0.84f),
    )

    // 2. 五道烫金分段 ridge（古书装访典型）
    val ridges = listOf(0.10f, 0.30f, 0.55f, 0.78f, 0.95f)
    val ridgeStroke = (w * 0.08f).coerceAtLeast(1.5f)
    ridges.forEach { y ->
        drawLine(
            color = palette.coverShadow.copy(alpha = 0.85f),
            start = Offset(w * 0.05f, h * y),
            end = Offset(w * 0.95f, h * y),
            strokeWidth = ridgeStroke
        )
        // ridge 上的烫金亮线
        drawLine(
            color = foil.copy(alpha = 0.7f),
            start = Offset(w * 0.15f, h * y),
            end = Offset(w * 0.85f, h * y),
            strokeWidth = (w * 0.025f).coerceAtLeast(0.6f)
        )
    }

    // 3. 中央竖排标题（双约束：宽度 ≤ 65% spine 宽，高度 ≤ 7% spine 高）
    val titleChars = listOf("岁", "时", "录")
    val titleSize = kotlin.math.min(w * 0.65f, h * 0.075f)
    val titleY1 = h * 0.34f
    val titleY2 = h * 0.56f
    val titlePaint = android.graphics.Paint().apply {
        color = foil.toArgb()
        textSize = titleSize
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.BOLD
        )
        textAlign = android.graphics.Paint.Align.CENTER
        setShadowLayer(titleSize * 0.18f, 0f, titleSize * 0.07f, palette.foil.accent.toArgb())
    }
    val titleStep = (titleY2 - titleY1) / 2f
    titleChars.forEachIndexed { idx, ch ->
        nc.drawText(ch, w / 2f, titleY1 + idx * titleStep, titlePaint)
    }

    // 3.5 年号 / 卷次（竖排小字，标题下方）
    val yearChars = listOf("二", "○", "二", "六")
    val yearSize = titleSize * 0.40f
    val yearY1 = titleY2 + titleStep * 0.7f
    val yearStep = yearSize * 1.05f
    val yearPaint = android.graphics.Paint().apply {
        color = foil.copy(alpha = 0.78f).toArgb()
        textSize = yearSize
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.NORMAL
        )
        textAlign = android.graphics.Paint.Align.CENTER
    }
    yearChars.forEachIndexed { idx, ch ->
        nc.drawText(ch, w / 2f, yearY1 + idx * yearStep, yearPaint)
    }
    // 卷次：年号下方 "卷一"
    val volSize = titleSize * 0.46f
    val volY = yearY1 + yearStep * yearChars.size + volSize * 0.4f
    val volPaint = android.graphics.Paint().apply {
        color = foil.copy(alpha = 0.85f).toArgb()
        textSize = volSize
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.BOLD
        )
        textAlign = android.graphics.Paint.Align.CENTER
    }
    listOf("卷", "一").forEachIndexed { idx, ch ->
        nc.drawText(ch, w / 2f, volY + idx * volSize * 1.15f, volPaint)
    }

    // 4. 顶 / 底 重装访：烫金双线 + 菱形
    listOf(h * 0.04f, h * 0.96f).forEach { y ->
        drawLine(
            color = foil.copy(alpha = 0.85f),
            start = Offset(w * 0.10f, y),
            end = Offset(w * 0.90f, y),
            strokeWidth = (w * 0.04f).coerceAtLeast(0.8f)
        )
    }
    // 顶部菱形
    val diaR = w * 0.18f
    listOf(h * 0.07f, h * 0.93f).forEach { cy ->
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, cy - diaR)
            lineTo(w / 2f + diaR, cy)
            lineTo(w / 2f, cy + diaR)
            lineTo(w / 2f - diaR, cy)
            close()
        }
        drawPath(p, foil.copy(alpha = 0.75f),
            style = Stroke(width = (w * 0.04f).coerceAtLeast(0.6f)))
        // 中心小点
        drawCircle(foil.copy(alpha = 0.9f), w * 0.04f, Offset(w / 2f, cy))
    }

    // 5. 上下阳阴边线
    drawLine(Color.Black.copy(alpha = 0.55f), Offset(0f, 0.5f), Offset(w, 0.5f),
        strokeWidth = 1.5f)
    drawLine(Color.Black.copy(alpha = 0.55f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f),
        strokeWidth = 1.5f)
}

/** 顶 / 底切口（水平立面）：垂直页线 + 渐变 + 装订线投影 */
private fun DrawScope.drawPageEdgeHorizontal(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.pageEdge,
                palette.pageEdge.copy(alpha = 0.9f),
                palette.pageEdgeDark
            )
        )
    )
    // 千层书页：基础密线 + 每 8 行一条深色"分卷线"，模拟厚书叠层
    val lines = (w / 1.4f).toInt().coerceAtLeast(1)
    val aMin = skin.geometry.pageStackLineAlphaMin
    val aMax = skin.geometry.pageStackLineAlphaMax
    for (i in 0 until lines) {
        val a = aMin + ((i % 11) / 11f) * (aMax - aMin)
        drawLine(
            color = palette.pageEdgeDark.copy(alpha = a),
            start = Offset(i * 1.4f, 0f),
            end = Offset(i * 1.4f, h),
            strokeWidth = 0.6f
        )
        // 每 8 张：一条更深更粗的"分卷线"（章节段）
        if (i % 8 == 0 && i > 0) {
            drawLine(
                color = palette.pageEdgeDark.copy(alpha = (aMax + 0.15f).coerceAtMost(0.95f)),
                start = Offset(i * 1.4f, h * 0.05f),
                end = Offset(i * 1.4f, h * 0.95f),
                strokeWidth = 1.0f,
            )
        }
    }
    // 顶部一道纸张高光（让书顶看起来"白白的"）
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = h * 0.35f,
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h * 0.35f),
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
            startX = 0f,
            endX = w * 0.15f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w * 0.15f, h)
    )
    drawLine(
        color = palette.pageEdgeDark.copy(alpha = 0.7f),
        start = Offset(0.5f, 0f),
        end = Offset(0.5f, h),
        strokeWidth = 1.2f
    )
    drawLine(
        color = palette.pageEdgeDark.copy(alpha = 0.7f),
        start = Offset(w - 0.5f, 0f),
        end = Offset(w - 0.5f, h),
        strokeWidth = 1.2f
    )
}
