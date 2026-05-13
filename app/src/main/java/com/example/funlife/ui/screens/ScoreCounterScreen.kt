// ScoreCounterScreen.kt - 精致游戏计分屏幕
package com.example.funlife.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import com.example.funlife.viewmodel.ScoreViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 游戏状态
enum class GameState {
    SETUP,      // 设置阶段（添加玩家）
    PLAYING     // 游戏中
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCounterScreen(
    viewModel: ScoreViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val players by viewModel.players.collectAsState()
    val victoryRecords by viewModel.victoryRecords.collectAsState()
    var gameState by remember { mutableStateOf(GameState.SETUP) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVictoryDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPlayerAddedBanner by remember { mutableStateOf(false) }
    var addedPlayerCount by remember { mutableStateOf(0) }
    var previousPlayerCount by remember { mutableStateOf(0) }
    var showReusePreviousPlayersDialog by remember { mutableStateOf(false) }
    var hasCheckedPreviousPlayers by remember { mutableStateOf(false) }
    
    // 监听玩家列表变化，更新添加计数
    LaunchedEffect(players.size) {
        if (players.size > previousPlayerCount) {
            addedPlayerCount = players.size
        }
        previousPlayerCount = players.size
    }
    
    var showEndGameConfirmDialog by remember { mutableStateOf(false) }
    var showNoPlayersDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }  // 🔥 动态对话框消息
    
    // 进入界面时检查是否有之前的玩家
    LaunchedEffect(Unit) {
        if (!hasCheckedPreviousPlayers && players.isNotEmpty()) {
            showReusePreviousPlayersDialog = true
            hasCheckedPreviousPlayers = true
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区域
        when (gameState) {
            GameState.SETUP -> {
                SetupScreen(
                    players = players,
                    onAddPlayer = { 
                        if (players.size >= 4) {
                            dialogMessage = "最多只能添加4名玩家\n当前已有${players.size}名玩家"
                            showNoPlayersDialog = true
                        } else {
                            showAddDialog = true
                        }
                    },
                    onDeletePlayer = { viewModel.deletePlayer(it) },
                    onStartGame = { 
                        if (players.isEmpty()) {
                            dialogMessage = "请先添加玩家\n至少需要2名玩家才能开始游戏"
                            showNoPlayersDialog = true
                        } else if (players.size >= 2) {
                            showPlayerAddedBanner = false
                            viewModel.startNewGameSession()  // 开始新游戏会话
                            gameState = GameState.PLAYING
                        }
                    },
                    onShowHistory = { showHistoryDialog = true },
                    onNavigateBack = onNavigateBack
                )
            }
            GameState.PLAYING -> {
                PlayingScreen(
                    players = players,
                    onIncreaseScore = { player, points -> 
                        viewModel.increaseScore(player, points)
                    },
                    onDecreaseScore = { player, points ->
                        viewModel.decreaseScore(player, points)
                    },
                    onNavigateBack = onNavigateBack,
                    onEndGame = { showEndGameConfirmDialog = true }
                )
            }
        }
        
        // 添加玩家成功横幅 - 从顶部滑下，一直显示直到开始游戏
        if (showPlayerAddedBanner && gameState == GameState.SETUP) {
            PlayerAddedBanner(
                playerCount = addedPlayerCount,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            )
        }
    }
    
    // 添加玩家对话框
    if (showAddDialog) {
        AddPlayerDialog(
            usedAvatars = players.map { it.avatar }.toSet(),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, avatar ->
                viewModel.addPlayer(name, avatar)
                showAddDialog = false
                showPlayerAddedBanner = true
            }
        )
    }
    
    // 胜利弹框
    if (showVictoryDialog) {
        VictoryDialog(
            players = players,
            viewModel = viewModel,  // 🔥 传递viewModel
            onDismiss = { showVictoryDialog = false },
            onNewGame = {
                viewModel.resetAllScores()
                viewModel.startNewGameSession()  // 🔥 启动新游戏会话
                showPlayerAddedBanner = false
                showVictoryDialog = false
                gameState = GameState.PLAYING
            },
            onBackToSetup = {
                showVictoryDialog = false
                gameState = GameState.SETUP
            },
            onRecordVictory = { playerName, avatar ->
                viewModel.recordVictory(playerName, avatar)
            }
        )
    }
    
    // 清空历史确认对话框状态
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    // 历史记录弹框
    if (showHistoryDialog) {
        VictoryHistoryDialog(
            victoryRecords = victoryRecords,
            onDismiss = { showHistoryDialog = false },
            onClearHistory = {
                showClearHistoryDialog = true
            }
        )
    }
    
    // 清空历史确认对话框
    if (showClearHistoryDialog) {
        ClearHistoryConfirmDialog(
            onDismiss = { showClearHistoryDialog = false },
            onConfirm = {
                viewModel.clearVictoryRecords()
                showClearHistoryDialog = false
                showHistoryDialog = false
            }
        )
    }
    
    // 重用之前玩家的确认对话框
    if (showReusePreviousPlayersDialog) {
        ReusePreviousPlayersDialog(
            playerCount = players.size,
            onDismiss = {
                showReusePreviousPlayersDialog = false
                viewModel.deleteAllPlayers()
            },
            onConfirm = {
                showReusePreviousPlayersDialog = false
                viewModel.resetAllScores()
                viewModel.startNewGameSession()  // 🔥 启动新游戏会话
                if (players.size >= 2) {
                    gameState = GameState.PLAYING
                }
            }
        )
    }
    
    // 结束游戏确认对话框
    if (showEndGameConfirmDialog) {
        Dialog(onDismissRequest = { showEndGameConfirmDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF0)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 警告图标
                    Text("⚠️", fontSize = 32.sp)
                    
                    // 标题
                    Text(
                        "确认结束游戏",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF8B4513)
                    )
                    
                    // 提示文字
                    Text(
                        "确定要结束当前游戏吗？\n游戏结果将被记录。",
                        fontSize = 13.sp,
                        color = Color(0xFF8B4513),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 按钮区域 - 横排显示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 取消按钮
                        OutlinedButton(
                            onClick = { showEndGameConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF8B4513)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Color(0xFFFFD0A0)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("继续", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // 确定按钮
                        Button(
                            onClick = {
                                showEndGameConfirmDialog = false
                                showVictoryDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("结束", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
    // 没有玩家提示对话框（通用提示对话框）
    if (showNoPlayersDialog) {
        Dialog(onDismissRequest = { showNoPlayersDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF0)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 提示图标（根据消息内容动态选择）
                    Text(
                        if (dialogMessage.contains("最多")) "⚠️" else "👥",
                        fontSize = 32.sp
                    )
                    
                    // 标题（根据消息内容动态选择）
                    Text(
                        if (dialogMessage.contains("最多")) "玩家已满" else "无法开始游戏",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF8B4513)
                    )
                    
                    // 提示文字（使用动态消息）
                    Text(
                        dialogMessage.ifEmpty { "请先添加玩家\n至少需要2名玩家才能开始游戏" },
                        fontSize = 13.sp,
                        color = Color(0xFF8B4513),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 确定按钮
                    Button(
                        onClick = { 
                            showNoPlayersDialog = false
                            dialogMessage = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("知道了", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ========== 设置阶段界面 ==========
@Composable
fun SetupScreen(
    players: List<com.example.funlife.data.model.Player>,
    onAddPlayer: () -> Unit,
    onDeletePlayer: (com.example.funlife.data.model.Player) -> Unit,
    onStartGame: () -> Unit,
    onShowHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 使用缓存加载背景图片
    val backgroundBitmap = remember {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/jifen.png")
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        backgroundBitmap?.let { bitmap ->
            // 显示背景图片，填充整个屏幕
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            
            // 返回按钮点击区域（左上角）
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
                    .size(60.dp, 60.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onNavigateBack()
                    }
            )
            
            // 在图片上叠加内容
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val screenHeight = maxHeight
                val screenWidth = maxWidth
                
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 数字区域 - 不再显示发光效果
                    Spacer(modifier = Modifier.height(screenHeight * 0.5f))
                    
                    // 按钮区域（调整间距）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight * 0.45f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 按钮1位置: 添加玩家
                        Spacer(modifier = Modifier.height(53.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable { onAddPlayer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "添加玩家",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD2691E),
                                modifier = Modifier.offset(x = 15.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        // 按钮2位置: 开始游戏
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable { 
                                    onStartGame()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "开始游戏",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D3A1A),
                                modifier = Modifier.offset(x = 15.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(35.dp))
                        
                        // 按钮3位置: 查看历史
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable { onShowHistory() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "查看历史",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B6F47),
                                modifier = Modifier.offset(x = 15.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
            }
        } ?: run {
            Text(
                "图片加载失败",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red
            )
        }
    }
}

@Composable
fun VictoryDialog(
    players: List<com.example.funlife.data.model.Player>,
    viewModel: ScoreViewModel,  // 🔥 添加viewModel参数
    onDismiss: () -> Unit,
    onNewGame: () -> Unit,
    onBackToSetup: () -> Unit,
    onRecordVictory: (String, String) -> Unit
) {
    val context = LocalContext.current
    
    // 显示操作详情对话框
    var showOperationDetails by remember { mutableStateOf(false) }
    
    val sortedPlayers = remember(players) {
        players.sortedByDescending { it.score }
    }
    
    val winner = sortedPlayers.firstOrNull()
    
    // 记录胜利
    LaunchedEffect(winner) {
        winner?.let {
            onRecordVictory(it.name, it.avatar)
        }
    }
    
    // 使用缓存加载获胜者头像
    val winnerAvatarBitmap = remember(winner?.avatar) {
        winner?.avatar?.let { avatar ->
            com.example.funlife.utils.ImageCache.loadImage(context, "login/$avatar.png")
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF0)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 庆祝图标
                    Text(
                        "🎉",
                        fontSize = 48.sp
                    )
                    
                    // 标题
                    Text(
                        "游戏结束",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                
                // 获胜者信息
                winner?.let { player ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 获胜者头像
                        winnerAvatarBitmap?.let { bitmap ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFFFFA500)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(6.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = player.name,
                                    modifier = Modifier.size(64.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        
                        // 冠军标识
                        Text(
                            "🏆 冠军 🏆",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        
                        // 获胜者名字
                        Text(
                            player.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B4513)
                        )
                        
                        // 获胜者分数
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "最终得分：",
                                fontSize = 15.sp,
                                color = Color(0xFF8B4513)
                            )
                            Text(
                                "${player.score}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                            Text(
                                "分",
                                fontSize = 15.sp,
                                color = Color(0xFF8B4513)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 按钮区域
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 重新开始按钮
                    Button(
                        onClick = onNewGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            "重新开始",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "重新开始",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // 查看详情按钮
                    OutlinedButton(
                        onClick = { showOperationDetails = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8B4513)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Color(0xFFFFD0A0)
                        )
                    ) {
                        Icon(
                            Icons.Default.Info,
                            "查看详情",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "查看详情",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
                
            // 🔥 右上角关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "关闭",
                    tint = Color(0xFF8B4513),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
        
        // 操作详情对话框
        if (showOperationDetails) {
            OperationDetailsDialog(
                viewModel = viewModel,  // 🔥 传递viewModel
                onDismiss = { showOperationDetails = false }
            )
        }
    }
}

@Composable
fun RefinedSetupPlayerCard(
    player: com.example.funlife.data.model.Player,
    index: Int,
    onDelete: () -> Unit
) {
    // 为每个玩家分配柔和的渐变色
    val gradientColors = remember(index) {
        when (index % 5) {
            0 -> listOf(Color(0xFFFFE8F0), Color(0xFFFFF0F5))  // 柔和粉
            1 -> listOf(Color(0xFFE8E8FF), Color(0xFFF0F0FF))  // 柔和紫
            2 -> listOf(Color(0xFFE0F7F4), Color(0xFFEFFBF9))  // 柔和青
            3 -> listOf(Color(0xFFFFF8E1), Color(0xFFFFFBF0))  // 柔和黄
            else -> listOf(Color(0xFFFFE8E8), Color(0xFFFFF0F0))  // 柔和红
        }
    }
    
    val accentColor = remember(index) {
        when (index % 5) {
            0 -> Color(0xFFFF6B9D)
            1 -> Color(0xFF6C63FF)
            2 -> Color(0xFF4ECDC4)
            3 -> Color(0xFFFFD93D)
            else -> Color(0xFFFF6B6B)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),  // 增加高度以容纳两行文字
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            // 极浅渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 序号徽章 - 精致版
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.8f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    
                    // 玩家信息
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            player.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50),
                            maxLines = 1
                        )
                        Text(
                            "玩家 ${index + 1}",
                            fontSize = 12.sp,
                            color = Color(0xFF95A5A6),
                            maxLines = 1
                        )
                    }
                }
                
                // 删除按钮 - 精致版
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFFFF6B6B).copy(alpha = 0.12f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        "删除",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RefinedEmptyPlayerState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        // 精致图标容器
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("👥", fontSize = 64.sp)
        }
        
        Text(
            "还没有玩家",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2C3E50)
        )
        Text(
            "点击下方按钮添加玩家",
            fontSize = 15.sp,
            color = Color(0xFF95A5A6)
        )
    }
}

// ========== 游戏进行中界面 ==========
@Composable
fun PlayingScreen(
    players: List<com.example.funlife.data.model.Player>,
    onIncreaseScore: (com.example.funlife.data.model.Player, Int) -> Unit,
    onDecreaseScore: (com.example.funlife.data.model.Player, Int) -> Unit,
    onNavigateBack: () -> Unit = {},
    onEndGame: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // 快速加分菜单状态 - 记录当前显示菜单的玩家索引
    var showQuickScoreMenuForPlayer by remember { mutableStateOf<Int?>(null) }
    var selectedQuickScore by remember { mutableStateOf<Int?>(null) }
    
    // 使用缓存加载记分卡背景图片
    val scorecardBitmap = remember {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/jifen_1.png")
    }
    
    // 按添加顺序显示玩家，最多显示4个
    val displayPlayers = remember(players) {
        players.take(4)
    }
    
    // 当前选中的玩家索引
    var selectedPlayerIndex by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 背景图片铺满整个屏幕
        scorecardBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds  // 铺满整个屏幕
            )
        }
        
        // 透明覆盖层 - 用于捕获点击关闭菜单
        if (showQuickScoreMenuForPlayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showQuickScoreMenuForPlayer = null
                        selectedQuickScore = null
                    }
            )
        }
        
        // 在图片上叠加内容
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight
                
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 顶部空白区域 - 对应黄色卡片区域的顶部
                    Spacer(modifier = Modifier.height(screenHeight * 0.44f))
                    
                    // 中间内容区域 - 显示所有玩家（竖排）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight * 0.34f)
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top)
                    ) {
                        displayPlayers.forEachIndexed { index, player ->
                            val avatarBitmap = remember(player.avatar) {
                                com.example.funlife.utils.ImageCache.loadImage(context, "login/${player.avatar}.png")
                            }
                            
                            // 每个玩家的卡片容器 - 用Box包装以支持快速加分菜单
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                // 玩家卡片
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            if (index == selectedPlayerIndex)
                                                Color(0xFFFFF9E6) else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedPlayerIndex = index }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                // 左侧：头像和名字
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(0.4f)
                                ) {
                                    // 头像
                                    avatarBitmap?.let { bitmap ->
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    Color.White,
                                                    CircleShape
                                                )
                                                .border(
                                                    2.dp,
                                                    Color(0xFFE0E0E0),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = player.name,
                                                modifier = Modifier.size(36.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                    
                                    // 玩家名字
                                    Text(
                                        text = player.name,
                                        fontSize = 16.sp,
                                        fontWeight = if (index == selectedPlayerIndex)
                                            FontWeight.Bold else FontWeight.Normal,
                                        color = Color(0xFF8B4513),
                                        maxLines = 1
                                    )
                                }
                                
                                // 右侧：操作区域（带边框的分数显示和操作按钮）
                                Row(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .height(44.dp)
                                        .background(
                                            Color(0xFFFFF4E0),
                                            RoundedCornerShape(22.dp)
                                        )
                                        .border(
                                            2.dp,
                                            Color(0xFFFFD0A0),
                                            RoundedCornerShape(22.dp)
                                        )
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 减分按钮
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                Color.White,
                                                CircleShape
                                            )
                                            .border(
                                                2.dp,
                                                Color(0xFFFFB366),
                                                CircleShape
                                            )
                                            .clickable { 
                                                selectedPlayerIndex = index
                                                onDecreaseScore(player, 1) 
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "−",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B4513),
                                            modifier = Modifier.offset(y = (-2).dp)
                                        )
                                    }
                                    
                                    // 分数显示
                                    Text(
                                        text = "${player.score}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF6B6B)
                                    )
                                    
                                    // 加分按钮（粉色，带爪印）- 支持长按快速加分
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                    ) {
                                        // 径向快速加分菜单 - 扇形展开
                                        if (showQuickScoreMenuForPlayer == index) {
                                            RadialQuickScoreMenu(
                                                onScoreSelected = { score ->
                                                    onIncreaseScore(player, score)
                                                    showQuickScoreMenuForPlayer = null
                                                    selectedQuickScore = null
                                                },
                                                onDismiss = {
                                                    showQuickScoreMenuForPlayer = null
                                                    selectedQuickScore = null
                                                },
                                                selectedScore = selectedQuickScore,
                                                onScoreHovered = { score ->
                                                    selectedQuickScore = score
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                            )
                                        }
                                        
                                        // 爪子按钮
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Color(0xFFFFB3BA),
                                                    CircleShape
                                                )
                                                .pointerInput(player) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            selectedPlayerIndex = index
                                                            onIncreaseScore(player, 1)
                                                        },
                                                        onLongPress = { offset ->
                                                            selectedPlayerIndex = index
                                                            showQuickScoreMenuForPlayer = index
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🐾",
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } // Row结束
                        } // Box结束
                    }
                    
                    // 底部排行榜区域
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight * 0.25f)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 排行榜列表（按分数排序，带动画）
                        val sortedPlayers = remember(players) {
                            players.sortedByDescending { it.score }
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .fillMaxHeight()
                                .padding(top = 50.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            itemsIndexed(
                                items = sortedPlayers.take(3),
                                key = { _, player -> player.id }
                            ) { rankIndex, player ->
                                val avatarBitmap = remember(player.avatar) {
                                    com.example.funlife.utils.ImageCache.loadImage(context, "login/${player.avatar}.png")
                                }
                                
                                // 排行榜玩家卡片 - 带动画，进一步缩小尺寸
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .animateItemPlacement(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                        .background(
                                            when (rankIndex) {
                                                0 -> Color(0xFFFFF9E6)  // 第一名：金色
                                                1 -> Color(0xFFF5F5F5)  // 第二名：银色
                                                2 -> Color(0xFFFFE8D6)  // 第三名：铜色
                                                else -> Color.White
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            when (rankIndex) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFFC0C0C0)
                                                2 -> Color(0xFFCD7F32)
                                                else -> Color(0xFFE0E0E0)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 排名徽章
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(
                                                    when (rankIndex) {
                                                        0 -> Color(0xFFFFD700)
                                                        1 -> Color(0xFFC0C0C0)
                                                        2 -> Color(0xFFCD7F32)
                                                        else -> Color(0xFF6A6BFF)
                                                    },
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (rankIndex) {
                                                    0 -> "🥇"
                                                    1 -> "🥈"
                                                    2 -> "🥉"
                                                    else -> "${rankIndex + 1}"
                                                },
                                                fontSize = if (rankIndex < 3) 12.sp else 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        // 头像
                                        avatarBitmap?.let { bitmap ->
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color.White, CircleShape)
                                                    .border(1.dp, Color(0xFFE0E0E0), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = player.name,
                                                    modifier = Modifier.size(18.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                        
                                        // 玩家名字
                                        Text(
                                            text = player.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (rankIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                            color = Color(0xFF8B4513),
                                            maxLines = 1
                                        )
                                    }
                                    
                                    // 分数
                                    Text(
                                        text = "${player.score}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (rankIndex) {
                                            0 -> Color(0xFFFFD700)
                                            1 -> Color(0xFFC0C0C0)
                                            2 -> Color(0xFFCD7F32)
                                            else -> Color(0xFF6A6BFF)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        
        // 返回按钮（左上角，可见）
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "返回",
                tint = Color(0xFF2C3E50),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // 结束按钮（右上角，可见）
        IconButton(
            onClick = onEndGame,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 12.dp)
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.Stop,
                "结束",
                tint = Color(0xFF2C3E50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RefinedPlayingPlayerCard(
    player: com.example.funlife.data.model.Player,
    rank: Int,
    totalPlayers: Int,
    leadingScore: Int,
    onIncreaseScore: (com.example.funlife.data.model.Player, Int) -> Unit,
    onDecreaseScore: (com.example.funlife.data.model.Player, Int) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    
    // 根据排名选择柔和渐变色 - 精致配色
    val cardGradient = when (rank) {
        1 -> listOf(Color(0xFFFFF8E6), Color(0xFFFFF1CC))  // 香槟金
        2 -> listOf(Color(0xFFF6F7FB), Color(0xFFECEEFA))  // 银灰紫
        3 -> listOf(Color(0xFFFFF5EE), Color(0xFFFCEBDD))  // 蜜桃米
        else -> listOf(Color(0xFFF2FBFA), Color(0xFFEAF7F6))  // 薄荷蓝
    }
    
    val rankBadgeGradient = when (rank) {
        1 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))  // 金色
        2 -> listOf(Color(0xFFC0C0C0), Color(0xFFA8A8A8))  // 银色
        3 -> listOf(Color(0xFFCD7F32), Color(0xFFB8860B))  // 铜色
        else -> listOf(Color(0xFF6A6BFF), Color(0xFF7C5CFF))  // 蓝紫
    }
    
    val scoreColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF6A6BFF)
    }
    
    // 计算与领先者的差距
    val scoreDiff = leadingScore - player.score
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)  // 紧凑高度
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)  // 扁平化
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 柔和渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(cardGradient),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 第一层：玩家信息区 (约60dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 排名徽章 - 精致立体版 (48dp)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(rankBadgeGradient),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rank <= 3) {
                                Text(
                                    getRankEmoji(rank),
                                    fontSize = 26.sp
                                )
                            } else {
                                Text(
                                    "#$rank",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        // 玩家名称
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                player.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C3E50),
                                maxLines = 1
                            )
                            // 状态标签
                            if (rank == 1) {
                                Text(
                                    "当前领先",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF6B9D),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            } else if (scoreDiff > 0) {
                                Text(
                                    "落后 $scoreDiff 分",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7F8C8D),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    // 当前分数
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            "${player.score}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            "分",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
                
                // 第三层：操作按钮区 (52dp高度)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // -1 按钮 - 白底红边
                    OutlinedButton(
                        onClick = { 
                            onDecreaseScore(player, 1)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFFFF6B6B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Color(0xFFFF6B6B)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            "-1",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // +1 按钮 - 粉紫渐变
                    Button(
                        onClick = { 
                            onIncreaseScore(player, 1)
                            scale = 0.96f
                            GlobalScope.launch {
                                delay(180)
                                scale = 1f
                            }
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF6FAE),
                                            Color(0xFFFF5F9E)
                                        )
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+1",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // +5 按钮 - 蓝紫渐变
                    Button(
                        onClick = { 
                            onIncreaseScore(player, 5)
                            scale = 0.96f
                            GlobalScope.launch {
                                delay(180)
                                scale = 1f
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF6A6BFF),
                                            Color(0xFF7C5CFF)
                                        )
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+5",
                                fontSize = 20.sp,
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

// ========== 对话框 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    usedAvatars: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // 可用头像列表
    val avatars = listOf("tx_1", "tx_2", "tx_3", "tx_4", "tx_5", "tx_6")
    
    Dialog(onDismissRequest = onDismiss) {
        // 粉色圆角面板 - 更宽的面板
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)  // 减少左右边距，让面板更宽
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF0F0)  // 浅粉色背景
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 输入框
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { 
                            Text(
                                "输入玩家名字",
                                color = Color(0xFFE8A5A5),
                                fontSize = 16.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFFFAF5),
                            unfocusedContainerColor = Color(0xFFFFFAF5),
                            focusedBorderColor = Color(0xFFFFB8B8),
                            unfocusedBorderColor = Color(0xFFFFD0D0)
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFF8B4513)
                        )
                    )
                    
                    // "选择头像" 提示文字
                    Text(
                        text = "选择头像",
                        fontSize = 16.sp,
                        color = Color(0xFFE8A5A5),
                        fontWeight = FontWeight.Medium
                    )
                    
                    // 头像选择网格 (2行3列)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (row in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (col in 0..2) {
                                    val index = row * 3 + col
                                    if (index < avatars.size) {
                                        val avatar = avatars[index]
                                        val isUsed = usedAvatars.contains(avatar)
                                        val isSelected = selectedAvatar == avatar
                                        
                                        AvatarItem(
                                            avatar = avatar,
                                            isSelected = isSelected,
                                            isUsed = isUsed,
                                            onClick = {
                                                if (!isUsed) {
                                                    selectedAvatar = avatar
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 确定和取消按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD0D0)
                            )
                        ) {
                            Text(
                                "取消",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // 确定按钮
                        Button(
                            onClick = {
                                if (name.isNotBlank() && selectedAvatar != null && !isSubmitting) {
                                    isSubmitting = true
                                    onConfirm(name.trim(), selectedAvatar!!)
                                }
                            },
                            enabled = name.isNotBlank() && selectedAvatar != null && !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9999),
                                disabledContainerColor = Color(0xFFFFD0D0)
                            )
                        ) {
                            Text(
                                if (isSubmitting) "添加中..." else "确定",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarItem(
    avatar: String,
    isSelected: Boolean,
    isUsed: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // 使用缓存加载头像图片
    val avatarBitmap = remember(avatar) {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/$avatar.png")
    }
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable(
                enabled = !isUsed,
                indication = null,  // 移除点击波纹效果
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 背景圆圈
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = when {
                        isSelected -> Color(0xFFFFB8B8)  // 选中：粉色
                        isUsed -> Color(0xFFE0E0E0)      // 已使用：灰色
                        else -> Color(0xFFFFFAF5)        // 未选中：浅色
                    },
                    shape = CircleShape
                )
                .border(
                    width = if (isSelected) 3.dp else 2.dp,
                    color = when {
                        isSelected -> Color(0xFFFF9999)  // 选中：深粉色边框
                        isUsed -> Color(0xFFBDBDBD)      // 已使用：灰色边框
                        else -> Color(0xFFFFD0D0)        // 未选中：浅粉色边框
                    },
                    shape = CircleShape
                )
        )
        
        // 头像图片
        avatarBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = avatar,
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (isUsed) Modifier.alpha(0.3f) else Modifier
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ReusePreviousPlayersDialog(
    playerCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF0)
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标
                Text("👥", fontSize = 32.sp)
                
                // 标题
                Text(
                    "发现已有玩家",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF8B4513)
                )
                
                // 提示文字
                Text(
                    "检测到上次游戏有 $playerCount 名玩家\n是否使用这些玩家继续游戏？",
                    fontSize = 13.sp,
                    color = Color(0xFF8B4513),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 按钮区域 - 横排显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 否按钮
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8B4513)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color(0xFFFFD0A0)
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("否", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // 是按钮
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("是", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getRankEmoji(rank: Int): String {
    return when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "🏅"
    }
}

// ========== 添加玩家成功横幅 - 从顶部滑下并持续显示 ==========
@Composable
fun PlayerAddedBanner(
    playerCount: Int,
    modifier: Modifier = Modifier
) {
    // 入场动画 - 从顶部滑下
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)  // 短暂延迟后开始动画
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { -it },  // 从顶部滑入
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },  // 向顶部滑出
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 成功图标
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // 提示文字
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "已添加 $playerCount 名玩家",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "继续添加或开始游戏",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

// ========== 胜利历史记录弹框 ==========
@Composable
fun VictoryHistoryDialog(
    victoryRecords: List<com.example.funlife.data.model.PlayerVictoryRecord>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    
    // 使用缓存加载背景图片
    val backgroundBitmap = remember {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/jifen_2.png")
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            backgroundBitmap?.let { bitmap ->
                // 背景图片
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                
                // 返回箭头点击区域（左上角）
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 20.dp)
                        .size(80.dp, 80.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onDismiss()
                        }
                )
                
                // 内容区域 - 显示在米色区域
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val screenHeight = maxHeight
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = screenHeight * 0.25f, bottom = screenHeight * 0.15f)
                            .padding(horizontal = 40.dp)
                    ) {
                        // 关闭按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "关闭",
                                    tint = Color(0xFF8B4513),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 记录列表
                        if (victoryRecords.isEmpty()) {
                            // 空状态
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "📊",
                                        fontSize = 48.sp
                                    )
                                    Text(
                                        "暂无胜利记录",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B4513)
                                    )
                                    Text(
                                        "开始游戏并获得胜利吧！",
                                        fontSize = 14.sp,
                                        color = Color(0xFFA0826D)
                                    )
                                }
                            }
                        } else {
                            // 有记录时显示列表
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(victoryRecords) { index, record ->
                                    VictoryRecordCard(record = record, rank = index + 1)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 清空历史按钮
                            Button(
                                onClick = onClearHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6B6B)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "清空历史",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "清空历史",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } ?: run {
                // 图片加载失败时的备用UI
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E6)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "胜利历史",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B4513)
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    "关闭",
                                    tint = Color(0xFF8B4513)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (victoryRecords.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无胜利记录",
                                    fontSize = 18.sp,
                                    color = Color(0xFF8B4513)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(victoryRecords) { index, record ->
                                    VictoryRecordCard(record = record, rank = index + 1)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = onClearHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6B6B)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "清空历史"
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("清空历史", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VictoryRecordCard(
    record: com.example.funlife.data.model.PlayerVictoryRecord,
    rank: Int
) {
    val context = LocalContext.current
    
    // 使用缓存加载玩家头像
    val avatarBitmap = remember(record.avatar) {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/${record.avatar}.png")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                avatarBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, Color(0xFFE0E0E0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = record.playerName,
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                // 玩家名字
                Text(
                    text = record.playerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8B4513),
                    maxLines = 1
                )
            }
            
            // 胜利次数
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${record.victoryCount}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
                Text(
                    text = "胜利",
                    fontSize = 14.sp,
                    color = Color(0xFF8B4513).copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ========== 清空历史确认对话框 ==========
@Composable
fun ClearHistoryConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF8E6)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 警告图标
                Text(
                    "⚠️",
                    fontSize = 48.sp
                )
                
                // 标题
                Text(
                    "确认清空历史记录",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513),
                    textAlign = TextAlign.Center
                )
                
                // 提示信息
                Text(
                    "确定要清空所有胜利记录吗？\n此操作不可恢复",
                    fontSize = 15.sp,
                    color = Color(0xFFA0826D),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(2.dp, Color(0xFFD4A574)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8B4513)
                        )
                    ) {
                        Text(
                            "取消",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // 确认按钮
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B)
                        )
                    ) {
                        Text(
                            "确认清空",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ========== 径向快速加分菜单（扇形展开，像花瓣）==========
@Composable
fun RadialQuickScoreMenu(
    onScoreSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    selectedScore: Int?,
    onScoreHovered: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreOptions = listOf(2, 4, 6)
    var hoveredScore by remember { mutableStateOf<Int?>(null) }
    
    // 展开动画
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    Box(
        modifier = modifier
            .size(120.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // 点击空白处（不在按钮上）则取消菜单
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                        
                        // 如果点击距离中心太近（在按钮范围内），不处理
                        // 否则取消菜单
                        if (distance > 60) {
                            onDismiss()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // 根据触摸位置计算选中的选项
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        
                        // 根据角度判断选中哪个选项
                        val normalizedAngle = (angle + 360) % 360
                        hoveredScore = when {
                            normalizedAngle >= 315 || normalizedAngle < 45 -> 6  // 右侧
                            normalizedAngle >= 45 && normalizedAngle < 135 -> 4   // 下方
                            normalizedAngle >= 135 && normalizedAngle < 225 -> 2  // 左侧
                            else -> null
                        }
                        onScoreHovered(hoveredScore)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY
                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        
                        val normalizedAngle = (angle + 360) % 360
                        hoveredScore = when {
                            normalizedAngle >= 315 || normalizedAngle < 45 -> 6
                            normalizedAngle >= 45 && normalizedAngle < 135 -> 4
                            normalizedAngle >= 135 && normalizedAngle < 225 -> 2
                            else -> null
                        }
                        onScoreHovered(hoveredScore)
                    },
                    onDragEnd = {
                        hoveredScore?.let { score ->
                            onScoreSelected(score)
                        } ?: onDismiss()
                    },
                    onDragCancel = {
                        onDismiss()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 绘制3个选项按钮，呈扇形分布
        scoreOptions.forEachIndexed { index, score ->
            val isSelected = selectedScore == score || hoveredScore == score
            
            // 计算每个按钮的位置（扇形分布）
            // +2在左上，+4在正上方，+6在右上
            val angle = when(index) {
                0 -> 135f  // +2 左上
                1 -> 90f   // +4 正上
                2 -> 45f   // +6 右上
                else -> 90f
            }
            
            val radius = 50.dp * animatedProgress.value
            val angleRad = Math.toRadians(angle.toDouble())
            val offsetX = (radius.value * cos(angleRad)).dp
            val offsetY = (radius.value * sin(angleRad)).dp
            
            // 按钮大小动画
            val buttonSize = if (isSelected) 42.dp else 38.dp
            val buttonScale = animatedProgress.value
            
            Surface(
                modifier = Modifier
                    .offset(x = offsetX, y = -offsetY)  // y取负值，因为Canvas坐标系向下为正
                    .size(buttonSize * buttonScale)
                    .graphicsLayer {
                        scaleX = if (isSelected) 1.1f else 1f
                        scaleY = if (isSelected) 1.1f else 1f
                    }
                    .pointerInput(score) {
                        detectTapGestures(
                            onPress = {
                                hoveredScore = score
                                onScoreHovered(score)
                                tryAwaitRelease()
                                onScoreSelected(score)
                            }
                        )
                    },
                shape = CircleShape,
                color = if (isSelected) Color(0xFFFF6B9D) else Color(0xFFFFB3BA),
                shadowElevation = if (isSelected) 6.dp else 3.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "+$score",
                        fontSize = if (isSelected) 18.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}


// ========== 操作详情对话框 ==========
@Composable
fun OperationDetailsDialog(
    viewModel: ScoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // 🔥 直接从数据库查询，不依赖Flow
    var allOperations by remember { mutableStateOf<List<com.example.funlife.data.model.ScoreOperation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val sessionId = viewModel.getCurrentSessionId()
    
    // 🔥 当前选中的玩家ID（null表示显示全部）
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }
    
    // 🔥 清空确认对话框状态
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var clearTargetPlayerName by remember { mutableStateOf<String?>(null) }
    
    // 🔥 修复：使用一次性查询而不是collect
    LaunchedEffect(sessionId) {
        android.util.Log.d("OperationDetailsDialog", "=== DIALOG OPENED ===")
        android.util.Log.d("OperationDetailsDialog", "Current session ID: $sessionId")
        
        isLoading = true
        try {
            val db = com.example.funlife.data.database.AppDatabase.getDatabase(context)
            val dao = db.scoreOperationDao()
            
            // 🔥 使用协程一次性查询，不使用collect
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // 查询所有会话的操作（包括历史会话）
                val ops = dao.getAllOperations()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    allOperations = ops
                    isLoading = false
                    android.util.Log.d("OperationDetailsDialog", "Loaded ${ops.size} operations from database")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OperationDetailsDialog", "Error loading operations", e)
            isLoading = false
        }
    }
    
    // 🔥 根据选中的玩家过滤操作
    val displayedOperations = remember(allOperations, selectedPlayerId) {
        if (selectedPlayerId == null) {
            allOperations
        } else {
            allOperations.filter { it.playerId == selectedPlayerId }
        }
    }
    
    // 🔥 获取所有参与的玩家（去重）并缓存每个玩家的操作数量
    val playersWithCounts = remember(allOperations) {
        allOperations
            .groupBy { Triple(it.playerId, it.playerName, it.playerAvatar) }
            .map { (player, ops) -> 
                Pair(player, ops.size)
            }
            .sortedByDescending { it.second }  // 按操作数量排序
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFFBF0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 操作详情",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 清空按钮
                        if (displayedOperations.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    clearTargetPlayerName = if (selectedPlayerId == null) {
                                        null  // 清空全部
                                    } else {
                                        playersWithCounts.find { it.first.first == selectedPlayerId }?.first?.second
                                    }
                                    showClearConfirmDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "清空记录",
                                    tint = Color(0xFFFF6B6B)
                                )
                            }
                        }
                        
                        // 关闭按钮
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                "关闭",
                                tint = Color(0xFF8B4513)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 🔥 玩家标签页
                if (playersWithCounts.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = if (selectedPlayerId == null) 0 else playersWithCounts.indexOfFirst { it.first.first == selectedPlayerId } + 1,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF8B4513),
                        edgePadding = 0.dp
                    ) {
                        // "全部"标签
                        Tab(
                            selected = selectedPlayerId == null,
                            onClick = { selectedPlayerId = null },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    "全部",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedPlayerId == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPlayerId == null) Color(0xFF8B4513) else Color.Gray
                                )
                                Text(
                                    "${allOperations.size}",
                                    fontSize = 12.sp,
                                    color = if (selectedPlayerId == null) Color(0xFFFF6B6B) else Color.Gray
                                )
                            }
                        }
                        
                        // 每个玩家的标签（使用缓存的数据）
                        playersWithCounts.forEach { (player, opCount) ->
                            val (playerId, playerName, playerAvatar) = player
                            val avatarBitmap = remember(playerAvatar) {
                                com.example.funlife.utils.ImageCache.loadImage(context, "login/$playerAvatar.png")
                            }
                            
                            Tab(
                                selected = selectedPlayerId == playerId,
                                onClick = { selectedPlayerId = playerId },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    // 玩家头像
                                    avatarBitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = playerName,
                                            modifier = Modifier.size(32.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 玩家名字
                                    Text(
                                        playerName,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedPlayerId == playerId) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedPlayerId == playerId) Color(0xFF8B4513) else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // 操作数量（使用缓存的值）
                                    Text(
                                        "${opCount}次",
                                        fontSize = 11.sp,
                                        color = if (selectedPlayerId == playerId) Color(0xFFFF6B6B) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 操作记录列表
                if (isLoading) {
                    // 加载中状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF8B4513)
                            )
                            Text(
                                text = "加载中...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else if (displayedOperations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🎮",
                                fontSize = 48.sp
                            )
                            Text(
                                text = if (selectedPlayerId == null) "暂无操作记录" else "该玩家暂无操作",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = if (selectedPlayerId == null) "开始游戏后，每次加分减分都会记录在这里" else "切换到其他标签查看更多",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // 🔥 按游戏会话ID分组，并按每组的最早时间戳降序排序（最新的在前）
                    val groupedOperations = remember(displayedOperations) {
                        displayedOperations
                            .groupBy { it.gameSessionId }
                            .toList()
                            .sortedByDescending { (_, operations) -> 
                                operations.minOfOrNull { it.timestamp } ?: 0L  // 按每局最早的操作时间降序排序
                            }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedOperations.forEachIndexed { gameIndex, (sessionId, operations) ->
                            // 游戏局数标题（倒序显示局数：最新的是最大的局数）
                            item {
                                GameSessionHeader(
                                    gameNumber = groupedOperations.size - gameIndex,  // 🔥 倒序计算局数
                                    operationCount = operations.size
                                )
                            }
                            
                            // 该局的所有操作
                            itemsIndexed(operations) { index, operation ->
                                OperationRecordCard(operation, index + 1, context)
                            }
                            
                            // 局与局之间的分隔线
                            if (gameIndex < groupedOperations.size - 1) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 🔥 清空记录确认对话框
    if (showClearConfirmDialog) {
        Dialog(onDismissRequest = { showClearConfirmDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF0)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 警告图标
                    Text("⚠️", fontSize = 32.sp)
                    
                    // 标题
                    Text(
                        "确认清空记录",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF8B4513)
                    )
                    
                    // 提示文字
                    Text(
                        if (clearTargetPlayerName == null) {
                            "确定要清空所有玩家的操作记录吗？\n此操作不可恢复！"
                        } else {
                            "确定要清空 $clearTargetPlayerName 的操作记录吗？\n此操作不可恢复！"
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF8B4513),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 取消按钮
                        OutlinedButton(
                            onClick = { 
                                showClearConfirmDialog = false
                                clearTargetPlayerName = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF8B4513)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Color(0xFFFFD0A0)
                            )
                        ) {
                            Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // 确定按钮
                        Button(
                            onClick = {
                                if (clearTargetPlayerName == null) {
                                    // 清空所有记录
                                    viewModel.clearAllOperations()
                                } else {
                                    // 清空指定玩家的记录
                                    selectedPlayerId?.let { playerId ->
                                        viewModel.clearPlayerOperations(playerId)
                                    }
                                }
                                showClearConfirmDialog = false
                                clearTargetPlayerName = null
                                // 重新加载数据
                                isLoading = true
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(300)
                                    val db = com.example.funlife.data.database.AppDatabase.getDatabase(context)
                                    val dao = db.scoreOperationDao()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val ops = dao.getAllOperations()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            allOperations = ops
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B)
                            )
                        ) {
                            Text("清空", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ========== 游戏局数标题 ==========
@Composable
fun GameSessionHeader(
    gameNumber: Int,
    operationCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF8E1),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 游戏图标
                Text(
                    text = "🎯",
                    fontSize = 20.sp
                )
                
                // 局数文字
                Text(
                    text = "第 $gameNumber 局",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513)
                )
            }
            
            // 操作次数
            Text(
                text = "$operationCount 次操作",
                fontSize = 13.sp,
                color = Color(0xFF8B4513).copy(alpha = 0.7f)
            )
        }
    }
}

// ========== 操作记录卡片 ==========
@Composable
fun OperationRecordCard(
    operation: com.example.funlife.data.model.ScoreOperation,
    sequenceNumber: Int,
    context: Context
) {
    val avatarBitmap = remember(operation.playerAvatar) {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/${operation.playerAvatar}.png")
    }
    
    // 根据操作类型选择颜色
    val operationColor = if (operation.operation > 0) {
        Color(0xFF4CAF50)  // 绿色表示加分
    } else {
        Color(0xFFFF6B6B)  // 红色表示减分
    }
    
    val operationText = if (operation.operation > 0) {
        "+${operation.operation}"
    } else {
        "${operation.operation}"
    }
    
    // 格式化时间
    val timeText = remember(operation.timestamp) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        sdf.format(java.util.Date(operation.timestamp))
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                text = "#$sequenceNumber",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(40.dp)
            )
            
            // 玩家头像和名字
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                avatarBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFFE0E0E0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = operation.playerName,
                            modifier = Modifier.size(26.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                Text(
                    text = operation.playerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
            }
            
            // 操作和分数变化
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = operationText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = operationColor
                )
                Text(
                    text = "${operation.scoreBefore} → ${operation.scoreAfter}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // 时间
            Text(
                text = timeText,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
