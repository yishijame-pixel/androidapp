package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import com.example.funlife.ui.screens.VipCardData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * VIP 完整权益弹窗
 *
 * 设计：
 *  - 半透明遮罩 + 居中圆角卡片
 *  - 顶部：VIP 标识 + 标题 + 副标题（如"30 天有效期"）
 *  - 中部：可滚动权益列表（emoji 圆形容器 + 标题 + 副文）
 *  - 底部：CTA「立即开通 ¥xx」按钮
 *
 * 复用 VipCardData（gradient/level/title/price/period 等都已具备）
 */
@Composable
fun VipBenefitsDialog(
    card: VipCardData,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "benefitsDialog")
    val shimmer by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "shimmer"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // 主卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                card.gradient.first().copy(alpha = 0.96f),
                                card.gradient.getOrElse(1) { card.gradient.first() }.copy(alpha = 0.96f),
                                card.gradient.last().copy(alpha = 0.96f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.85f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(shimmer * 1000f - 500f, 0f),
                            end = androidx.compose.ui.geometry.Offset(shimmer * 1000f + 500f, 1000f)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { /* 拦截点击不关闭 */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // ── 顶部：图标 + 标题 + 关闭按钮 ──
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // emoji 圆形
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.6f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = card.icon, fontSize = 26.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${card.period} · ¥${card.price} · ${card.level.fullBenefits.size} 项专属权益",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 关闭按钮
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── 分割线 ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Spacer(Modifier.height(14.dp))

                    // ── 权益列表（滚动）──
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        card.level.fullBenefits.forEachIndexed { idx, b ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.10f + (idx % 3) * 0.015f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                // emoji 容器
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.4f),
                                                    Color.White.copy(alpha = 0.15f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = b.emoji, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        text = b.desc,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── 底部 CTA ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFFE082),
                                        Color(0xFFFFC107),
                                        Color(0xFFFF8F00)
                                    )
                                )
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👍 我知道了，继续选购",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6B3E00)
                        )
                    }
                }
            }
        }
    }
}
