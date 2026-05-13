// VipUpgradePrompt.kt - VIP升级提示组件
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * VIP升级提示卡片
 */
@Composable
fun VipUpgradePromptCard(
    currentVipLevel: Int,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentVipLevel < 3) {
        val nextLevel = currentVipLevel + 1
        val (icon, title, benefits) = when (nextLevel) {
            1 -> Triple(
                "⭐",
                "升级到VIP1",
                listOf("商品50金币起", "每日20金币", "专属头像框")
            )
            2 -> Triple(
                "💎",
                "升级到VIP2",
                listOf("商品20金币起", "每日50金币", "背包1000个", "更多头像框")
            )
            3 -> Triple(
                "👑",
                "升级到VIP3",
                listOf("商品1金币起", "每日100金币", "背包无限", "专属特效", "自定义背景")
            )
            else -> return
        }
        
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = when (nextLevel) {
                                    1 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                    2 -> listOf(Color(0xFF00BCD4), Color(0xFF0097A7))
                                    3 -> listOf(Color(0xFFFF6B9D), Color(0xFFFFD700))
                                    else -> listOf(Color.Gray, Color.LightGray)
                                }
                            )
                        )
                )
                
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            icon,
                            fontSize = 32.sp
                        )
                        Column {
                            Text(
                                title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "解锁更多特权",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        benefits.take(3).forEach { benefit ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    benefit,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "立即升级",
                            color = when (nextLevel) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFF00BCD4)
                                3 -> Color(0xFFFF6B9D)
                                else -> Color.Gray
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * VIP升级成功动画
 */
@Composable
fun VipUpgradeSuccessAnimation(
    newVipLevel: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(0.5f) }
    
    LaunchedEffect(Unit) {
        visible = true
        animate(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            scale = value
        }
        delay(3000)
        visible = false
        delay(300)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = when (newVipLevel) {
                                    1 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                    2 -> listOf(Color(0xFF00BCD4), Color(0xFF0097A7))
                                    3 -> listOf(Color(0xFFFF6B9D), Color(0xFFFFD700))
                                    else -> listOf(Color.Gray, Color.LightGray)
                                }
                            )
                        )
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    ) {
                        // VIP图标
                        Text(
                            when (newVipLevel) {
                                1 -> "⭐"
                                2 -> "💎"
                                3 -> "👑"
                                else -> "🎉"
                            },
                            fontSize = 80.sp
                        )
                        
                        Text(
                            "恭喜升级！",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            when (newVipLevel) {
                                1 -> "VIP1 普通VIP"
                                2 -> "VIP2 年费VIP"
                                3 -> "VIP3 终身VIP"
                                else -> "VIP"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        
                        Text(
                            "解锁更多专属特权",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * VIP对比表
 */
@Composable
fun VipComparisonDialog(
    currentVipLevel: Int,
    onDismiss: () -> Unit,
    onUpgrade: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "VIP特权对比",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF95A5A6)
                        )
                    }
                }
                
                Divider(color = Color(0xFFF0F0F0))
                
                // VIP对比内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VipComparisonItem(
                        level = 1,
                        icon = "⭐",
                        name = "普通VIP",
                        price = "永久",
                        benefits = listOf(
                            "商品50金币起",
                            "每日20金币",
                            "专属头像框",
                            "金色特效"
                        ),
                        isCurrent = currentVipLevel == 1,
                        onUpgrade = { onUpgrade(1) }
                    )
                    
                    VipComparisonItem(
                        level = 2,
                        icon = "💎",
                        name = "年费VIP",
                        price = "365天",
                        benefits = listOf(
                            "商品20金币起",
                            "每日50金币",
                            "背包1000个",
                            "钻石特效",
                            "更多头像框"
                        ),
                        isCurrent = currentVipLevel == 2,
                        onUpgrade = { onUpgrade(2) }
                    )
                    
                    VipComparisonItem(
                        level = 3,
                        icon = "👑",
                        name = "终身VIP",
                        price = "永久",
                        benefits = listOf(
                            "商品1金币起",
                            "每日100金币",
                            "背包无限",
                            "皇冠特效",
                            "流星效果",
                            "自定义背景",
                            "专属客服"
                        ),
                        isCurrent = currentVipLevel == 3,
                        onUpgrade = { onUpgrade(3) }
                    )
                }
            }
        }
    }
}

@Composable
fun VipComparisonItem(
    level: Int,
    icon: String,
    name: String,
    price: String,
    benefits: List<String>,
    isCurrent: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFFF6B35).copy(alpha = 0.1f) else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 24.sp)
                    Column {
                        Text(
                            name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Text(
                            price,
                            fontSize = 12.sp,
                            color = Color(0xFF95A5A6)
                        )
                    }
                }
                
                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50)
                    ) {
                        Text(
                            "当前",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                benefits.forEach { benefit ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            benefit,
                            fontSize = 13.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }
            
            if (!isCurrent) {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("升级")
                }
            }
        }
    }
}
