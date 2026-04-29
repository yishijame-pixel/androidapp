// RiddleGameScreen.kt - 🎨 精致猜谜游戏页面
package com.example.funlife.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.example.funlife.repository.RiddleRepository
import com.example.funlife.viewmodel.RiddleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    
    Scaffold(
        topBar = {
            // 🎨 超可爱扁平化顶部导航栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFF5F8),
                                Color(0xFFFFF0F5),
                                Color(0xFFFFF5FB)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左侧：返回按钮
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Color.White.copy(alpha = 0.7f),
                                CircleShape
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
                            tint = Color(0xFFFF6FAE),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // 中间：标题
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 可爱的emoji背景
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Color.White.copy(alpha = 0.6f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🧩",
                                fontSize = 22.sp
                            )
                        }
                        
                        Text(
                            "猜谜挑战",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF6FAE),
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0xFFFF6FAE).copy(alpha = 0.2f),
                                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }
                    
                    // 右侧：规则按钮（占位，保持对称）
                    Box(
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF5F8),
                            Color(0xFFFFF0F5),
                            Color(0xFFFFF5FB),
                            Color(0xFFFFF0FA)
                        )
                    )
                )
        ) {
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

// 🎨 进度信息卡片 - 扁平可爱风格
@Composable
fun ProgressInfoCard(
    currentIndex: Int,
    totalCount: Int,
    score: Int,
    streak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(88.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 可爱的波浪背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFE5F0),
                                Color(0xFFFFF0F8),
                                Color(0xFFFFE5F5),
                                Color(0xFFFFF0FA)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
            
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CuteInfoItem(icon = "✏️", value = "${currentIndex + 1}", label = "当前", color = Color(0xFFFF6B9D))
                CuteInfoItem(icon = "📚", value = "$totalCount", label = "总数", color = Color(0xFF9B7EDE))
                CuteInfoItem(icon = "⭐", value = "$score", label = "得分", color = Color(0xFFFFB347))
                CuteInfoItem(icon = "🔥", value = "$streak", label = "连胜", color = Color(0xFFFF6B6B))
            }
        }
    }
}

@Composable
fun CuteInfoItem(icon: String, value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(
            value, 
            fontSize = 24.sp, 
            fontWeight = FontWeight.ExtraBold, 
            color = color,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
        Text(label, fontSize = 11.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.SemiBold)
    }
}

// 🎨 谜题主卡片 - 扁平可爱风格
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .scale(scale)
            .graphicsLayer(rotationZ = rotation),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 可爱的渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF0F8),
                                Color(0xFFFFF5FB),
                                Color.White
                            ),
                            center = androidx.compose.ui.geometry.Offset(0.5f, 0.3f),
                            radius = 1000f
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
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when(riddle.category) {
                            "谐音梗" -> Color(0xFFFF6FAE).copy(alpha = 0.18f)
                            "脑筋急转弯" -> Color(0xFF60A5FA).copy(alpha = 0.18f)
                            "成语" -> Color(0xFFF6C445).copy(alpha = 0.18f)
                            else -> Color(0xFF8B5CF6).copy(alpha = 0.18f)
                        },
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when(riddle.category) {
                                    "谐音梗" -> Color(0xFFFF6FAE)
                                    "脑筋急转弯" -> Color(0xFF60A5FA)
                                    "成语" -> Color(0xFFF6C445)
                                    else -> Color(0xFF8B5CF6)
                                }
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(
                                Color(0xFFFFF9E6),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        repeat(5) { index ->
                            Text(
                                if (index < riddle.difficulty) "⭐" else "☆",
                                fontSize = 15.sp,
                                color = if (index < riddle.difficulty) Color(0xFFFFC107) else Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
                
                // 中间：题目文字（超可爱）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFAFC),
                                    Color(0xFFFFF5F9)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = riddle.question,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier.fillMaxWidth(),
                        style = androidx.compose.ui.text.TextStyle(
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

// 🎨 答案输入区 - 可爱风格
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
        modifier = Modifier.fillMaxWidth().height(58.dp),
        placeholder = { 
            Text(
                "💭 请输入你的答案...", 
                fontSize = 15.sp, 
                color = Color(0xFFBDBDBD)
            ) 
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFFFFAFC),
            disabledContainerColor = Color(0xFFF5F5F5),
            focusedBorderColor = Color(0xFFFF6FAE),
            unfocusedBorderColor = Color(0xFFFFD0E5)
        ),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D3748)
        ),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { 
                // 只有当答案不为空时才提交
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
                        .background(Color(0xFFFFF0F5), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Clear, 
                        "清空", 
                        tint = Color(0xFFFF6FAE), 
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}

// 🎨 操作按钮区 - 扁平可爱风格
@Composable
fun ActionButtonsSection(
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    enabled: Boolean
) {
    var submitScale by remember { mutableStateOf(1f) }
    var skipScale by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 跳过按钮 - 扁平可爱风格
        Button(
            onClick = {
                scope.launch {
                    skipScale = 0.95f
                    delay(100)
                    skipScale = 1f
                }
                onSkip()
            },
            modifier = Modifier.weight(1f).height(56.dp).scale(skipScale),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF60A5FA)
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF60A5FA)),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏭️", fontSize = 18.sp)
                Text("跳过", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        
        // 提交按钮 - 扁平超可爱渐变
        Button(
            onClick = {
                scope.launch {
                    submitScale = 0.95f
                    delay(100)
                    submitScale = 1f
                }
                onSubmit()
            },
            modifier = Modifier.weight(1.5f).height(56.dp).scale(submitScale),
            shape = RoundedCornerShape(18.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color(0xFFE5E7EB)
            ),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (enabled) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF6FAE),
                                    Color(0xFFFF8FB3),
                                    Color(0xFFFFB3C6)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)))
                        },
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 18.sp)
                    Text("提交答案", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}

// 🎨 结果对话框
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * alpha)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.86f).scale(scale),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(if (isCorrect) "🎉" else "💭", fontSize = 58.sp)
                Text(
                    if (isCorrect) "回答正确！" else "再想想",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF4FD1C5) else Color(0xFFFF8A80)
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
