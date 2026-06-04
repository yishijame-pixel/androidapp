// HomeScreen.kt - 美观的首页
package com.example.funlife.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
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
            context,
            database
        )
    }
    val userVip by vipRepository.getUserVip(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    val vipLevel = userVip?.getCurrentVipLevel() ?: com.example.funlife.data.model.VipLevel.NORMAL
    
    // 获取用户头像
    val userAvatarDao = remember { database.userAvatarDao() }
    val userAvatar by userAvatarDao.getUserAvatar(userSession?.userId ?: 0L)
        .collectAsState(initial = null)

    // 登录后：先确保社交会话，再强制补拉好友待办 → 收件箱 + 铃铛红点
    LaunchedEffect(userSession?.userId) {
        val uid = userSession?.userId ?: return@LaunchedEffect
        if (uid > 0L) {
            com.example.funlife.social.SocialSessionManager.warmStartAsync(context)
            com.example.funlife.social.SocialInboxSync.syncNowAsync(context, force = true)
        }
    }
    
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

    // 🔒 token 健康度一次性提示：当前 session 有效但本地无 device_token 时，
    //    提示用户重新登录一次以补领（多见于服务端旧版漏配 IDENTITY_SECRET 的老账号）
    val tokenHealthy by authViewModel.tokenHealthy.collectAsState()
    var showTokenReloginDialog by remember(userSession?.userId, tokenHealthy) {
        mutableStateOf(false)
    }
    LaunchedEffect(tokenHealthy, userSession?.userId) {
        val uid = userSession?.userId ?: return@LaunchedEffect
        if (tokenHealthy) return@LaunchedEffect
        val prefs = context.getSharedPreferences("token_health", android.content.Context.MODE_PRIVATE)
        val key = "token_relogin_prompted_$uid"
        if (!prefs.getBoolean(key, false)) {
            showTokenReloginDialog = true
        }
    }
    if (showTokenReloginDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showTokenReloginDialog = false
                userSession?.userId?.let { uid ->
                    context.getSharedPreferences("token_health", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("token_relogin_prompted_$uid", true).apply()
                }
            },
            title = { androidx.compose.material3.Text("安全升级") },
            text = {
                androidx.compose.material3.Text(
                    "为提升账号安全，本次需要重新登录一次。\n登录后金币、VIP、习惯打卡等记录都将保留，不会丢失。"
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showTokenReloginDialog = false
                    userSession?.userId?.let { uid ->
                        context.getSharedPreferences("token_health", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("token_relogin_prompted_$uid", true).apply()
                    }
                    authViewModel.logout()
                    com.example.funlife.navigation.restartAppForLogout(context)
                }) { androidx.compose.material3.Text("立即重新登录") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showTokenReloginDialog = false
                    userSession?.userId?.let { uid ->
                        context.getSharedPreferences("token_health", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("token_relogin_prompted_$uid", true).apply()
                    }
                }) { androidx.compose.material3.Text("稍后") }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 🔥 渐变背景放到外层 Box，避免 LazyColumn contentPadding 在状态栏区造成色阶分界
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE1BEE7),  // 顶部浅紫色
                        Color(0xFFF3E5F5),  // 中间更浅的紫色
                        Color(0xFFFCE4EC),  // 底部粉色
                        Color(0xFFFFF0F5)   // 最底部浅粉色
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // 🔥 沉浸式：背景延伸到顶，内容从状态栏下面开始；底部 = App Tab 90dp + 系统导航 + 呼吸 30dp
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 90.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 30.dp
            )
        ) {
        // 置顶纪念日展示区域
        item {
            if (pinnedAnniversary != null) {
                PinnedAnniversaryHeader(
                    anniversary = pinnedAnniversary!!,
                    onUnpin = { anniversaryViewModel.unpinAnniversary(pinnedAnniversary!!) },
                    onClick = { navController.navigate("anniversary") },
                    userSession = userSession,
                    avatarUri = userAvatar?.avatarUri,
                    vipLevel = vipLevel,
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
                        com.example.funlife.navigation.restartAppForLogout(context)
                    },
                    onOpenInbox = {
                        navController.navigate(com.example.funlife.navigation.Screen.Inbox.route)
                    },
                )
            }
        }

        // 装饰性波浪元素
        item {
            DecorativeWaves(
                userPreferences = userPreferences,
                userId = userSession?.userId ?: 0L,
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
                },
            )
        }

        // 功能卡片网格
        item {
            FunctionCardsSection(navController)
        }

        // 目标小组件
        if (countdowns.isNotEmpty()) {
            item {
                GoalWidgetSection(
                    countdowns = countdowns.take(3),
                    onViewAll = { navController.navigate("goal") },
                )
            }
        }

        // 最近纪念日预览
        if (anniversaries.isNotEmpty()) {
            item {
                RecentAnniversariesSection(
                    anniversaries = anniversaries.take(3),
                    onViewAll = { navController.navigate("anniversary") },
                )
            }
        }

        // 游戏排行榜预览
        if (players.isNotEmpty()) {
            item {
                LeaderboardSection(
                    players = players.take(3),
                    onViewAll = { navController.navigate("score_counter") },
                )
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
    vipLevel: com.example.funlife.data.model.VipLevel = com.example.funlife.data.model.VipLevel.NORMAL,  // 🔥 新增
    onOpenInbox: () -> Unit = {},
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
            
            com.example.funlife.ui.components.NotificationBellButton(
                onClick = onOpenInbox,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp),
                iconTint = Color.White,
                bubbleBackground = Color.Black.copy(alpha = 0.35f),
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
    onLogout: () -> Unit,
    onOpenInbox: () -> Unit = {}
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
        // 🔥 透明背景 - 让外层 Box 渐变直接透过，避免状态栏下出现色阶分界线
        // (原本这里独立渐变会和外层 LazyColumn 的渐变在状态栏底部冲突)
        
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
                
                com.example.funlife.ui.components.NotificationBellButton(
                    onClick = onOpenInbox,
                    iconTint = Color.White,
                    bubbleBackground = Color.White.copy(alpha = 0.28f),
                )
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
                // 🔥 移除日期胶囊（寄语卡片底部已显示日期，避免重复）
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
            }
        }
    }
}

