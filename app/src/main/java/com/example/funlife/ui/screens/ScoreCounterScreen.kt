// ScoreCounterScreen.kt - 精致游戏计分屏幕
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.ScoreViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 游戏状态
enum class GameState {
    SETUP,      // 设置阶段（添加玩家）
    PLAYING,    // 游戏中
    FINISHED    // 游戏结束
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCounterScreen(
    viewModel: ScoreViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val players by viewModel.players.collectAsState()
    var gameState by remember { mutableStateOf(GameState.SETUP) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEndGameDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            // 精致顶部导航栏 - 72dp高度，半透明磨砂背景
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 游戏图标容器 - 精致版
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B9D).copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎮", fontSize = 20.sp)
                        }
                        
                        Text(
                            when (gameState) {
                                GameState.SETUP -> "游戏设置"
                                GameState.PLAYING -> "游戏进行中"
                                GameState.FINISHED -> "游戏结束"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2C3E50)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .background(
                                Color(0xFFF5F5F5).copy(alpha = 0.8f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            "返回",
                            tint = Color(0xFF2C3E50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    // 游戏中显示结束按钮
                    if (gameState == GameState.PLAYING) {
                        Button(
                            onClick = { showEndGameDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B6B).copy(alpha = 0.12f),
                                contentColor = Color(0xFFFF6B6B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop, 
                                "结束", 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("结束", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.78f)  // 半透明磨砂效果
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (gameState) {
                GameState.SETUP -> {
                    SetupScreen(
                        players = players,
                        onAddPlayer = { showAddDialog = true },
                        onDeletePlayer = { viewModel.deletePlayer(it) },
                        onStartGame = { 
                            if (players.size >= 2) {
                                gameState = GameState.PLAYING
                            }
                        }
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
                        }
                    )
                }
                GameState.FINISHED -> {
                    FinishedScreen(
                        players = players,
                        onNewGame = {
                            viewModel.resetAllScores()
                            gameState = GameState.SETUP
                        },
                        onBackToSetup = {
                            gameState = GameState.SETUP
                        }
                    )
                }
            }
        }
    }
    
    // 添加玩家对话框
    if (showAddDialog) {
        AddPlayerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addPlayer(name)
                showAddDialog = false
            }
        )
    }
    
    // 结束游戏确认对话框
    if (showEndGameDialog) {
        EndGameDialog(
            onDismiss = { showEndGameDialog = false },
            onConfirm = {
                gameState = GameState.FINISHED
                showEndGameDialog = false
            }
        )
    }
}

// ========== 设置阶段界面 ==========
@Composable
fun SetupScreen(
    players: List<com.example.funlife.data.model.Player>,
    onAddPlayer: () -> Unit,
    onDeletePlayer: (com.example.funlife.data.model.Player) -> Unit,
    onStartGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8FA),  // #FFF8FA
                        Color(0xFFFDF5F7),  // #FDF5F7
                        Color(0xFFFAF3F6)   // #FAF3F6
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 提示卡片 - 精致紧凑版
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box {
                    // 柔和渐变装饰背景
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFE8B5).copy(alpha = 0.25f),
                                        Color(0xFFF7C6E8).copy(alpha = 0.25f),
                                        Color(0xFFDCCBFF).copy(alpha = 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                    
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 灯泡图标 - 精致版
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 28.sp)
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "添加玩家",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C3E50)
                            )
                            Text(
                                "至少需要2名玩家才能开始游戏",
                                fontSize = 14.sp,
                                color = Color(0xFF7F8C8D)
                            )
                        }
                    }
                }
            }
            
            // 玩家列表
            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    RefinedEmptyPlayerState()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(players) { index, player ->
                        RefinedSetupPlayerCard(
                            player = player,
                            index = index,
                            onDelete = { onDeletePlayer(player) }
                        )
                    }
                }
            }
            
            // 底部按钮区 - 精致紧凑版
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 添加玩家按钮 - 青绿色
                Button(
                    onClick = onAddPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(22.dp),
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
                                        Color(0xFF4FD1C5),
                                        Color(0xFF38B2AC)
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                "添加",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                            Text(
                                "添加玩家", 
                                fontSize = 20.sp, 
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // 开始游戏按钮 - 紫粉渐变
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    enabled = players.size >= 2,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (players.size >= 2) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF5FA2),
                                            Color(0xFF6C63FF)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE0E0E0),
                                            Color(0xFFBDBDBD)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow, 
                                "开始",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Text(
                                if (players.size >= 2) "开始游戏" else "需要至少2名玩家",
                                fontSize = 22.sp,
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
    onDecreaseScore: (com.example.funlife.data.model.Player, Int) -> Unit
) {
    // 按分数排序
    val sortedPlayers = remember(players) {
        players.sortedByDescending { it.score }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8FA),  // #FFF8FA
                        Color(0xFFFDF5F7),  // #FDF5F7
                        Color(0xFFFAF3F6)   // #FAF3F6
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 排行榜标题卡片 - 精致紧凑版 (96dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(96.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 柔和渐变背景
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFE8B5).copy(alpha = 0.25f),
                                        Color(0xFFF7C6E8).copy(alpha = 0.25f),
                                        Color(0xFFDCCBFF).copy(alpha = 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 奖杯图标 - 精致版
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFFFFA500)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏆", fontSize = 32.sp)
                            }
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "实时排行榜",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2C3E50)
                                )
                                Text(
                                    "${players.size}位玩家",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7F8C8D)
                                )
                            }
                        }
                        
                        // 显示领先者和最高分
                        if (sortedPlayers.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "🔥 领先",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF6B6B),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${sortedPlayers.first().score}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B9D)
                                )
                            }
                        }
                    }
                }
            }
            
            // 玩家计分卡片列表 - 紧凑间距
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(sortedPlayers) { index, player ->
                    RefinedPlayingPlayerCard(
                        player = player,
                        rank = index + 1,
                        totalPlayers = sortedPlayers.size,
                        leadingScore = sortedPlayers.first().score,
                        onIncreaseScore = onIncreaseScore,
                        onDecreaseScore = onDecreaseScore
                    )
                }
            }
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

