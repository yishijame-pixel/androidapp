// AnniversaryCard.kt - 纪念日卡片组件
package com.example.funlife.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
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
    onEdit: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = anniversary.getDaysRemaining()
    var isExpanded by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    
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
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
                this.alpha = alpha
            }
            .animateContentSize()
            .clickable { showDetailDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        // 🔥 相框部分 - 使用透明中间区域的相框
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
        ) {
            // 根据卡片宽度计算图标大小
            val cardWidth = maxWidth
            val iconSize = (cardWidth * 0.06f).coerceIn(20.dp, 28.dp)  // 图标大小为卡片宽度的6%
            val iconFontSize = (iconSize.value * 0.7f).sp  // 字体大小为图标的70%
            val iconSpacing = (iconSize * 0.3f).coerceAtLeast(4.dp)  // 间距为图标的30%
            val iconPadding = (cardWidth * 0.02f).coerceIn(4.dp, 8.dp)  // 边距为卡片宽度的2%
            val context = androidx.compose.ui.platform.LocalContext.current
            val frameBitmap = remember(anniversary.frameId) {
                com.example.funlife.utils.ImageCache.loadImage(context, "login/${anniversary.frameId}.png")
            }
            
            // 🔥 用户上传的图片显示在底层
            if (!anniversary.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = Uri.parse(anniversary.imageUri),
                    contentDescription = "纪念日图片",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 50.dp,
                            end = 50.dp,
                            top = 40.dp,
                            bottom = 40.dp
                        )
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 占位符
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 50.dp,
                            end = 50.dp,
                            top = 40.dp,
                            bottom = 40.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📷",
                            fontSize = 48.sp,
                            color = Color.Gray.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            
            // 🔥 透明相框边框覆盖在最上层
            frameBitmap?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "相框边框",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
            
            // 🔥 右上角可展开操作菜单（自适应大小）
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(iconPadding),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(iconSpacing)
            ) {
                // 可爱的菜单按钮（小星星图标）
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clickable { showActions = !showActions },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showActions) "✖️" else "⭐",
                        fontSize = iconFontSize,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (showActions) 0f else -15f
                        }
                    )
                }
                
                // 展开的操作按钮
                AnimatedVisibility(
                    visible = showActions,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(iconSpacing),
                        horizontalAlignment = Alignment.End
                    ) {
                        // 置顶按钮
                        Box(
                            modifier = Modifier
                                .size(iconSize * 0.9f)  // 操作按钮比菜单按钮小10%
                                .clickable { 
                                    onPin()
                                    showActions = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (anniversary.isPinned) "📌" else "📍",
                                fontSize = iconFontSize * 0.9f
                            )
                        }
                        
                        // 编辑按钮
                        Box(
                            modifier = Modifier
                                .size(iconSize * 0.9f)
                                .clickable { 
                                    onEdit()
                                    showActions = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✏️",
                                fontSize = iconFontSize * 0.9f
                            )
                        }
                        
                        // 删除按钮
                        Box(
                            modifier = Modifier
                                .size(iconSize * 0.9f)
                                .clickable { 
                                    onDelete()
                                    showActions = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🗑️",
                                fontSize = iconFontSize * 0.9f
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 详情对话框
    if (showDetailDialog) {
        AnniversaryDetailDialog(
            anniversary = anniversary,
            onDismiss = { showDetailDialog = false }
        )
    }
}
