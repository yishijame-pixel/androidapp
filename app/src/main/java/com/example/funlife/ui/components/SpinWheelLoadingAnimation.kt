// SpinWheelLoadingAnimation.kt - 转盘加载动画（v2 全新设计）
//
// 设计理念：
//   • 同心三环 + 反向旋转 → 形成华丽的"幸运能量场"
//   • 中心金币 3D 翻转 → 在 🎁 / ⭐ 两面间循环
//   • 6 颗奖品 emoji 在远轨上"绕飞" + 呼吸缩放
//   • 底部 5 颗节奏点位 → 波浪传递取代传统进度条
//   • 文字使用渐变 brush，副标题英文小字
//
// 配色：奶油白中心 → 桃粉中环 → 淡薰衣草外缘的柔光场
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpinWheelLoadingAnimation() {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // Dialog 沉浸到状态栏 + 导航栏
        val view = androidx.compose.ui.platform.LocalView.current
        SideEffect {
            val w = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            w?.let {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
                @Suppress("DEPRECATION")
                it.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                it.navigationBarColor = android.graphics.Color.TRANSPARENT
                androidx.core.view.WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }

        // ─── 动画状态 ───────────────────────────────────────────────
        val t = rememberInfiniteTransition(label = "loading")
        // 三环旋转（不同速度 + 反向）
        val ringOuter by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "rOuter")
        val ringMiddle by t.animateFloat(360f, 0f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rMid")
        val ringInner by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "rIn")
        // 6 个奖品 emoji 绕轨
        val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "orbit")
        // emoji 自身缩放呼吸
        val breathe by t.animateFloat(
            0.85f, 1.05f,
            infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breathe"
        )
        // 中心金币 3D 翻转（0..360）
        val flip by t.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing)),
            label = "flip"
        )
        // 底部波浪点
        val wave by t.animateFloat(
            0f, 5f,
            infiniteRepeatable(tween(1500, easing = LinearEasing)),
            label = "wave"
        )
        // 文字光泽
        val shimmer by t.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "shimmer"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFCF5),  // 中央奶油暖白
                            Color(0xFFFFD6E5),  // 中环粉桃
                            Color(0xFFC9A8E8)   // 外环淡薰衣草
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 背景柔光晕（粉橘 + 蓝紫）
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .blur(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF8FA3).copy(alpha = 0.40f),
                                Color(0xFFB892E8).copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // ═════ 主体：三环 + 中心金币 + 绕轨 emoji ═════
                Box(
                    modifier = Modifier.size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 同心三环
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val rOuter = size.minDimension * 0.46f
                        val rMid = size.minDimension * 0.36f
                        val rIn = size.minDimension * 0.26f

                        // 外环：金粉渐变弧（顺时针）
                        rotate(ringOuter, pivot = Offset(cx, cy)) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFFF6F91),
                                        Color(0xFFFFB347),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy)
                                ),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(cx - rOuter, cy - rOuter),
                                size = Size(rOuter * 2, rOuter * 2),
                                style = Stroke(width = 10f, cap = StrokeCap.Round)
                            )
                        }
                        // 中环：紫粉渐变弧（逆时针）
                        rotate(ringMiddle, pivot = Offset(cx, cy)) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF7E57C2),
                                        Color(0xFFFF8FA3),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy)
                                ),
                                startAngle = 0f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = Offset(cx - rMid, cy - rMid),
                                size = Size(rMid * 2, rMid * 2),
                                style = Stroke(width = 7f, cap = StrokeCap.Round)
                            )
                        }
                        // 内环：橙黄虚线点弧（顺时针）
                        rotate(ringInner, pivot = Offset(cx, cy)) {
                            for (i in 0 until 12) {
                                val a = (i * 30f) * (PI / 180.0).toFloat()
                                val px = cx + (rIn * cos(a))
                                val py = cy + (rIn * sin(a))
                                drawCircle(
                                    color = if (i % 2 == 0) Color(0xFFFFB347) else Color(0xFFFF8F4F),
                                    radius = 4.5f,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    // 6 个奖品 emoji 在外环绕飞
                    val prizeEmojis = listOf("🎁", "🎯", "💎", "🍀", "⭐", "🎉")
                    prizeEmojis.forEachIndexed { i, e ->
                        val angle = (orbit + i * 60f) * (PI / 180.0).toFloat()
                        val r = 105.dp
                        val phase = ((orbit + i * 60f) % 360f) / 360f
                        // 根据角度产生「前后景」感（上半圈淡，下半圈亮）
                        val depth = (sin(angle) + 1f) / 2f  // 0..1
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = r * cos(angle),
                                    y = r * sin(angle)
                                )
                                .size((26 + (depth * 8)).dp)
                                .scale(breathe * (0.85f + depth * 0.3f))
                                .graphicsLayer { alpha = 0.55f + depth * 0.45f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                e,
                                fontSize = (16 + depth * 6).sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 中心金币（3D 翻转，正反两面）
                    val angleRad = flip * (PI / 180.0).toFloat()
                    val showFront = cos(angleRad) >= 0f
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .graphicsLayer {
                                rotationY = flip
                                cameraDistance = 12 * density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (showFront) {
                            CoinFace(emoji = "🎁", from = Color(0xFFFFD66B), to = Color(0xFFFF8F4F))
                        } else {
                            // 反面镜像，再次 rotateY 180 让 emoji 不被反过来
                            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                                CoinFace(emoji = "⭐", from = Color(0xFFB892E8), to = Color(0xFF7E57C2))
                            }
                        }
                    }
                }

                // ═════ 文字：标题渐变 + 副标题 ═════
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "💫 召唤好运中",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6F91),
                                    Color(0xFFFF8F4F),
                                    Color(0xFF7E57C2),
                                    Color(0xFFFF6F91)
                                ),
                                startX = -300f + shimmer * 600f,
                                endX = 300f + shimmer * 600f
                            )
                        )
                    )
                    Text(
                        "SUMMONING YOUR FORTUNE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B3F58).copy(alpha = 0.65f),
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // ═════ 波浪点（5 个） ═════
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val accents = listOf(
                        Color(0xFFFF6F91),
                        Color(0xFFFF8F4F),
                        Color(0xFFFFB347),
                        Color(0xFF7E57C2),
                        Color(0xFFB892E8)
                    )
                    repeat(5) { i ->
                        // 距离当前 wave 头的相位（0..1，越接近越亮越大）
                        val d = ((wave - i + 5) % 5)
                        val phase = (1f - (d / 1.4f)).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .size((8 + phase * 8).dp)
                                .scale(0.8f + phase * 0.4f)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            accents[i],
                                            accents[i].copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                .graphicsLayer {
                                    alpha = 0.35f + phase * 0.65f
                                }
                        )
                    }
                }
            }
        }
    }
}

// 金币正面 / 反面通用样式：渐变圆 + 内嵌 emoji + 高光
@Composable
private fun CoinFace(emoji: String, from: Color, to: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(from, to)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 高光
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(x = (-14).dp, y = (-14).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
        )
        // 内圈描边
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = size.minDimension / 2 - 6f,
                style = Stroke(width = 1.5f)
            )
        }
        Text(
            emoji,
            fontSize = 38.sp,
            textAlign = TextAlign.Center
        )
    }
}
