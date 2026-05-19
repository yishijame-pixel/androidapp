// HomeScreen.kt - 美观的首页
package com.example.funlife.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.UserPreferences
import com.example.funlife.viewmodel.AnniversaryViewModel
import com.example.funlife.viewmodel.ScoreViewModel
import com.example.funlife.viewmodel.GoalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Size
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun HomeScreen(
    navController: NavController,
    anniversaryViewModel: AnniversaryViewModel,
    scoreViewModel: ScoreViewModel,
    authViewModel: com.example.funlife.viewmodel.AuthViewModel,
    goalViewModel: GoalViewModel
) {
    val anniversaries by anniversaryViewModel.anniversaries.collectAsState()
    val pinnedAnniversary by anniversaryViewModel.pinnedAnniversary.collectAsState()
    val players by scoreViewModel.players.collectAsState()
    val userSession = authViewModel.getCurrentSession()
    val countdowns by goalViewModel.countdowns.collectAsState()
    
    // 获取VIP状态
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { (context.applicationContext as com.example.funlife.FunLifeApplication).database }
    val vipRepository = remember {
        com.example.funlife.repository.VipRepository(
            database.userVipDao(),
            database.redeemCodeDao(),
            database.coinDao(),
            context
        )
    }
    val userVip by vipRepository.getUserVip(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    val vipLevel = userVip?.getCurrentVipLevel() ?: com.example.funlife.data.model.VipLevel.NORMAL
    
    // 获取用户头像
    val userAvatarDao = remember { database.userAvatarDao() }
    val userAvatar by userAvatarDao.getUserAvatar(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    
    // VIP首次进入特效状态
    var showFirstEntryEffect by remember { mutableStateOf(false) }
    
    // 检测VIP激活（从VIP页面返回）
    LaunchedEffect(vipLevel, userSession?.userId) {
        val uid = userSession?.userId ?: return@LaunchedEffect
        if (vipLevel != com.example.funlife.data.model.VipLevel.NORMAL) {
            // 🔒 安全修复：VIP 激活动画标志按 userId 隔离，避免 A 账号刚激活 VIP 后 B 账号登录也被弹动画
            val prefs = context.getSharedPreferences("vip_animation", android.content.Context.MODE_PRIVATE)
            val key = "show_first_entry_effect_$uid"
            val shouldShowAnimation = prefs.getBoolean(key, false)

            if (shouldShowAnimation) {
                showFirstEntryEffect = true
                prefs.edit().putBoolean(key, false).apply()
            }
        }
    }
    
    // 获取用户偏好设置
    val userPreferencesRepository = remember {
        com.example.funlife.repository.UserPreferencesRepository(
            (context.applicationContext as com.example.funlife.FunLifeApplication).database.userPreferencesDao()
        )
    }
    val userPreferences by userPreferencesRepository.getPreferences(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    
    val scope = rememberCoroutineScope()
    
    // 动画状态
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE1BEE7),  // 顶部浅紫色
                            Color(0xFFF3E5F5),  // 中间更浅的紫色
                            Color(0xFFFCE4EC),  // 底部粉色
                            Color(0xFFFFF0F5)   // 最底部浅粉色
                        )
                    )
                ),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
        // 置顶纪念日展示区域
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically()
            ) {
                if (pinnedAnniversary != null) {
                    PinnedAnniversaryHeader(
                        anniversary = pinnedAnniversary!!,
                        onUnpin = { anniversaryViewModel.unpinAnniversary(pinnedAnniversary!!) },
                        onClick = { navController.navigate("anniversary") },
                        userSession = userSession,  // 🔥 传递用户会话
                        avatarUri = userAvatar?.avatarUri,  // 🔥 传递头像URI
                        vipLevel = vipLevel  // 🔥 传递VIP等级
                    )
                } else {
                    WelcomeHeader(
                        userSession = userSession,
                        avatarUri = userAvatar?.avatarUri,
                        vipLevel = vipLevel,
                        showFirstEntryEffect = showFirstEntryEffect,
                        onFirstEntryComplete = {
                            showFirstEntryEffect = false
                        },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(com.example.funlife.navigation.Screen.Welcome.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
        
        // 装饰性波浪元素
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically()
            ) {
                DecorativeWaves(
                    userPreferences = userPreferences,
                    onTextEdit = { newText ->
                        scope.launch {
                            userSession?.userId?.let { userId ->
                                userPreferencesRepository.updateHomePanelText(userId, newText)
                            }
                        }
                    },
                    onStyleChange = { newStyle ->
                        scope.launch {
                            userSession?.userId?.let { userId ->
                                userPreferencesRepository.updateHomePanelTextStyle(userId, newStyle)
                            }
                        }
                    }
                )
            }
        }
        
        // 功能卡片网格
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically()
            ) {
                FunctionCardsSection(navController)
            }
        }
        
        // 目标小组件
        if (countdowns.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically()
                ) {
                    GoalWidgetSection(
                        countdowns = countdowns.take(3),
                        onViewAll = { navController.navigate("goal") }
                    )
                }
            }
        }
        
        // 最近纪念日预览
        if (anniversaries.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically()
                ) {
                    RecentAnniversariesSection(
                        anniversaries = anniversaries.take(3),
                        onViewAll = { navController.navigate("anniversary") }
                    )
                }
            }
        }
        
        // 游戏排行榜预览
        if (players.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically()
                ) {
                    LeaderboardSection(
                        players = players.take(3),
                        onViewAll = { navController.navigate("score_counter") }
                    )
                }
            }
        }
    }
    }
}

