// VipProfileScreen.kt - VIP个人主页界面
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.AvatarFrame
import com.example.funlife.data.model.ProfileBackground
import com.example.funlife.repository.ProfileRepository
import com.example.funlife.ui.components.*
import com.example.funlife.viewmodel.AuthViewModel
import com.example.funlife.viewmodel.VipProfileViewModel
import com.example.funlife.viewmodel.ScoreViewModel
import kotlinx.coroutines.delay
import android.net.Uri

@Composable
fun VipProfileScreen(
    authViewModel: AuthViewModel,
    scoreViewModel: ScoreViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as FunLifeApplication
    val currentSession = authViewModel.getCurrentSession()
    
    // 手动创建VipProfileViewModel
    val vipProfileViewModel: VipProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VipProfileViewModel(
                    profileRepository = ProfileRepository(
                        application.database.userAvatarDao(),
                        application.database.userDao(),
                        application.database.coinDao(),
                        application.database.dailyRewardDao()
                    ),
                    userId = currentSession?.userId ?: 0L,
                    context = context
                ) as T
            }
        }
    )
    
    val userAvatar by vipProfileViewModel.userAvatar.collectAsState()
    val userStatistics by vipProfileViewModel.userStatistics.collectAsState()
    val vipLevel by vipProfileViewModel.vipLevel.collectAsState()
    val allFrames by vipProfileViewModel.allFrames.collectAsState()
    val allBackgrounds by vipProfileViewModel.allBackgrounds.collectAsState()
    val message by vipProfileViewModel.message.collectAsState()
    
    // 获取金币数量
    val userCoins by remember {
        application.database.coinDao().getUserCoins(currentSession?.userId ?: 0L)
    }.collectAsState(initial = null)
    val currentCoins = userCoins?.coins ?: 0
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFrameSelector by remember { mutableStateOf(false) }
    var showBackgroundSelector by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    
    // 初始化默认数据（仅首次）
    LaunchedEffect(Unit) {
        if (allFrames.isEmpty()) {
            vipProfileViewModel.initializeDefaultData()
        }
        delay(100)
        visible = true
    }
    
    // 显示消息
    message?.let { msg ->
        LaunchedEffect(msg) {
            delay(2000)
            vipProfileViewModel.clearMessage()
        }
    }
    
    // 获取当前背景
    val currentBackground = allBackgrounds.find { it.id == userAvatar?.backgroundId }
    val currentFrame = allFrames.find { it.id == userAvatar?.frameId }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // VIP背景
        VipProfileBackground(
            vipLevel = vipLevel,
            background = currentBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                
                // 头像区域 - 完全重新设计
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 头像容器 - 使用wrapContentSize确保不裁剪
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(30.dp)  // 足够的padding确保边框动画不被裁剪
                        ) {
                            // 背景光晕层
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFE1BEE7).copy(alpha = 0.4f),
                                                Color(0xFFFCE4EC).copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            
                            // 头像和边框层
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(130.dp)
                            ) {
                                VipAvatarFrame(frame = currentFrame) {
                                    AvatarUploader(
                                        currentAvatarUri = userAvatar?.avatarUri,
                                        onAvatarSelected = { uri ->
                                            vipProfileViewModel.updateAvatarUri(uri)
                                        },
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                        }
                        
                        // 用户名
                        Text(
                            if (currentSession?.nickname?.isNotEmpty() == true) 
                                currentSession.nickname 
                            else 
                                currentSession?.username ?: "未登录",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        
                        // 等级和金币信息卡片
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // VIP等级
                                VipLevelBadge(vipLevel = vipLevel)
                                
                                // 分隔线
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFFE0E0E0),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                
                                // 金币
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💰", fontSize = 20.sp)
                                    Text(
                                        currentCoins.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // 功能网格卡片 - 改进设计
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(700, delayMillis = 200, easing = FastOutSlowInEasing)
                    )
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // 第一行：3个功能
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ImprovedIconFunctionItem(
                                    icon = "📦",
                                    label = "背包",
                                    bgColor = Color(0xFF9C27B0),
                                    onClick = { /* TODO */ }
                                )
                                ImprovedIconFunctionItem(
                                    icon = "🖼️",
                                    label = "头像框",
                                    bgColor = Color(0xFFFF6B9D),
                                    onClick = { showFrameSelector = true }
                                )
                                ImprovedIconFunctionItem(
                                    icon = "🎨",
                                    label = "背景",
                                    bgColor = Color(0xFF2196F3),
                                    onClick = { showBackgroundSelector = true }
                                )
                            }
                            
                            // 第二行：3个功能
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ImprovedIconFunctionItem(
                                    icon = "✅",
                                    label = "签到",
                                    bgColor = Color(0xFF4CAF50),
                                    onClick = {
                                        vipProfileViewModel.clearMessage()
                                    }
                                )
                                ImprovedIconFunctionItem(
                                    icon = "📊",
                                    label = "统计",
                                    bgColor = Color(0xFFFF9800),
                                    onClick = { /* TODO */ }
                                )
                                ImprovedIconFunctionItem(
                                    icon = "⚙️",
                                    label = "设置",
                                    bgColor = Color(0xFF607D8B),
                                    onClick = { /* TODO */ }
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // 退出登录按钮 - 改进设计
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700, delayMillis = 400))
                ) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE1BEE7).copy(alpha = 0.6f),
                                            Color(0xFFFCE4EC).copy(alpha = 0.6f)
                                        )
                                    ),
                                    RoundedCornerShape(27.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "退出登录",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(100.dp))
            }
        }
        
        // 消息提示
        message?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Color(0xFF2C3E50),
                contentColor = Color.White
            ) {
                Text(msg)
            }
        }
    }
    
    // 头像框选择器对话框
    if (showFrameSelector) {
        FrameSelectorDialog(
            frames = allFrames,
            currentFrameId = userAvatar?.frameId,
            vipLevel = vipLevel,
            userCoins = currentCoins,
            onDismiss = { showFrameSelector = false },
            onSelect = { frame ->
                vipProfileViewModel.updateFrame(frame.id)
                showFrameSelector = false
            },
            onPurchase = { frame ->
                vipProfileViewModel.purchaseFrame(frame)
            }
        )
    }
    
    // 背景选择器对话框
    if (showBackgroundSelector) {
        BackgroundSelectorDialog(
            backgrounds = allBackgrounds,
            currentBackgroundId = userAvatar?.backgroundId,
            vipLevel = vipLevel,
            userCoins = currentCoins,
            onDismiss = { showBackgroundSelector = false },
            onSelect = { background ->
                vipProfileViewModel.updateBackground(background.id)
                showBackgroundSelector = false
            },
            onPurchase = { background ->
                vipProfileViewModel.purchaseBackground(background)
            }
        )
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

/**
 * VIP用户信息卡片 - 美化版
 */
@Composable
fun VipUserInfoCard(
    username: String,
    nickname: String,
    vipLevel: Int,
    frame: AvatarFrame?,
    avatarUri: String?,
    onAvatarClick: (Uri?) -> Unit,
    onFrameClick: () -> Unit,
    onBackgroundClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBFE),
                            Color(0xFFFFF8FC),
                            Color.White
                        )
                    )
                )
        ) {
            // 装饰性背景元素
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE1BEE7).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
                    .size(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFCE4EC).copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像 + 头像框 + 上传功能
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // 头像光晕效果
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE1BEE7).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    
                    VipAvatarFrame(frame = frame) {
                        AvatarUploader(
                            currentAvatarUri = avatarUri,
                            onAvatarSelected = { uri -> onAvatarClick(uri) },
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // 用户名
                Text(
                    username,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(Modifier.height(12.dp))
                
                // VIP等级标识
                VipLevelBadge(vipLevel = vipLevel)
                
                if (nickname.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
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
                                nickname,
                                fontSize = 15.sp,
                                color = Color(0xFFFF6B35),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // 快捷按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BeautifulActionButton(
                        icon = Icons.Default.Face,
                        text = "头像框",
                        gradient = listOf(Color(0xFFFF6B9D), Color(0xFFFF8FB3)),
                        onClick = onFrameClick
                    )
                    BeautifulActionButton(
                        icon = Icons.Default.Image,
                        text = "背景",
                        gradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8)),
                        onClick = onBackgroundClick
                    )
                }
            }
        }
    }
}