// ========== 游戏结束界面 ==========
@Composable
fun FinishedScreen(
    players: List<com.example.funlife.data.model.Player>,
    onNewGame: () -> Unit,
    onBackToSetup: () -> Unit
) {
    val sortedPlayers = remember(players) {
        players.sortedByDescending { it.score }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        
        // 冠军庆祝
        if (sortedPlayers.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🎉", fontSize = 60.sp)
                Text(
                    "游戏结束",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏆", fontSize = 48.sp)
                        Text(
                            "冠军",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            sortedPlayers.first().name,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${sortedPlayers.first().score} 分",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // 最终排名
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "最终排名",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                sortedPlayers.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                getRankEmoji(index + 1),
                                fontSize = 28.sp
                            )
                            Text(
                                player.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            "${player.score} 分",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (index < sortedPlayers.size - 1) {
                        Divider()
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // 底部按钮
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, "新游戏")
                Spacer(Modifier.width(8.dp))
                Text("开始新游戏", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            OutlinedButton(
                onClick = onBackToSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Settings, "设置")
                Spacer(Modifier.width(8.dp))
                Text("返回设置", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ========== 对话框 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎮", fontSize = 24.sp)
                Text("添加玩家", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("玩家名称") },
                placeholder = { Text("例如：小明") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EndGameDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("🏁", fontSize = 48.sp)
        },
        title = { 
            Text("结束游戏", fontWeight = FontWeight.Bold)
        },
        text = { 
            Text("确定要结束当前游戏吗？\n将显示最终排名。")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun getRankEmoji(rank: Int): String {
    return when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "🏅"
    }
}
