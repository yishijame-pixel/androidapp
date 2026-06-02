// RiddleGameScreen.kt - 🎨 精致猜谜游戏页面
package com.example.funlife.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.repository.RiddleRepository
import com.example.funlife.utils.SoundEffect
import com.example.funlife.utils.SoundEffectManager
import com.example.funlife.viewmodel.RiddleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

private const val TAG = "RiddleGameScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiddleGameScreen(
    userId: Long,
    database: com.example.funlife.data.database.AppDatabase,
    onNavigateBack: () -> Unit = {}
) {
    Log.d(TAG, "RiddleGameScreen started for userId: $userId")
    
    val viewModel = remember(userId, database) {
        Log.d(TAG, "Creating RiddleViewModel")
        RiddleViewModel(
            repository = RiddleRepository(
                riddleDao = database.riddleDao(),
                progressDao = database.riddleProgressDao(),
                statsDao = database.riddleStatsDao()
            ),
            userId = userId
        )
    }
    
    val currentRiddleFromVM by viewModel.currentRiddle.collectAsState()
    val currentIndex by viewModel.currentRiddleIndex.collectAsState()
    val allRiddles by viewModel.allRiddles.collectAsState()
    val userAnswer by viewModel.userAnswer.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()
    
    // 🔥 修复：直接从列表计算当前题目，作为备用方案
    val currentRiddle = remember(allRiddles, currentIndex) {
        allRiddles.getOrNull(currentIndex)
    }
    
    // 日志输出
    LaunchedEffect(allRiddles.size, currentRiddle, currentRiddleFromVM) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "UI State Update:")
        Log.d(TAG, "  allRiddles.size: ${allRiddles.size}")
        Log.d(TAG, "  currentIndex: $currentIndex")
        Log.d(TAG, "  currentRiddle (computed): ${currentRiddle?.question?.take(30)}")
        Log.d(TAG, "  currentRiddleFromVM: ${currentRiddleFromVM?.question?.take(30)}")
        Log.d(TAG, "  isInitialized: $isInitialized")
        Log.d(TAG, "========================================")
    }
    
    val scrollState = rememberScrollState()
    
    // 🎵 音效管理
    val context = LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    
    // 启动背景音乐 + 退出停止
    DisposableEffect(Unit) {
        soundManager.play(SoundEffect.RIDDLE_BGM, volume = 0.3f, loop = true)
        onDispose {
            soundManager.stop(SoundEffect.RIDDLE_BGM)
        }
    }
    
    // 🎯 监听答题结果播放音效
    LaunchedEffect(showResult, isCorrect) {
        if (showResult) {
            if (isCorrect) {
                soundManager.play(SoundEffect.RIDDLE_CORRECT, volume = 0.8f)
            } else {
                soundManager.play(SoundEffect.RIDDLE_WRONG, volume = 0.6f)
            }
        }
    }
    
    // 🏆 检测通关：所有题目都答完
    val totalAnswered = stats?.totalAnswered ?: 0
    val totalCount = allRiddles.size
    var hasPlayedCompleteSound by remember { mutableStateOf(false) }
    LaunchedEffect(totalAnswered, totalCount) {
        if (totalCount > 0 && totalAnswered >= totalCount && !hasPlayedCompleteSound) {
            hasPlayedCompleteSound = true
            soundManager.play(SoundEffect.RIDDLE_COMPLETE, volume = 1.0f)
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // � 游戏HUD风格顶栏
            val hudTrans = rememberInfiniteTransition(label = "hud")
            val titleGlow by hudTrans.animateFloat(
                initialValue = 0.5f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "titleGlow"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1A0A2E),
                                Color(0xFF2D1B4E),
                                Color(0xFF1A0A2E)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .height(70.dp)
            ) {
                // 底部霓虹分割线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF00F5FF).copy(alpha = titleGlow),
                                    Color(0xFFFF00E5).copy(alpha = titleGlow),
                                    Color(0xFFFFD700).copy(alpha = titleGlow),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 返回按钮 - 霓虹圆形
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00F5FF).copy(alpha = 0.3f),
                                        Color(0xFF00F5FF).copy(alpha = 0.05f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable(
                                onClick = onNavigateBack,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF00F5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // 标题 - 霓虹发光
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎮", fontSize = 26.sp)
                        Text(
                            "猜谜挑战",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0xFFFF00E5).copy(alpha = titleGlow),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 20f
                                ),
                                letterSpacing = 2.sp
                            )
                        )
                    }
                    
                    // 占位
                    Box(modifier = Modifier.size(46.dp))
                }
            }
        }
    ) { paddingValues ->
        // 🎨 动态炫彩背景
        val bgTransition = rememberInfiniteTransition(label = "bg")
        val bgShift by bgTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
            label = "bgShift"
        )
        val orbFloat by bgTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
            label = "orbFloat"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0218),
                            Color(0xFF1A0A2E),
                            Color(0xFF2D1B4E),
                            Color(0xFF1A0A2E),
                            Color(0xFF0A0218)
                        )
                    )
                )
        ) {
            // 网格地板线 - 游戏感
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 60f
                val gridColor = Color(0xFF00F5FF).copy(alpha = 0.06f)
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gridSpacing
                }
            }
            // 浮动霓虹光球
            Canvas(modifier = Modifier.fillMaxSize()) {
                val orbColors = listOf(
                    Color(0xFFFF00E5), Color(0xFF00F5FF), Color(0xFFFFD700),
                    Color(0xFF00FF88), Color(0xFFFF6F00), Color(0xFFB94FFF)
                )
                orbColors.forEachIndexed { idx, c ->
                    val phase = (orbFloat + idx * 0.16f) % 1f
                    val ox = size.width * (0.1f + 0.8f * ((idx * 0.17f + phase) % 1f))
                    val oy = size.height * (0.05f + 0.9f * ((idx * 0.31f + phase * 0.7f) % 1f))
                    val r = 100f + 40f * sin(phase * PI.toFloat() * 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c.copy(alpha = 0.25f), c.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(ox, oy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(ox, oy)
                    )
                }
            }
            // 闪烁霓虹星点
            Canvas(modifier = Modifier.fillMaxSize()) {
                repeat(60) { i ->
                    val x = ((i * 37f + 11f) % size.width)
                    val y = ((i * 53f + 19f) % size.height)
                    val tw = (0.3f + 0.7f * ((sin(bgShift * PI.toFloat() * 2f + i * 0.5f) + 1f) / 2f)).coerceIn(0f, 1f)
                    val starColor = when (i % 4) {
                        0 -> Color(0xFFFFD700)
                        1 -> Color(0xFF00F5FF)
                        2 -> Color(0xFFFF00E5)
                        else -> Color.White
                    }
                    drawCircle(
                        color = starColor.copy(alpha = tw),
                        radius = if (i % 8 == 0) 3f else 1.5f,
                        center = Offset(x, y)
                    )
                    if (i % 8 == 0) {
                        drawCircle(
                            color = starColor.copy(alpha = tw * 0.3f),
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
            if (!isInitialized) {
                // 加载中
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                        Text(
                            "正在加载题目...",
                            fontSize = 16.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else if (allRiddles.isEmpty()) {
                // 没有题目
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("❌", fontSize = 48.sp)
                        Text(
                            "题库为空",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        Button(onClick = onNavigateBack) {
                            Text("返回")
                        }
                    }
                }
            } else {
                // 有题目，显示内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 进度信息卡片
                    ProgressInfoCard(
                        currentIndex = currentIndex,
                        totalCount = allRiddles.size,
                        score = stats?.totalScore ?: 0,
                        streak = stats?.currentStreak ?: 0
                    )
                    
                    // 谜题展示主卡片
                    if (currentRiddle != null) {
                        RiddleMainCard(riddle = currentRiddle!!)
                    } else {
                        // 调试卡片 - 显示详细信息
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 210.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("⚠️ 调试信息", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                
                                Text("题库大小: ${allRiddles.size}", fontSize = 14.sp)
                                Text("当前索引: $currentIndex", fontSize = 14.sp)
                                Text("已初始化: $isInitialized", fontSize = 14.sp)
                                Text("currentRiddle: ${currentRiddle?.question ?: "null"}", fontSize = 12.sp)
                                
                                if (allRiddles.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("前3题预览:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    allRiddles.take(3).forEachIndexed { index, riddle ->
                                        Text(
                                            "${index + 1}. ${riddle.question.take(30)}...",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        // 强制刷新
                                        viewModel.nextRiddle()
                                        viewModel.previousRiddle()
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("刷新")
                                }
                            }
                        }
                    }
                    
                    // 答案输入区
                    AnswerInputSection(
                        answer = userAnswer,
                        onAnswerChange = { viewModel.updateAnswer(it) },
                        onSubmit = { viewModel.submitAnswer() },
                        enabled = !showResult
                    )
                    
                    // 操作按钮区
                    ActionButtonsSection(
                        onSubmit = { viewModel.submitAnswer() },
                        onSkip = { viewModel.skipRiddle() },
                        enabled = !showResult && userAnswer.isNotBlank()
                    )
                    
                    Spacer(Modifier.height(40.dp))
                }
            }
            
            // 答题反馈弹窗
            if (showResult) {
                ResultDialog(
                    isCorrect = isCorrect,
                    correctAnswer = currentRiddle?.answer ?: "",
                    onNext = {
                        viewModel.dismissResult()
                        viewModel.nextRiddle()
                    }
                )
            }
        }
    }
}

// � 游戏HUD状态面板
@Composable
fun ProgressInfoCard(
    currentIndex: Int,
    totalCount: Int,
    score: Int,
    streak: Int
) {
    val progress = if (totalCount > 0) (currentIndex + 1).toFloat() / totalCount else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF1A0A2E).copy(alpha = 0.85f), Color(0xFF2D1B4E).copy(alpha = 0.85f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF00F5FF), Color(0xFFFF00E5), Color(0xFFFFD700))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        // 关卡进度条
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "LEVEL ${currentIndex + 1}/${totalCount}",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00F5FF),
                style = androidx.compose.ui.text.TextStyle(
                    letterSpacing = 1.5.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF00F5FF), offset = Offset(0f, 0f), blurRadius = 8f
                    )
                )
            )
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }
        Spacer(Modifier.height(6.dp))
        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFF0A0218), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF00F5FF), Color(0xFFFF00E5), Color(0xFFFFD700))
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Spacer(Modifier.height(12.dp))
        // 状态项
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonStatItem(icon = "🎯", value = "${currentIndex + 1}", label = "当前", color = Color(0xFF00F5FF))
            NeonStatItem(icon = "📚", value = "$totalCount", label = "总数", color = Color(0xFFB94FFF))
            NeonStatItem(icon = "⭐", value = "$score", label = "得分", color = Color(0xFFFFD700))
            NeonStatItem(icon = "🔥", value = "$streak", label = "连击", color = Color(0xFFFF00E5))
        }
    }
}