@Composable
fun DecorativeWaves(
    userPreferences: UserPreferences?,
    userId: Long = 0L,
    onTextEdit: (String) -> Unit,
    onStyleChange: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    // ═══════════════════════════════════════════════════════════════
    // 🌸 全新设计：今日寄语精致卡片
    //    - 玻璃拟态粉紫渐变 + 旋转光晕装饰
    //    - 顶部「今日寄语」徽章 + 编辑按钮
    //    - 中央显示用户自定义艺术字（沿用 ArtisticText 组件）
    //    - 底部日期 + 一年进度环 + 浮动星粒子
    // ═══════════════════════════════════════════════════════════════

    val displayText = userPreferences?.homePanelText ?: ""
    val textStyle = userPreferences?.homePanelTextStyle ?: "pink"
    val artisticStyle = when (textStyle) {
        "purple" -> com.example.funlife.ui.components.ArtisticTextStyle.PURPLE
        "blue" -> com.example.funlife.ui.components.ArtisticTextStyle.BLUE
        "gold" -> com.example.funlife.ui.components.ArtisticTextStyle.GOLD
        "rainbow" -> com.example.funlife.ui.components.ArtisticTextStyle.RAINBOW
        else -> com.example.funlife.ui.components.ArtisticTextStyle.PINK
    }

    // 日期信息
    val today = remember { java.time.LocalDate.now() }
    val dayOfYear = remember { today.dayOfYear }
    val totalDaysInYear = remember { if (today.isLeapYear) 366 else 365 }
    val yearProgress = remember { dayOfYear.toFloat() / totalDaysInYear.toFloat() }
    val weekdayLabel = remember {
        when (today.dayOfWeek.value) {
            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
            5 -> "周五"; 6 -> "周六"; else -> "周日"
        }
    }

    // 🚀 已移除寄语卡片的 6 个并行 infinite 动画（haloRotation / sparkPhase / breath /
    //    cornerRot / shimmer / petalDrift）。这些原本每帧重绘 drawBehind/Canvas，
    //    是首页静止时 GPU 主要负载来源。改为静态值，视觉装饰保留，停帧节能。
    val haloRotation = 0f
    val sparkPhase = 0.5f
    val breath = 1f
    val cornerRot = 0f
    val shimmer = 0.5f
    val petalDrift = 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 卡片主体（玻璃拟态：粉→紫→蓝渐变 + 白色高光层）──
        // ⚠ 不用 heightIn，让 Column wrapContent 决定高度—避免 weight(1f) 折叠为 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD6E8),  // 樱粉
                            Color(0xFFFCC8E2),  // 珊瑚粉
                            Color(0xFFE0C3FC),  // 薰衣草紫
                            Color(0xFFC6B4F5),  // 东方紫
                            Color(0xFFB8E0FF)   // 天空蓝
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFFFFD6E8).copy(alpha = 0.6f),
                            Color(0xFFE0C3FC).copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            // ── 旋转径向光晕（左上 + 右下）+ 多层光斑 + 边框流光 + 闪光小星 ──
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width; val h = size.height
                // 1) 底层广面光斑（3 个软色圈底色）
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFE5F5).copy(alpha = 0.45f), Color.Transparent)
                    ),
                    radius = w * 0.4f,
                    center = Offset(w * 0.15f, h * 0.85f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE6D5FF).copy(alpha = 0.4f), Color.Transparent)
                    ),
                    radius = w * 0.35f,
                    center = Offset(w * 0.9f, h * 0.15f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD8F0FF).copy(alpha = 0.35f), Color.Transparent)
                    ),
                    radius = w * 0.28f,
                    center = Offset(w * 0.5f, h * 0.55f)
                )

                // 2) 旋转主光晕（左上正 + 右下反）
                rotate(haloRotation, pivot = Offset(w * 0.2f, h * 0.3f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(w * 0.2f, h * 0.3f),
                            radius = size.minDimension * 0.55f
                        ),
                        radius = size.minDimension * 0.55f,
                        center = Offset(w * 0.2f, h * 0.3f)
                    )
                }
                rotate(-haloRotation, pivot = Offset(w * 0.85f, h * 0.75f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFB6E1).copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(w * 0.85f, h * 0.75f),
                            radius = size.minDimension * 0.5f
                        ),
                        radius = size.minDimension * 0.5f,
                        center = Offset(w * 0.85f, h * 0.75f)
                    )
                }

                // 3) 边框流光圆（沿周滚动的亮点）
                val perimeter = 2 * (w + h)
                val shimmerPos = shimmer * perimeter
                val sx: Float; val sy: Float
                when {
                    shimmerPos < w -> { sx = shimmerPos; sy = 0f }
                    shimmerPos < w + h -> { sx = w; sy = shimmerPos - w }
                    shimmerPos < 2 * w + h -> { sx = w - (shimmerPos - w - h); sy = h }
                    else -> { sx = 0f; sy = h - (shimmerPos - 2 * w - h) }
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent)
                    ),
                    radius = 25f,
                    center = Offset(sx, sy)
                )

                // 4) 上升碎花粉（8 颗，水平微摆）
                val petalSeeds = listOf(0.10f, 0.22f, 0.38f, 0.50f, 0.62f, 0.74f, 0.86f, 0.95f)
                petalSeeds.forEachIndexed { i, baseX ->
                    val phase = (petalDrift + i * 0.125f) % 1f
                    val y = h * (1.05f - phase * 1.15f)
                    val sway = kotlin.math.sin((phase * 6.28f + i).toDouble()).toFloat() * 14f
                    val x = w * baseX + sway
                    val pAlpha = ((1f - phase) * (phase * 4f).coerceAtMost(1f)).coerceIn(0f, 1f) * 0.55f
                    if (y in -10f..h + 10f) {
                        val petalColor = when (i % 3) {
                            0 -> Color(0xFFFF80AB)
                            1 -> Color(0xFFE040FB)
                            else -> Color(0xFFFFD54F)
                        }
                        drawCircle(
                            color = petalColor.copy(alpha = pAlpha),
                            radius = 3.5f + (i % 3) * 0.6f,
                            center = Offset(x, y)
                        )
                    }
                }

                // 5) 浮动闪光小星（8 颗）
                val sparkPositions = listOf(
                    Offset(0.15f, 0.25f), Offset(0.85f, 0.20f),
                    Offset(0.25f, 0.78f), Offset(0.80f, 0.85f),
                    Offset(0.55f, 0.15f), Offset(0.10f, 0.55f),
                    Offset(0.92f, 0.50f), Offset(0.45f, 0.92f)
                )
                sparkPositions.forEachIndexed { i, p ->
                    val phase = (sparkPhase + i * 0.13f) % 1f
                    val alpha = (kotlin.math.sin(phase * kotlin.math.PI).toFloat()).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.9f),
                        radius = 2.5f + alpha * 1.5f,
                        center = Offset(w * p.x, h * p.y)
                    )
                    if (alpha > 0.5f) {
                        val cx = w * p.x; val cy = h * p.y
                        val len = 4f + alpha * 4f
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.8f),
                            start = Offset(cx - len, cy), end = Offset(cx + len, cy),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.8f),
                            start = Offset(cx, cy - len), end = Offset(cx, cy + len),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // ── 四角 emoji 装饰（左上/右上/左下/右下，反向轻微转动）──
            Text("💕", fontSize = 16.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 50.dp)
                    .graphicsLayer { rotationZ = -cornerRot * 0.3f; alpha = 0.85f })
            Text("✨", fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 50.dp, top = 14.dp)
                    .graphicsLayer { rotationZ = cornerRot * 0.4f; alpha = 0.8f })
            Text("🌸", fontSize = 18.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 70.dp)
                    .graphicsLayer { rotationZ = cornerRot * 0.3f; alpha = 0.75f })
            Text("⭐", fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 86.dp, bottom = 14.dp)
                    .graphicsLayer { rotationZ = -cornerRot * 0.35f; alpha = 0.8f })
            Text("🌸", fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                    .graphicsLayer { rotationZ = -cornerRot * 0.2f; alpha = 0.7f })
            Text("💖", fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                    .graphicsLayer { rotationZ = cornerRot * 0.25f; alpha = 0.7f })

            // ─── 内容 Column（不用 fillMaxSize，让 wrapContent 决定实际高度—修复 weight 折叠 bug）───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // 🚀 移除徽章 badgeBreath / badgeRainbow 动画，改为静态
                val badgeBreath = 1f
                val badgeRainbow = 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // ① 今日寄语徽章（彩虹流光描边 + 双花反向旋转 + 微脉冲）
                    Box(
                        modifier = Modifier
                            .graphicsLayer { scaleX = badgeBreath; scaleY = badgeBreath }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    "🌸", fontSize = 14.sp,
                                    modifier = Modifier.graphicsLayer { rotationZ = cornerRot * 0.8f }
                                )
                                Text(
                                    "今日寄语",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF8E24AA),
                                    letterSpacing = 1.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(
                                            color = Color(0xFFE040FB).copy(alpha = 0.5f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                                Text(
                                    "🌸", fontSize = 14.sp,
                                    modifier = Modifier.graphicsLayer { rotationZ = -cornerRot * 0.8f }
                                )
                            }
                        }
                        // ✨ 彩虹描边光晕（matchParentSize 贴在徽章上方，不带负 padding）
                        Canvas(modifier = Modifier.matchParentSize()) {
                            rotate(badgeRainbow) {
                                drawRoundRect(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFFFF80AB), Color(0xFFE040FB),
                                            Color(0xFF7C4DFF), Color(0xFF40C4FF),
                                            Color(0xFFFFD54F), Color(0xFFFF80AB)
                                        )
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                        size.height / 2f, size.height / 2f
                                    ),
                                    style = Stroke(width = 2.5f)
                                )
                            }
                        }
                    }

                    // ② 编辑按钮（旋转金色光圈 + 呼吸缩放）
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(30.dp)
                                .graphicsLayer { scaleX = badgeBreath; scaleY = badgeBreath }
                                .clickable { showEditDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "编辑寄语",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF8E24AA)
                                )
                            }
                        }
                        // 外圈虚线光环（matchParentSize，不影响布局）
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { rotationZ = badgeRainbow }
                        ) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent, Color(0xFFFFD54F),
                                        Color.Transparent, Color(0xFFFF80AB),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ─── ② 中部：艺术字（用户自定义 or 默认金句）+ 两侧装饰花 ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🌸",
                        fontSize = 22.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = -cornerRot * 0.5f
                            alpha = 0.85f
                        }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { scaleX = breath; scaleY = breath },
                        contentAlignment = Alignment.Center
                    ) {
                        if (HomePanelQuotes.shouldUseDailyQuote(displayText)) {
                            // 每日语录：按 userId + 当天日期挑一句（用户级隔离）
                            DefaultPanelContent(style = artisticStyle, userId = userId)
                        } else {
                            com.example.funlife.ui.components.ArtisticText(
                                text = displayText,
                                style = artisticStyle
                            )
                        }
                    }
                    Text(
                        "🌸",
                        fontSize = 22.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = cornerRot * 0.5f
                            alpha = 0.85f
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ─── ③ 底部：日期 + 年度进度环 ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左：日期 + 周几
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                today.monthValue.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6A1B9A)
                            )
                            Text(
                                "月",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A1B9A).copy(alpha = 0.75f),
                                modifier = Modifier.padding(start = 1.dp, bottom = 3.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                today.dayOfMonth.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6A1B9A)
                            )
                            Text(
                                "日",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A1B9A).copy(alpha = 0.75f),
                                modifier = Modifier.padding(start = 1.dp, bottom = 3.dp)
                            )
                        }
                        Text(
                            "${today.year}年 · $weekdayLabel",
                            fontSize = 11.sp,
                            color = Color(0xFF8E24AA).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 右：年度进度环
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 5.dp.toPx()
                            // 底环
                            drawArc(
                                color = Color.White.copy(alpha = 0.5f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            // 进度弧（粉→紫→金渐变）
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFFF80AB),
                                        Color(0xFFE040FB),
                                        Color(0xFFFFD54F),
                                        Color(0xFFFF80AB)
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * yearProgress,
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(yearProgress * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6A1B9A)
                            )
                            Text(
                                "$dayOfYear/$totalDaysInYear",
                                fontSize = 7.5.sp,
                                color = Color(0xFF8E24AA).copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 编辑对话框（全新美化版：实时预览 + 卡片色板 + 每日金句快速预览）
    if (showEditDialog) {
        HomePanelEditDialog(
            initialText = userPreferences?.homePanelText ?: "",
            initialStyle = userPreferences?.homePanelTextStyle ?: "pink",
            userId = userId,
            onDismiss = { showEditDialog = false },
            onConfirm = { newText, newStyle ->
                onTextEdit(newText)
                onStyleChange(newStyle)
                showEditDialog = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════
// 🎀 全新「今日寄语」编辑对话框
//   - 顶部预览卡（实时艺术字 + 当前色板）
//   - 多行输入框（占位提示「留空 = 显示每日精选」）
//   - "🎲 来一句"按钮：从语录库随机抽，自动填入
//   - "🧹 清空"按钮：清掉走每日精选
//   - 5 个色板卡片（圆形渐变球 + 名称 + 选中态描边）
// ════════════════════════════════════════════════════════════
@Composable
private fun HomePanelEditDialog(
    initialText: String,
    initialStyle: String,
    userId: Long,
    onDismiss: () -> Unit,
    onConfirm: (text: String, style: String) -> Unit,
) {
    // 老用户的 "少女心面板" 等占位符进入弹窗时不预填，保持空状态以鼓励"留空"
    val sanitizedInitial = remember(initialText) {
        if (HomePanelQuotes.shouldUseDailyQuote(initialText)) "" else initialText
    }
    var text by remember { mutableStateOf(sanitizedInitial) }
    var styleKey by remember { mutableStateOf(initialStyle) }

    val artisticStyle = remember(styleKey) {
        when (styleKey) {
            "purple" -> com.example.funlife.ui.components.ArtisticTextStyle.PURPLE
            "blue" -> com.example.funlife.ui.components.ArtisticTextStyle.BLUE
            "gold" -> com.example.funlife.ui.components.ArtisticTextStyle.GOLD
            "rainbow" -> com.example.funlife.ui.components.ArtisticTextStyle.RAINBOW
            else -> com.example.funlife.ui.components.ArtisticTextStyle.PINK
        }
    }
    val previewText = if (text.isBlank()) HomePanelQuotes.quoteFor(userId) else text

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF5FB),
                            Color(0xFFFFEEF6),
                            Color(0xFFF5E6FF),
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color(0xFFFFB6D9).copy(alpha = 0.6f),
                            Color(0xFFD7B6FF).copy(alpha = 0.5f),
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 顶部标题 ──────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌸", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "编辑今日寄语",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8E24AA),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF8E24AA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ── 实时预览卡 ────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD6E8),
                                    Color(0xFFE0C3FC),
                                    Color(0xFFB8E0FF),
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (text.isBlank()) "👇 实时预览（当前显示每日精选）" else "👇 实时预览",
                            fontSize = 11.sp,
                            color = Color(0xFF6A1B9A).copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(contentAlignment = Alignment.Center) {
                            com.example.funlife.ui.components.ArtisticText(
                                text = previewText,
                                style = artisticStyle
                            )
                        }
                    }
                }

                // ── 输入框 ────────────────────────────────
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 40) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义寄语") },
                    placeholder = { Text("留空 = 每天显示一条精选语录 ✨") },
                    minLines = 1,
                    maxLines = 3,
                    supportingText = {
                        Text(
                            "${text.length}/40 · 建议 6~16 字最佳，过长会自动缩小",
                            fontSize = 11.sp,
                            color = Color(0xFF8E24AA).copy(alpha = 0.7f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE040FB),
                        unfocusedBorderColor = Color(0xFFCE93D8).copy(alpha = 0.6f),
                        cursorColor = Color(0xFFE040FB),
                        focusedLabelColor = Color(0xFF8E24AA),
                    ),
                    shape = RoundedCornerShape(14.dp),
                )

                // ── 快速操作：随机一句 / 清空 ────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // 随机抽一句填入（不点保存就不写库）
                            val pool = HomePanelQuotes.QUOTES
                            if (pool.isNotEmpty()) {
                                val rnd = (Math.random() * pool.size).toInt().coerceIn(0, pool.size - 1)
                                text = pool[rnd]
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFB6D9))
                    ) {
                        Text("🎲 来一句", fontSize = 13.sp, color = Color(0xFFD81B60))
                    }
                    OutlinedButton(
                        onClick = { text = "" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD7B6FF))
                    ) {
                        Text("🧹 清空·用每日精选", fontSize = 12.sp, color = Color(0xFF6A1B9A))
                    }
                }

                // ── 颜色主题（卡片色板）─────────────────
                Text(
                    "选择艺术字主题",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A1B9A)
                )
                val styleOptions = listOf(
                    StyleOption("pink",    "粉色梦幻", listOf(Color(0xFFFF80AB), Color(0xFFEC407A))),
                    StyleOption("purple",  "紫色魅惑", listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A))),
                    StyleOption("blue",    "蓝色海洋", listOf(Color(0xFF42A5F5), Color(0xFF1565C0))),
                    StyleOption("gold",    "金色辉煌", listOf(Color(0xFFFFEB3B), Color(0xFFF57F17))),
                    StyleOption("rainbow", "彩虹绚丽", listOf(Color(0xFFFF80AB), Color(0xFF7C4DFF), Color(0xFFFFD54F))),
                )
                // 第一行 3 个，第二行 2 个
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    styleOptions.subList(0, 3).forEach { opt ->
                        StyleSwatchCard(
                            option = opt,
                            selected = styleKey == opt.key,
                            onClick = { styleKey = opt.key },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    styleOptions.subList(3, 5).forEach { opt ->
                        StyleSwatchCard(
                            option = opt,
                            selected = styleKey == opt.key,
                            onClick = { styleKey = opt.key },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── 底部按钮 ──────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFCE93D8)),
                    ) {
                        Text("取消", color = Color(0xFF8E24AA), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onConfirm(text.trim(), styleKey) },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE040FB),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("✨ 保存", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class StyleOption(val key: String, val displayName: String, val gradient: List<Color>)

@Composable
private fun StyleSwatchCard(
    option: StyleOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) option.gradient.first() else Color(0xFFE1BEE7).copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(brush = Brush.linearGradient(option.gradient))
            )
            Text(
                text = option.displayName,
                fontSize = 11.sp,
                color = if (selected) Color(0xFF6A1B9A) else Color(0xFF8E24AA).copy(alpha = 0.7f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
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
        FuncItem("时光信箱", "✉️", listOf(Color(0xFFB39DDB), Color(0xFF7E57C2)), "letter_mailbox"),
        FuncItem("好友", "👥", listOf(Color(0xFF8B6CF7), Color(0xFF6B4CE6)), "friends"),
        FuncItem("阅光书房", "📖", listOf(Color(0xFFFFB74D), Color(0xFFFF8A65)), "reading_room"),
        FuncItem("日记本", "📔", listOf(Color(0xFF8E6E53), Color(0xFFB23A48)), "diary_book"),
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
        // 标题 - 渐变文字 + 静态星 + 动画箭头胶囊（🚀 性能：移除星旋转持续帧驱动）
        val titleInfinite = rememberInfiniteTransition(label = "titleAnim")
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
                fontSize = 22.sp
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
                            modifier = Modifier.size(width = 104.dp, height = 112.dp),
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
                        Spacer(modifier = Modifier.size(width = 104.dp, height = 112.dp))
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
    
    // 🚀 已移除图标呼吸动画 — 之前每张卡都有独立的 infiniteTransition
    // 一个首页 ~10 张卡 = 10 个并行帧驱动，是首页静止时 CPU 占用主要来源
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
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标 - 加呼吸缩放动画
                Text(
                    text = icon,
                    fontSize = 28.sp
                )

                // 标题 - 单行优先，超长才两行；字号 12.5 更易读
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.35f),
                            offset = Offset(1f, 1.5f),
                            blurRadius = 3f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
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
            items(anniversaries, key = { it.id }) { anniversary ->
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
private fun DefaultPanelContent(
    style: com.example.funlife.ui.components.ArtisticTextStyle,
    userId: Long = 0L
) {
    // 🌸 按 userId + 当天日期从语录库选一句（用户级隔离）
    //   - 不再显示日期（卡片底部已有大日期 + 进度环）
    //   - 不再有闪烁动画（用户反馈刺眼）
    //   - 让艺术字独占视觉焦点
    val today = remember { java.time.LocalDate.now() }
    val quote = remember(userId, today) { HomePanelQuotes.quoteFor(userId, today) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        com.example.funlife.ui.components.ArtisticText(
            text = quote,
            style = style
        )
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
