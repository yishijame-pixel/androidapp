// PurchaseSuccessDialog.kt - 购买成功动画对话框
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun PurchaseSuccessDialog(
    frameName: String,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var checkVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        delay(200)
        checkVisible = true
        delay(1500)
        onDismiss()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.7f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
            ),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 32.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 成功图标动画
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 外圈光晕
                            val infiniteTransition = rememberInfiniteTransition(label = "glow")
                            val glowScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.3f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "glowScale"
                            )
                            
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .scale(glowScale)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF4CAF50).copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            
                            // 绿色圆圈
                            Box(
                                Modifier
                                    .size(80.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF4CAF50),
                                                Color(0xFF66BB6A)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // 对勾动画
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = checkVisible,
                                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                                        initialScale = 0.3f,
                                        animationSpec = spring(
                                            dampingRatio = 0.5f,
                                            stiffness = 200f
                                        )
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                        
                        // 成功文字
                        Text(
                            "购买成功！",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E0D06)
                        )
                        
                        // 商品名称
                        Text(
                            frameName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7A3D24)
                        )
                        
                        // 提示文字
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "已放入背包",
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
