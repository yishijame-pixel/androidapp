// VipHomeEffects.kt - VIP首页展示特效
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.VipLevel
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * VIP头像光环效果 - 优雅的渐变边框
 */
@Composable
fun VipAvatarHalo(
    vipLevel: VipLevel,
    modifier: Modifier = Modifier
) {
    val vipColors = when (vipLevel) {
        VipLevel.VIP3 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700)) // 金色渐变
        VipLevel.VIP2 -> listOf(Color(0xFF00D9FF), Color(0xFF0099FF), Color(0xFF00D9FF)) // 蓝色渐变
        VipLevel.VIP1 -> listOf(Color(0xFFFFB800), Color(0xFFFF8C00), Color(0xFFFFB800)) // 橙色渐变
        else -> return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 呼吸效果
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    
    Box(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
            }
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.width / 2
                
                // 绘制渐变圆环边框
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = vipColors,
                        center = Offset(centerX, centerY)
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f)
                )
                
                // 外层柔和光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            vipColors[0].copy(alpha = 0.15f),
                            vipColors[0].copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius + 12f
                    ),
                    radius = radius + 12f,
                    center = Offset(centerX, centerY)
                )
                
                // 旋转的小光点（4个）
                repeat(4) { index ->
                    val angle = (rotation + index * 90f) * (PI / 180f).toFloat()
                    val dotRadius = radius - 2f
                    val dotX = centerX + cos(angle) * dotRadius
                    val dotY = centerY + sin(angle) * dotRadius
                    
                    // 光点
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 2.5f,
                        center = Offset(dotX, dotY)
                    )
                    
                    // 光点光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                vipColors[1].copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            radius = 6f
                        ),
                        radius = 6f,
                        center = Offset(dotX, dotY)
                    )
                }
            }
    )
}

/**
 * 🏅 VIP 等级徽章 — 4 个等级用 Canvas 矢量绘制独立徽章设计
 *   VIP1: 金色 5 角星 + 4 颗旋转闪光
 *   VIP2: 蓝色钻石（六边形 + 切面高光）
 *   VIP3: 紫金王冠 + 3 颗宝石
 *   PERMANENT: 彩虹极光环 + 中央闪烁星
 */