/**
 * 美化的操作按钮 - 带渐变背景
 */
@Composable
fun BeautifulActionButton(
    icon: ImageVector,
    text: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(colors = gradient),
                    RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 小型操作按钮
 */
@Composable
fun SmallActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * VIP资源卡片（金币和背包）- 美化版
 */
@Composable
fun VipResourceCard(
    coins: Int,
    inventoryCount: Int,
    inventoryMax: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF9C4).copy(alpha = 0.3f),
                            Color(0xFFFFE082).copy(alpha = 0.3f),
                            Color(0xFFFFF9C4).copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            // 装饰性星星
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✨", fontSize = 12.sp, modifier = Modifier.graphicsLayer { alpha = 0.4f })
                Text("💫", fontSize = 14.sp, modifier = Modifier.graphicsLayer { alpha = 0.5f })
                Text("⭐", fontSize = 12.sp, modifier = Modifier.graphicsLayer { alpha = 0.4f })
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BeautifulResourceItem(
                    icon = "💰",
                    label = "金币",
                    value = coins.toString(),
                    gradient = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                )
                
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE0E0E0),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                BeautifulResourceItem(
                    icon = "🎒",
                    label = "背包",
                    value = if (inventoryMax == Int.MAX_VALUE) "$inventoryCount/∞" else "$inventoryCount/$inventoryMax",
                    gradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8))
                )
            }
        }
    }
}

