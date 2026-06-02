// VipActivationAnimation.kt
// ════════════════════════════════════════════════════════════════════════
// 全新设计：礼盒揭晓 + 光晕加冕（推倒重来 v3）
// ------------------------------------------------------------------------
// 6 大视觉层（按 z 顺序）：
//   ① 全屏爆发闪光（150ms 强光闪一下）
//   ② 旋转光柱（6 道从中心辐射的光，缓慢旋转）
//   ③ 外圈彩环（双圈反向旋转）
//   ④ 上升粒子（12 颗圆+星混合，错峰）
//   ⑤ 礼花碎条 Confetti（18 条彩纸从顶部洒下）
//   ⑥ 中央卡片（emoji 旋转 + 等级名艺术字 + 金币滚动）
//
// 等级专属：颜色集中在 VipTheme，VIP1 金 / VIP2 蓝钻 / VIP3 紫金 / PERMANENT 彩虹
// 总时长 ~3.5s：入场 1.2s → 停留 1.6s → 退场 0.7s
// ════════════════════════════════════════════════════════════════════════

package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.model.VipLevel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

// ─── 主题 ───────────────────────────────────────────
private data class VipTheme(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val gradientTop: Color,
    val gradientBottom: Color,
    val accent: Color,
    val glow: Color,
    val confettiColors: List<Color>,
    val rayColor: Color,
) {
    companion object {
        fun of(level: VipLevel): VipTheme = when (level) {
            VipLevel.VIP1 -> VipTheme(
                title = "月卡 VIP",
                subtitle = "30 天精致体验已开启",
                emoji = "⭐",
                gradientTop = Color(0xFFFFE082),
                gradientBottom = Color(0xFFFB8C00),
                accent = Color(0xFFFFB300),
                glow = Color(0xFFFFC107),
                confettiColors = listOf(
                    Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFFD54F),
                    Color(0xFFFF8F00), Color(0xFFFFFFFF),
                ),
                rayColor = Color(0xFFFFD54F),
            )
            VipLevel.VIP2 -> VipTheme(
                title = "年卡 VIP",
                subtitle = "365 天尊贵特权",
                emoji = "💎",
                gradientTop = Color(0xFF80D8FF),
                gradientBottom = Color(0xFF0277BD),
                accent = Color(0xFF00B0FF),
                glow = Color(0xFF40C4FF),
                confettiColors = listOf(
                    Color(0xFFB3E5FC), Color(0xFF40C4FF), Color(0xFF0288D1),
                    Color(0xFFE1F5FE), Color(0xFFFFFFFF),
                ),
                rayColor = Color(0xFF80D8FF),
            )
            // VIP3 与 PERMANENT 都是「终身 VIP」，统一紫金双色 + 皇冠
            VipLevel.VIP3, VipLevel.PERMANENT -> VipTheme(
                title = "终身 VIP",
                subtitle = "一次买断 · 终身畅享",
                emoji = "👑",
                gradientTop = Color(0xFFFFD54F),
                gradientBottom = Color(0xFFAB47BC),
                accent = Color(0xFFFFD700),
                glow = Color(0xFFE040FB),
                confettiColors = listOf(
                    Color(0xFFFFD700), Color(0xFFE040FB), Color(0xFF7C4DFF),
                    Color(0xFFFFE082), Color(0xFFCE93D8), Color(0xFFFFFFFF),
                ),
                rayColor = Color(0xFFFFD700),
            )
            else -> VipTheme(
                title = "会员激活成功",
                subtitle = "感谢支持",
                emoji = "🎉",
                gradientTop = Color(0xFFFFCDD2),
                gradientBottom = Color(0xFFE91E63),
                accent = Color(0xFFEC407A),
                glow = Color(0xFFFF80AB),
                confettiColors = listOf(Color(0xFFFFCDD2), Color(0xFFEC407A), Color(0xFFE91E63)),
                rayColor = Color(0xFFFF80AB),
            )
        }
    }
}