@Composable
fun NeonStatItem(icon: String, value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color, offset = Offset(0f, 0f), blurRadius = 12f
                )
            )
        )
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
    }
}

// � 谜题主卡片 - 游戏风格
@Composable
fun RiddleMainCard(riddle: com.example.funlife.data.model.Riddle) {
    var scale by remember { mutableStateOf(0.94f) }
    var rotation by remember { mutableStateOf(-2f) }
    
    LaunchedEffect(riddle.id) {
        scale = 0.94f
        rotation = -2f
        delay(100)
        scale = 1f
        rotation = 0f
    }
    
    // 卡片边框流光动画
    val cardTrans = rememberInfiniteTransition(label = "card")
    val borderShift by cardTrans.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "borderShift"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .scale(scale)
            .graphicsLayer(rotationZ = rotation)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF1A0A2E).copy(alpha = 0.92f),
                        Color(0xFF2D1B4E).copy(alpha = 0.92f),
                        Color(0xFF1A0A2E).copy(alpha = 0.92f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF00F5FF), Color(0xFFFF00E5),
                        Color(0xFFFFD700), Color(0xFF00FF88), Color(0xFF00F5FF)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 内部装饰渐变光晕
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF00E5).copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(200f, 100f),
                            radius = 400f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部：分类和难度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val catColor = when(riddle.category) {
                        "谐音梗" -> Color(0xFFFF00E5)
                        "脑筋急转弯" -> Color(0xFF00F5FF)
                        "成语" -> Color(0xFFFFD700)
                        else -> Color(0xFFB94FFF)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                catColor.copy(alpha = 0.15f),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = catColor.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when(riddle.category) {
                                    "谐音梗" -> "🎵"
                                    "脑筋急转弯" -> "💡"
                                    "成语" -> "📖"
                                    else -> "🎯"
                                },
                                fontSize = 16.sp
                            )
                            Text(
                                riddle.category,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = catColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    letterSpacing = 0.5.sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = catColor, offset = Offset(0f, 0f), blurRadius = 8f
                                    )
                                )
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(
                                Color(0xFFFFD700).copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        repeat(5) { index ->
                            Text(
                                if (index < riddle.difficulty) "⭐" else "☆",
                                fontSize = 15.sp,
                                color = if (index < riddle.difficulty) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                
                // 🎮 题目展示区 - 游戏屏风格
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A0218).copy(alpha = 0.6f),
                                    Color(0xFF1A0A2E).copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            1.dp,
                            Color(0xFF00F5FF).copy(alpha = 0.4f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = riddle.question,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(
                            letterSpacing = 0.8.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF00F5FF).copy(alpha = 0.5f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        )
                    )
                }
            }
        }
    }
}

