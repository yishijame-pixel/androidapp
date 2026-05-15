// VipMemberCard.kt - VIP会员卡片组件（基于Figma设计复刻）
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * VIP会员卡片组件 - 完美复刻Figma设计
 * 特点：金色渐变、粒子动画、悬停效果、可收缩
 * @param isVip 是否是VIP用户
 * @param vipLevel VIP等级
 * @param isExpanded 是否展开（true=完整显示，false=收缩显示）
 * @param onClick 点击回调
 */
@Composable
fun VipMemberCard(
    isVip: Boolean = false,
    vipLevel: com.example.funlife.data.model.VipLevel = com.example.funlife.data.model.VipLevel.NORMAL,
    isExpanded: Boolean = true,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // 动画状态
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.6f),
        label = "scale"
    )
    
    val offsetY by animateDpAsState(
        targetValue = if (isHovered) (-3).dp else 0.dp,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.6f),
        label = "offsetY"
    )
    
    // 🔥 高度动画：展开120dp，收缩60dp
    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) 120.dp else 60.dp,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.7f),
        label = "cardHeight"
    )
    
    // 扫光动画
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    Box(
        modifier = Modifier
            .width(360.dp)
            .height(cardHeight)  // 🔥 使用动画高度
            .offset(y = offsetY)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // 外部光晕
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x73FBBF24), // rgba(251,191,36,0.45)
                            Color(0x26F59E0B), // rgba(245,158,11,0.15)
                            Color.Transparent
                        ),
                        radius = 400f
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        )
        
        // 卡片主体
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A0A00), // #1a0a00
                                Color(0xFF2D1500), // #2d1500
                                Color(0xFF1C1008), // #1c1008
                                Color(0xFF2A1200), // #2a1200
                                Color(0xFF0F0600)  // #0f0600
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0x80FBBF24), // rgba(251,191,36,0.5)
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                // 顶部金色线条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFFDE68A), // #fde68a
                                    Color(0xFFFBBF24), // #fbbf24
                                    Color(0xFFFDE68A),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // 内部纹理
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x0FFBBF24), // rgba(251,191,36,0.06)
                                    Color.Transparent
                                ),
                                center = Offset(0.3f, 0.5f),
                                radius = 800f
                            )
                        )
                )
                
                // 扫光效果
                if (isHovered) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x1FFDE68A), // rgba(253,230,138,0.12)
                                        Color.Transparent
                                    ),
                                    startX = shimmerOffset * 1000f,
                                    endX = (shimmerOffset + 0.4f) * 1000f
                                )
                            )
                    )
                }
                
                // 粒子星星
                ParticleStars()
                
                // 卡片内容 - 根据展开/收缩状态显示不同布局
                if (isExpanded) {
                    // 🔥 展开状态：显示完整内容
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 主要内容区域
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 皇冠图标
                            CrownIcon(isHovered = isHovered)
                            
                            // 文字内容
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // VIP徽章
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFFB45309),
                                                        Color(0xFFFBBF24),
                                                        Color(0xFFB45309)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "VIP",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.5.sp,
                                            color = Color(0xFF1A0A00)
                                        )
                                    }
                                    
                                    // 标题 - 根据VIP状态显示不同文字
                                    Text(
                                        text = if (isVip) {
                                            when (vipLevel) {
                                                com.example.funlife.data.model.VipLevel.VIP3 -> "终身VIP专属特权"
                                                com.example.funlife.data.model.VipLevel.VIP2 -> "年费VIP专属特权"
                                                com.example.funlife.data.model.VipLevel.VIP1 -> "普通VIP专属特权"
                                                else -> "专属优惠特权"
                                            }
                                        } else {
                                            "开通VIP享受超值优惠"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.3.sp,
                                        maxLines = 1,
                                        style = LocalTextStyle.current.copy(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFFDE68A),
                                                    Color(0xFFFBBF24),
                                                    Color(0xFFFDE68A),
                                                    Color(0xFFF59E0B)
                                                )
                                            )
                                        )
                                    )
                                }
                                
                                // 副标题 - 根据VIP状态显示不同文字
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (isVip) {
                                        Text(
                                            "所有商品仅",
                                            fontSize = 11.sp,
                                            color = Color(0xBFFDE68A),
                                            letterSpacing = 0.2.sp
                                        )
                                        Text(
                                            " ¥1 ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                        Text(
                                            "金币！",
                                            fontSize = 11.sp,
                                            color = Color(0xBFFDE68A)
                                        )
                                    } else {
                                        Text(
                                            "VIP用户商品仅需",
                                            fontSize = 10.sp,
                                            color = Color(0xBFFDE68A),
                                            letterSpacing = 0.2.sp
                                        )
                                        Text(
                                            " ¥1 ",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                        Text(
                                            "金币，立省90%！",
                                            fontSize = 10.sp,
                                            color = Color(0xBFFDE68A)
                                        )
                                    }
                                }
                            }
                            
                            // 右箭头
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // 分隔线
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(1.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x40FBBF24), // rgba(251,191,36,0.25)
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        // 🔥 底部特权列表 - 去掉"专属客服"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val perks = if (isVip) {
                                when (vipLevel) {
                                    com.example.funlife.data.model.VipLevel.VIP3 -> listOf("无限容量", "专属折扣", "优先发货")
                                    com.example.funlife.data.model.VipLevel.VIP2 -> listOf("超大容量", "专属折扣", "优先发货")
                                    com.example.funlife.data.model.VipLevel.VIP1 -> listOf("大容量", "专属折扣", "优先发货")
                                    else -> listOf("限时折扣", "专属折扣", "优先发货")
                                }
                            } else {
                                listOf("回馈折扣", "专属折扣", "优先发货")
                            }
                            
                            perks.forEach { perk ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                color = Color(0xFFFBBF24),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        perk,
                                        fontSize = 11.sp,
                                        color = Color(0xA6FDE68A), // rgba(253,230,138,0.65)
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 🔥 收缩状态：只显示核心信息（VIP徽章 + 简短标题 + 箭头）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // VIP徽章
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFB45309),
                                            Color(0xFFFBBF24),
                                            Color(0xFFB45309)
                                        )
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "VIP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = Color(0xFF1A0A00)
                            )
                        }
                        
                        // 简短标题
                        Text(
                            text = if (isVip) {
                                when (vipLevel) {
                                    com.example.funlife.data.model.VipLevel.VIP3 -> "终身VIP · 所有商品仅¥1"
                                    com.example.funlife.data.model.VipLevel.VIP2 -> "年费VIP · 所有商品仅¥1"
                                    com.example.funlife.data.model.VipLevel.VIP1 -> "普通VIP · 所有商品仅¥1"
                                    else -> "VIP专属 · 所有商品仅¥1"
                                }
                            } else {
                                "开通VIP · 商品仅¥1金币"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            style = LocalTextStyle.current.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFDE68A),
                                        Color(0xFFFBBF24),
                                        Color(0xFFFDE68A),
                                        Color(0xFFF59E0B)
                                    )
                                )
                            )
                        )
                        
                        // 右箭头
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // 底部金色线条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x4DFBBF24), // rgba(251,191,36,0.3)
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * 皇冠图标组件
 */
@Composable
private fun CrownIcon(isHovered: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 0f,
        animationSpec = spring(stiffness = 200f),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        // 图标光晕
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(1.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x99FBBF24), // rgba(251,191,36,0.6)
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // 图标背景
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7C3408),
                            Color(0xFFB45309),
                            Color(0xFFD97706),
                            Color(0xFF92400E)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xB3FBBF24), // rgba(251,191,36,0.7)
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "👑",
                fontSize = 28.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation
                }
            )
        }
    }
}

/**
 * 粒子星星效果
 */
@Composable
private fun ParticleStars() {
    val particles = remember {
        List(16) {
            VipCardParticleData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 1f
            )
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = Color(0xFFFFD700),
                radius = particle.size,
                center = Offset(
                    x = size.width * particle.x,
                    y = size.height * particle.y
                ),
                alpha = 0.3f
            )
        }
    }
}

private data class VipCardParticleData(
    val x: Float,
    val y: Float,
    val size: Float
)
