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
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    
    // 获取用户偏好设置
    val context = androidx.compose.ui.platform.LocalContext.current
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
                        onClick = { navController.navigate("anniversary") }
                    )
                } else {
                    WelcomeHeader(
                        userSession = userSession,
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

@Composable
fun PinnedAnniversaryHeader(
    anniversary: Anniversary,
    onUnpin: () -> Unit,
    onClick: () -> Unit
) {
    val daysRemaining = anniversary.getDaysRemaining()
    val isToday = daysRemaining == 0L
    val isPast = daysRemaining < 0
    
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
                // 半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.6f)
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
                                Color.White.copy(alpha = 0.2f),
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
            
            // 内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部标签
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "置顶纪念日",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 底部信息
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 纪念日名称
                    Text(
                        text = anniversary.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 32.sp
                    )
                    
                    // 日期
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = anniversary.getFormattedDate(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    // 倒计时卡片
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.95f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, text, color) = when {
                                isToday -> Triple("🎉", "就是今天！", Color(0xFFFF6B35))
                                isPast -> Triple("📅", "已过去 ${-daysRemaining} 天", Color(0xFF95A5A6))
                                else -> Triple("⏰", "还有 $daysRemaining 天", Color(0xFF4ECDC4))
                            }
                            
                            Text(
                                text = icon,
                                fontSize = 28.sp
                            )
                            
                            Column {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                                if (!isToday && !isPast) {
                                    Text(
                                        text = "期待与你相见",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
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
                    // 用户头像 - 简单圆形
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                    }
                    
                    // 用户名和昵称
                    Column {
                        Text(
                            text = userSession?.nickname ?: "0000",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "@${userSession?.username ?: "yishi"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // 分享按钮
                IconButton(
                    onClick = { /* TODO: 分享功能 */ },
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // 底部：问候语和图标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = greetingEmoji,
                    fontSize = 48.sp
                )
                
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 36.sp
                )
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
                            
                            com.example.funlife.ui.components.ArtisticText(
                                text = displayText,
                                style = artisticStyle
                            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✨",
                fontSize = 22.sp
            )
            Text(
                text = "功能中心",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        
        // 第一行：纪念日 + 幸运转盘 + 游戏计分
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "纪念日",
                icon = "🎂",
                gradient = listOf(Color(0xFFB39DDB), Color(0xFF9575CD)),
                onClick = { navController.navigate("anniversary") }
            )
            
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "幸运转盘",
                icon = "🎡",
                gradient = listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)),
                onClick = { 
                    navController.navigate("spin_wheel") {
                        launchSingleTop = true
                    }
                }
            )
            
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "游戏计分",
                icon = "🎵",
                gradient = listOf(Color(0xFFFFD54F), Color(0xFFFFCA28)),
                onClick = { navController.navigate("score_counter") }
            )
        }
        
        // 第二行：商城 + 宠物屋 + 习惯打卡
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "商城",
                icon = "🛒",
                gradient = listOf(Color(0xFF81C784), Color(0xFF66BB6A)),
                onClick = { navController.navigate("shop") }
            )
            
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "宠物屋",
                icon = "🐾",
                gradient = listOf(Color(0xFFFFB6C1), Color(0xFFFF69B4)),
                onClick = { navController.navigate("pet") }
            )
            
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "习惯打卡",
                icon = "✅",
                gradient = listOf(Color(0xFF4DD0E1), Color(0xFF26C6DA)),
                onClick = { navController.navigate("habit") }
            )
        }
        
        // 第三行：目标管理 + 猜谜游戏 + 心情日记
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "目标管理",
                icon = "🎯",
                gradient = listOf(Color(0xFFFFB74D), Color(0xFFFFA726)),
                onClick = { navController.navigate("goal") }
            )
            
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "猜谜游戏",
                icon = "🧩",
                gradient = listOf(Color(0xFFFF6FAE), Color(0xFF8B5CF6)),
                onClick = { navController.navigate("riddle_game") }
            )
            
            FunctionCard(
                modifier = Modifier.weight(1f),
                title = "心情日记",
                icon = "📝",
                gradient = listOf(Color(0xFF90CAF9), Color(0xFF64B5F6)),
                onClick = { navController.navigate("mood") }
            )
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
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                onClick = {
                    pressed = true
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(gradient))
        ) {
            // 可爱的装饰圆圈（左上角）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = (-10).dp, y = (-10).dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
            
            // 可爱的装饰圆圈（右下角）
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 15.dp, y = 15.dp)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        CircleShape
                    )
            )
            
            // 小星星装饰（右上角）
            Text(
                "✨",
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // 图标
                Text(
                    text = icon,
                    fontSize = 44.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 标题 - 增加阴影和描边效果
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
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