@Composable
fun VipBadgeIcon(
    vipLevel: VipLevel,
    modifier: Modifier = Modifier
) {
    if (vipLevel == VipLevel.NORMAL) return

    val infinite = rememberInfiniteTransition(label = "badge")
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val swing by infinite.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swing"
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .size(30.dp)
            .graphicsLayer {
                rotationZ = swing
                scaleX = pulse
                scaleY = pulse
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f

            // ─── 通用：底层径向辉光 ───
            val glowColor = when (vipLevel) {
                VipLevel.PERMANENT -> Color(0xFFE040FB)
                VipLevel.VIP3 -> Color(0xFFFFD700)
                VipLevel.VIP2 -> Color(0xFF00D9FF)
                VipLevel.VIP1 -> Color(0xFFFFC107)
                else -> Color(0xFFFFD700)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.2f
                ),
                radius = r * 1.2f,
                center = Offset(cx, cy)
            )

            when (vipLevel) {
                // ⭐ VIP1: 5 角金星 + 4 颗旋转闪光
                VipLevel.VIP1 -> {
                    // 主星阴影
                    drawStar(cx, cy + 0.8f, r * 0.85f, r * 0.32f, Color(0x33000000))
                    // 主星渐变填充
                    val starPath = createStarPath(cx, cy, r * 0.85f, r * 0.32f)
                    drawPath(
                        path = starPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF8F00)),
                            start = Offset(cx - r, cy - r),
                            end = Offset(cx + r, cy + r)
                        )
                    )
                    // 中心白色高光
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = r * 0.18f,
                        center = Offset(cx - r * 0.12f, cy - r * 0.15f)
                    )
                    // 4 颗旋转小闪光
                    for (i in 0..3) {
                        val ang = ((rotation + i * 90f) * PI / 180f).toFloat()
                        val px = cx + cos(ang) * r * 1.05f
                        val py = cy + sin(ang) * r * 1.05f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = 1.5f,
                            center = Offset(px, py)
                        )
                    }
                }

                // 💎 VIP2: 六边形钻石 + 上半亮 + 下半暗 + 切面线
                VipLevel.VIP2 -> {
                    val hex = createPolygonPath(cx, cy, r * 0.85f, 6, -90f)
                    // 阴影
                    drawPath(
                        path = createPolygonPath(cx, cy + 0.8f, r * 0.85f, 6, -90f),
                        color = Color(0x33000000)
                    )
                    // 主体渐变（从上到下蓝色）
                    drawPath(
                        path = hex,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE1F5FE),
                                Color(0xFF40C4FF),
                                Color(0xFF0277BD),
                                Color(0xFF01579B)
                            ),
                            start = Offset(cx, cy - r),
                            end = Offset(cx, cy + r)
                        )
                    )
                    // 上方亮三角切面
                    val topFacet = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy - r * 0.85f)
                        lineTo(cx - r * 0.5f, cy - r * 0.15f)
                        lineTo(cx + r * 0.5f, cy - r * 0.15f)
                        close()
                    }
                    drawPath(
                        path = topFacet,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    // 中心反光（带 shimmer 动画）
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f * shimmer),
                        radius = r * 0.12f,
                        center = Offset(cx - r * 0.15f, cy - r * 0.05f)
                    )
                    // 切面分隔线
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(cx - r * 0.7f, cy),
                        end = Offset(cx + r * 0.7f, cy),
                        strokeWidth = 0.8f
                    )
                }

                // 👑 VIP3 / PERMANENT: 紫金王冠 + 3 颗宝石
                //    PERMANENT 额外加金辉环 + 顶部脉冲大宝石（"永久至尊"差异化）
                VipLevel.VIP3, VipLevel.PERMANENT -> {
                    // 皇冠 path（5 尖角 + 底带）
                    val crown = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - r * 0.85f, cy + r * 0.35f)
                        lineTo(cx - r * 0.65f, cy - r * 0.2f)
                        lineTo(cx - r * 0.4f, cy + r * 0.1f)
                        lineTo(cx - r * 0.15f, cy - r * 0.5f)
                        lineTo(cx + r * 0.15f, cy - r * 0.5f)
                        lineTo(cx + r * 0.4f, cy + r * 0.1f)
                        lineTo(cx + r * 0.65f, cy - r * 0.2f)
                        lineTo(cx + r * 0.85f, cy + r * 0.35f)
                        lineTo(cx + r * 0.85f, cy + r * 0.55f)
                        lineTo(cx - r * 0.85f, cy + r * 0.55f)
                        close()
                    }
                    // 阴影
                    drawPath(
                        path = createOffsetCrown(cx, cy + 0.8f, r),
                        color = Color(0x33000000)
                    )
                    // 主体紫金渐变（流光）
                    drawPath(
                        path = crown,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFE082),
                                Color(0xFFFFD700),
                                Color(0xFFFF6F00),
                                Color(0xFFAB47BC),
                                Color(0xFFFFD700)
                            ),
                            start = Offset(shimmer * size.width - size.width * 0.5f, 0f),
                            end = Offset(shimmer * size.width + size.width * 0.5f, size.height)
                        )
                    )
                    // 顶部高光线
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(cx - r * 0.6f, cy + r * 0.4f),
                        end = Offset(cx + r * 0.6f, cy + r * 0.4f),
                        strokeWidth = 1.2f
                    )
                    // 3 颗宝石（红中蓝、紫中、绿右）
                    drawCircle(Color(0xFFD32F2F), r * 0.13f, Offset(cx - r * 0.4f, cy + r * 0.5f))
                    drawCircle(Color(0xFF7B1FA2), r * 0.16f, Offset(cx, cy + r * 0.5f))
                    drawCircle(Color(0xFF2E7D32), r * 0.13f, Offset(cx + r * 0.4f, cy + r * 0.5f))
                    // 宝石高光
                    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.05f, Offset(cx - r * 0.42f, cy + r * 0.48f))
                    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.06f, Offset(cx - r * 0.02f, cy + r * 0.48f))
                    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.05f, Offset(cx + r * 0.38f, cy + r * 0.48f))
                    // 尖角宝石（金色 sparkle 大圆）
                    drawCircle(Color(0xFFFFEE58), r * 0.1f, Offset(cx, cy - r * 0.55f))
                    drawCircle(Color.White.copy(alpha = 0.95f), r * 0.04f, Offset(cx - r * 0.02f, cy - r * 0.57f))

                    // ── PERMANENT 专属增强：金辉环 + 顶部脉冲大宝石（与 VIP3 区分）──
                    if (vipLevel == VipLevel.PERMANENT) {
                        // 外金辉环
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.9f),
                                    Color(0xFFE040FB).copy(alpha = 0.6f),
                                    Color(0xFFFFD700).copy(alpha = 0.9f),
                                    Color(0xFFAB47BC).copy(alpha = 0.6f),
                                    Color(0xFFFFD700).copy(alpha = 0.9f),
                                ),
                                center = Offset(cx, cy)
                            ),
                            radius = r * 1.05f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.6f)
                        )
                        // 顶部脉冲大宝石（替换原 sparkle）
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color(0xFFFFEE58), Color(0xFFE040FB)),
                                center = Offset(cx, cy - r * 0.55f),
                                radius = r * 0.18f * pulse,
                            ),
                            radius = r * 0.16f * pulse,
                            center = Offset(cx, cy - r * 0.55f),
                        )
                    }
                }

                else -> {}
            }
        }
    }
}