@Composable
fun PinnedAnniversaryHeader(
    anniversary: Anniversary,
    onUnpin: () -> Unit,
    onClick: () -> Unit,
    userSession: com.example.funlife.data.model.UserSession? = null,  // 🔥 新增
    avatarUri: String? = null,  // 🔥 新增
    vipLevel: com.example.funlife.data.model.VipLevel = com.example.funlife.data.model.VipLevel.NORMAL  // 🔥 新增
) {
    val daysRemaining = anniversary.getDaysRemaining()
    val isToday = daysRemaining == 0L
    val isPast = daysRemaining < 0
    
    // 🔥 获取装备的头像框
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { (context.applicationContext as com.example.funlife.FunLifeApplication).database }
    val userPreferencesDao = remember { database.userPreferencesDao() }
    val userPrefs by userPreferencesDao.getPreferences(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    val equippedAvatarFrame = userPrefs?.equippedAvatarFrame
    
    // 🔥 旋转时钟动画（秒针60秒转一圈，分针缓慢旋转）
    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val secondHandRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondHand"
    )
    val minuteHandRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "minuteHand"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 主内容区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // 背景图片或渐变
            if (anniversary.imageUri != null) {
                AsyncImage(
                    model = anniversary.imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 半透明遮罩 - 底部加深以保证文字可读
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            } else {
                // 默认渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B9D),
                                    Color(0xFFFF8FB3),
                                    Color(0xFFFFA5C8)
                                )
                            )
                        )
                )
            }
            
            // 装饰圆圈
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-30).dp)
                    .size(150.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // 取消置顶按钮
            IconButton(
                onClick = onUnpin,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "取消置顶",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // ═══════════════════════════════════════════════════════
            // 🎨 底部信息区域（无背景，直接叠加在渐变上）
            // ═══════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 第一行：头像 + 纪念日信息 + 旋转时钟
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 🔥 左侧：用户头像+头像框
                        com.example.funlife.ui.components.AvatarWithFrame(
                            avatarUri = avatarUri,
                            frameAssetPath = equippedAvatarFrame,
                            frameSize = if (equippedAvatarFrame != null) 72.dp else 54.dp,
                            defaultText = userSession?.nickname?.firstOrNull()?.toString()?.uppercase() ?: "U",
                            vipLevel = vipLevel
                        )
                        
                        // 中间：纪念日名称 + 日期
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = anniversary.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 22.sp,
                                maxLines = 1
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = anniversary.getFormattedDate(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        
                        // 🔥 右侧：旋转时钟
                        Canvas(modifier = Modifier.size(46.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2 - 3.dp.toPx()
                            
                            // 表盘外圈
                            drawCircle(
                                color = Color.White.copy(alpha = 0.5f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1.5f.dp.toPx())
                            )
                            
                            // 12个刻度
                            for (i in 0..11) {
                                val angle = Math.toRadians((i * 30.0) - 90.0)
                                val isMain = i % 3 == 0
                                val startR = radius - if (isMain) 6.dp.toPx() else 3.dp.toPx()
                                val endR = radius - 1.dp.toPx()
                                drawLine(
                                    color = Color.White.copy(alpha = if (isMain) 0.9f else 0.4f),
                                    start = Offset(
                                        center.x + startR * cos(angle).toFloat(),
                                        center.y + startR * sin(angle).toFloat()
                                    ),
                                    end = Offset(
                                        center.x + endR * cos(angle).toFloat(),
                                        center.y + endR * sin(angle).toFloat()
                                    ),
                                    strokeWidth = if (isMain) 1.5f.dp.toPx() else 0.8f.dp.toPx()
                                )
                            }
                            
                            // 分针
                            val minuteAngle = Math.toRadians((minuteHandRotation - 90.0).toDouble())
                            val minuteLen = radius * 0.5f
                            drawLine(
                                color = Color.White.copy(alpha = 0.95f),
                                start = center,
                                end = Offset(
                                    center.x + minuteLen * cos(minuteAngle).toFloat(),
                                    center.y + minuteLen * sin(minuteAngle).toFloat()
                                ),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            
                            // 秒针（粉色，60秒一圈）
                            val secondAngle = Math.toRadians((secondHandRotation - 90.0).toDouble())
                            val secondLen = radius * 0.72f
                            drawLine(
                                color = Color(0xFFFF6B9D),
                                start = Offset(
                                    center.x - 3.dp.toPx() * cos(secondAngle).toFloat(),
                                    center.y - 3.dp.toPx() * sin(secondAngle).toFloat()
                                ),
                                end = Offset(
                                    center.x + secondLen * cos(secondAngle).toFloat(),
                                    center.y + secondLen * sin(secondAngle).toFloat()
                                ),
                                strokeWidth = 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            
                            // 中心圆点
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)
                            drawCircle(color = Color(0xFFFF6B9D), radius = 1.2f.dp.toPx(), center = center)
                        }
                    }
                    
                    // 第二行：倒计时胶囊徽章（居中，白色半透明获形）
                    val (countdownIcon, countdownText, countdownColor) = when {
                        isToday -> Triple("🎉", "就是今天！", Color(0xFFFF6B35))
                        isPast -> Triple("📅", "已过去 ${-daysRemaining} 天", Color(0xFF95A5A6))
                        else -> Triple("⏰", "还有 $daysRemaining 天", Color(0xFF4ECDC4))
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.2f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = countdownIcon, fontSize = 18.sp)
                                Text(
                                    text = countdownText,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                if (!isToday && !isPast) {
                                    Text(
                                        text = "·",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "期待与你相见",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 底部柔和过渡区域 - 渐变遮罩（匹配首页背景色）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .offset(y = 240.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFF3E5F5).copy(alpha = 0.4f),
                            Color(0xFFFCE4EC).copy(alpha = 0.7f),
                            Color(0xFFFFF0F5)
                        )
                    )
                )
        )
        
        // 装饰性波浪过渡
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 260.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                val waveWidth = size.width
                val waveHeight = size.height
                val wavePath = androidx.compose.ui.graphics.Path()
                
                wavePath.moveTo(0f, waveHeight * 0.3f)
                
                val waveCount = 5
                val waveLength = waveWidth / waveCount
                
                for (i in 0..waveCount) {
                    val x = i * waveLength
                    wavePath.cubicTo(
                        x + waveLength * 0.25f, waveHeight * 0.1f,
                        x + waveLength * 0.75f, waveHeight * 0.5f,
                        x + waveLength, waveHeight * 0.3f
                    )
                }
                
                wavePath.lineTo(waveWidth, waveHeight)
                wavePath.lineTo(0f, waveHeight)
                wavePath.close()
                
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFFFCE4EC).copy(alpha = 0.6f),
                            androidx.compose.ui.graphics.Color(0xFFFFF0F5)
                        )
                    )
                )
            }
            
            // 装饰小星星
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .offset(y = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("✨", "💫", "⭐", "✨", "💫").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 12.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = 0.5f
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeHeader(
    userSession: com.example.funlife.data.model.UserSession?,
    avatarUri: String? = null,
    vipLevel: com.example.funlife.data.model.VipLevel = com.example.funlife.data.model.VipLevel.NORMAL,
    showFirstEntryEffect: Boolean = false,
    onFirstEntryComplete: () -> Unit = {},
    onLogout: () -> Unit
) {
    val currentHour = remember { LocalDateTime.now().hour }
    val greeting = when (currentHour) {
        in 5..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..23 -> "晚上好"
        else -> "夜深了"
    }
    
    val greetingEmoji = when (currentHour) {
        in 5..11 -> "🌅"
        in 12..13 -> "☀️"
        in 14..17 -> "🌤️"
        in 18..23 -> "🌙"
        else -> "⭐"
    }
    
    // 🔥 获取装备的头像框
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { (context.applicationContext as com.example.funlife.FunLifeApplication).database }
    val userPreferencesDao = remember { database.userPreferencesDao() }
    val userPrefs by userPreferencesDao.getPreferences(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    val equippedAvatarFrame = userPrefs?.equippedAvatarFrame
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        // 渐变背景 - 底部透明，与下面内容融合
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE1BEE7),  // 顶部浅紫色
                            Color(0xFFF3E5F5),  // 中间更浅
                            Color(0xFFFCE4EC).copy(alpha = 0.7f),  // 底部开始透明
                                                              Color.Transparent   // 完全透明，融合到下面
                        )
                    )
                )
        )
        
        // VIP首次进入光芒特效
        if (showFirstEntryEffect && vipLevel != com.example.funlife.data.model.VipLevel.NORMAL) {
            com.example.funlife.ui.components.VipFirstEntryEffect(
                vipLevel = vipLevel,
                avatarCenter = Offset(45f, 60f), // 头像中心位置
                onComplete = onFirstEntryComplete
            )
        }
        
        // 内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：用户信息和分享按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🔥 使用带头像框的头像组件
                    com.example.funlife.ui.components.AvatarWithFrame(
                        avatarUri = avatarUri,
                        frameAssetPath = equippedAvatarFrame,
                        frameSize = if (equippedAvatarFrame != null) 90.dp else 54.dp,
                        defaultText = userSession?.nickname?.firstOrNull()?.toString()?.uppercase() ?: "U",
                        vipLevel = vipLevel
                    )
                    
                    // 用户名和昵称 - VIP用户名发光
                    Column {
                        if (vipLevel != com.example.funlife.data.model.VipLevel.NORMAL) {
                            com.example.funlife.ui.components.VipGlowingText(
                                text = userSession?.nickname ?: "用户",
                                vipLevel = vipLevel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = userSession?.nickname ?: "用户",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "@${userSession?.username ?: "guest"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // 分享按钮 - 圆形半透明胶囊背景，更精致
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .clickable(
                            onClick = { /* TODO: 分享功能 */ },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // 底部：问候语和图标 - 增加摆动动画 + 日期副标题
            val greetInfinite = rememberInfiniteTransition(label = "greetWiggle")
            val emojiWiggle by greetInfinite.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.EaseInOut),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "emojiW"
            )
            val emojiScale by greetInfinite.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.EaseInOut),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "emojiS"
            )
            val todayLabel = remember {
                val cal = java.util.Calendar.getInstance()
                val m = cal.get(java.util.Calendar.MONTH) + 1
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val w = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.SUNDAY -> "周日"
                    java.util.Calendar.MONDAY -> "周一"
                    java.util.Calendar.TUESDAY -> "周二"
                    java.util.Calendar.WEDNESDAY -> "周三"
                    java.util.Calendar.THURSDAY -> "周四"
                    java.util.Calendar.FRIDAY -> "周五"
                    else -> "周六"
                }
                "$m 月 $d 日  ·  $w"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            rotationZ = emojiWiggle
                            scaleX = emojiScale
                            scaleY = emojiScale
                        }
                ) {
                    CuteGreetingIcon(hour = currentHour)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = Shadow(
                                color = Color(0xFF8E24AA).copy(alpha = 0.35f),
                                offset = Offset(2f, 3f),
                                blurRadius = 8f
                            )
                        ),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 32.sp,
                        letterSpacing = 1.sp
                    )
                    // 日期胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.28f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = todayLabel,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DecorativeWaves(
    userPreferences: UserPreferences?,
    onTextEdit: (String) -> Unit,
    onStyleChange: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    
    // 加载少女心面板图片
    val panelBitmap = remember {
        try {
            context.assets.open("dibu/mb.png").use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)?.let {
                    it.asImageBitmap()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DecorativeWaves", "Failed to load panel image: ${e.message}")
            null
        }
    }
    
    // 使用 BoxWithConstraints 获取屏幕宽度（不含padding）
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        val screenWidth = maxWidth
        
        panelBitmap?.let { bitmap ->
            // 计算图片高度（基于有padding的宽度）
            val contentWidth = screenWidth - 40.dp
            val imageAspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
            val imageHeight = contentWidth * imageAspectRatio
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 主面板内容区域（有padding）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 背景光晕效果
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .offset(x = 0.dp, y = 0.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFB6C1).copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        center = Offset(0.5f, 0.5f),
                                        radius = 800f
                                    )
                                )
                        )
                        
                        // 图片
                        androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = "少女心面板",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                        )
                        
                        // 顶部装饰 - 漂浮的爱心和星星
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                        ) {
                            // 左上角装饰
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 12.dp, top = 12.dp)
                            ) {
                                Text(
                                    text = "💖",
                                    fontSize = 20.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.7f
                                        rotationZ = -15f
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✨",
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .graphicsLayer {
                                            alpha = 0.6f
                                        }
                                )
                            }
                            
                            // 右上角装饰（编辑按钮旁边）
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 55.dp, top = 12.dp)
                            ) {
                                Text(
                                    text = "🌸",
                                    fontSize = 18.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.7f
                                        rotationZ = 15f
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💫",
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .graphicsLayer {
                                            alpha = 0.6f
                                        }
                                )
                            }
                            
                            // 左侧中间装饰
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "🌟",
                                    fontSize = 16.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.5f
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "💕",
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .graphicsLayer {
                                            alpha = 0.6f
                                            rotationZ = -10f
                                        }
                                )
                            }
                            
                            // 右侧中间装饰
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "🌺",
                                    fontSize = 17.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.6f
                                        rotationZ = 20f
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "✨",
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .padding(end = 5.dp)
                                        .graphicsLayer {
                                            alpha = 0.5f
                                        }
                                )
                            }
                            
                            // 底部左侧装饰
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 15.dp, bottom = 25.dp)
                            ) {
                                Text(
                                    text = "🎀",
                                    fontSize = 18.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.7f
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "💝",
                                    fontSize = 15.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.6f
                                        rotationZ = 10f
                                    }
                                )
                            }
                            
                            // 底部右侧装饰
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 15.dp, bottom = 25.dp)
                            ) {
                                Text(
                                    text = "🌷",
                                    fontSize = 16.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.6f
                                        rotationZ = -15f
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⭐",
                                    fontSize = 14.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = 0.7f
                                    }
                                )
                            }
                        }
                        
                        // 底部渐变遮罩 - 柔和过渡
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFFFF0F5).copy(alpha = 0.3f),
                                            Color(0xFFFFF0F5).copy(alpha = 0.6f),
                                            Color(0xFFFFF0F5).copy(alpha = 0.9f)
                                        )
                                    )
                                )
                        )
                        
                        // 艺术字文字叠加在图片中间
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .padding(horizontal = 40.dp, vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayText = userPreferences?.homePanelText ?: "少女心面板"
                            val textStyle = userPreferences?.homePanelTextStyle ?: "pink"
                            
                            val artisticStyle = when(textStyle) {
                                "purple" -> com.example.funlife.ui.components.ArtisticTextStyle.PURPLE
                                "blue" -> com.example.funlife.ui.components.ArtisticTextStyle.BLUE
                                "gold" -> com.example.funlife.ui.components.ArtisticTextStyle.GOLD
                                "rainbow" -> com.example.funlife.ui.components.ArtisticTextStyle.RAINBOW
                                else -> com.example.funlife.ui.components.ArtisticTextStyle.PINK
                            }
                            
                            // 默认状态（用户未自定义）→ 显示精致的日期 + 每日金句
                            // 自定义后 → 显示用户的艺术字
                            if (displayText.isBlank()) {
                                DefaultPanelContent(style = artisticStyle)
                            } else {
                                com.example.funlife.ui.components.ArtisticText(
                                    text = displayText,
                                    style = artisticStyle
                                )
                            }
                        }
                        
                        // 编辑提示图标（右上角）
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // 底部装饰波浪 - 铺满整个屏幕宽度（无padding）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                ) {
                    // 可爱的波浪装饰 - 波浪形状填充
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        val waveWidth = size.width
                        val waveHeight = size.height
                        val wavePath = androidx.compose.ui.graphics.Path()
                        
                        // 从左上角开始绘制波浪曲线
                        wavePath.moveTo(0f, waveHeight * 0.5f)
                        
                        val waveCount = 4
                        val waveLength = waveWidth / waveCount
                        
                        // 绘制波浪顶部曲线
                        for (i in 0..waveCount) {
                            val x = i * waveLength
                            wavePath.cubicTo(
                                x + waveLength * 0.25f, waveHeight * 0.2f,
                                x + waveLength * 0.75f, waveHeight * 0.8f,
                                x + waveLength, waveHeight * 0.5f
                            )
                        }
                        
                        // 连接到右下角
                        wavePath.lineTo(waveWidth, waveHeight)
                        // 连接到左下角
                        wavePath.lineTo(0f, waveHeight)
                        // 闭合路径
                        wavePath.close()
                        
                        // 绘制渐变波浪填充
                        drawPath(
                            path = wavePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color(0xFFFFF0F5).copy(alpha = 0.9f),
                                    androidx.compose.ui.graphics.Color(0xFFFFF0F5)
                                )
                            )
                        )
                    }
                    
                    // 装饰性小星星
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                            .offset(y = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("✨", "💫", "⭐", "✨").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 14.sp,
                                modifier = Modifier.graphicsLayer {
                                    alpha = 0.6f
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 编辑对话框
    if (showEditDialog) {
        var editText by remember { mutableStateOf(userPreferences?.homePanelText ?: "少女心面板") }
        var selectedStyle by remember { mutableStateOf(userPreferences?.homePanelTextStyle ?: "pink") }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "编辑面板文字",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // 使用 Column 包裹，设置最大高度
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("自定义文字") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "建议不超过10个字，最多2行",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Divider()
                    
                    Text(
                        "选择颜色主题",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 颜色主题选择 - 第一行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorStyleButton(
                            style = "pink",
                            displayName = "粉色",
                            color = Color(0xFFFF69B4),
                            selected = selectedStyle == "pink",
                            onClick = { selectedStyle = "pink" },
                            modifier = Modifier.weight(1f)
                        )
                        ColorStyleButton(
                            style = "purple",
                            displayName = "紫色",
                            color = Color(0xFF9C27B0),
                            selected = selectedStyle == "purple",
                            onClick = { selectedStyle = "purple" },
                            modifier = Modifier.weight(1f)
                        )
                        ColorStyleButton(
                            style = "blue",
                            displayName = "蓝色",
                            color = Color(0xFF2196F3),
                            selected = selectedStyle == "blue",
                            onClick = { selectedStyle = "blue" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // 颜色主题选择 - 第二行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorStyleButton(
                            style = "gold",
                            displayName = "金色",
                            color = Color(0xFFFFD700),
                            selected = selectedStyle == "gold",
                            onClick = { selectedStyle = "gold" },
                            modifier = Modifier.weight(1f)
                        )
                        ColorStyleButton(
                            style = "rainbow",
                            displayName = "彩虹",
                            color = Color(0xFFFF9800),
                            selected = selectedStyle == "rainbow",
                            onClick = { selectedStyle = "rainbow" },
                            modifier = Modifier.weight(1f)
                        )
                        // 占位空间，保持对齐
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTextEdit(editText)
                        onStyleChange(selectedStyle)
                        showEditDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ColorStyleButton(
    style: String,
    displayName: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.3f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = color
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CuteButton(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = Color(0xFFFFB6C1)
        ),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFDB7093),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuickStatsSection(
    anniversaryCount: Int,
    playerCount: Int,
    totalScore: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStatItem(
                icon = "🎂",
                label = "纪念日",
                value = anniversaryCount.toString(),
                color = Color(0xFFFF6B9D)
            )
            
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = Color(0xFFE0E0E0)
            )
            
            QuickStatItem(
                icon = "👥",
                label = "玩家",
                value = playerCount.toString(),
                color = Color(0xFF4ECDC4)
            )
            
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = Color(0xFFE0E0E0)
            )
            
            QuickStatItem(
                icon = "🏆",
                label = "总分",
                value = totalScore.toString(),
                color = Color(0xFFFFD700)
            )
        }
    }
}

@Composable
fun QuickStatItem(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 24.sp
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF757575),
            fontSize = 13.sp
        )
    }
}

