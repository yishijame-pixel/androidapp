// ProfileScreen.kt - 个人中心页面（精致美化版）
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val currentSession = authViewModel.getCurrentSession()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF5F2),
                        Color(0xFFFFFAF8),
                        Color.White
                    )
                )
            )
    ) {
        // 顶部大型装饰背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF6B35),
                            Color(0xFFFF8C61),
                            Color(0xFFFFA07A).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 动态装饰圆形 - 左上
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-60).dp, y = 40.dp)
                .scale(scale)
                .alpha(0.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        // 动态装饰圆形 - 右上
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = 80.dp)
                .rotate(rotate * 0.5f)
                .alpha(0.12f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB74D),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        // 小装饰点
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 40.dp, y = 200.dp)
                .rotate(-rotate * 0.3f)
                .alpha(0.08f)
                .background(
                    Color(0xFFFF8E53),
                    CircleShape
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(Modifier.height(50.dp))
            
            // 用户信息卡片 - 全新设计
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 卡片内部渐变背景
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B35).copy(alpha = 0.1f),
                                            Color(0xFFFF8E53).copy(alpha = 0.05f)
                                        )
                                    )
                                )
                        )
                        
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 头像 - 带光晕效果
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                // 外层光晕
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFF6B35).copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                
                                // 头像主体
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFFF6B35),
                                                    Color(0xFFFF8E53),
                                                    Color(0xFFFFA07A)
                                                )
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // 用户名
                            Text(
                                currentSession?.username ?: "未登录",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // 昵称标签
                            if (!currentSession?.nickname.isNullOrEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFF6B35).copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6B35),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            currentSession?.nickname ?: "",
                                            fontSize = 15.sp,
                                            color = Color(0xFFFF6B35),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // 功能列表 - 分组设计
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(600, delayMillis = 200)
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 账户设置组
                    Text(
                        "账户设置",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF95A5A6),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Column {
                            ProfileMenuItem(
                                icon = Icons.Outlined.AccountCircle,
                                title = "个人资料",
                                subtitle = "查看和编辑个人信息",
                                iconColor = Color(0xFFFF6B35),
                                onClick = { /* TODO */ },
                                showDivider = true
                            )
                            
                            ProfileMenuItem(
                                icon = Icons.Outlined.Notifications,
                                title = "通知设置",
                                subtitle = "管理通知偏好",
                                iconColor = Color(0xFFFF8E53),
                                onClick = { /* TODO */ },
                                showDivider = false
                            )
                        }
                    }
                    
                    // 应用设置组
                    Text(
                        "应用设置",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF95A5A6),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Column {
                            ProfileMenuItem(
                                icon = Icons.Outlined.Settings,
                                title = "偏好设置",
                                subtitle = "主题、语言等",
                                iconColor = Color(0xFFFFB74D),
                                onClick = { /* TODO */ },
                                showDivider = true
                            )
                            
                            ProfileMenuItem(
                                icon = Icons.Outlined.Info,
                                title = "关于应用",
                                subtitle = "版本 1.0.0",
                                iconColor = Color(0xFF9C27B0),
                                onClick = { /* TODO */ },
                                showDivider = false
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 退出登录按钮 - 全新设计
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFFEBEE),
                                            Color(0xFFFFF5F5)
                                        )
                                    ),
                                    RoundedCornerShape(30.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.ExitToApp,
                                    contentDescription = null,
                                    tint = Color(0xFFE74C3C),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "退出登录",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE74C3C)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
    
    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "退出登录",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            },
            text = {
                Text(
                    "确定要退出登录吗？",
                    color = Color(0xFF7F8C8D)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onLogout()
                    }
                ) {
                    Text(
                        "确定",
                        color = Color(0xFFE74C3C),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        "取消",
                        color = Color(0xFF95A5A6)
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图标容器 - 渐变背景
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                iconColor.copy(alpha = 0.15f),
                                iconColor.copy(alpha = 0.08f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            // 文字内容
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF95A5A6)
                )
            }
            
            // 箭头
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFBDC3C7),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 分隔线
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = Color(0xFFF0F0F0),
                thickness = 1.dp
            )
        }
    }
}
