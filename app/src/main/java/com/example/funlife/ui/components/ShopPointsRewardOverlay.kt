// ShopPointsRewardOverlay.kt - 转盘抽中积分时的全屏奖励动画
package com.example.funlife.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.funlife.ui.utils.rememberScreenAdapter
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * 🎯 转盘抽中积分奖励的全屏覆盖动画
 *
 * 设计要点：
 *  - 视觉权重 >> 普通结果动画：弹簧缩放 + 闪光 + 旋转光环 + 8 颗放射粒子
 *  - 自动 2.5 秒消失（也可点击关闭）
 *  - 严格按 ScreenAdapter 缩放，兼容小屏 / 平板
 *  - 不依赖外部资源，纯 Compose 绘制
 *
 * 用法：放在转盘 Screen 根 Box 的 zIndex 最高层，由 ViewModel 触发 visible=true
 */
@Composable
fun ShopPointsRewardOverlay(
    visible: Boolean,
    pointsAmount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pointsAmount <= 0) return

    // 自动关闭计时器
    LaunchedEffect(visible, pointsAmount) {
        if (visible) {
            delay(2500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(260)),
        modifier = modifier.zIndex(999f)
    ) {
        ShopPointsCelebration(pointsAmount = pointsAmount, onTap = onDismiss)
    }
}

@Composable
private fun ShopPointsCelebration(
    pointsAmount: Int,
    onTap: () -> Unit
) {
    val sa = rememberScreenAdapter()

    // ─ 入场缩放（弹簧效果）
    val entered = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered.value = true }
    val cardScale by animateFloatAsState(
        targetValue = if (entered.value) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )

    // ─ 持续光环旋转
    val infinite = rememberInfiniteTransition(label = "halo")
    val haloAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloAngle"
    )
    // ─ 数字脉冲
    val pulse by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }
    // ─ 半透明遮罩（点击关闭）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sa.dp(280))
                .scale(cardScale),
            contentAlignment = Alignment.Center
        ) {
            // 后层：旋转放射光线（用 8 个细长圆条围一圈）
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .rotate(haloAngle)
            ) {
                repeat(8) { i ->
                    val angleDeg = i * 45f
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val r = sa.dp(110)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (cos(rad) * r.value).dp,
                                y = (sin(rad) * r.value).dp
                            )
                            .size(width = sa.dp(8), height = sa.dp(28))
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.9f),
                                        Color(0xFFFFA500).copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .rotate(angleDeg + 90f)
                    )
                }
            }

            // 中层：金色圆盘 + 阴影
            Box(
                modifier = Modifier
                    .size(sa.dp(200))
                    .shadow(elevation = sa.dp(24), shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF6B0),
                                Color(0xFFFFC92E),
                                Color(0xFFFF8A00)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✨",
                        fontSize = sa.sp(36)
                    )
                    Spacer(Modifier.height(sa.dp(2)))
                    Text(
                        text = "+$pointsAmount",
                        fontSize = sa.sp(64),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5B2C00),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.scale(pulse)
                    )
                    Text(
                        text = "积分",
                        fontSize = sa.sp(18),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5B2C00),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 底部说明（轻量提示）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sa.dp(80)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "幸运掉落  ·  可在商品转盘消耗",
                fontSize = sa.sp(13),
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