@Composable
fun FunctionCardsSection(navController: NavController) {
    // 所有功能项数据
    data class FuncItem(val title: String, val icon: String, val gradient: List<Color>, val route: String)
    val items = listOf(
        FuncItem("纪念日", "🎂", listOf(Color(0xFFB39DDB), Color(0xFF9575CD)), "anniversary"),
        FuncItem("幸运转盘", "🎡", listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)), "spin_wheel"),
        FuncItem("游戏计分", "🎵", listOf(Color(0xFFFFD54F), Color(0xFFFFCA28)), "score_counter"),
        FuncItem("商城", "🛒", listOf(Color(0xFFFF6B9D), Color(0xFFFF8FB3)), "shop"),
        FuncItem("背包", "🎒", listOf(Color(0xFFFFB74D), Color(0xFFFFA726)), "inventory"),
        FuncItem("宠物屋", "🐾", listOf(Color(0xFFFFB6C1), Color(0xFFFF69B4)), "pet"),
        FuncItem("习惯打卡", "✅", listOf(Color(0xFF4DD0E1), Color(0xFF26C6DA)), "habit"),
        FuncItem("目标管理", "🎯", listOf(Color(0xFFFFB74D), Color(0xFFFFA726)), "goal"),
        FuncItem("猜谜游戏", "🧩", listOf(Color(0xFFFF6FAE), Color(0xFF8B5CF6)), "riddle_game"),
        FuncItem("骰子游戏", "🎲", listOf(Color(0xFFFF80AB), Color(0xFFEC407A)), "dice_game"),
        FuncItem("心情日记", "📝", listOf(Color(0xFF90CAF9), Color(0xFF64B5F6)), "mood"),
        FuncItem("VIP会员", "👑", listOf(Color(0xFFFFD700), Color(0xFFFF6B9D)), "vip"),
        FuncItem("聊天记账", "💬", listOf(Color(0xFFFF8A80), Color(0xFFFF5252)), "chat_bill"),
        FuncItem("头像框", "🖼️", listOf(Color(0xFF9C27B0), Color(0xFFE91E63)), "avatar_frame_shop"),
    )
    
    // 按每列2个分组（上下两行）
    val columns = items.chunked(2)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题 - 渐变文字 + 旋转星 + 动画箭头胶囊
        val titleInfinite = rememberInfiniteTransition(label = "titleAnim")
        val starRot by titleInfinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(4500, easing = androidx.compose.animation.core.LinearEasing)
            ),
            label = "starR"
        )
        val arrowDx by titleInfinite.animateFloat(
            initialValue = 0f,
            targetValue = 6f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.EaseInOut),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "arrowD"
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✨",
                fontSize = 22.sp,
                modifier = Modifier.graphicsLayer { rotationZ = starRot }
            )
            Text(
                text = "功能中心",
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFF9C27B0),
                            Color(0xFF673AB7)
                        )
                    ),
                    shadow = Shadow(
                        color = Color(0xFFFFB6C1).copy(alpha = 0.6f),
                        offset = Offset(0f, 2f),
                        blurRadius = 6f
                    )
                ),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFE0EC),
                                Color(0xFFF3E5F5)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "左滑查看更多",
                    fontSize = 11.sp,
                    color = Color(0xFFAD1457),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "→",
                    fontSize = 12.sp,
                    color = Color(0xFFAD1457),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(x = arrowDx.dp)
                )
            }
        }
        
        // 🔥 横向滚动两行网格
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(columns) { columnPair ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    columnPair.forEach { item ->
                        FunctionCard(
                            modifier = Modifier.size(width = 100.dp, height = 100.dp),
                            title = item.title,
                            icon = item.icon,
                            gradient = item.gradient,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    // 如果列不足2个，填充空间保持对齐
                    if (columnPair.size < 2) {
                        Spacer(modifier = Modifier.size(width = 100.dp, height = 100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FunctionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    // 图标的呼吸缩放
    val cardInfinite = rememberInfiniteTransition(label = "cardBreath")
    val iconBreath by cardInfinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "iconB"
    )
    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = gradient.first().copy(alpha = 0.6f),
                spotColor = gradient.last().copy(alpha = 0.6f)
            )
            .clickable(
                onClick = {
                    pressed = true
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(gradient))
        ) {
            // 顶部高光层 - 让卡片有玻璃质感
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // 可爱的装饰圆圈（左上角）
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
            
            // 可爱的装饰圆圈（右下角）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        CircleShape
                    )
            )
            
            // 小星星装饰（右上角）
            Text(
                "✨",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(2.dp))
                
                // 图标 - 加呼吸缩放动画
                Text(
                    text = icon,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .graphicsLayer {
                            scaleX = iconBreath
                            scaleY = iconBreath
                        }
                )
                
                // 标题 - 允许两行显示，减小字体
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f)
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 13.sp,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
    
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

@Composable
fun RecentAnniversariesSection(
    anniversaries: List<com.example.funlife.data.model.Anniversary>,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎂",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "最近纪念日",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            TextButton(onClick = onViewAll) {
                Text("查看全部")
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(anniversaries) { anniversary ->
                MiniAnniversaryCard(anniversary)
            }
        }
    }
}

