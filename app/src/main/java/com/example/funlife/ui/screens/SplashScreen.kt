package com.example.funlife.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🎨 全新启动页：梦幻渐变 + 可爱小狗 logo + 浮动装饰 + 优雅进度条
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 入场动画延迟
        delay(80)
        showContent = true
        // 1.6 秒进度
        for (i in 0..100) {
            progress = i / 100f
            delay(16)
        }
        delay(120)
        onTimeout()
    }

    // ═══════════════ 动画值 ═══════════════
    val infinite = rememberInfiniteTransition(label = "splash")

    // logo 上下浮动
    val logoBounce by infinite.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce"
    )
    // logo 微缩放
    val logoScale by infinite.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    // 光晕旋转
    val haloAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "halo"
    )
    // 光晕缩放呼吸
    val haloScale by infinite.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "haloScale"
    )
    // 文字流光
    val shimmer by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    // 入场缩放
    val entryScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 240f),
        label = "entry"
    )
    val entryAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500),
        label = "entryAlpha"
    )

    // ═══════════════ 浮动装饰粒子 ═══════════════
    val particles = remember {
        List(18) {
            FloatingParticle(
                x = Random.nextFloat(),
                yStart = Random.nextFloat() * 1.2f + 0.2f,
                size = Random.nextFloat() * 8f + 4f,
                speed = Random.nextFloat() * 0.5f + 0.5f,
                color = listOf(
                    Color(0xFFFFB6C1), Color(0xFFFFD180), Color(0xFFB39DDB),
                    Color(0xFF80DEEA), Color(0xFFFFCC80), Color(0xFFF8BBD0)
                ).random(),
                shape = if (Random.nextBoolean()) ParticleShape.Heart else ParticleShape.Circle
            )
        }
    }
    val particleAnim by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "particles"
    )

    // ═══════════════ UI ═══════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE4E1),  // 浅樱粉
                        Color(0xFFFFF0E5),  // 暖米色
                        Color(0xFFFFE5C2),  // 浅杏色
                        Color(0xFFFFD6A5)   // 暖橙
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ─── 浮动粒子背景 ───
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            particles.forEach { p ->
                val phase = (particleAnim + p.x) % 1f
                val y = h * (p.yStart - phase * p.speed)
                if (y in -20f..h + 20f) {
                    val cx = w * p.x
                    val alpha = (1f - phase * 0.6f).coerceIn(0.2f, 1f)
                    val c = p.color.copy(alpha = alpha * 0.6f)
                    when (p.shape) {
                        ParticleShape.Circle -> {
                            drawCircle(c, p.size, Offset(cx, y))
                        }
                        ParticleShape.Heart -> {
                            // 简易爱心 = 两个圆 + 三角
                            val s = p.size
                            drawCircle(c, s * 0.5f, Offset(cx - s * 0.35f, y - s * 0.1f))
                            drawCircle(c, s * 0.5f, Offset(cx + s * 0.35f, y - s * 0.1f))
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - s * 0.85f, y + s * 0.1f)
                                lineTo(cx + s * 0.85f, y + s * 0.1f)
                                lineTo(cx, y + s * 0.85f)
                                close()
                            }
                            drawPath(path, c)
                        }
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .scale(entryScale)
        ) {
            Spacer(Modifier.weight(1.4f))

            // ─────────────────────────────────────────────
            //  Logo 区：旋转光晕 + 跳动小狗图标
            // ─────────────────────────────────────────────
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // 旋转光晕（多边花瓣）
                Canvas(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(haloScale)
                        .rotate(haloAngle)
                ) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val petals = 12
                    for (i in 0 until petals) {
                        val ang = (Math.PI * 2 / petals * i).toFloat()
                        val px = cx + cos(ang) * size.width * 0.42f
                        val py = cy + sin(ang) * size.height * 0.42f
                        drawCircle(
                            color = Color(0xFFFFFFFF).copy(alpha = 0.45f),
                            radius = size.width * 0.085f,
                            center = Offset(px, py)
                        )
                    }
                }

                // 内圈柔光
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(haloScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.85f),
                                    Color(0xFFFFFFFF).copy(alpha = 0.0f)
                                )
                            ),
                            CircleShape
                        )
                )

                // 小狗 logo（用 launcher icon 渲染）
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(y = logoBounce.dp)
                        .scale(logoScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFE0B2))
                            )
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "一十",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ─────────────────────────────────────────────
            //  品牌主标题「一十」
            // ─────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // 阴影层
                Text(
                    text = "一十",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0x33000000),
                    modifier = Modifier.offset(x = 2.dp, y = 3.dp),
                    style = TextStyle(letterSpacing = 12.sp)
                )
                // 主体（渐变填充用 brush）
                Text(
                    text = "一十",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF7043),
                                Color(0xFFFF9800),
                                Color(0xFFFFC107),
                                Color(0xFFFF6F61)
                            ),
                            start = Offset(shimmer * 200f, 0f),
                            end = Offset(shimmer * 200f + 300f, 200f)
                        ),
                        letterSpacing = 12.sp
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            // ─── 副标题 ───
            Text(
                text = "趣 味 生 活",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8D6E63),
                style = TextStyle(letterSpacing = 8.sp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // ─── 装饰小心标语 ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DecorDot(Color(0xFFFFB74D))
                Text(
                    "让每一天都充满惊喜",
                    fontSize = 12.sp,
                    color = Color(0xFFA1887F),
                    fontWeight = FontWeight.Medium
                )
                DecorDot(Color(0xFFE57373))
            }

            Spacer(Modifier.weight(1f))

            // ─────────────────────────────────────────────
            //  进度条（彩色渐变 + 百分比）
            // ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(8.dp)
                    .background(
                        Color(0xFFFFFFFF).copy(alpha = 0.55f),
                        RoundedCornerShape(4.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFB74D),
                                    Color(0xFFFF7043),
                                    Color(0xFFE91E63),
                                    Color(0xFFAB47BC)
                                )
                            ),
                            RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA1887F),
                modifier = Modifier.alpha(entryAlpha)
            )

            Spacer(Modifier.weight(0.6f))

            // ─── 底部品牌 ───
            Text(
                text = "✨ Loading magic ✨",
                fontSize = 11.sp,
                color = Color(0xFFBCAAA4),
                fontWeight = FontWeight.Medium,
                style = TextStyle(letterSpacing = 2.sp),
                modifier = Modifier
                    .alpha(entryAlpha)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun DecorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(color, CircleShape)
    )
}

private data class FloatingParticle(
    val x: Float,
    val yStart: Float,
    val size: Float,
    val speed: Float,
    val color: Color,
    val shape: ParticleShape
)

private enum class ParticleShape { Circle, Heart }

