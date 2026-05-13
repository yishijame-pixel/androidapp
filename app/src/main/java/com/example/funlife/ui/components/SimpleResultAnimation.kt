// SimpleResultAnimation.kt - 抖动卡片 + 明显粒子
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.SpinWheelMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SimpleResultAnimation(
    result: String,
    mode: SpinWheelMode,
    onDismiss: () -> Unit,
    panelSkin: String = "js_1"  // 添加皮肤参数
) {
    var visible by remember { mutableStateOf(true) }
    val overallAlpha = remember { Animatable(1f) }
    
    // 抖动效果 - 只抖0.3秒就停止
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // 快速抖动3次（每次0.1秒）
        launch {
            repeat(3) {
                shakeOffset.animateTo(8f, tween(50))
                shakeOffset.animateTo(-8f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))  // 回到中心，停止
        }
        
        // 展示2秒
        delay(2000)
        overallAlpha.animateTo(0f, tween(200))
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = overallAlpha.value },
            Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            )
            
            // 显示卡片（无粒子效果）
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = shakeOffset.value
                }
            ) {
                StaticCard(result = result, mode = mode, panelSkin = panelSkin)
            }
        }
    }
}

@Composable
fun StaticCard(
    result: String,
    mode: SpinWheelMode,
    panelSkin: String = "js_1"  // 默认使用 js_1
) {
    val context = LocalContext.current
    
    // 加载面板图片
    val panelBitmap = remember(panelSkin) {
        try {
            context.assets.open("login/$panelSkin.png").use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("StaticCard", "Failed to load panel image: ${e.message}")
            null
        }
    }
    
    val (bgColors, accentColor) = when (mode) {
        SpinWheelMode.NORMAL -> Pair(
            listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9)),
            Color(0xFF1976D2)
        )
        SpinWheelMode.ADVANCED -> Pair(
            listOf(Color(0xFFF3E5F5), Color(0xFFCE93D8)),
            Color(0xFF7B1FA2)
        )
        SpinWheelMode.LUCKY -> Pair(
            listOf(Color(0xFFFFF9C4), Color(0xFFFFEE58)),
            Color(0xFFF57F17)
        )
    }
    
    Card(
        modifier = Modifier
            .width(360.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 如果有图片，使用图片背景；否则使用渐变色
            if (panelBitmap != null) {
                Image(
                    bitmap = panelBitmap,
                    contentDescription = "结算面板",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = bgColors))
                        .padding(40.dp)
                )
            }
            
            // 只显示结果文字，叠加在红色框框位置（下方偏下）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 280.dp, bottom = 40.dp, start = 40.dp, end = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    result,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF5D4037),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.White.copy(alpha = 0.8f),
                            offset = Offset(2f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }
    }
}
