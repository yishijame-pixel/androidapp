// AnniversaryDetailDialog.kt - 超精美的纪念日详情对话框
package com.example.funlife.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.funlife.data.model.Anniversary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnniversaryDetailDialog(
    anniversary: Anniversary,
    onDismiss: () -> Unit
) {
    val daysRemaining = anniversary.getDaysRemaining()
    
    // 根据剩余天数选择渐变色
    val gradientColors = when {
        daysRemaining > 30 -> listOf(
            Color(0xFF667eea),
            Color(0xFF764ba2),
            Color(0xFFf093fb)
        )
        daysRemaining in 8..30 -> listOf(
            Color(0xFFfa709a),
            Color(0xFFfee140),
            Color(0xFFffa751)
        )
        daysRemaining >= 0 -> listOf(
            Color(0xFFff6b9d),
            Color(0xFFc06c84),
            Color(0xFFf67280)
        )
        else -> listOf(
            Color(0xFF8e9eab),
            Color(0xFFeef2f3),
            Color(0xFFbdc3c7)
        )
    }
    
    // 动画效果
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "alpha"
    )
    
    // 今天的特殊效果
    val isToday = daysRemaining == 0L
    val pulseScale by animateFloatAsState(
        targetValue = if (isToday) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // 旋转动画（装饰元素）
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 漂浮动画
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .graphicsLayer {
                        scaleX = scale * pulseScale
                        scaleY = scale * pulseScale
                        this.alpha = alpha
                    },
                shape = RoundedCornerShape(40.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 顶部装饰区域
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp)
                        ) {
                            // 背景图片或渐变
                            if (!anniversary.imageUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = Uri.parse(anniversary.imageUri),
                                    contentDescription = "背景图片",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // 多层渐变遮罩 - 更柔和
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.1f),
                                                    Color.Black.copy(alpha = 0.3f),
                                                    Color.Black.copy(alpha = 0.6f),
                                                    Color.Black.copy(alpha = 0.8f)
                                                )
                                            )
                                        )
                                )
                            } else {
                                // 精美渐变背景
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = gradientColors,
                                                start = Offset(0f, 0f),
                                                end = Offset(1000f, 1000f)
                                            )
                                        )
                                )
                            }
                            
                            // 装饰性圆圈（背景）
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotation * 0.5f)
                            ) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2
                                
                                // 绘制多个装饰圆圈
                                for (i in 0..5) {
                                    val angle = (i * 60f + rotation) * Math.PI / 180
                                    val radius = 150f + i * 30f
                                    val x = centerX + (radius * cos(angle)).toFloat()
                                    val y = centerY + (radius * sin(angle)).toFloat()
                                    
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.1f),
                                        radius = 40f + i * 10f,
                                        center = Offset(x, y)
                                    )
                                }
                            }
                            
                            // 主要内容
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 装饰性星星环绕
                                Box(
                                    modifier = Modifier.size(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 旋转的星星
                                    for (i in 0..7) {
                                        val angle = (i * 45f + rotation * 2f) * Math.PI / 180
                                        val radius = 80f
                                        val offsetX = (radius * cos(angle)).toFloat()
                                        val offsetY = (radius * sin(angle)).toFloat()
                                        
                                        Text(
                                            text = "✨",
                                            fontSize = 20.sp,
                                            modifier = Modifier
                                                .offset(x = offsetX.dp, y = offsetY.dp)
                                                .scale(0.8f + (sin(rotation * Math.PI / 180 + i) * 0.2f).toFloat())
                                        )
                                    }
                                    
                                    // 中心 Emoji - 超大号
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.95f),
                                        shadowElevation = 16.dp,
                                        modifier = Modifier
                                            .size(140.dp)
                                            .offset(y = floatOffset.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = anniversary.getTypeEnum().emoji,
                                                fontSize = 80.sp
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // 标题 - 带光晕效果
                                Text(
                                    text = anniversary.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 36.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .graphicsLayer {
                                            shadowElevation = 8f
                                        }
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // 重要程度星星 - 金色闪耀
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(anniversary.importance) { index ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .scale(1f + (sin((rotation + index * 45) * Math.PI / 180) * 0.15f).toFloat())
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 内容区域 - 精美卡片式设计
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 日期信息卡片 - 玻璃态设计
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = gradientColors[0].copy(alpha = 0.1f),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = gradientColors[0].copy(alpha = 0.2f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "📅",
                                                fontSize = 28.sp
                                            )
                                        }
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = anniversary.getFormattedDate(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = gradientColors[0]
                                        )
                                        
                                        if (anniversary.isYearly) {
                                            Text(
                                                text = "🔄 每年重复",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = gradientColors[1]
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 剩余天数 - 超大号展示卡片
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = Color.White,
                                shadowElevation = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(gradientColors),
                                        shape = RoundedCornerShape(32.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val daysText = when {
                                        daysRemaining > 0 -> "距离还有"
                                        daysRemaining == 0L -> "🎉"
                                        else -> "已经过去"
                                    }
                                    
                                    Text(
                                        text = daysText,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 20.sp
                                    )
                                    
                                    // 超大数字
                                    if (daysRemaining == 0L) {
                                        Text(
                                            text = "就是今天！",
                                            style = MaterialTheme.typography.displayLarge,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 48.sp,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${kotlin.math.abs(daysRemaining)}",
                                                style = MaterialTheme.typography.displayLarge,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 96.sp,
                                                color = Color.White,
                                                lineHeight = 96.sp
                                            )
                                            Text(
                                                text = "天",
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 40.sp,
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                        }
                                    }
                                    
                                    // 装饰性小爱心
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        repeat(5) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 已过年数卡片
                            val yearsPassed = anniversary.getYearsPassed()
                            if (anniversary.isYearly && yearsPassed > 0) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFFFFE5E5),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFFCCCC),
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "💝",
                                                    fontSize = 28.sp
                                                )
                                            }
                                        }
                                        
                                        Column {
                                            Text(
                                                text = "已经 $yearsPassed 年了",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE91E63)
                                            )
                                            Text(
                                                text = "时光飞逝，珍惜当下",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFE91E63).copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 进度条 - 精美设计
                            if (anniversary.isYearly && daysRemaining >= 0) {
                                val progress = 1f - (daysRemaining.toFloat() / 365f)
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "年度进度",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = gradientColors[0]
                                        )
                                        
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = gradientColors[0]
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(20.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(gradientColors[0].copy(alpha = 0.1f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progress)
                                                .background(
                                                    brush = Brush.horizontalGradient(gradientColors),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                        )
                                    }
                                }
                            }
                            
                            // 备注卡片 - 精美设计
                            if (!anniversary.note.isNullOrEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFFFFF9E6),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📝",
                                                fontSize = 24.sp
                                            )
                                            Text(
                                                text = "备注",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9800)
                                            )
                                        }
                                        Text(
                                            text = anniversary.note,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF5D4037),
                                            lineHeight = 28.sp
                                        )
                                    }
                                }
                            }
                            
                            // 底部装饰
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) {
                                    Text(
                                        text = "✨",
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                                Text(
                                    text = "FunLife",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = gradientColors[0].copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                repeat(3) {
                                    Text(
                                        text = "✨",
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                    
                    // 关闭按钮 - 精美设计
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = gradientColors[0],
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
