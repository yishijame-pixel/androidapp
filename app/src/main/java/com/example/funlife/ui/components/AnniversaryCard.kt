// AnniversaryCard.kt - 纪念日卡片组件
package com.example.funlife.ui.components

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.funlife.data.model.Anniversary

@Composable
fun AnniversaryCard(
    anniversary: Anniversary,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = anniversary.getDaysRemaining()
    var isExpanded by remember { mutableStateOf(false) }
    
    // 进入动画
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )
    
    // 根据剩余天数选择渐变色
    val gradientColors = when {
        daysRemaining > 30 -> listOf(
            Color(0xFF4ECDC4),
            Color(0xFF44A5A0)
        )
        daysRemaining in 8..30 -> listOf(
            Color(0xFFFF8C42),
            Color(0xFFFF6B35)
        )
        daysRemaining >= 0 -> listOf(
            Color(0xFFFF6B9D),
            Color(0xFFE74C3C)
        )
        else -> listOf(
            Color(0xFF95A5A6),
            Color(0xFF7F8C8D)
        )
    }
    
    // 今天的特殊效果
    val isToday = daysRemaining == 0L
    val pulseScale by animateFloatAsState(
        targetValue = if (isToday) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
                this.alpha = alpha
            }
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 12.dp else 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // 背景图片（如果有）
            if (!anniversary.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = Uri.parse(anniversary.imageUri),
                    contentDescription = "背景图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isExpanded) 220.dp else 180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // 半透明遮罩，确保文字可读
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isExpanded) 220.dp else 180.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧彩色条
                    if (anniversary.imageUri.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(80.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    brush = Brush.verticalGradient(gradientColors)
                                )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    // 中间内容
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 类型emoji + 标题
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = anniversary.getTypeEnum().emoji,
                                fontSize = 28.sp
                            )
                            
                            Column {
                                Text(
                                    text = anniversary.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                
                                // 重要程度星星
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    repeat(anniversary.importance) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 日期和年份信息
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = anniversary.getFormattedDate(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    Color.White.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            
                            // 每年重复标记
                            if (anniversary.isYearly) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                        Color.White.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    Text(
                                        text = "🔄 每年",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 已过年数（如果有）
                        val yearsPassed = anniversary.getYearsPassed()
                        if (anniversary.isYearly && yearsPassed > 0) {
                            Text(
                                text = "已经 $yearsPassed 年了 💝",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    Color.White.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        
                        // 剩余天数显示
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                Color.White.copy(alpha = 0.9f)
                            } else {
                                gradientColors[0].copy(alpha = 0.15f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val daysText = when {
                                    daysRemaining > 0 -> "还有 $daysRemaining 天"
                                    daysRemaining == 0L -> "🎉 就是今天！"
                                    else -> "已过去 ${-daysRemaining} 天"
                                }
                                
                                Text(
                                    text = daysText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                        gradientColors[0]
                                    } else {
                                        gradientColors[0]
                                    }
                                )
                            }
                        }
                        
                        // 进度条
                        if (anniversary.isYearly && daysRemaining >= 0) {
                            val progress = 1f - (daysRemaining.toFloat() / 365f)
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = gradientColors[0],
                                trackColor = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    Color.White.copy(alpha = 0.3f)
                                } else {
                                    gradientColors[0].copy(alpha = 0.2f)
                                }
                            )
                        }
                        
                        // 展开的详细信息
                        if (isExpanded && !anniversary.note.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    Color.White.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "📝 备注",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                            Color.White.copy(alpha = 0.9f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = anniversary.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (!anniversary.imageUri.isNullOrEmpty()) {
                                            Color.White.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // 操作按钮
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // 置顶按钮
                        IconButton(
                            onClick = onPin,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (anniversary.isPinned) {
                                    Icons.Default.PushPin
                                } else {
                                    Icons.Outlined.PushPin
                                },
                                contentDescription = if (anniversary.isPinned) "取消置顶" else "置顶",
                                tint = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    if (anniversary.isPinned) Color(0xFFFFD700) else Color.White
                                } else {
                                    if (anniversary.isPinned) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 删除按钮
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 展开/收起按钮
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (isExpanded) "收起" else "展开",
                                tint = if (!anniversary.imageUri.isNullOrEmpty()) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