// � 答案输入区 - 游戏控制台风格
@Composable
fun AnswerInputSection(
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        placeholder = {
            Text(
                "💭 输入你的答案...",
                fontSize = 15.sp,
                color = Color(0xFF00F5FF).copy(alpha = 0.5f)
            )
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1A0A2E).copy(alpha = 0.7f),
            unfocusedContainerColor = Color(0xFF1A0A2E).copy(alpha = 0.5f),
            disabledContainerColor = Color(0xFF0A0218).copy(alpha = 0.5f),
            focusedBorderColor = Color(0xFF00F5FF),
            unfocusedBorderColor = Color(0xFF00F5FF).copy(alpha = 0.4f),
            cursorColor = Color(0xFF00F5FF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        ),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (answer.trim().isNotBlank()) {
                    onSubmit()
                }
            }
        ),
        trailingIcon = {
            if (answer.isNotEmpty() && enabled) {
                IconButton(
                    onClick = { onAnswerChange("") },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFF00E5).copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        "清空",
                        tint = Color(0xFFFF00E5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}

// � 操作按钮区 - 霓虹游戏风格
@Composable
fun ActionButtonsSection(
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    enabled: Boolean
) {
    var submitScale by remember { mutableStateOf(1f) }
    var skipScale by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()
    
    // 提交按钮发光脉冲
    val infTrans = rememberInfiniteTransition(label = "btn")
    val submitGlow by infTrans.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "submitGlow"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ⏭ 跳过按钮 - 立体霓虹胶囊
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .scale(skipScale),
            contentAlignment = Alignment.Center
        ) {
            // 外发光光晕
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00F5FF).copy(alpha = 0.5f),
                                Color(0xFF00F5FF).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )
            // 按钮主体
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF1B5E6F),
                                Color(0xFF0A2E3E),
                                Color(0xFF051A24)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF00F5FF), Color(0xFF00B8D4).copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        scope.launch {
                            skipScale = 0.93f
                            delay(100)
                            skipScale = 1f
                        }
                        onSkip()
                    },
                contentAlignment = Alignment.Center
            ) {
                // 顶部玻璃高光
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 6.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                        )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏭️", fontSize = 20.sp)
                    Text(
                        "跳过",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF7DF9FF),
                        style = androidx.compose.ui.text.TextStyle(
                            letterSpacing = 2.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF00F5FF), offset = Offset(0f, 0f), blurRadius = 14f
                            )
                        )
                    )
                }
            }
        }
        
        // ⚡ 提交按钮 - 立体霓虹胶囊 + 脉冲光晕
        Box(
            modifier = Modifier
                .weight(1.6f)
                .height(64.dp)
                .scale(submitScale),
            contentAlignment = Alignment.Center
        ) {
            // 外脉冲发光
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (enabled) listOf(
                                Color(0xFFFF00E5).copy(alpha = 0.6f * submitGlow),
                                Color(0xFFB94FFF).copy(alpha = 0.3f * submitGlow),
                                Color.Transparent
                            ) else listOf(Color.Transparent, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )
            // 按钮主体
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(
                        brush = if (enabled) Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF1493),
                                Color(0xFFB94FFF),
                                Color(0xFF6E00FF),
                                Color(0xFF00B4D8)
                            )
                        ) else Brush.horizontalGradient(
                            listOf(Color(0xFF3A2A5E), Color(0xFF2A1A4E))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 2.5.dp,
                        brush = if (enabled) Brush.sweepGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFFFFF).copy(alpha = submitGlow),
                                Color(0xFFFFD700),
                                Color(0xFFFF00E5),
                                Color(0xFFFFD700)
                            )
                        ) else Brush.horizontalGradient(
                            listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(
                        enabled = enabled,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        scope.launch {
                            submitScale = 0.93f
                            delay(100)
                            submitScale = 1f
                        }
                        onSubmit()
                    },
                contentAlignment = Alignment.Center
            ) {
                // 顶部玻璃高光
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                        )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 闪电图标圆形背景
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFD700).copy(alpha = if (enabled) 0.9f else 0.2f),
                                        Color(0xFFFFD700).copy(alpha = if (enabled) 0.4f else 0.1f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 20.sp)
                    }
                    Text(
                        "提交答案",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                        style = androidx.compose.ui.text.TextStyle(
                            letterSpacing = 2.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = if (enabled) Color.White.copy(alpha = submitGlow) else Color.Transparent,
                                offset = Offset(0f, 0f),
                                blurRadius = 16f
                            )
                        )
                    )
                }
            }
        }
    }
}