@Composable
fun BeautifulResourceItem(
    icon: String,
    label: String,
    value: String,
    gradient: List<Color>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 图标背景
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.radialGradient(
                        colors = gradient.map { it.copy(alpha = 0.15f) }
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )
        }
        
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF757575),
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
    }
}

@Composable
fun ResourceItem(
    icon: String,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF95A5A6)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
    }
}

/**
 * VIP特权卡片 - 美化版
 */
@Composable
fun VipPrivilegesCard(vipLevel: Int) {
    val privileges = when (vipLevel) {
        1 -> listOf(
            "✓ 商品50金币起" to Color(0xFFFFD700),
            "✓ 每日20金币" to Color(0xFFFFD700)
        )
        2 -> listOf(
            "✓ 商品20金币起" to Color(0xFF00BCD4),
            "✓ 每日50金币" to Color(0xFF00BCD4),
            "✓ 背包1000个" to Color(0xFF00BCD4)
        )
        3 -> listOf(
            "✓ 商品1金币起" to Color(0xFFFF6B9D),
            "✓ 每日100金币" to Color(0xFFFF6B9D),
            "✓ 背包无限" to Color(0xFFFF6B9D),
            "✓ 优先使用新功能" to Color(0xFFFF6B9D),
            "✓ 专属客服" to Color(0xFFFF6B9D),
            "✓ 自定义背景" to Color(0xFFFF6B9D)
        )
        else -> emptyList()
    }
    
    val gradientColors = when (vipLevel) {
        1 -> listOf(Color(0xFFFFF9C4), Color(0xFFFFECB3))
        2 -> listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA))
        3 -> listOf(Color(0xFFF8BBD0), Color(0xFFF48FB1))
        else -> listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.3f) } + Color.White
                    )
                )
        ) {
            // 装饰性皇冠
            Text(
                text = "👑",
                fontSize = 60.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-10).dp)
                    .graphicsLayer {
                        alpha = 0.15f
                        rotationZ = 15f
                    }
            )
            
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B9D).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⭐", fontSize = 20.sp)
                    }
                    
                    Text(
                        "VIP特权",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE0E0E0),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                privileges.forEach { (privilege, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            privilege.removePrefix("✓ "),
                            fontSize = 16.sp,
                            color = Color(0xFF2C3E50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * VIP快捷操作卡片 - 美化版
 */
@Composable
fun VipQuickActionsCard(
    onFrameShopClick: () -> Unit,
    onBackgroundShopClick: () -> Unit,
    onBadgeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8EAF6).copy(alpha = 0.5f),
                            Color.White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF9C27B0).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚀", fontSize = 20.sp)
                    }
                    
                    Text(
                        "快捷入口",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BeautifulQuickActionItem(
                        icon = "🖼️",
                        label = "头像框商城",
                        gradient = listOf(Color(0xFFFF6B9D), Color(0xFFFF8FB3)),
                        onClick = onFrameShopClick
                    )
                    BeautifulQuickActionItem(
                        icon = "🎨",
                        label = "背景商城",
                        gradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8)),
                        onClick = onBackgroundShopClick
                    )
                    BeautifulQuickActionItem(
                        icon = "🏆",
                        label = "勋章墙",
                        gradient = listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
                        onClick = onBadgeClick
                    )
                }
            }
        }
    }
}

