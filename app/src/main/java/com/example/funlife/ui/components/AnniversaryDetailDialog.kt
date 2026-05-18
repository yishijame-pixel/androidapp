// AnniversaryDetailDialog.kt - 沉浸式纪念日详情页（动画增强版）
package com.example.funlife.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AnniversaryDetailDialog(
    anniversary: Anniversary,
    onDismiss: () -> Unit
) {
    val daysRemaining = anniversary.getDaysRemaining()
    val isToday = daysRemaining == 0L
    val isPast = daysRemaining < 0
    
    // ═══════════════════════════════════════════════════════
    // 实时倒计时（精确到秒）
    // ═══════════════════════════════════════════════════════
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = LocalDateTime.now()
        }
    }
    
    val targetDate = remember(anniversary) {
        try {
            LocalDate.parse(anniversary.date.toString())
        } catch (e: Exception) { null }
    }
    
    val totalSecondsRemaining = remember(now, targetDate) {
        if (targetDate != null && !isPast && !isToday) {
            ChronoUnit.SECONDS.between(now, targetDate.atStartOfDay())
                .coerceAtLeast(0)
        } else 0L
    }
    
    val countdownDays = totalSecondsRemaining / 86400
    val countdownHours = (totalSecondsRemaining % 86400) / 3600
    val countdownMinutes = (totalSecondsRemaining % 3600) / 60
    val countdownSeconds = totalSecondsRemaining % 60
    
    // ═══════════════════════════════════════════════════════
    // 入场动画
    // ═══════════════════════════════════════════════════════
    var enterVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        enterVisible = true
    }
    
    // ═══════════════════════════════════════════════════════
    // 持续动画
    // ═══════════════════════════════════════════════════════
    val infiniteTransition = rememberInfiniteTransition(label = "detail")
    
    // 照片微呼吸（缓慢放大缩小）
    val photoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "photoScale"
    )
    
    // 光环旋转
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "ringRotation"
    )
    
    // 光环呼吸
    val ringGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringGlow"
    )
    
    // 浮动粒子数据
    val particles = remember {
        val symbols = listOf("♥", "♡", "✦", "✧", "·", "✿", "❋", "⋆")
        List(10) {
            AnnivParticle(
                symbol = symbols[it % symbols.size],
                xRatio = Random.nextFloat(),
                startDelay = Random.nextFloat(),
                speed = Random.nextFloat() * 1.5f + 0.8f,
                size = Random.nextInt(10, 26)
            )
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ═══════════════════════════════════════════════════════
            // 背景层
            // ═══════════════════════════════════════════════════════
            if (!anniversary.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = Uri.parse(anniversary.imageUri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = photoScale * 1.1f; scaleY = photoScale * 1.1f }
                        .blur(30.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            } else {
                // 动态渐变色背景
                val gradientShift by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(10000, easing = LinearEasing)
                    ), label = "gradShift"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val colors = listOf(
                                Color(0xFF667eea), Color(0xFF764ba2),
                                Color(0xFFf093fb), Color(0xFF667eea)
                            )
                            val shift = gradientShift * size.height
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = colors,
                                    startY = -shift,
                                    endY = size.height * 2 - shift
                                )
                            )
                        }
                )
            }
            
            // ═══════════════════════════════════════════════════════
            // 浮动粒子（多样化）
            // ═══════════════════════════════════════════════════════
            particles.forEachIndexed { index, p ->
                val animY by infiniteTransition.animateFloat(
                    initialValue = 1.1f + p.startDelay * 0.4f,
                    targetValue = -0.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween((9000 / p.speed).toInt(), easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pY_$index"
                )
                val animSway by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 6.28f,
                    animationSpec = infiniteRepeatable(
                        animation = tween((4000 / p.speed).toInt(), easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pSway_$index"
                )
                val particleAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pAlpha_$index"
                )
                Text(
                    text = p.symbol,
                    color = Color.White.copy(alpha = (0.08f + (index % 4) * 0.06f) * (0.5f + particleAlpha * 0.5f)),
                    fontSize = p.size.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = p.xRatio * size.width + sin(animSway.toDouble()).toFloat() * 40f
                            translationY = animY * size.height
                            rotationZ = animSway * 30f
                        }
                )
            }
            
            // ═══════════════════════════════════════════════════════
            // 主内容（带入场动画）
            // ═══════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 顶部操作栏
                AnimatedVisibility(
                    visible = enterVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -it }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, "编辑", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                
                // 照片区域（带微缩放呼吸动画）
                AnimatedVisibility(
                    visible = enterVisible,
                    enter = fadeIn(tween(800, delayMillis = 200)) + scaleIn(tween(800, delayMillis = 200), initialScale = 0.9f)
                ) {
                    if (!anniversary.imageUri.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(280.dp)
                                .clip(RoundedCornerShape(24.dp))
                        ) {
                            AsyncImage(
                                model = Uri.parse(anniversary.imageUri),
                                contentDescription = "纪念照片",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = photoScale
                                        scaleY = photoScale
                                    },
                                contentScale = ContentScale.Crop
                            )
                            // 底部渐变
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                        )
                                    )
                            )
                            // 照片上叠加纪念日名称
                            Text(
                                text = anniversary.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        offset = Offset(1f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(if (!anniversary.imageUri.isNullOrEmpty()) 24.dp else 60.dp))
                
                // ═══════════════════════════════════════════════════════
                // 倒计时卡片（核心区域）
                // ═══════════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = enterVisible,
                    enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(tween(800, delayMillis = 400)) { it / 3 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        // 如果没有照片，在这里显示名称
                        if (anniversary.imageUri.isNullOrEmpty()) {
                            Text(
                                text = anniversary.name,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        offset = Offset(1f, 2f), blurRadius = 4f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // 日期行
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = anniversary.getFormattedDate(),
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // ─────── 分隔装饰线 ───────
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Box(modifier = Modifier.weight(1f).height(0.5.dp)
                                .background(Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.35f))
                                )))
                            val heartPulse by infiniteTransition.animateFloat(
                                initialValue = 0.8f, targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "heartPulse"
                            )
                            Text(
                                "  ♥  ",
                                color = Color(0xFFFF6B9D).copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = heartPulse; scaleY = heartPulse
                                }
                            )
                            Box(modifier = Modifier.weight(1f).height(0.5.dp)
                                .background(Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                                )))
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // ═══════════════════════════════════════════════════════
                        // 大数字倒计时 + 旋转光环
                        // ═══════════════════════════════════════════════════════
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 旋转彩色光环
                            Canvas(
                                modifier = Modifier
                                    .size(200.dp)
                                    .graphicsLayer { alpha = ringGlow }
                            ) {
                                val strokeWidth = 3.dp.toPx()
                                // 外环
                                rotate(ringRotation) {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFFF6B9D),
                                                Color(0xFFFFD700),
                                                Color(0xFF667eea),
                                                Color(0xFFf093fb),
                                                Color(0xFFFF6B9D)
                                            )
                                        ),
                                        startAngle = 0f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                // 内环（反向旋转）
                                rotate(-ringRotation * 0.7f) {
                                    val innerPad = 12.dp.toPx()
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.2f),
                                                Color.White.copy(alpha = 0.5f),
                                                Color.White.copy(alpha = 0.1f),
                                                Color.White.copy(alpha = 0.3f),
                                            )
                                        ),
                                        startAngle = 0f,
                                        sweepAngle = 200f,
                                        useCenter = false,
                                        topLeft = Offset(innerPad, innerPad),
                                        size = androidx.compose.ui.geometry.Size(
                                            size.width - innerPad * 2, size.height - innerPad * 2
                                        ),
                                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                            
                            // 中心数字
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                when {
                                    isToday -> {
                                        Text("🎉", fontSize = 42.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("今天！", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700))
                                    }
                                    isPast -> {
                                        Text(
                                            text = "${-daysRemaining}",
                                            fontSize = 52.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = Color.Black.copy(alpha = 0.2f),
                                                    offset = Offset(2f, 3f), blurRadius = 6f
                                                )
                                            )
                                        )
                                        Text("天", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f),
                                            letterSpacing = 2.sp)
                                    }
                                    else -> {
                                        Text(
                                            text = "$countdownDays",
                                            fontSize = 52.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = Color.Black.copy(alpha = 0.2f),
                                                    offset = Offset(2f, 3f), blurRadius = 6f
                                                )
                                            )
                                        )
                                        Text("天", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f),
                                            letterSpacing = 2.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // ═══════════════════════════════════════════════════════
                        // 精确倒计时条（时:分:秒）
                        // ═══════════════════════════════════════════════════════
                        if (!isToday) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isPast) {
                                    CountdownUnit(value = countdownHours, label = "时")
                                    CountdownSeparator(infiniteTransition)
                                    CountdownUnit(value = countdownMinutes, label = "分")
                                    CountdownSeparator(infiniteTransition)
                                    CountdownUnit(value = countdownSeconds, label = "秒")
                                } else {
                                    Text(
                                        "已过去的珍贵记忆",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 情感文案
                            val emotionText = when {
                                isPast -> "每一天都值得被铭记 ✨"
                                daysRemaining <= 3 -> "就快到了，好期待！💕"
                                daysRemaining <= 7 -> "最后一周倒计时 ⏰"
                                daysRemaining <= 30 -> "期待与你相见 🌸"
                                else -> "每一天都在靠近你 💫"
                            }
                            Text(
                                text = emotionText,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.55f),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // ═══════════════════════════════════════════════════════
                // 操作按钮（入场动画）
                // ═══════════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = enterVisible,
                    enter = fadeIn(tween(600, delayMillis = 700)) + slideInVertically(tween(600, delayMillis = 700)) { it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("添加回忆", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.88f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Share, null, tint = Color(0xFF764ba2), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("分享纪念", color = Color(0xFF764ba2), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 倒计时子组件
// ═══════════════════════════════════════════════════════
private data class AnnivParticle(
    val symbol: String,
    val xRatio: Float,
    val startDelay: Float,
    val speed: Float,
    val size: Int
)

@Composable
private fun CountdownUnit(value: Long, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun CountdownSeparator(transition: InfiniteTransition) {
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "separator"
    )
    Text(
        ":",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = alpha),
        modifier = Modifier.padding(bottom = 16.dp)
    )
}
