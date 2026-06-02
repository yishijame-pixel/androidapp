// ProductSpinWheel.kt - 商品转盘组件（豪华版）
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.SpinWheelViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 清新明亮配色 - 饱和度适中，文字清晰
private val sectorGradients = listOf(
    Pair(Color(0xFFFF8A8A), Color(0xFFFFB3B3)),  // 樱花粉
    Pair(Color(0xFFFFCC4D), Color(0xFFFFE08A)),  // 暖阳黄
    Pair(Color(0xFF7DD4A3), Color(0xFFAFE8C8)),  // 薄荷绿
    Pair(Color(0xFF6BB8E8), Color(0xFF9DD1F0)),  // 天空蓝
    Pair(Color(0xFFFFAA7B), Color(0xFFFFCCAA)),  // 蜜桃橙
    Pair(Color(0xFFBB99E8), Color(0xFFD4BBF5)),  // 薰衣草紫
    Pair(Color(0xFF6DB8D0), Color(0xFF99D4E4)),  // 清澈蓝
    Pair(Color(0xFFF09090), Color(0xFFFFB8B8))   // 珊瑚粉
)

@Composable
fun ProductSpinWheel(
    prizes: List<SpinWheelViewModel.ProductPrize>,
    shopPoints: Int,
    userCoins: Int,
    onSpin: () -> Unit,
    isSpinning: Boolean,
    resultPrize: SpinWheelViewModel.ProductPrize?,
    onResultDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** 🧪 可选：测试转盘（不扣积分），仅 Debug 显示按钮 */
    onTestSpin: (() -> Unit)? = null
) {
    var isAnimating by remember { mutableStateOf(false) }
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var showResult by remember { mutableStateOf(false) }
    // 记录上一次已处理的奖品引用，避免重复触发旋转
    var lastHandledPrize by remember { mutableStateOf<SpinWheelViewModel.ProductPrize?>(null) }

    // 旋转动画
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = 4500,
            easing = CubicBezierEasing(0.15f, 0.85f, 0.10f, 1.0f)
        ),
        finishedListener = {
            isAnimating = false
            if (resultPrize != null) {
                showResult = true
            }
        },
        label = "wheelRotation"
    )

    // 🎯 关键修复：监听 resultPrize 变化，按奖品索引精确计算目标角度，使指针落在该扇形中心
    //
    // 扇形布局（绘制时）：sector i 的 startAngle = i*sweep - 90，center = i*sweep + sweep/2 - 90
    // 指针固定在屏幕 12 点方向（screen angle = -90 即 270）
    // 旋转 R 度后 sector i 的 center 变为：i*sweep + sweep/2 - 90 + R
    // 想让它对准指针：i*sweep + sweep/2 - 90 + R ≡ -90 (mod 360)
    //   → R ≡ -i*sweep - sweep/2 (mod 360)
    //   → R ≡ (360 - i*sweep - sweep/2) mod 360
    LaunchedEffect(resultPrize) {
        val prize = resultPrize ?: run {
            lastHandledPrize = null
            return@LaunchedEffect
        }
        if (prize === lastHandledPrize) return@LaunchedEffect // 同一次结果已处理，避免重复转
        if (prizes.isEmpty()) return@LaunchedEffect
        val prizeIndex = prizes.indexOf(prize)
        if (prizeIndex < 0) return@LaunchedEffect

        lastHandledPrize = prize
        isAnimating = true
        showResult = false

        val sweep = 360f / prizes.size
        // 落到扇形 i 中心所需的旋转角（mod 360）
        val landingAngle = ((360f - prizeIndex * sweep - sweep / 2f) % 360f + 360f) % 360f

        // 在扇形内随机微偏移，让指针不总是落在正中（±35% 扇形范围，避免太靠近分界线）
        val randomJitter = (Random.nextFloat() - 0.5f) * sweep * 0.7f
        val finalLandingMod = ((landingAngle + randomJitter) % 360f + 360f) % 360f

        // 当前角度归一化
        val currentMod = ((targetRotation % 360f) + 360f) % 360f
        var diff = finalLandingMod - currentMod
        if (diff < 0f) diff += 360f

        // 至少 6 整圈 + 对齐偏移，营造连续加速→减速的视觉效果
        targetRotation = targetRotation + 360f * 6 + diff
    }

    // 外圈灯泡闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "lights")
    val lightPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightPhase"
    )

    // 外圈旋转光晕
    val outerGlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerGlow"
    )

    // 呼吸光效
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ═══════════════════════════════════════════════════════
        // 💎 紧凑信息芯片栏 - 单行容纳 积分/消耗/可抽次数（节省 ~100dp 垂直空间）
        // ═══════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 积分芯片（红粉渐变）
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("⭐", fontSize = 14.sp)
                    Text(
                        "积分",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "$shopPoints",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // 消耗芯片（紫色渐变）
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🎯", fontSize = 14.sp)
                    Text(
                        "10/次",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // 可抽次数芯片（金色）
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFF9800))
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🎰", fontSize = 13.sp)
                    Text(
                        "${shopPoints / 10}次",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════
        // 🎡 专业转盘设计 - 指针固定不旋转
        // ═══════════════════════════════════════════════════════
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // 根据屏幕宽度自适应转盘大小
            val availableSize = minOf(maxWidth, 360.dp)
            val wheelSize = availableSize - 40.dp
            val outerRingSize = availableSize

            // 第1层：柔和呼吸光晕（最底层）
            Canvas(
                modifier = Modifier
                    .size(outerRingSize + 16.dp)
                    .graphicsLayer { alpha = breatheAlpha }
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF8BBD0).copy(alpha = 0.3f),
                            Color(0xFFE1BEE7).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2
                )
            }

            // 第2层：外环装饰圈（固定不转）
            Canvas(modifier = Modifier.size(outerRingSize)) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerR = size.minDimension / 2
                val ringWidth = 18f
                val innerR = outerR - ringWidth

                // 粉色渐变外环
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF48FB1), Color(0xFFF8BBD0)),
                        center = center, radius = outerR
                    ),
                    radius = outerR, center = center
                )
                // 浅色内边
                drawCircle(color = Color(0xFFFCE4EC), radius = innerR, center = center)

                // 外环高光
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = -130f, sweepAngle = 80f,
                    useCenter = false,
                    style = Stroke(width = ringWidth * 0.6f, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - outerR + ringWidth / 2, center.y - outerR + ringWidth / 2),
                    size = Size((outerR - ringWidth / 2) * 2, (outerR - ringWidth / 2) * 2)
                )

                // 装饰点 - 20颗，柔和金色/粉色交替
                val dotCount = 20
                val dotR = outerR - ringWidth / 2
                for (i in 0 until dotCount) {
                    val angle = Math.toRadians((i * 360.0 / dotCount) - 90.0)
                    val dx = center.x + (dotR * cos(angle)).toFloat()
                    val dy = center.y + (dotR * sin(angle)).toFloat()
                    val isGold = ((i + (lightPhase * dotCount).toInt()) % 2 == 0)
                    val dotSize = if (isGold) 4.5f else 3f
                    drawCircle(
                        color = if (isGold) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f),
                        radius = dotSize, center = Offset(dx, dy)
                    )
                    if (isGold) {
                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                            radius = 8f, center = Offset(dx, dy)
                        )
                    }
                }
            }

            // 第3层：转盘主体（旋转）
            Canvas(
                modifier = Modifier
                    .size(wheelSize)
                    .rotate(animatedRotation)
            ) {
                // 🔒 防御：奖品列表为空时直接返回，避免 360f/0 = Infinity 触发异常
                if (prizes.isEmpty()) return@Canvas
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.minDimension / 2
                val sweep = 360f / prizes.size

                // 白色底
                drawCircle(color = Color.White, radius = r, center = Offset(cx, cy))

                prizes.forEachIndexed { index, prize ->
                    val startAngle = index * sweep - 90f
                    val (c1, c2) = sectorGradients[index % sectorGradients.size]

                    // 扇区填充 - 提高饱和度
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(c2, c1, c1),
                            center = Offset(cx, cy), radius = r
                        ),
                        startAngle = startAngle, sweepAngle = sweep,
                        useCenter = true, size = Size(size.width, size.height)
                    )

                    // 扇区内侧柔光
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(cx, cy), radius = r * 0.45f
                        ),
                        startAngle = startAngle, sweepAngle = sweep,
                        useCenter = true, size = Size(size.width, size.height)
                    )

                    // 白色分割线
                    val la = Math.toRadians(startAngle.toDouble())
                    drawLine(
                        color = Color.White,
                        start = Offset(cx, cy),
                        end = Offset(cx + (r * cos(la)).toFloat(), cy + (r * sin(la)).toFloat()),
                        strokeWidth = 2.5f, cap = StrokeCap.Round
                    )

                    // 文字和图标（沿径向旋转）
                    val midAngle = startAngle + sweep / 2
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.rotate(midAngle + 90f, cx, cy)

                        // emoji图标 - 大号清晰
                        val emojiPaint = android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 48f
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(prize.icon, cx, cy - r * 0.62f, emojiPaint)

                        // 文字白色描边底
                        val strokePaint = android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 28f
                            color = android.graphics.Color.WHITE
                            isFakeBoldText = true
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 6f
                        }
                        canvas.nativeCanvas.drawText(prize.name, cx, cy - r * 0.38f, strokePaint)

                        // 文字主体 - 深色加粗
                        val textPaint = android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 28f
                            color = android.graphics.Color.argb(240, 50, 40, 40)
                            isFakeBoldText = true
                            isAntiAlias = true
                            setShadowLayer(2f, 0f, 1f, android.graphics.Color.argb(60, 0, 0, 0))
                        }
                        canvas.nativeCanvas.drawText(prize.name, cx, cy - r * 0.38f, textPaint)

                        canvas.nativeCanvas.restore()
                    }
                }

                // 白色外边
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = r, center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )

                // 中心圆盘
                val centerR = r * 0.14f
                // 外环
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFE082), Color(0xFFFFCA28), Color(0xFFFFB300))
                    ),
                    radius = centerR + 4f, center = Offset(cx, cy)
                )
                // 内圆
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFE082))
                    ),
                    radius = centerR, center = Offset(cx, cy)
                )
                // 高光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(cx - 3f, cy - 4f)
                    ),
                    radius = centerR * 0.5f,
                    center = Offset(cx - 3f, cy - 4f)
                )
                // 星形标记（用小圆点代替）
                drawCircle(
                    color = Color(0xFFFF8F00).copy(alpha = 0.5f),
                    radius = 3f, center = Offset(cx, cy - centerR * 0.4f)
                )
                drawCircle(
                    color = Color(0xFFFF8F00).copy(alpha = 0.5f),
                    radius = 2f, center = Offset(cx - centerR * 0.35f, cy + centerR * 0.2f)
                )
                drawCircle(
                    color = Color(0xFFFF8F00).copy(alpha = 0.5f),
                    radius = 2f, center = Offset(cx + centerR * 0.35f, cy + centerR * 0.2f)
                )
            }

            // 第4层：指针（固定不旋转！独立Canvas在转盘之上）
            Canvas(
                modifier = Modifier
                    .size(wheelSize)
            ) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.minDimension / 2
                val centerR = r * 0.14f

                // 短小精致的水滴指针
                val pW = 10f         // 三角半宽
                val tipY = cy - centerR - 36f   // 尖端（短小）
                val baseY = cy - centerR + 4f   // 底部贴中心圆

                // 柔和阴影
                drawPath(
                    Path().apply {
                        moveTo(cx + 1.5f, tipY + 2f)
                        lineTo(cx - pW + 1.5f, baseY + 2f)
                        lineTo(cx + pW + 1.5f, baseY + 2f)
                        close()
                    },
                    Color.Black.copy(alpha = 0.12f)
                )

                // 指针三角主体
                val tri = Path().apply {
                    moveTo(cx, tipY)
                    lineTo(cx - pW, baseY)
                    lineTo(cx + pW, baseY)
                    close()
                }
                drawPath(
                    tri,
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF5722), Color(0xFFE53935)),
                        startY = tipY, endY = baseY
                    )
                )
                drawPath(tri, Color.White.copy(alpha = 0.9f), style = Stroke(width = 2f))

                // 底部圆球 - 盖住三角底边
                val ballR = 12f
                val ballY = baseY + ballR * 0.3f
                // 球阴影
                drawCircle(Color.Black.copy(alpha = 0.08f), radius = ballR + 1f, center = Offset(cx + 1f, ballY + 1.5f))
                // 球主体
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF7043), Color(0xFFE53935), Color(0xFFC62828)),
                        center = Offset(cx - 2f, ballY - 3f)
                    ),
                    radius = ballR, center = Offset(cx, ballY)
                )
                // 球边框
                drawCircle(Color.White.copy(alpha = 0.8f), radius = ballR, center = Offset(cx, ballY), style = Stroke(width = 2f))
                // 球高光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(cx - 3f, ballY - 4f)
                    ),
                    radius = 5f, center = Offset(cx - 3f, ballY - 4f)
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        // 🎰 豪华旋转按钮
        // ═══════════════════════════════════════════════════════
        val canSpin = shopPoints >= 10 && !isAnimating

        val buttonScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (canSpin) 1.04f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "btnScale"
        )
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(56.dp)
                .scale(buttonScale)
                .shadow(
                    if (canSpin) 12.dp else 4.dp,
                    RoundedCornerShape(28.dp),
                    ambientColor = if (canSpin) Color(0xFFFF6B6B).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f),
                    spotColor = if (canSpin) Color(0xFFFF6B6B).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (canSpin) Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFFF8E53),
                            Color(0xFFFF6B6B)
                        )
                    ) else Brush.horizontalGradient(
                        colors = listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
                    )
                )
                .clickable(enabled = canSpin) {
                    // 🎯 修复：不再在这里随机设置 targetRotation。
                    // 让 onSpin() 触发 ViewModel 算出 prize 后通过 resultPrize 回传，
                    // 由 LaunchedEffect(resultPrize) 精确计算落点角度，确保指针与弹窗一致。
                    if (canSpin && !isAnimating) {
                        onSpin()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 光泽滑过效果
            if (canSpin) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = shimmerOffset * size.width
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 200f
                            )
                        )
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isAnimating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "抽奖中...",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                } else {
                    Text("🎰", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (canSpin) "消耗10积分 · 开始抽奖" else "积分不足",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }

        // 🧪 测试按钮（仅 Debug 构建 + 调用方传入了 onTestSpin 才显示）
        if (com.example.funlife.BuildConfig.DEBUG && onTestSpin != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(horizontal = 16.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF7E57C2).copy(alpha = 0.85f))
                    .clickable(enabled = !isAnimating) {
                        if (!isAnimating) onTestSpin.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🧪", fontSize = 14.sp)
                    Text(
                        "测试转盘（不扣积分）",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ═══════════════════════════════════════════════════════
        // 🎁 奖品一览 - 精致卡片网格
        // ═══════════════════════════════════════════════════════
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFFF8F0)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                                    ),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎁", fontSize = 14.sp)
                        }
                        Text(
                            "奖品一览",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D1810)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "共${prizes.size}种奖品",
                            fontSize = 11.sp,
                            color = Color(0xFF999999)
                        )
                    }

                    // 网格布局 - 两行四列
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..3) {
                                val index = row * 4 + col
                                if (index < prizes.size) {
                                    val prize = prizes[index]
                                    val (c1, _) = sectorGradients[index % sectorGradients.size]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .background(
                                                c1.copy(alpha = 0.08f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                c1.copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(prize.icon, fontSize = 26.sp)
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                prize.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF444444),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // ═══════════════════════════════════════════════════════
    // 🎊 豪华结果弹窗
    // ═══════════════════════════════════════════════════════
    if (showResult && resultPrize != null) {
        // 弹窗入场动画
        var dialogVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            dialogVisible = true
        }

        AlertDialog(
            onDismissRequest = {
                showResult = false
                onResultDismiss()
            },
            title = null,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 顶部彩带
                    Text("🎊✨🎉", fontSize = 32.sp)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "恭喜获得",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF888888)
                    )

                    Spacer(Modifier.height(16.dp))

                    // 奖品展示卡片
                    val prizeIndex = prizes.indexOf(resultPrize).coerceAtLeast(0)
                    val (prizeColor, _) = sectorGradients[prizeIndex % sectorGradients.size]

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        prizeColor.copy(alpha = 0.15f),
                                        prizeColor.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            prizeColor.copy(alpha = 0.2f),
                                            prizeColor.copy(alpha = 0.08f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(2.dp, prizeColor.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(resultPrize.icon, fontSize = 40.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        resultPrize.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = prizeColor
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = prizeColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            if (resultPrize.type == "coins") "💰 金币已到账" else "🎒 已放入背包",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            color = prizeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "剩余积分：$shopPoints",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                                )
                            )
                            .clickable {
                                showResult = false
                                onResultDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "太棒了！",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }
}