@Composable
fun BeautifulQuickActionItem(
    icon: String,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.radialGradient(
                        colors = gradient.map { it.copy(alpha = 0.2f) }
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 36.sp
            )
        }
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF616161),
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun QuickActionItem(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 36.sp
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF7F8C8D)
        )
    }
}

/**
 * VIP统计卡片 - 美化版
 */
@Composable
fun VipStatisticsCard(statistics: com.example.funlife.data.model.UserStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF3E0).copy(alpha = 0.4f),
                            Color.White
                        )
                    )
                )
        ) {
            // 装饰性图表图标
            Text(
                text = "📊",
                fontSize = 70.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 25.dp, y = 25.dp)
                    .graphicsLayer {
                        alpha = 0.1f
                        rotationZ = -15f
                    }
            )
            
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF9800).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📈", fontSize = 20.sp)
                    }
                    
                    Text(
                        "个人统计",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE0E0E0),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                BeautifulStatisticRow(
                    icon = "📅",
                    label = "注册天数",
                    value = "${statistics.registeredDays}天",
                    color = Color(0xFF4CAF50)
                )
                BeautifulStatisticRow(
                    icon = "✅",
                    label = "累计签到",
                    value = "${statistics.totalCheckIns}天",
                    color = Color(0xFF2196F3)
                )
                BeautifulStatisticRow(
                    icon = "💰",
                    label = "累计金币",
                    value = "${statistics.totalCoins}",
                    color = Color(0xFFFF9800)
                )
                BeautifulStatisticRow(
                    icon = "👑",
                    label = "VIP天数",
                    value = "${statistics.vipDays}天",
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
fun BeautifulStatisticRow(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp
                )
            }
            
            Text(
                label,
                fontSize = 16.sp,
                color = Color(0xFF616161),
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color = Color(0xFF7F8C8D)
        )
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2C3E50)
        )
    }
}

/**
 * VIP设置卡片 - 美化版
 */
@Composable
fun VipSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD).copy(alpha = 0.5f),
                            Color.White
                        )
                    )
                )
        ) {
            Column {
                BeautifulSettingsMenuItem(
                    icon = Icons.Outlined.AccountCircle,
                    title = "个人资料",
                    subtitle = "查看和编辑个人信息",
                    iconColor = Color(0xFF2196F3),
                    onClick = { /* TODO */ }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE0E0E0),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                BeautifulSettingsMenuItem(
                    icon = Icons.Outlined.Settings,
                    title = "应用设置",
                    subtitle = "主题、语言等",
                    iconColor = Color(0xFF9C27B0),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun BeautifulSettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            iconColor.copy(alpha = 0.15f),
                            iconColor.copy(alpha = 0.05f)
                        )
                    ),
                    RoundedCornerShape(14.dp)
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
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E)
            )
        }
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Color(0xFFF5F5F5),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color(0xFFFF6B35).copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(24.dp)
            )
        }
        
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
        
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDC3C7),
            modifier = Modifier.size(24.dp)
        )
    }
}


/**
 * 图标功能项 - 参考图片的圆角方形图标
 */