private enum class Phase { Enter, Hold, Exit }

private const val ENTER_MS = 1200
private const val HOLD_MS = 1600L
private const val EXIT_MS = 700

@Composable
fun VipActivationAnimation(
    vipLevel: VipLevel,
    coins: Int,
    onDismiss: () -> Unit,
) {
    if (vipLevel == VipLevel.NORMAL) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val theme = remember(vipLevel) { VipTheme.of(vipLevel) }

    var phase by remember { mutableStateOf(Phase.Enter) }

    val enterT by animateFloatAsState(
        targetValue = if (phase == Phase.Enter) 0f else 1f,
        animationSpec = tween(ENTER_MS, easing = FastOutSlowInEasing),
        label = "enterT",
    )
    val exitT by animateFloatAsState(
        targetValue = if (phase == Phase.Exit) 1f else 0f,
        animationSpec = tween(EXIT_MS, easing = FastOutLinearInEasing),
        label = "exitT",
    )
    val displayCoins by animateIntAsState(
        targetValue = if (phase == Phase.Enter) 0 else coins,
        animationSpec = tween(1100, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "coins",
    )

    // 持续旋转的光柱与外环（轻量 infinite，仅 1-2 个）
    val rayInfinite = rememberInfiniteTransition(label = "rays")
    val rayRot by rayInfinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rayRot",
    )
    val ringRot by rayInfinite.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRot",
    )

    LaunchedEffect(Unit) {
        delay(40)
        phase = Phase.Hold
        delay(ENTER_MS + HOLD_MS)
        phase = Phase.Exit
        delay(EXIT_MS.toLong() + 60)
        onDismiss()
    }

    val overall = (enterT * (1f - exitT)).coerceIn(0f, 1f)

    // 一次性生成 confetti / 上升粒子的随机参数
    val confetti = remember { generateConfetti(20) }
    val particles = remember { generateParticles(14) }

    Dialog(
        onDismissRequest = { /* 等动画自然结束 */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * overall)),
            contentAlignment = Alignment.Center,
        ) {
            // ① 入场爆发闪光（前 200ms 全屏白光）
            BurstFlash(t = enterT)

            // ② 背景旋转光柱
            RotatingRays(theme = theme, rot = rayRot, alpha = overall)

            // ③ 外圈双层彩环
            OuterRing(theme = theme, rot = ringRot, alpha = overall)

            // ④ 中心径向辉光
            BackgroundGlow(theme = theme, alpha = overall)

            // ⑤ 上升粒子
            FloatingParticles(theme = theme, t = enterT, particles = particles)

            // ⑥ 顶部 confetti 礼花
            ConfettiRain(t = enterT, items = confetti, theme = theme)

            // ⑦ 中央卡片
            CelebrationCard(
                theme = theme,
                coins = coins,
                displayCoins = displayCoins,
                enterT = enterT,
                overall = overall,
                rayRot = rayRot,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// ① 入场爆发闪光
// ════════════════════════════════════════════════════════════
@Composable
private fun BurstFlash(t: Float) {
    if (t > 0.18f) return
    val a = 1f - (t / 0.18f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = a * 0.85f))
    )
}

