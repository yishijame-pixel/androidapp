// ProfileScreen.kt - 个人中心页面（温暖色调设计 - 复刻React版本）
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// 🎨 调色板 - 温暖色调
private object WarmPalette {
    val bg = Color(0xFFFEF3E8)
    val bgDeep = Color(0xFFFDE8D0)
    val white = Color(0xFFFFFFFF)
    val hero1 = Color(0xFFFF5222)
    val hero2 = Color(0xFFFF8C3A)
    val hero3 = Color(0xFFFFC55E)
    val coral = Color(0xFFFF5222)
    val amber = Color(0xFFF5A623)
    val rose = Color(0xFFE8505B)
    val sage = Color(0xFF6DAB8A)
    val ink = Color(0xFF1E0D06)
    val brown = Color(0xFF7A3D24)
    val muted = Color(0xFFB07D66)
    val border = Color(0x1FC86E46)  // rgba(200,110,70,0.12)
    val shadow = Color(0x19B4501E)  // rgba(180,80,30,0.1)
}

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as com.example.funlife.FunLifeApplication
    val currentSession = authViewModel.getCurrentSession()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // 🔥 创建VipProfileViewModel来管理头像
    val vipProfileViewModel: com.example.funlife.viewmodel.VipProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return com.example.funlife.viewmodel.VipProfileViewModel(
                    profileRepository = com.example.funlife.repository.ProfileRepository(
                        application.database.userAvatarDao(),
                        application.database.userDao(),
                        application.database.coinDao(),
                        application.database.dailyRewardDao(),
                        application.database
                    ),
                    userId = currentSession?.userId ?: 0L,
                    context = context
                ) as T
            }
        }
    )
    
    val userAvatar by vipProfileViewModel.userAvatar.collectAsState()
    
    // 🔥 获取VIP状态
    val vipRepository = remember {
        com.example.funlife.repository.VipRepository(
            application.database.userVipDao(),
            application.database.redeemCodeDao(),
            application.database.coinDao(),
            context,
            application.database
        )
    }
    val userVip by vipRepository.getUserVip(currentSession?.userId ?: 0L)
        .collectAsState(initial = null)
    val vipLevel = userVip?.getCurrentVipLevel() ?: com.example.funlife.data.model.VipLevel.NORMAL
    
    // 🔥 获取金币数量和积分
    val userCoins by remember {
        application.database.coinDao().getUserCoins(currentSession?.userId ?: 0L)
    }.collectAsState(initial = null)
    val currentCoins = userCoins?.coins ?: 0
    val currentShopPoints = userCoins?.shopPoints ?: 0
    
    // 入场动画
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    // 🎨 Hero 自定义背景 URI (从 SharedPreferences 读取，写入后立即刷新 UI)
    var heroBgUri by remember(currentSession?.userId) {
        mutableStateOf(
            currentSession?.userId?.let {
                com.example.funlife.utils.HeroBackgroundHelper.getHeroBackgroundUri(context, it)
            }
        )
    }
    val heroPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        val uid = currentSession?.userId ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val saved = com.example.funlife.utils.HeroBackgroundHelper
                .saveHeroBackground(context, uri, uid)
            if (saved != null) heroBgUri = saved
        }
    }
    var showHeroBgMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPalette.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ══════════════════════════════════════════════════════════
            // 🎨 HERO BANNER - 渐变顶部横幅（带波浪底部）
            // ══════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)  // 🔥 调整Banner高度到250dp
            ) {
                // 渐变背景 / 自定义图片
                if (heroBgUri != null) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(heroBgUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "自定义背景",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // 暗色蒙版让顶部文字更易读
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.30f),
                                        Color.Black.copy(alpha = 0.10f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        WarmPalette.hero1,
                                        WarmPalette.hero2,
                                        WarmPalette.hero3
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                    )
                }
                
                // 装饰圆形 - 右上
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .offset(x = (-50).dp, y = (-50).dp)
                        .align(Alignment.TopStart)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                
                // 装饰圆形 - 左上
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .offset(x = (-30).dp, y = 30.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                )
                
                // 装饰圆形 - 右下
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = (-60).dp, y = (-30).dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                )
                
                // 状态栏区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PROFILE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 🎨 编辑背景按钮
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    Color.White.copy(alpha = 0.22f),
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable { showHeroBgMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Wallpaper,
                                contentDescription = "更换背景",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            DropdownMenu(
                                expanded = showHeroBgMenu,
                                onDismissRequest = { showHeroBgMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📷  上传自定义背景") },
                                    onClick = {
                                        showHeroBgMenu = false
                                        heroPickerLauncher.launch("image/*")
                                    }
                                )
                                if (heroBgUri != null) {
                                    DropdownMenuItem(
                                        text = { Text("🔄  恢复默认渐变") },
                                        onClick = {
                                            showHeroBgMenu = false
                                            currentSession?.userId?.let { uid ->
                                                com.example.funlife.utils.HeroBackgroundHelper
                                                    .clearHeroBackground(context, uid)
                                                heroBgUri = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        com.example.funlife.ui.components.NotificationBellButton(
                            onClick = onNavigateToInbox,
                            variant = com.example.funlife.ui.components.NotificationBellVariant.Profile,
                            iconTint = Color.White,
                            bubbleBackground = Color.White.copy(alpha = 0.18f),
                        )
                    }
                }
                
                // 波浪底部（使用Canvas绘制）
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)  // 🔥 增加波浪高度：40dp → 60dp
                        .align(Alignment.BottomCenter)
                ) {
                    val path = Path().apply {
                        val width = size.width
                        val height = size.height
                        
                        // 起点
                        moveTo(0f, height * 0.5f)
                        
                        // 波浪曲线
                        cubicTo(
                            width * 0.2f, height * 1.25f,
                            width * 0.45f, 0f,
                            width * 0.65f, height * 0.625f
                        )
                        cubicTo(
                            width * 0.8f, height * 1.05f,
                            width * 0.9f, height * 0.375f,
                            width, height * 0.5f
                        )
                        
                        // 封闭路径
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        color = WarmPalette.bg
                    )
                }
                
                // 头像（重叠在波浪上）
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 52.dp)  // 🔥 往下移动（从42dp增加到52dp）
                ) {
                    // 🔥 获取装备的头像框（从商城购买的）
                    val userPreferencesDao = remember { application.database.userPreferencesDao() }
                    val userPrefs by userPreferencesDao.getPreferences(currentSession?.userId ?: 0L)
                        .collectAsState(initial = null)
                    val equippedAvatarFrame = userPrefs?.equippedAvatarFrame
                    
                    var showAvatarUploadDialog by remember { mutableStateOf(false) }
                    
                    if (equippedAvatarFrame != null) {
                        // ═══════════════════════════════════════════════════════
                        // 🎨 使用商城购买的头像框（完全替代橙色边框）
                        // ═══════════════════════════════════════════════════════
                        val avatarInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = avatarInteractionSource,
                                    indication = null  // 🔥 禁用点击涟漪阴影
                                ) { showAvatarUploadDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.funlife.ui.components.AvatarWithFrame(
                                avatarUri = userAvatar?.avatarUri,
                                frameAssetPath = equippedAvatarFrame,
                                frameSize = 150.dp,
                                defaultText = currentSession?.username?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                vipLevel = vipLevel  // 🔥 动态读取实际VIP等级
                            )
                            
                            // 编辑按钮 - 右下角
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-8).dp, y = (-8).dp)  // 🔥 更大的负偏移，紧贴头像框
                                    .background(WarmPalette.white, CircleShape)
                                    .border(2.dp, WarmPalette.hero2, CircleShape)
                                    .clickable { showAvatarUploadDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "编辑头像",
                                    tint = WarmPalette.coral,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // ═══════════════════════════════════════════════════════
                        // 🟠 使用默认的橙色圆形边框
                        // ═══════════════════════════════════════════════════════
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 橙色圆形边框（外层）
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(
                                        width = 4.dp,
                                        color = WarmPalette.hero2,
                                        shape = CircleShape
                                    )
                            )
                            
                            // 白色间隔环
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .border(
                                        width = 3.dp,
                                        color = WarmPalette.white,
                                        shape = CircleShape
                                    )
                            )
                            
                            // 头像主体
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .clickable { showAvatarUploadDialog = true }
                            ) {
                                if (userAvatar?.avatarUri != null) {
                                    // 显示用户上传的头像
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(userAvatar?.avatarUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "头像",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    // 默认头像
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        WarmPalette.hero3,
                                                        WarmPalette.hero2
                                                    )
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "🐱",
                                            fontSize = 44.sp
                                        )
                                    }
                                }
                            }
                            
                            // 编辑按钮
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-8).dp, y = (-8).dp)  // 🔥 更大的负偏移，紧贴头像框
                                    .background(WarmPalette.white, CircleShape)
                                    .border(2.dp, WarmPalette.hero2, CircleShape)
                                    .clickable { showAvatarUploadDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "编辑头像",
                                    tint = WarmPalette.coral,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // 头像上传对话框
                    if (showAvatarUploadDialog) {
                        com.example.funlife.ui.components.AvatarUploadDialog(
                            onDismiss = { showAvatarUploadDialog = false },
                            onAvatarSelected = { uri ->
                                vipProfileViewModel.updateAvatarUri(uri)
                                showAvatarUploadDialog = false
                            }
                        )
                    }
                }
            }
            
            // ══════════════════════════════════════════════════════════
            // 👤 用户名 + 标签
            // ══════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 用户名
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentSession?.username ?: "yishiya",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarmPalette.ink,
                        letterSpacing = (-0.5).sp
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = WarmPalette.muted,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { /* TODO: 编辑用户名 */ }
                    )
                }
                
                Spacer(Modifier.height(10.dp))
                
                // 徽章行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // VIP徽章
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = WarmPalette.white,
                        border = BorderStroke(1.5.dp, WarmPalette.border),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (vipLevel != com.example.funlife.data.model.VipLevel.NORMAL)
                                    Icons.Default.Diamond else Icons.Default.Star,
                                contentDescription = null,
                                tint = when (vipLevel) {
                                    com.example.funlife.data.model.VipLevel.VIP3 -> Color(0xFFFFD700)
                                    com.example.funlife.data.model.VipLevel.VIP2 -> Color(0xFF00D9FF)
                                    com.example.funlife.data.model.VipLevel.VIP1 -> Color(0xFFFFB800)
                                    else -> WarmPalette.coral
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                when (vipLevel) {
                                    com.example.funlife.data.model.VipLevel.VIP3 -> "终身VIP"
                                    com.example.funlife.data.model.VipLevel.VIP2 -> "VIP2"
                                    com.example.funlife.data.model.VipLevel.VIP1 -> "VIP1"
                                    else -> "普通用户"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (vipLevel) {
                                    com.example.funlife.data.model.VipLevel.VIP3 -> Color(0xFFDAA520)
                                    com.example.funlife.data.model.VipLevel.VIP2 -> Color(0xFF00AACC)
                                    com.example.funlife.data.model.VipLevel.VIP1 -> Color(0xFFCC8800)
                                    else -> WarmPalette.brown
                                }
                            )
                        }
                    }
                    
                    // 金币徽章
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF8EB),
                        border = BorderStroke(1.5.dp, Color(0x4DF5A623)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.clickable { /* TODO: 金币详情 */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Diamond,
                                contentDescription = null,
                                tint = WarmPalette.amber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                currentCoins.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92600A)
                            )
                        }
                    }
                    
                    // 积分徽章
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF0F0),
                        border = BorderStroke(1.5.dp, Color(0x4DFF6B6B)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⭐",
                                fontSize = 11.sp
                            )
                            Text(
                                "${currentShopPoints}积分",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFCC3333)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            // ══════════════════════════════════════════════════════════
            // 📋 功能菜单列表
            // ══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 280)) + 
                        slideInVertically(initialOffsetY = { it / 4 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuItemCard(
                        icon = Icons.Default.Inventory,
                        label = "背包",
                        subtitle = "道具与收藏",
                        color = WarmPalette.coral,
                        bgColor = Color(0xFFFFF4F1),
                        borderColor = Color(0x26FF5222),
                        tag = "NEW",
                        onClick = onNavigateToInventory,
                        delay = 0
                    )
                    MenuItemCard(
                        icon = Icons.Default.People,
                        label = "好友",
                        subtitle = "添加好友 · 管理备注",
                        color = Color(0xFF8B6CF7),
                        bgColor = Color(0xFFF7F4FF),
                        borderColor = Color(0x268B6CF7),
                        tag = "Beta",
                        onClick = onNavigateToFriends,
                        delay = 60
                    )
                    MenuItemCard(
                        icon = Icons.Default.Image,
                        label = "头像框",
                        subtitle = "外观个性定制",
                        color = WarmPalette.rose,
                        bgColor = Color(0xFFFFF2F3),
                        borderColor = Color(0x26E8505B),
                        tag = null,
                        onClick = { /* TODO */ },
                        delay = 120
                    )
                    MenuItemCard(
                        icon = Icons.Default.Wallpaper,
                        label = "背景",
                        subtitle = "主题皮肤管理",
                        color = WarmPalette.amber,
                        bgColor = Color(0xFFFFFBF0),
                        borderColor = Color(0x2EF5A623),
                        tag = null,
                        onClick = { /* TODO */ },
                        delay = 180
                    )
                    MenuItemCard(
                        icon = Icons.Default.CalendarMonth,
                        label = "签到",
                        subtitle = "每日领取奖励",
                        color = WarmPalette.sage,
                        bgColor = Color(0xFFF1FBF5),
                        borderColor = Color(0x2E6DAB8A),
                        tag = "今日",
                        onClick = { /* TODO */ },
                        delay = 240
                    )
                    MenuItemCard(
                        icon = Icons.Default.BarChart,
                        label = "统计",
                        subtitle = "习惯数据报告",
                        color = Color(0xFF8B6CF7),
                        bgColor = Color(0xFFF7F4FF),
                        borderColor = Color(0x268B6CF7),
                        tag = null,
                        onClick = { /* TODO */ },
                        delay = 300
                    )
                    MenuItemCard(
                        icon = Icons.Default.Settings,
                        label = "设置",
                        subtitle = "账户与偏好",
                        color = WarmPalette.muted,
                        bgColor = Color(0xFFFDF8F6),
                        borderColor = Color(0x1FB07D66),
                        tag = null,
                        onClick = onNavigateToSettings,
                        delay = 360
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // ══════════════════════════════════════════════════════════
            // 🚪 退出登录按钮
            // ══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 700))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showLogoutDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x0FE8505B),
                    border = BorderStroke(1.5.dp, Color(0x2EE8505B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            tint = WarmPalette.rose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "退出登录",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmPalette.rose
                        )
                    }
                }
            }
            
            // 底部导航栏空间（90dp Tab + 系统导航 + 30dp 呼吸）
            Spacer(Modifier.height(90.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 30.dp))
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
                    color = WarmPalette.ink
                )
            },
            text = {
                Text(
                    "确定要退出登录吗？",
                    color = WarmPalette.muted
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
                        color = WarmPalette.rose,
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
                        color = WarmPalette.muted
                    )
                }
            },
            containerColor = WarmPalette.white,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════
// 📊 统计卡片组件
// ══════════════════════════════════════════════════════════
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    delay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f)
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = bgColor,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(17.dp)
                    )
                }
                
                // 数值
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        value,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmPalette.ink,
                        lineHeight = 22.sp
                    )
                    Text(
                        unit,
                        fontSize = 11.sp,
                        color = WarmPalette.muted
                    )
                }
                
                // 标签
                Text(
                    label,
                    fontSize = 10.sp,
                    color = WarmPalette.muted
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// 📋 菜单项卡片组件
// ══════════════════════════════════════════════════════════
@Composable
fun MenuItemCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    tag: String?,
    onClick: () -> Unit,
    delay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = tween(400)
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = WarmPalette.white,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标气泡
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(bgColor, RoundedCornerShape(14.dp))
                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 文字内容
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmPalette.ink
                        )
                        if (tag != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = bgColor,
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Text(
                                    tag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = WarmPalette.muted
                    )
                }
                
                // 箭头
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0x59B46E50),  // rgba(180,110,80,0.35)
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