@Composable
fun IconFunctionItem(
    icon: String,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // 圆角方形背景 + 图标
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    bgColor.copy(alpha = 0.15f),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 36.sp
            )
        }
        
        // 标签
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF2C3E50),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 改进的图标功能项 - 更精致的设计
 * 带有渐变背景、阴影效果和点击动画
 */
@Composable
fun ImprovedIconFunctionItem(
    icon: String,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
            .padding(4.dp)
    ) {
        // 圆角方形背景 + 图标 + 渐变效果
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            shadowElevation = if (isPressed) 2.dp else 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                bgColor.copy(alpha = 0.2f),
                                bgColor.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 装饰性光晕
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                
                Text(
                    text = icon,
                    fontSize = 38.sp
                )
            }
        }
        
        // 标签
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF2C3E50),
            fontWeight = FontWeight.SemiBold
        )
    }
    
    // 重置按压状态
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

/**
 * VIP个人主页头部导航栏
 * 美化的渐变背景头部，显示用户基本信息
 */
@Composable
fun VipProfileHeader(
    username: String,
    nickname: String,
    vipLevel: Int,
    coins: Int
) {
    // 根据VIP等级选择渐变色
    val gradientColors = when (vipLevel) {
        1 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00))  // 金色渐变
        2 -> listOf(Color(0xFF00BCD4), Color(0xFF0097A7), Color(0xFF00838F))  // 青色渐变
        3 -> listOf(Color(0xFFFF6B9D), Color(0xFFFF8FB3), Color(0xFFFFA5C8))  // 粉色渐变
        else -> listOf(Color(0xFF9C27B0), Color(0xFFAB47BC), Color(0xFFBA68C8))  // 紫色渐变（普通用户）
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = gradientColors)
                )
        )
        
        // 装饰性圆圈
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-30).dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        // 内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的主页",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // 金币显示
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💰",
                            fontSize = 18.sp
                        )
                        Text(
                            text = coins.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // 底部：用户信息
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 用户名
                Text(
                    text = if (nickname.isNotEmpty()) nickname else username,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // VIP等级标签
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, text) = when (vipLevel) {
                        1 -> "⭐" to "普通VIP"
                        2 -> "💎" to "年费VIP"
                        3 -> "👑" to "终身VIP"
                        else -> "👤" to "普通用户"
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = icon,
                                fontSize = 14.sp
                            )
                            Text(
                                text = text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // 用户名（如果有昵称）
                    if (nickname.isNotEmpty()) {
                        Text(
                            text = "@$username",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 紧凑的用户信息卡片 - 包含头像、用户名、VIP等级、金币
 */
@Composable
fun CompactUserInfoCard(
    username: String,
    nickname: String,
    vipLevel: Int,
    frame: AvatarFrame?,
    avatarUri: String?,
    coins: Int,
    onAvatarClick: (Uri?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBFE),
                            Color.White
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    VipAvatarFrame(frame = frame) {
                        AvatarUploader(
                            currentAvatarUri = avatarUri,
                            onAvatarSelected = { uri -> onAvatarClick(uri) },
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }
                
                // 用户信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (nickname.isNotEmpty()) nickname else username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    VipLevelBadge(vipLevel = vipLevel)
                }
                
                // 金币显示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("💰", fontSize = 28.sp)
                    Text(
                        coins.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

/**
 * 2x2网格布局 - 资源和快捷操作
 */
@Composable
fun GridResourceAndActions(
    coins: Int,
    inventoryCount: Int,
    inventoryMax: Int,
    consecutiveDays: Int,
    onFrameShopClick: () -> Unit,
    onBackgroundShopClick: () -> Unit,
    onCheckInClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行：背包 + 签到
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GridItemCard(
                modifier = Modifier.weight(1f),
                icon = "🎒",
                title = "背包",
                value = if (inventoryMax == Int.MAX_VALUE) "$inventoryCount/∞" else "$inventoryCount/$inventoryMax",
                gradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8)),
                onClick = { /* TODO */ }
            )
            
            GridItemCard(
                modifier = Modifier.weight(1f),
                icon = "✅",
                title = "签到",
                value = "${consecutiveDays}天",
                gradient = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A)),
                onClick = onCheckInClick
            )
        }
        
        // 第二行：头像框商城 + 背景商城
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GridItemCard(
                modifier = Modifier.weight(1f),
                icon = "🖼️",
                title = "头像框",
                value = "商城",
                gradient = listOf(Color(0xFFFF6B9D), Color(0xFFFF8FB3)),
                onClick = onFrameShopClick
            )
            
            GridItemCard(
                modifier = Modifier.weight(1f),
                icon = "🎨",
                title = "背景",
                value = "商城",
                gradient = listOf(Color(0xFF2196F3), Color(0xFF42A5F5)),
                onClick = onBackgroundShopClick
            )
        }
    }
}