// ════════════════════════════════════════════════════════════
// ② 旋转光柱（6 道从中心辐射的光）
// ════════════════════════════════════════════════════════════
@Composable
private fun RotatingRays(theme: VipTheme, rot: Float, alpha: Float) {
    if (alpha < 0.05f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = max(size.width, size.height) * 0.7f
        rotate(rot, pivot = Offset(cx, cy)) {
            for (i in 0 until 6) {
                val baseAngle = i * 60f
                rotate(baseAngle, pivot = Offset(cx, cy)) {
                    // 用三角形 path 模拟"扇形光柱"
                    val path = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx - maxR * 0.06f, cy - maxR)
                        lineTo(cx + maxR * 0.06f, cy - maxR)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                theme.rayColor.copy(alpha = 0.18f * alpha),
                                Color.Transparent,
                            ),
                            startY = cy,
                            endY = cy - maxR,
                        ),
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// ③ 外圈双层彩环（在卡片外，绕中心旋转）
// ════════════════════════════════════════════════════════════
@Composable
private fun OuterRing(theme: VipTheme, rot: Float, alpha: Float) {
    if (alpha < 0.05f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r1 = size.minDimension * 0.46f
        val r2 = size.minDimension * 0.52f
        rotate(rot, pivot = Offset(cx, cy)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        theme.gradientTop.copy(alpha = 0.0f),
                        theme.gradientTop.copy(alpha = 0.6f),
                        theme.gradientBottom.copy(alpha = 0.0f),
                        theme.glow.copy(alpha = 0.5f),
                        theme.gradientTop.copy(alpha = 0.0f),
                    ),
                    center = Offset(cx, cy),
                ),
                radius = r1,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f),
                alpha = alpha,
            )
        }
        rotate(-rot * 0.6f, pivot = Offset(cx, cy)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        theme.glow.copy(alpha = 0.5f),
                        Color.Transparent,
                        theme.accent.copy(alpha = 0.5f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                ),
                radius = r2,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f),
                alpha = alpha,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// ④ 中心径向辉光
// ════════════════════════════════════════════════════════════
@Composable
private fun BackgroundGlow(theme: VipTheme, alpha: Float) {
    if (alpha <= 0.01f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = max(size.width, size.height) * 0.65f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.glow.copy(alpha = 0.5f * alpha),
                    theme.glow.copy(alpha = 0.18f * alpha),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = maxR,
            ),
            radius = maxR,
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.32f * alpha),
                    theme.glow.copy(alpha = 0.22f * alpha),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = 220f,
            ),
            radius = 220f,
            center = Offset(cx, cy),
        )
    }
}

// ════════════════════════════════════════════════════════════
// ⑤ 上升粒子（圆+星形混合）
// ════════════════════════════════════════════════════════════
private data class ParticleSpec(
    val baseX: Float,        // 屏幕宽度的占比 0~1
    val phaseDelay: Float,   // 启动延迟 0~0.5
    val sizeBase: Float,     // 基础大小（px）
    val isStar: Boolean,
    val swayAmp: Float,      // 横向摆动幅度
    val colorIdx: Int,       // 0=top 1=glow 2=accent
)

private fun generateParticles(n: Int): List<ParticleSpec> {
    val rng = Random(42)
    return List(n) {
        ParticleSpec(
            baseX = rng.nextFloat() * 0.9f + 0.05f,
            phaseDelay = rng.nextFloat() * 0.5f,
            sizeBase = 5f + rng.nextFloat() * 4f,
            isStar = rng.nextFloat() > 0.55f,
            swayAmp = 0.04f + rng.nextFloat() * 0.06f,
            colorIdx = it % 3,
        )
    }
}

@Composable
private fun FloatingParticles(theme: VipTheme, t: Float, particles: List<ParticleSpec>) {
    if (t <= 0.05f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        particles.forEachIndexed { i, p ->
            val local = ((t - p.phaseDelay).coerceAtLeast(0f) /
                    (1f - p.phaseDelay).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
            val y = h * 1.05f - local * (h * 0.85f)
            val sway = sin((local * 6.28f * 1.2f + i * 0.7f).toDouble()).toFloat() * (w * p.swayAmp)
            val x = w * p.baseX + sway
            val a = when {
                local < 0.2f -> local / 0.2f
                local > 0.7f -> ((1f - local) / 0.3f).coerceAtLeast(0f)
                else -> 1f
            } * 0.85f
            val color = when (p.colorIdx) {
                0 -> theme.gradientTop
                1 -> theme.glow
                else -> theme.accent
            }
            if (p.isStar) {
                drawSimpleStar(Offset(x, y), p.sizeBase * 1.3f, color.copy(alpha = a))
            } else {
                drawCircle(
                    color = color.copy(alpha = a),
                    radius = p.sizeBase,
                    center = Offset(x, y),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = a * 0.4f), Color.Transparent)
                ),
                radius = p.sizeBase * 3.2f,
                center = Offset(x, y),
            )
        }
    }
}

