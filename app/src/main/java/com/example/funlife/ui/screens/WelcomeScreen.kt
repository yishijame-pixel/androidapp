// WelcomeScreen.kt - 创意欢迎页
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onNavigateToHome()
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    
    // 背景装饰动画
    val rotate1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate1"
    )
    
    val rotate2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate2"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Logo 呼吸动画
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    
    // 小狗摇尾巴动画
    val dogTailRotate by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dogTail"
    )
    
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        contentVisible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF9C4),
                        Color(0xFFFFE082),
                        Color(0xFFFFCA28)
                    )
                )
            )
    ) {
        // 装饰圆点 - 漂浮效果
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 30.dp, y = 80.dp)
                .scale(scale)
                .alpha(0.15f)
                .background(
                    Color(0xFFFFB74D),
                    CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 150.dp)
                .rotate(rotate1)
                .alpha(0.12f)
                .background(
                    Color(0xFFFF8A65),
                    CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-120).dp)
                .rotate(rotate2)
                .alpha(0.1f)
                .background(
                    Color(0xFFFFD54F),
                    CircleShape
                )
        )
        
        // 小爪印装饰
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .offset(
                        x = (50 + index * 40).dp,
                        y = (200 + index * 30).dp
                    )
                    .rotate(rotate1 * 0.3f)
                    .alpha(0.08f)
                    .background(
                        Color(0xFFFF6B9D),
                        CircleShape
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))
            
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(1000)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 可爱的小狗 Logo
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(logoScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // 外圈光晕
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFB74D).copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        
                        // 主圆形背景
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFB74D),
                                            Color(0xFFFF8A65)
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // 使用 Emoji 小狗
                            Text(
                                "🐶",
                                fontSize = 70.sp,
                                modifier = Modifier.offset(y = (-5).dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // 应用名称 "一十"
                    Text(
                        "一十",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF424242),
                        letterSpacing = 4.sp
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // 副标题
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.6f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "让生活更有趣 🎉",
                            fontSize = 16.sp,
                            color = Color(0xFF424242),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.weight(0.8f))
            
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(1000, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(1000, delayMillis = 400)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 登录按钮
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B9D),
                                            Color(0xFFFF8A65)
                                        )
                                    ),
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "开始使用",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    // 注册按钮
                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.5.dp,
                            Color.White
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "创建账号",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF424242),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(60.dp))
        }
    }
}