/**
 * 网格项卡片
 */
@Composable
fun GridItemCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    value: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradient.map { it.copy(alpha = 0.15f) } + Color.White
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = value,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradient[0]
                    )
                }
            }
        }
    }
}

/**
 * 紧凑的VIP特权卡片
 */
@Composable
fun CompactVipPrivilegesCard(vipLevel: Int) {
    val privileges = when (vipLevel) {
        1 -> listOf("商品50金币起", "每日20金币")
        2 -> listOf("商品20金币起", "每日50金币", "背包1000个")
        3 -> listOf("商品1金币起", "每日100金币", "背包无限", "优先新功能")
        else -> emptyList()
    }
    
    val gradientColors = when (vipLevel) {
        1 -> listOf(Color(0xFFFFF9C4), Color(0xFFFFECB3))
        2 -> listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA))
        3 -> listOf(Color(0xFFF8BBD0), Color(0xFFF48FB1))
        else -> listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.3f) } + Color.White
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👑", fontSize = 20.sp)
                    Text(
                        "VIP特权",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                // 使用FlowRow显示特权（紧凑布局）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    privileges.take(2).forEach { privilege ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = gradientColors[0].copy(alpha = 0.2f)
                        ) {
                            Text(
                                "✓ $privilege",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF2C3E50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                if (privileges.size > 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        privileges.drop(2).forEach { privilege ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = gradientColors[0].copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "✓ $privilege",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFF2C3E50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 紧凑的统计卡片
 */
@Composable
fun CompactStatisticsCard(statistics: com.example.funlife.data.model.UserStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF3E0).copy(alpha = 0.3f),
                            Color.White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📊", fontSize = 20.sp)
                    Text(
                        "个人统计",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                // 2x2网格显示统计
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactStatItem(
                            modifier = Modifier.weight(1f),
                            icon = "📅",
                            label = "注册",
                            value = "${statistics.registeredDays}天",
                            color = Color(0xFF4CAF50)
                        )
                        CompactStatItem(
                            modifier = Modifier.weight(1f),
                            icon = "✅",
                            label = "签到",
                            value = "${statistics.totalCheckIns}天",
                            color = Color(0xFF2196F3)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactStatItem(
                            modifier = Modifier.weight(1f),
                            icon = "💰",
                            label = "金币",
                            value = "${statistics.totalCoins}",
                            color = Color(0xFFFF9800)
                        )
                        CompactStatItem(
                            modifier = Modifier.weight(1f),
                            icon = "👑",
                            label = "VIP",
                            value = "${statistics.vipDays}天",
                            color = Color(0xFF9C27B0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactStatItem(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
                Text(
                    value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

/**
 * 紧凑的设置卡片
 */
@Composable
fun CompactSettingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD).copy(alpha = 0.4f),
                            Color.White
                        )
                    )
                )
        ) {
            Column {
                CompactSettingsMenuItem(
                    icon = Icons.Outlined.AccountCircle,
                    title = "个人资料",
                    iconColor = Color(0xFF2196F3),
                    onClick = { /* TODO */ }
                )
                
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFE0E0E0).copy(alpha = 0.5f)
                )
                
                CompactSettingsMenuItem(
                    icon = Icons.Outlined.Settings,
                    title = "应用设置",
                    iconColor = Color(0xFF9C27B0),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun CompactSettingsMenuItem(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    iconColor.copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2C3E50)
        )
        
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(20.dp)
        )
    }
}