// ─── helper: 5 角星 path ───
private fun createStarPath(
    cx: Float, cy: Float, outerR: Float, innerR: Float
): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0..9) {
        val angle = (PI * 2 * i / 10 - PI / 2).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// ─── helper: 正多边形 path ───
private fun createPolygonPath(
    cx: Float, cy: Float, r: Float, sides: Int, startAngleDeg: Float
): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    val start = startAngleDeg * (PI / 180f).toFloat()
    for (i in 0 until sides) {
        val angle = start + (PI * 2 * i / sides).toFloat()
        val x = cx + cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// ─── helper: 王冠阴影 path（offset）───
private fun createOffsetCrown(cx: Float, cy: Float, r: Float): androidx.compose.ui.graphics.Path {
    return androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - r * 0.85f, cy + r * 0.35f)
        lineTo(cx - r * 0.65f, cy - r * 0.2f)
        lineTo(cx - r * 0.4f, cy + r * 0.1f)
        lineTo(cx - r * 0.15f, cy - r * 0.5f)
        lineTo(cx + r * 0.15f, cy - r * 0.5f)
        lineTo(cx + r * 0.4f, cy + r * 0.1f)
        lineTo(cx + r * 0.65f, cy - r * 0.2f)
        lineTo(cx + r * 0.85f, cy + r * 0.35f)
        lineTo(cx + r * 0.85f, cy + r * 0.55f)
        lineTo(cx - r * 0.85f, cy + r * 0.55f)
        close()
    }
}

// ─── helper: 5 角星阴影（drawStar 简化版）───
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    cx: Float, cy: Float, outerR: Float, innerR: Float, color: Color
) {
    drawPath(createStarPath(cx, cy, outerR, innerR), color)
}

/**
 * VIP用户名发光效果 - 精致的文字阴影
 */