// 🎨 结果对话框 - 炫彩版
@Composable
fun ResultDialog(
    isCorrect: Boolean,
    correctAnswer: String,
    onNext: () -> Unit
) {
    var scale by remember { mutableStateOf(0.85f) }
    var alpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        scale = 0.85f
        alpha = 0f
        delay(50)
        scale = 1f
        alpha = 1f
    }
    
    // emoji 跳动动画
    val infTrans = rememberInfiniteTransition(label = "result")
    val emojiScale by infTrans.animateFloat(
        initialValue = 0.95f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emojiScale"
    )
    val ringRot by infTrans.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRot"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * alpha)),
        contentAlignment = Alignment.Center
    ) {
        // 答对时撒花光晕
        if (isCorrect) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                repeat(20) { i ->
                    val ang = (i * 18f + ringRot) * PI.toFloat() / 180f
                    val r = 180f + (i % 3) * 40f
                    val px = cx + cos(ang) * r
                    val py = cy + sin(ang) * r
                    val starColor = listOf(
                        Color(0xFFFFD700), Color(0xFFFF6FAE), Color(0xFF4FD1C5)
                    )[i % 3]
                    drawCircle(color = starColor.copy(alpha = 0.8f), radius = 4f, center = Offset(px, py))
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(0.86f).scale(scale),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (isCorrect) Brush.verticalGradient(
                            listOf(Color(0xFFFFFAFD), Color(0xFFE0FFF8), Color(0xFFFFFAFD))
                        ) else Brush.verticalGradient(
                            listOf(Color(0xFFFFFAFC), Color(0xFFFFF0F0), Color(0xFFFFFAFC))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    if (isCorrect) "🎉" else "💭",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(emojiScale)
                )
                Text(
                    if (isCorrect) "回答正确！" else "再想想",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCorrect) Color(0xFF4FD1C5) else Color(0xFFFF8A80),
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = (if (isCorrect) Color(0xFF4FD1C5) else Color(0xFFFF8A80)).copy(alpha = 0.4f),
                            offset = Offset(0f, 3f),
                            blurRadius = 8f
                        )
                    )
                )
                
                if (!isCorrect) {
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = Color(0xFFFFFBF0)
                    ) {
                        Column(
                            modifier = Modifier.padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("正确答案", fontSize = 13.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
                            Text(correctAnswer, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF6C445))
                        }
                    }
                }
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFFF6FAE), Color(0xFF8B5CF6))),
                                shape = RoundedCornerShape(17.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isCorrect) "下一题" else "继续挑战",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            }
        }
    }
}
