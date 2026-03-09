// ScoreCounterScreen.kt - 游戏计分屏幕（美化版）
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.components.PlayerCard
import com.example.funlife.viewmodel.ScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCounterScreen(
    viewModel: ScoreViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val players by viewModel.players.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    
    // 计算统计数据
    val totalPlayers = players.size
    val totalScore = players.sumOf { it.score }
    val highestScore = players.maxOfOrNull { it.score } ?: 0
    val averageScore = if (totalPlayers > 0) totalScore / totalPlayers else 0
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "游戏计分",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 统计按钮
                if (players.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = { showStatsDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "统计")
                    }
                }
                
                // 重置按钮
                if (players.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = { showResetDialog = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "重置")
                    }
                }
                
                // 添加玩家按钮
                ExtendedFloatingActionButton(
                    onClick = { showDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "添加") },
                    text = { Text("添加玩家") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 顶部统计卡片
            if (players.isNotEmpty()) {
                StatsHeader(
                    totalPlayers = totalPlayers,
                    totalScore = totalScore,
                    highestScore = highestScore,
                    averageScore = averageScore
                )
            }
            
            // 玩家列表
            if (players.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = players,
                        key = { _, player -> player.id }
                    ) { index, player ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            PlayerCard(
                                player = player,
                                rank = index + 1,
                                onIncrease = { 
                                    viewModel.increaseScore(player)
                                },
                                onDecrease = { 
                                    viewModel.decreaseScore(player)
                                },
                                onDelete = { 
                                    viewModel.deletePlayer(player)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 添加玩家对话框
    if (showDialog) {
        AddPlayerDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                viewModel.addPlayer(name)
                showDialog = false
            }
        )
    }
    
    // 重置确认对话框
    if (showResetDialog) {
        ResetConfirmDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.resetAllScores()
                showResetDialog = false
            }
        )
    }
    
    // 统计对话框
    if (showStatsDialog) {
        StatsDialog(
            players = players,
            totalScore = totalScore,
            averageScore = averageScore,
            onDismiss = { showStatsDialog = false }
        )
    }
}

@Composable
fun StatsHeader(
    totalPlayers: Int,
    totalScore: Int,
    highestScore: Int,
    averageScore: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 装饰性背景
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = "👥",
                    label = "玩家",
                    value = totalPlayers.toString()
                )
                StatItem(
                    icon = "🎯",
                    label = "总分",
                    value = totalScore.toString()
                )
                StatItem(
                    icon = "🏆",
                    label = "最高",
                    value = highestScore.toString()
                )
                StatItem(
                    icon = "📊",
                    label = "平均",
                    value = averageScore.toString()
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: String,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        // 动画效果的奖杯
        var scale by remember { mutableStateOf(1f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                ) { value, _ -> scale = value }
                animate(
                    initialValue = 1.2f,
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                ) { value, _ -> scale = value }
            }
        }
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🏆",
                fontSize = (60 * scale).sp
            )
        }
        
        Text(
            text = "还没有玩家",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "点击下方按钮添加第一个玩家\n开始精彩的游戏计分吧！",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
                Text(
                    "添加玩家",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        "💡 输入玩家的名称或昵称",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("玩家名称") },
                    placeholder = { Text("例如：小明") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        title = { 
            Text(
                "重置分数",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text(
                "确定要重置所有玩家的分数吗？\n此操作无法撤销。",
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确定重置")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsDialog(
    players: List<com.example.funlife.data.model.Player>,
    totalScore: Int,
    averageScore: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 24.sp)
                Text(
                    "游戏统计",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 总体统计
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "总体数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        StatRow("参与玩家", "${players.size} 人")
                        StatRow("总分数", "$totalScore 分")
                        StatRow("平均分", "$averageScore 分")
                        StatRow("最高分", "${players.maxOfOrNull { it.score } ?: 0} 分")
                        StatRow("最低分", "${players.minOfOrNull { it.score } ?: 0} 分")
                    }
                }
                
                // 排名详情
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "排名详情",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        players.forEachIndexed { index, player ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        getRankEmoji(index + 1),
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        player.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    "${player.score} 分",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("关闭")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
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