private fun DrawScope.drawSimpleStar(c: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0..9) {
        val ang = (PI * 2 * i / 10 - PI / 2).toFloat()
        val rr = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + cos(ang) * rr
        val y = c.y + sin(ang) * rr
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

// ════════════════════════════════════════════════════════════
// ⑥ 顶部 confetti 礼花碎条
// ════════════════════════════════════════════════════════════
private data class ConfettiSpec(
    val baseX: Float,        // 0~1 起始横坐标
    val driftX: Float,       // 横向漂移幅度（屏宽占比，正负随机）
    val rotSpeed: Float,     // 自旋速度（度/进度）
    val initialRot: Float,
    val phaseDelay: Float,   // 0~0.3
    val colorIdx: Int,
    val width: Float,
    val height: Float,
)

private fun generateConfetti(n: Int): List<ConfettiSpec> {
    val rng = Random(7)
    return List(n) {
        ConfettiSpec(
            baseX = rng.nextFloat(),
            driftX = (rng.nextFloat() - 0.5f) * 0.3f,
            rotSpeed = (rng.nextFloat() - 0.5f) * 720f,
            initialRot = rng.nextFloat() * 360f,
            phaseDelay = rng.nextFloat() * 0.3f,
            colorIdx = it,
            width = 6f + rng.nextFloat() * 4f,
            height = 12f + rng.nextFloat() * 8f,
        )
    }
}

@Composable
private fun ConfettiRain(t: Float, items: List<ConfettiSpec>, theme: VipTheme) {
    if (t <= 0.05f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        items.forEach { c ->
            val local = ((t - c.phaseDelay).coerceAtLeast(0f) /
                    (1f - c.phaseDelay).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
            // 从 -10% 屏高 → 110% 屏高
            val y = -h * 0.1f + local * h * 1.2f
            val x = w * c.baseX + w * c.driftX * local
            val rot = c.initialRot + c.rotSpeed * local
            val alpha = when {
                local < 0.1f -> local / 0.1f
                local > 0.85f -> ((1f - local) / 0.15f).coerceAtLeast(0f)
                else -> 1f
            }
            val color = theme.confettiColors[c.colorIdx % theme.confettiColors.size]
            rotate(rot, pivot = Offset(x, y)) {
                drawRect(
                    color = color.copy(alpha = alpha * 0.95f),
                    topLeft = Offset(x - c.width / 2, y - c.height / 2),
                    size = Size(c.width, c.height),
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// ⑦ 中央卡片（礼盒揭晓）
// ════════════════════════════════════════════════════════════
@Composable
private fun CelebrationCard(
    theme: VipTheme,
    coins: Int,
    displayCoins: Int,
    enterT: Float,
    overall: Float,
    rayRot: Float,
) {
    // ease-out-back 弹入：0.4→1.06→1.0
    val scale = remember(enterT) {
        when {
            enterT < 0.6f -> {
                val k = enterT / 0.6f
                val s = 1f - (1f - k) * (1f - k)
                0.4f + s * 0.7f
            }
            enterT < 0.85f -> {
                val k = (enterT - 0.6f) / 0.25f
                1.1f - k * 0.1f
            }
            else -> 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.84f)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = overall
            },
        contentAlignment = Alignment.Center,
    ) {
        // ─ 卡片背后的辐射光线（从卡片 emoji 位置向外的"加冕光"）
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.18f
            val r = size.minDimension * 0.7f
            rotate(rayRot * 0.5f, pivot = Offset(cx, cy)) {
                for (i in 0 until 12) {
                    rotate(i * 30f, pivot = Offset(cx, cy)) {
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    theme.glow.copy(alpha = 0.45f),
                                    Color.Transparent,
                                ),
                                startY = cy,
                                endY = cy - r,
                            ),
                            start = Offset(cx, cy),
                            end = Offset(cx, cy - r),
                            strokeWidth = 2f,
                        )
                    }
                }
            }
        }

        // ─ 卡片本体（玻璃拟态白底 + 等级渐变描边）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.97f),
                            Color(0xFFFFFAFC),
                            Color(0xFFF6EFFF),
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(theme.gradientTop, theme.gradientBottom, theme.gradientTop)
                    ),
                    shape = RoundedCornerShape(28.dp),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ① 顶部 emoji（旋入 + 缩放）
                val emojiAlpha = ((enterT - 0.10f) / 0.20f).coerceIn(0f, 1f)
                val emojiRot = (1f - emojiAlpha) * 180f
                Text(
                    text = theme.emoji,
                    fontSize = 56.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = emojiAlpha
                        rotationZ = emojiRot
                        scaleX = 0.5f + emojiAlpha * 0.5f
                        scaleY = 0.5f + emojiAlpha * 0.5f
                    }
                )

                // ② "✨ 恭喜激活 ✨"
                val titleAlpha = ((enterT - 0.30f) / 0.20f).coerceIn(0f, 1f)
                Text(
                    text = "✨  恭喜激活  ✨",
                    fontSize = 14.sp,
                    color = theme.accent.copy(alpha = titleAlpha),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                )

                // ③ 等级名（艺术字）
                val nameAlpha = ((enterT - 0.35f) / 0.25f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = nameAlpha
                        val pop = 0.85f + nameAlpha * 0.15f
                        scaleX = pop; scaleY = pop
                    },
                ) {
                    Text(
                        text = theme.title,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            drawStyle = Stroke(width = 9f),
                            shadow = Shadow(
                                color = theme.glow.copy(alpha = 0.55f),
                                blurRadius = 18f,
                            ),
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = theme.title,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            brush = Brush.verticalGradient(
                                colors = listOf(theme.gradientTop, theme.gradientBottom)
                            ),
                            shadow = Shadow(
                                color = theme.gradientBottom.copy(alpha = 0.35f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f,
                            ),
                        ),
                        textAlign = TextAlign.Center,
                    )
                }

                // ④ subtitle
                Text(
                    text = theme.subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF555555).copy(
                        alpha = ((enterT - 0.45f) / 0.20f).coerceIn(0f, 1f)
                    ),
                    fontWeight = FontWeight.Medium,
                )

                // ⑤ 金币奖励
                if (coins > 0) {
                    val coinAlpha = ((enterT - 0.50f) / 0.25f).coerceIn(0f, 1f)
                    // 金币每跳一次 pop 强调
                    val coinPop = remember(displayCoins) { Animatable(1f) }
                    LaunchedEffect(displayCoins) {
                        if (displayCoins > 0 && displayCoins < coins) {
                            coinPop.snapTo(1.08f)
                            coinPop.animateTo(1f, tween(180))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .graphicsLayer {
                                alpha = coinAlpha
                                scaleX = coinPop.value
                                scaleY = coinPop.value
                            }
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        theme.gradientTop.copy(alpha = 0.25f),
                                        theme.gradientBottom.copy(alpha = 0.25f),
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = theme.accent.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50),
                            )
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("🪙", fontSize = 18.sp)
                        Text(
                            text = "+$displayCoins 金币",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.accent,
                        )
                    }
                    Text(
                        text = "已自动发放至账户",
                        fontSize = 11.sp,
                        color = Color(0xFF999999).copy(alpha = coinAlpha),
                    )
                }

                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