@Composable
fun MiniAnniversaryCard(anniversary: com.example.funlife.data.model.Anniversary) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = anniversary.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = anniversary.getFormattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardSection(
    players: List<com.example.funlife.data.model.Player>,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "排行榜",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            TextButton(onClick = onViewAll) {
                Text("查看全部")
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                players.forEachIndexed { index, player ->
                    MiniPlayerRow(player, index + 1)
                    
                    if (index < players.size - 1) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerRow(player: com.example.funlife.data.model.Player, rank: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rankEmoji = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "🏅"
            }
            
            Text(
                text = rankEmoji,
                fontSize = 24.sp
            )
            
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ) {
            Text(
                text = "${player.score} 分",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}


@Composable
fun GoalWidgetSection(
    countdowns: List<com.example.funlife.data.model.Countdown>,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "我的目标",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            TextButton(onClick = onViewAll) {
                Text("查看全部")
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                countdowns.forEach { countdown ->
                    MiniGoalCard(countdown)
                }
            }
        }
    }
}

@Composable
fun MiniGoalCard(countdown: com.example.funlife.data.model.Countdown) {
    val daysRemaining = countdown.getDaysRemaining()
    val color = Color(android.graphics.Color.parseColor(countdown.color))
    
    val statusColor = when {
        daysRemaining < 0 -> Color(0xFF95A5A6)
        daysRemaining == 0L -> Color(0xFFE74C3C)
        daysRemaining <= 7 -> Color(0xFFFF6B35)
        daysRemaining <= 30 -> Color(0xFFFFD700)
        else -> Color(0xFF4ECDC4)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(countdown.icon, fontSize = 24.sp)
            }
            
            // 标题和日期
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    countdown.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    try {
                        val date = LocalDate.parse(countdown.targetDate)
                        val formatter = DateTimeFormatter.ofPattern("MM月dd日")
                        date.format(formatter)
                    } catch (e: Exception) {
                        countdown.targetDate
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 倒计时标签
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = statusColor.copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        daysRemaining > 0 -> "⏰"
                        daysRemaining == 0L -> "🎉"
                        else -> "✓"
                    },
                    fontSize = 14.sp
                )
                Text(
                    when {
                        daysRemaining > 0 -> "$daysRemaining 天"
                        daysRemaining == 0L -> "今天"
                        else -> "已过"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🌸 默认面板内容：精致的日期 + 每日金句
// 用户未自定义文字时显示，比重复的"少女心面板"更有意境
// ════════════════════════════════════════════════════════════
@Composable
private fun DefaultPanelContent(style: com.example.funlife.ui.components.ArtisticTextStyle) {
    // 每日金句库 - 按日期循环
    val dailyQuotes = remember {
        listOf(
            "今天也是元气满满的一天 ✨",
            "做自己生活的小太阳 ☀️",
            "愿你被温柔以待 💗",
            "慢慢来，比较快 🌱",
            "每天都是新的开始 🌸",
            "微笑是最好的妆容 😊",
            "把日子过成诗 📜",
            "心怀热爱，奔赴山海 🌊",
            "做一个温柔且有力量的人 💪",
            "保持热爱，奔赴下一场山海 🏔️",
            "去做让自己开心的事 🎈",
            "今日份的小确幸送给你 🍀",
            "愿所有美好都不期而遇 🌟",
            "做温柔细腻的自己 🌷",
            "生活明朗，万物可爱 🦋",
            "把平凡的日子过成诗 ✍️",
            "种自己的花，爱自己的宇宙 🌌",
            "你本来就很可爱 💖",
            "晚风轻，星河长 🌃",
            "今天的你也很棒哦 👏",
            "心有所向，日复一日 🎯",
            "再小的努力都会被时光看见 ⏳",
            "愿你成为自己的光 💡",
            "做温柔的人，过温柔的生活 🌼",
            "每一天都值得被认真对待 🎀",
            "保持期待，保持热爱 🔥",
            "你值得一切美好 🎁",
            "慢就是快，少就是多 🍃",
            "心情好，世界都明亮 🌈",
            "做一个有趣的人 🎪",
            "认真生活的人最美 💄"
        )
    }
    
    // 根据日期取金句
    val calendar = remember { java.util.Calendar.getInstance() }
    val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val weekDay = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.SUNDAY -> "星期日"
        java.util.Calendar.MONDAY -> "星期一"
        java.util.Calendar.TUESDAY -> "星期二"
        java.util.Calendar.WEDNESDAY -> "星期三"
        java.util.Calendar.THURSDAY -> "星期四"
        java.util.Calendar.FRIDAY -> "星期五"
        else -> "星期六"
    }
    val quote = dailyQuotes[dayOfYear % dailyQuotes.size]
    
    // 闪烁动画
    val infinite = rememberInfiniteTransition(label = "panelGlow")
    val pulse by infinite.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部：日期胶囊
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            style.outer.copy(alpha = 0.85f),
                            style.middle.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$month 月 $day 日 · $weekDay",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
        
        // 中部：大字号金句（艺术字）
        Box(
            modifier = Modifier.graphicsLayer { alpha = pulse },
            contentAlignment = Alignment.Center
        ) {
            com.example.funlife.ui.components.ArtisticText(
                text = quote,
                style = style
            )
        }
        
        // 底部：小装饰线 + emoji
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(style.middle.copy(alpha = 0.7f))
            )
            Text(
                text = "˙ᵕ˙",
                fontSize = 12.sp,
                color = style.shadow.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(style.middle.copy(alpha = 0.7f))
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// 🌞🌜 可爱的问候图标 - Canvas 绘制，不依赖 emoji 字体
// 根据当前小时显示不同图标：太阳/微笑太阳/夕阳+云/月亮笑脸/睡眠月亮
// ════════════════════════════════════════════════════════════
@Composable
private fun CuteGreetingIcon(hour: Int) {
    val infinite = rememberInfiniteTransition(label = "iconAnim")
    val rayRot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(12000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "rayR"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "twi"
    )
    val zPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "zP"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        when (hour) {
            in 5..11 -> drawMorningSun(cx, cy, w, rayRot, twinkle)
            in 12..13 -> drawNoonSun(cx, cy, w, rayRot)
            in 14..17 -> drawAfternoonSun(cx, cy, w, rayRot, twinkle)
            in 18..23 -> drawEveningMoon(cx, cy, w, twinkle)
            else -> drawNightMoon(cx, cy, w, zPulse)
        }
    }
}

// 🌅 早晨：橙黄色太阳 + 笑脸 + 光芒 + 粉色腮红
private fun DrawScope.drawMorningSun(cx: Float, cy: Float, w: Float, rayRot: Float, twinkle: Float) {
    val sunR = w * 0.30f
    // 光芒（旋转）
    rotate(degrees = rayRot, pivot = Offset(cx, cy)) {
        for (i in 0 until 8) {
            val ang = (i * 45f) * (PI / 180f).toFloat()
            val x1 = cx + cos(ang) * sunR * 1.15f
            val y1 = cy + sin(ang) * sunR * 1.15f
            val x2 = cx + cos(ang) * sunR * 1.55f
            val y2 = cy + sin(ang) * sunR * 1.55f
            drawLine(
                color = Color(0xFFFFC107),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
    // 太阳光晕
    drawCircle(
        color = Color(0xFFFFD54F).copy(alpha = 0.4f),
        radius = sunR * 1.20f,
        center = Offset(cx, cy)
    )
    // 太阳主体（径向渐变）
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFEB3B), Color(0xFFFF9800)),
            center = Offset(cx - sunR * 0.2f, cy - sunR * 0.2f),
            radius = sunR * 1.3f
        ),
        radius = sunR,
        center = Offset(cx, cy)
    )
    // 高光
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = sunR * 0.25f,
        center = Offset(cx - sunR * 0.35f, cy - sunR * 0.35f)
    )
    // 眼睛（弯弯笑眼）
    drawArc(
        color = Color(0xFF3E2723),
        startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - sunR * 0.45f, cy - sunR * 0.20f),
        size = Size(sunR * 0.30f, sunR * 0.25f),
        style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFF3E2723),
        startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + sunR * 0.15f, cy - sunR * 0.20f),
        size = Size(sunR * 0.30f, sunR * 0.25f),
        style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
    // 粉色腮红
    drawCircle(
        color = Color(0xFFFF80AB).copy(alpha = 0.6f),
        radius = sunR * 0.13f,
        center = Offset(cx - sunR * 0.40f, cy + sunR * 0.15f)
    )
    drawCircle(
        color = Color(0xFFFF80AB).copy(alpha = 0.6f),
        radius = sunR * 0.13f,
        center = Offset(cx + sunR * 0.40f, cy + sunR * 0.15f)
    )
    // 微笑嘴巴
    drawArc(
        color = Color(0xFF3E2723),
        startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - sunR * 0.22f, cy + sunR * 0.15f),
        size = Size(sunR * 0.44f, sunR * 0.30f),
        style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
    // 小闪光
    val twR = sunR * 0.18f * twinkle
    drawCircle(Color.White.copy(alpha = twinkle), twR * 0.4f, Offset(cx + sunR * 1.4f, cy - sunR * 0.9f))
}