@Composable
fun VipGlowingText(
    text: String,
    vipLevel: VipLevel,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vipText")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    when (vipLevel) {
        // ⭐ VIP1 金色星辰 — 纯金文本 + 金光脉动阴影
        VipLevel.VIP1 -> {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = Color(0xFFFFC107),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFFFEB3B).copy(alpha = pulse * 0.85f),
                        offset = Offset(0f, 0f),
                        blurRadius = 10f
                    )
                ),
                modifier = modifier
            )
        }
        // 💎 VIP2 冰晶蓝钻 — 蓝色三色线性渐变文本 + 冰蓝辉光晕
        VipLevel.VIP2 -> {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE1F5FE),
                            Color(0xFF40C4FF),
                            Color(0xFF0288D1)
                        )
                    ),
                    shadow = Shadow(
                        color = Color(0xFF00D9FF).copy(alpha = pulse * 0.85f),
                        offset = Offset(0f, 0f),
                        blurRadius = 12f
                    )
                ),
                modifier = modifier
            )
        }
        // 👑 VIP3 紫金王冠 — 紫金双色流光渐变 + 金光辉背
        VipLevel.VIP3 -> {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFE082),
                            Color(0xFFFFD700),
                            Color(0xFFFF6F00),
                            Color(0xFFAB47BC),
                            Color(0xFFFFD700)
                        ),
                        start = Offset(shimmer * 200f - 100f, 0f),
                        end = Offset(shimmer * 200f + 200f, 50f)
                    ),
                    shadow = Shadow(
                        color = Color(0xFFFFD700).copy(alpha = pulse * 0.9f),
                        offset = Offset(0f, 0f),
                        blurRadius = 14f
                    )
                ),
                modifier = modifier
            )
        }
        // 🌟 永久会员 — 彩虹流动渐变 + 极光辉
        VipLevel.PERMANENT -> {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF1744),
                            Color(0xFFFF9100),
                            Color(0xFFFFEA00),
                            Color(0xFF00E676),
                            Color(0xFF00B0FF),
                            Color(0xFF651FFF),
                            Color(0xFFE040FB),
                            Color(0xFFFF1744)
                        ),
                        start = Offset(shimmer * 360f - 180f, 0f),
                        end = Offset(shimmer * 360f + 180f, 60f)
                    ),
                    shadow = Shadow(
                        color = Color(0xFFE040FB).copy(alpha = pulse * 0.85f),
                        offset = Offset(0f, 0f),
                        blurRadius = 16f
                    )
                ),
                modifier = modifier
            )
        }
        else -> {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = Color.White,
                modifier = modifier
            )
        }
    }
}

/**
 * 首次进入VIP光芒扩散特效
 */
@Composable
fun VipFirstEntryEffect(
    vipLevel: VipLevel,
    avatarCenter: Offset,
    onComplete: () -> Unit
) {
    val vipColor = when (vipLevel) {
        VipLevel.VIP3 -> Color(0xFFFFD700)
        VipLevel.VIP2 -> Color(0xFF00D9FF)
        VipLevel.VIP1 -> Color(0xFFFFB800)
        else -> return
    }
    
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 1500L
        
        while (animationProgress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            animationProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            delay(16)
        }
        
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val progress = animationProgress
                val alpha = if (progress < 0.5f) {
                    progress * 2f
                } else {
                    (1f - progress) * 2f
                }
                
                // 扩散的光环
                repeat(5) { ring ->
                    val ringProgress = (progress - ring * 0.1f).coerceIn(0f, 1f)
                    val ringRadius = 50f + ringProgress * 300f
                    val ringAlpha = alpha * (1f - ringProgress) * (1f - ring * 0.15f)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                vipColor.copy(alpha = ringAlpha * 0.6f),
                                vipColor.copy(alpha = ringAlpha * 0.3f),
                                Color.Transparent
                            ),
                            center = avatarCenter,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = avatarCenter
                    )
                }
                
                // 中心闪光
                if (progress < 0.3f) {
                    val flashAlpha = (0.3f - progress) / 0.3f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = flashAlpha * 0.8f),
                                vipColor.copy(alpha = flashAlpha * 0.5f),
                                Color.Transparent
                            ),
                            center = avatarCenter,
                            radius = 80f
                        ),
                        radius = 80f,
                        center = avatarCenter
                    )
                }
                
                // 星星粒子
                repeat(12) { star ->
                    val starAngle = (star * 30f + progress * 60f) * (PI / 180f).toFloat()
                    val starDistance = 60f + progress * 200f
                    val starX = avatarCenter.x + cos(starAngle) * starDistance
                    val starY = avatarCenter.y + sin(starAngle) * starDistance
                    val starAlpha = alpha * (1f - progress)
                    
                    drawCircle(
                        color = vipColor.copy(alpha = starAlpha),
                        radius = 4f,
                        center = Offset(starX, starY)
                    )
                    
                    // 星星光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                vipColor.copy(alpha = starAlpha * 0.6f),
                                Color.Transparent
                            ),
                            radius = 12f
                        ),
                        radius = 12f,
                        center = Offset(starX, starY)
                    )
                }
            }
    )
}
