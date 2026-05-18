package com.example.funlife.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.utils.AnniversaryReminderManager

/**
 * App 内顶部悬浮提醒条
 * - 一直显示直到用户手动点 X 关闭
 * - 点击主体跳转纪念日页面
 */
@Composable
fun InAppReminderBanner(
    onClick: () -> Unit
) {
    val visible by AnniversaryReminderManager.bannerVisible.collectAsState()
    val count by AnniversaryReminderManager.bannerCount.collectAsState()

    val infinite = rememberInfiniteTransition(label = "bannerAnim")
    val bellRot by infinite.animateFloat(
        initialValue = -12f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(220, easing = EaseInOut), RepeatMode.Reverse),
        label = "bell"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .shadow(12.dp, RoundedCornerShape(50), ambientColor = Color(0xFFEC407A), spotColor = Color(0xFFEC407A))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFCAD4),
                            Color(0xFFFF80AB),
                            Color(0xFFEC407A),
                            Color(0xFFD81B60)
                        )
                    )
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 摇动的铃铛
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🔔",
                        fontSize = 22.sp,
                        modifier = Modifier.graphicsLayer { rotationZ = bellRot }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日纪念日提醒 🎀",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        style = TextStyle(shadow = Shadow(Color(0xFFAD1457), Offset(0f, 2f), 4f))
                    )
                    Text(
                        text = "✨ 你有 $count 个值得庆祝的日子！点击查看 💗",
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
                // X 关闭按钮
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f))
                        .clickable {
                            AnniversaryReminderManager.dismissInAppBanner()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