// ☀️ 中午：明亮大太阳 + 多光芒
private fun DrawScope.drawNoonSun(cx: Float, cy: Float, w: Float, rayRot: Float) {
    val sunR = w * 0.32f
    rotate(degrees = rayRot, pivot = Offset(cx, cy)) {
        for (i in 0 until 12) {
            val ang = (i * 30f) * (PI / 180f).toFloat()
            val len = if (i % 2 == 0) 1.65f else 1.40f
            val x1 = cx + cos(ang) * sunR * 1.10f
            val y1 = cy + sin(ang) * sunR * 1.10f
            val x2 = cx + cos(ang) * sunR * len
            val y2 = cy + sin(ang) * sunR * len
            drawLine(
                color = Color(0xFFFFB300),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFB300)),
            center = Offset(cx - sunR * 0.2f, cy - sunR * 0.2f),
            radius = sunR * 1.3f
        ),
        radius = sunR,
        center = Offset(cx, cy)
    )
    drawCircle(Color.White.copy(alpha = 0.6f), sunR * 0.25f, Offset(cx - sunR * 0.35f, cy - sunR * 0.35f))
    // 同款笑脸
    drawArc(Color(0xFF3E2723), 200f, 140f, false,
        topLeft = Offset(cx - sunR * 0.45f, cy - sunR * 0.20f),
        size = Size(sunR * 0.30f, sunR * 0.25f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawArc(Color(0xFF3E2723), 200f, 140f, false,
        topLeft = Offset(cx + sunR * 0.15f, cy - sunR * 0.20f),
        size = Size(sunR * 0.30f, sunR * 0.25f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawCircle(Color(0xFFFF80AB).copy(alpha = 0.6f), sunR * 0.13f, Offset(cx - sunR * 0.40f, cy + sunR * 0.15f))
    drawCircle(Color(0xFFFF80AB).copy(alpha = 0.6f), sunR * 0.13f, Offset(cx + sunR * 0.40f, cy + sunR * 0.15f))
    drawArc(Color(0xFF3E2723), 20f, 140f, false,
        topLeft = Offset(cx - sunR * 0.22f, cy + sunR * 0.15f),
        size = Size(sunR * 0.44f, sunR * 0.30f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

// 🌤️ 下午：太阳 + 一朵小云朵
private fun DrawScope.drawAfternoonSun(cx: Float, cy: Float, w: Float, rayRot: Float, twinkle: Float) {
    val sunR = w * 0.26f
    val sunCx = cx - w * 0.05f
    val sunCy = cy - w * 0.05f
    // 太阳光芒
    rotate(degrees = rayRot, pivot = Offset(sunCx, sunCy)) {
        for (i in 0 until 8) {
            val ang = (i * 45f) * (PI / 180f).toFloat()
            val x1 = sunCx + cos(ang) * sunR * 1.15f
            val y1 = sunCy + sin(ang) * sunR * 1.15f
            val x2 = sunCx + cos(ang) * sunR * 1.50f
            val y2 = sunCy + sin(ang) * sunR * 1.50f
            drawLine(Color(0xFFFFA726), Offset(x1, y1), Offset(x2, y2), 4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
    // 太阳
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE082), Color(0xFFFF8A65)),
            center = Offset(sunCx - sunR * 0.2f, sunCy - sunR * 0.2f),
            radius = sunR * 1.3f
        ),
        radius = sunR,
        center = Offset(sunCx, sunCy)
    )
    drawCircle(Color.White.copy(alpha = 0.6f), sunR * 0.22f, Offset(sunCx - sunR * 0.30f, sunCy - sunR * 0.30f))
    // 太阳笑眼
    drawArc(Color(0xFF3E2723), 200f, 140f, false,
        topLeft = Offset(sunCx - sunR * 0.45f, sunCy - sunR * 0.15f),
        size = Size(sunR * 0.30f, sunR * 0.22f),
        style = Stroke(3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawArc(Color(0xFF3E2723), 200f, 140f, false,
        topLeft = Offset(sunCx + sunR * 0.15f, sunCy - sunR * 0.15f),
        size = Size(sunR * 0.30f, sunR * 0.22f),
        style = Stroke(3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawArc(Color(0xFF3E2723), 20f, 140f, false,
        topLeft = Offset(sunCx - sunR * 0.18f, sunCy + sunR * 0.15f),
        size = Size(sunR * 0.36f, sunR * 0.26f),
        style = Stroke(3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // 云朵（右下角，由3个圆叠成）
    val cloudCx = cx + w * 0.20f
    val cloudCy = cy + w * 0.20f
    val cloudR = w * 0.13f
    drawCircle(Color.White, cloudR, Offset(cloudCx - cloudR * 0.8f, cloudCy))
    drawCircle(Color.White, cloudR * 1.2f, Offset(cloudCx, cloudCy - cloudR * 0.3f))
    drawCircle(Color.White, cloudR, Offset(cloudCx + cloudR * 0.8f, cloudCy))
    drawOval(Color.White, topLeft = Offset(cloudCx - cloudR * 1.5f, cloudCy - cloudR * 0.2f), 
        size = Size(cloudR * 3f, cloudR * 1.2f))
    // 云朵小腮红
    drawCircle(Color(0xFFFFAB91).copy(alpha = 0.5f), cloudR * 0.18f, Offset(cloudCx - cloudR * 0.45f, cloudCy + cloudR * 0.20f))
    drawCircle(Color(0xFFFFAB91).copy(alpha = 0.5f), cloudR * 0.18f, Offset(cloudCx + cloudR * 0.45f, cloudCy + cloudR * 0.20f))
}

// 🌙 晚上：月亮 + 笑脸 + 星星
private fun DrawScope.drawEveningMoon(cx: Float, cy: Float, w: Float, twinkle: Float) {
    val moonR = w * 0.30f
    // 月亮光晕
    drawCircle(Color(0xFFBBDEFB).copy(alpha = 0.4f), moonR * 1.25f, Offset(cx, cy))
    // 月亮主体（淡黄渐变）
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F)),
            center = Offset(cx - moonR * 0.2f, cy - moonR * 0.2f),
            radius = moonR * 1.3f
        ),
        radius = moonR,
        center = Offset(cx, cy)
    )
    // 月亮"咬一口"凹陷（深蓝色圆覆盖右上）
    drawCircle(
        Color(0xFF7E57C2).copy(alpha = 0.0f),
        moonR * 0.85f,
        Offset(cx + moonR * 0.5f, cy - moonR * 0.4f)
    )
    // 月亮表面圆斑
    drawCircle(Color(0xFFFFB300).copy(alpha = 0.25f), moonR * 0.15f, Offset(cx + moonR * 0.30f, cy + moonR * 0.30f))
    drawCircle(Color(0xFFFFB300).copy(alpha = 0.25f), moonR * 0.10f, Offset(cx - moonR * 0.45f, cy + moonR * 0.10f))
    // 笑眼
    drawArc(Color(0xFF3E2723), 200f, 140f, false,
        topLeft = Offset(cx - moonR * 0.45f, cy - moonR * 0.20f),
        size = Size(moonR * 0.30f, moonR * 0.25f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawArc(Color(0xFF3E2723), 200f, 140f, false,
        topLeft = Offset(cx + moonR * 0.15f, cy - moonR * 0.20f),
        size = Size(moonR * 0.30f, moonR * 0.25f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // 粉腮红
    drawCircle(Color(0xFFFF80AB).copy(alpha = 0.55f), moonR * 0.13f, Offset(cx - moonR * 0.40f, cy + moonR * 0.15f))
    drawCircle(Color(0xFFFF80AB).copy(alpha = 0.55f), moonR * 0.13f, Offset(cx + moonR * 0.40f, cy + moonR * 0.15f))
    // 嘴
    drawArc(Color(0xFF3E2723), 20f, 140f, false,
        topLeft = Offset(cx - moonR * 0.22f, cy + moonR * 0.15f),
        size = Size(moonR * 0.44f, moonR * 0.30f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // 周围闪烁星星
    drawSparkle(Offset(cx + moonR * 1.30f, cy - moonR * 0.70f), w * 0.06f * twinkle, Color(0xFFFFEB3B))
    drawSparkle(Offset(cx - moonR * 1.30f, cy + moonR * 0.50f), w * 0.05f * (1f - twinkle + 0.4f), Color(0xFFFFC107))
    drawSparkle(Offset(cx + moonR * 1.20f, cy + moonR * 1.00f), w * 0.04f * twinkle, Color(0xFFFFEB3B))
}

// ⭐ 深夜：闭眼睡眠月亮 + Z Z z
private fun DrawScope.drawNightMoon(cx: Float, cy: Float, w: Float, zPulse: Float) {
    val moonR = w * 0.28f
    drawCircle(Color(0xFF7E57C2).copy(alpha = 0.3f), moonR * 1.25f, Offset(cx, cy))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE1F5FE), Color(0xFF90CAF9)),
            center = Offset(cx - moonR * 0.2f, cy - moonR * 0.2f),
            radius = moonR * 1.3f
        ),
        radius = moonR,
        center = Offset(cx, cy)
    )
    drawCircle(Color(0xFF42A5F5).copy(alpha = 0.20f), moonR * 0.15f, Offset(cx + moonR * 0.30f, cy + moonR * 0.30f))
    // 闭眼（短弧线）
    drawArc(Color(0xFF1A237E), 0f, 180f, false,
        topLeft = Offset(cx - moonR * 0.45f, cy - moonR * 0.05f),
        size = Size(moonR * 0.30f, moonR * 0.10f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawArc(Color(0xFF1A237E), 0f, 180f, false,
        topLeft = Offset(cx + moonR * 0.15f, cy - moonR * 0.05f),
        size = Size(moonR * 0.30f, moonR * 0.10f),
        style = Stroke(4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawCircle(Color(0xFFFF80AB).copy(alpha = 0.5f), moonR * 0.13f, Offset(cx - moonR * 0.40f, cy + moonR * 0.20f))
    drawCircle(Color(0xFFFF80AB).copy(alpha = 0.5f), moonR * 0.13f, Offset(cx + moonR * 0.40f, cy + moonR * 0.20f))
    // 小嘴
    drawOval(Color(0xFF1A237E),
        topLeft = Offset(cx - moonR * 0.06f, cy + moonR * 0.30f),
        size = Size(moonR * 0.12f, moonR * 0.09f))
    // 飘动的 Z
    val zAlpha1 = (1f - zPulse).coerceIn(0f, 1f)
    val zAlpha2 = if (zPulse > 0.3f) ((zPulse - 0.3f) / 0.7f).coerceIn(0f, 1f) * (1f - zPulse + 0.3f) else 0f
    drawZ(Offset(cx + moonR * 1.0f - zPulse * 10f, cy - moonR * 0.9f - zPulse * 16f), w * 0.10f, Color(0xFF7986CB).copy(alpha = zAlpha1))
    drawZ(Offset(cx + moonR * 1.30f, cy - moonR * 0.50f - zPulse * 10f), w * 0.07f, Color(0xFF7986CB).copy(alpha = zAlpha2))
}

// 四角星
private fun DrawScope.drawSparkle(center: Offset, size: Float, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size * 0.25f, center.y - size * 0.25f)
        lineTo(center.x + size, center.y)
        lineTo(center.x + size * 0.25f, center.y + size * 0.25f)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size * 0.25f, center.y + size * 0.25f)
        lineTo(center.x - size, center.y)
        lineTo(center.x - size * 0.25f, center.y - size * 0.25f)
        close()
    }
    drawPath(path, color)
}

// Z 字符
private fun DrawScope.drawZ(topLeft: Offset, size: Float, color: Color) {
    val stroke = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    // 上横
    drawLine(color, Offset(topLeft.x, topLeft.y), Offset(topLeft.x + size, topLeft.y), stroke.width, cap = stroke.cap)
    // 斜线
    drawLine(color, Offset(topLeft.x + size, topLeft.y), Offset(topLeft.x, topLeft.y + size), stroke.width, cap = stroke.cap)
    // 下横
    drawLine(color, Offset(topLeft.x, topLeft.y + size), Offset(topLeft.x + size, topLeft.y + size), stroke.width, cap = stroke.cap)
}
