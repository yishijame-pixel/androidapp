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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.funlife.data.DiaryBookCustomizationStore
import com.example.funlife.ui.components.diarybook.skin.rememberBookCustomization
import com.example.funlife.R
import androidx.compose.ui.res.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 与 MagicBook3DWidget 一致的默认视角：偏右 22° + 俯 15° */
private const val DEFAULT_BOOK_ROT_Y = -22f
private const val DEFAULT_BOOK_ROT_X = 15f

/** 厚度相对封面宽度的比例（Filament 立方体 depthNorm） */
private fun depthNormForPages(pageCount: Int): Float = when {
    pageCount <= 1000  -> 0.25f
    pageCount <= 5000  -> 0.35f
    pageCount <= 10000 -> 0.45f
    else               -> 0.60f
}

/** 封面位图超采样，与 3D 版一致减少锯齿 */
private const val FACE_TEX_SCALE = 1.4f

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
    val stage = bookStageThemeFor(skin.id.raw)

    // ── 厚度（与 MagicBook3DWidget depthNorm 比例一致，不再用固定 14dp）──
    val depthNorm = remember(pageCount) { depthNormForPages(pageCount) }
    val thicknessDp = remember(widthDp, depthNorm) {
        (widthDp.value * depthNorm).dp
    }

    val widthPx  = with(density) { widthDp.toPx() }
    val heightPx = with(density) { heightDp.toPx() }
    val thickPx  = with(density) { thicknessDp.toPx() }

    val customization = rememberBookCustomization()
    val defaultTitle = stringResource(R.string.diary_book_default_title)
    val defaultSubtitle = stringResource(R.string.diary_book_default_subtitle)
    val coverTitle = DiaryBookCustomizationStore.resolveTitle(customization, defaultTitle)
    val coverOwnerLine = DiaryBookCustomizationStore.resolveOwnerLine(customization, defaultSubtitle)
    val ownerNameRaw = customization.ownerName

    // ── 6 个面位图（1.4× 超采样，与 3D 版一致）─────────────────
    val texWPx = (widthPx * FACE_TEX_SCALE).toInt()
    val texHPx = (heightPx * FACE_TEX_SCALE).toInt()
    val texTPx = (thickPx * FACE_TEX_SCALE).toInt()
    val faces = remember(skin, texWPx, texHPx, texTPx, coverTitle, coverOwnerLine, ownerNameRaw) {
        buildFaceBitmaps(
            skin,
            texWPx,
            texHPx,
            texTPx,
            coverTitle,
            coverOwnerLine,
            ownerNameRaw,
        )
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
    val floatYpx    = if (breathing) (-6f + phase * 12f) else 0f
    // 呼吸微摆：与 3D 版 idleRotY / idleRotX 一致
    val idleRotY    = if (breathing) (-3f + phase * 6f) else 0f
    val idleRotX    = if (breathing) (-1.5f + phase * 3f) else 0f

    // ── 拖拽旋转（偏移量；松手后回正到默认视角）────────────
    val dragRotY = remember { Animatable(0f) }
    val dragRotX = remember { Animatable(0f) }
    val coScope  = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // 始终叠加默认视角 + 呼吸 + 拖拽偏移（拖拽时不再丢掉 base，避免起手跳变）
    val totalRotY = DEFAULT_BOOK_ROT_Y + idleRotY + dragRotY.value
    val totalRotX = DEFAULT_BOOK_ROT_X + idleRotX + dragRotX.value

    // 松手 1.5s 无操作 → 700ms 回正（与 MagicBook3DWidget 一致）
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            delay(1500)
            val startY = dragRotY.value
            val startX = dragRotX.value
            val steps = 700 / 16
            for (i in 1..steps) {
                val pp = 1f - (1f - i.toFloat() / steps).let { t -> t * t * t }
                dragRotY.snapTo(startY + (0f - startY) * pp)
                dragRotX.snapTo(startX + (0f - startX) * pp)
                delay(16)
            }
            dragRotY.snapTo(0f)
            dragRotX.snapTo(0f)
        }
    }

    // ── 容器尺寸（与 3D 版留白一致）────────────────────
    val containerW = widthDp + 200.dp
    val containerH = heightDp + 240.dp

    Box(
        modifier = Modifier
            .width(containerW)
            .height(containerH),
        contentAlignment = Alignment.Center
    ) {
        // 地面反照（剧场气场色，火光下不用纯黑阴影）
        Canvas(
            modifier = Modifier
                .size(width = widthDp * 0.9f, height = 22.dp)
                .graphicsLayer {
                    translationY = with(density) { (heightDp / 2 + 36.dp).toPx() } + floatYpx * 0.3f
                    val t = (floatYpx / 12f).coerceIn(-1f, 1f)
                    scaleX = 1f - t * 0.12f
                    scaleY = (1f - t * 0.12f) * 0.7f
                    alpha = stage.groundAlpha - t * 0.18f
                }
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stage.ground.copy(alpha = stage.groundAlpha),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
            )
        }

        // ── 真 3D 立方体（手动投影 + drawBitmapMesh）──
        Canvas(
            modifier = Modifier
                .size(widthDp + 80.dp, heightDp + 80.dp)
                .graphicsLayer { translationY = floatYpx }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onDragEnd = {
                            isDragging = false
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onDragCancel = {
                            isDragging = false
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    ) { change, drag ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        coScope.launch {
                            dragRotY.snapTo(
                                (dragRotY.value + drag.x * 0.5f).coerceIn(-180f, 180f)
                            )
                            dragRotX.snapTo(
                                (dragRotX.value - drag.y * 0.3f).coerceIn(-60f, 60f)
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

    val cameraDist = maxOf(widthPx, heightPx) * 3.0f  // 对齐 Filament camera z=3.0

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
        // 3D Y 轴向上、屏幕 Y 向下，朝相机的面在屏幕空间为 CW（area > 0）
        val area = (pb.x - pa.x) * (pc.y - pa.y) - (pb.y - pa.y) * (pc.x - pa.x) +
                   (pc.x - pa.x) * (pd.y - pa.y) - (pc.y - pa.y) * (pd.x - pa.x)
        if (area <= 0f) continue

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

// LRU 缓存：相同 (skinId + 尺寸 + 书名 + 署名) 不再重渲染
private data class FaceBitmapsKey(
    val skinId: String,
    val wPx: Int,
    val hPx: Int,
    val tPx: Int,
    val bookTitle: String,
    val ownerLine: String,
    val ownerNameRaw: String,
)
private val faceBitmapsCache: android.util.LruCache<FaceBitmapsKey, FaceBitmaps> =
    android.util.LruCache(36)

internal fun buildFaceBitmaps(
    skin: BookSkin,
    wPx: Int,
    hPx: Int,
    tPx: Int,
    bookTitle: String = "岁时录",
    ownerLine: String = "一  人  一  册",
    ownerNameRaw: String = "",
): FaceBitmaps {
    val key = FaceBitmapsKey(skin.id.raw, wPx, hPx, tPx, bookTitle, ownerLine, ownerNameRaw)
    faceBitmapsCache.get(key)?.let { return it }

    val raw = FaceBitmaps(
        front  = renderToBitmap(wPx, hPx) { drawMiniCover(skin, bookTitle, ownerLine) },
        back   = renderToBitmap(wPx, hPx) { drawBackCoverArt(skin) },
        right  = renderToBitmap(maxOf(tPx, 4), hPx) { drawPageEdgeVerticalEnhanced(skin) },
        left   = renderToBitmap(maxOf(tPx, 4), hPx) {
            drawSpineVerticalEnhanced(skin, bookTitle, ownerNameRaw, ownerLine)
        },
        top    = renderToBitmap(wPx, maxOf(tPx, 4)) { drawPageEdgeHorizontalEnhanced(skin) },
        bottom = renderToBitmap(wPx, maxOf(tPx, 4)) { drawPageEdgeHorizontalEnhanced(skin) },
    )
    // 赤焰：封面灼烧焦痕烘焙到纹理；霁月闪电改由 SkinFx 全屏动画呈现，不在封面上画静态雷劈
    val themed = when (skin.id.raw) {
        "builtin::chiyan" -> applyScorchEffect(raw)
        else              -> raw
    }
    // 深邃剧场：压暗侧面，避免浅色反白成光晕
    val seatStrength = if (skin.id.raw == "builtin::chiyan") 0.62f else 0.50f
    val result = seatSideFacesIntoTheater(themed, skin.palette.coverShadow, seatStrength)
    faceBitmapsCache.put(key, result)
    return result
}

/** 深邃剧场压暗：侧面纹理压暗，前/后封保持原样。 */
private fun seatSideFacesIntoTheater(faces: FaceBitmaps, tint: Color, strength: Float): FaceBitmaps {
    return faces.copy(
        right  = seatFace(faces.right,  tint, strength * 0.88f),
        left   = seatFace(faces.left,   tint, strength * 0.58f),  // 书脊少压暗，保留署名可读
        top    = seatFace(faces.top,    tint, strength * 0.85f),
        bottom = seatFace(faces.bottom, tint, strength * 0.85f),
    )
}

private fun seatFace(src: Bitmap, tint: Color, strength: Float): Bitmap {
    val w = src.width.toFloat()
    val h = src.height.toFloat()
    if (w < 1f || h < 1f) return src
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }

    val dr = (tint.red   * 0.28f).coerceIn(0f, 1f)
    val dg = (tint.green * 0.28f).coerceIn(0f, 1f)
    val db = (tint.blue  * 0.28f).coerceIn(0f, 1f)
    fun lerpUp(c: Float) = (1f - strength) + c * strength
    val mr = (lerpUp(dr) * 255f).toInt().coerceIn(0, 255)
    val mg = (lerpUp(dg) * 255f).toInt().coerceIn(0, 255)
    val mb = (lerpUp(db) * 255f).toInt().coerceIn(0, 255)
    val mulColor = (0xFF shl 24) or (mr shl 16) or (mg shl 8) or mb
    paint.colorFilter = android.graphics.PorterDuffColorFilter(
        mulColor, android.graphics.PorterDuff.Mode.MULTIPLY
    )
    canvas.drawBitmap(src, 0f, 0f, paint)
    paint.colorFilter = null

    val rgb  = tint.toArgb() and 0x00FFFFFF
    val aTop = (255f * strength * 0.7f).toInt().coerceIn(0, 255)
    val aBot = (255f * strength * 0.30f).toInt().coerceIn(0, 255)
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, h,
        intArrayOf((aTop shl 24) or rgb, (aTop shl 24) or rgb, (aBot shl 24) or rgb),
        floatArrayOf(0f, 0.7f, 1f),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, h, paint)
    return out
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
