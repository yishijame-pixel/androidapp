// ═══════════════════════════════════════════════════════════════════════════
// BookStageAmbient.kt — 魔法书剧场氛围层
//
// 在 Hub 背景渐变之上叠加：顶部聚光、四角暗角、皮肤色浮尘、底部光池。
// 全部使用皮肤暖/冷光色，绝不用大面积纯白，避免深背景反白。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin

/**
 * 剧场氛围叠加层，铺满父容器。应在背景渐变之上、内容 Column 之下绘制。
 */
@Composable
fun BookStageAmbient(
    stage: BookStageTheme,
    skinRawId: String,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "stage_ambient")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val w = size.width
        val h = size.height
        if (w < 1f || h < 1f) return@Canvas

        // 1. 顶部舞台聚光（椭圆，皮肤色，极淡）
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    stage.spotlight.copy(alpha = 0.14f + pulse * 0.06f),
                    stage.spotlight.copy(alpha = 0.04f),
                    Color.Transparent,
                ),
                center = Offset(w / 2f, h * 0.02f),
                radius = w * 0.85f,
            ),
            topLeft = Offset(w * 0.05f, -h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.90f, h * 0.55f),
        )

        // 2. 四角暗角（增加深邃感，不挡中央书本）
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.72f,
            ),
        )

        // 3. 皮肤色浮尘（12 颗，极慢漂移）
        val dustN = 12
        for (i in 0 until dustN) {
            val seed = i * 0.173f
            val px = w * (((i * 73 + 17) % 100) / 100f + sin((drift * 6.28f + seed).toDouble()).toFloat() * 0.04f)
            val py = h * (((i * 41 + 29) % 100) / 100f + drift * 0.08f) % 1f * h
            val twinkle = (0.35f + 0.65f * sin((pulse * 6.28f + i * 1.1f).toDouble()).toFloat())
                .coerceIn(0f, 1f)
            val r = 1.2f + (i % 3) * 0.6f
            drawCircle(
                color = stage.halo.copy(alpha = (0.12f * twinkle).coerceIn(0f, 1f)),
                radius = r,
                center = Offset(px, py),
                blendMode = BlendMode.Plus,
            )
            drawCircle(
                color = stage.haloCore.copy(alpha = (0.06f * twinkle).coerceIn(0f, 1f)),
                radius = r * 3.5f,
                center = Offset(px, py),
                blendMode = BlendMode.Plus,
            )
        }

        // 4. 底部光池（赤焰/霁月等皮肤强化氛围，其余皮肤极淡）
        drawBottomLightPool(skinRawId, stage, w, h, pulse)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBottomLightPool(
    skinRawId: String,
    stage: BookStageTheme,
    w: Float,
    h: Float,
    pulse: Float,
) {
    val cx = w / 2f
    val baseY = h * 0.92f
    val (coreAlpha, midAlpha, spread) = when (skinRawId) {
        "builtin::chiyan"    -> Triple(0.38f + pulse * 0.12f, 0.55f, 1.0f)
        "builtin::jiyue"     -> Triple(0.22f + pulse * 0.08f, 0.35f, 0.85f)
        "builtin::xinghe"    -> Triple(0.20f + pulse * 0.06f, 0.30f, 0.80f)
        "builtin::qingchuan" -> Triple(0.18f + pulse * 0.05f, 0.28f, 0.75f)
        "builtin::qingluan"  -> Triple(0.16f + pulse * 0.05f, 0.25f, 0.75f)
        "builtin::xuanbing"  -> Triple(0.20f + pulse * 0.07f, 0.32f, 0.82f)
        "builtin::zixiao"    -> Triple(0.22f + pulse * 0.08f, 0.34f, 0.85f)
        "builtin::liujin"    -> Triple(0.24f + pulse * 0.08f, 0.36f, 0.88f)
        "builtin::molong"    -> Triple(0.18f + pulse * 0.06f, 0.28f, 0.78f)
        "builtin::shanhu"    -> Triple(0.19f + pulse * 0.06f, 0.30f, 0.80f)
        "builtin::jingleng"  -> Triple(0.21f + pulse * 0.07f, 0.33f, 0.84f)
        else                 -> Triple(0.12f + pulse * 0.04f, 0.20f, 0.70f)
    }

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                stage.ground.copy(alpha = midAlpha),
                stage.ground.copy(alpha = midAlpha * 0.5f),
                Color.Transparent,
            ),
            center = Offset(cx, baseY),
            radius = w * 0.55f * spread,
        ),
        topLeft = Offset(cx - w * 0.55f * spread, baseY - h * 0.06f),
        size = androidx.compose.ui.geometry.Size(w * 1.1f * spread, h * 0.12f),
        blendMode = BlendMode.Plus,
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                stage.haloCore.copy(alpha = coreAlpha),
                stage.halo.copy(alpha = coreAlpha * 0.4f),
                Color.Transparent,
            ),
            center = Offset(cx, baseY - 4f),
            radius = w * 0.28f * spread,
        ),
        topLeft = Offset(cx - w * 0.28f * spread, baseY - h * 0.04f),
        size = androidx.compose.ui.geometry.Size(w * 0.56f * spread, h * 0.07f),
        blendMode = BlendMode.Plus,
    )
}
