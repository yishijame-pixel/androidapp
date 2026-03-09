// HistoryScreen.kt - 历史记录屏幕
package com.example.funlife.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.GameHistory
import com.example.funlife.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val history by viewModel.recentHistory.collectAsState()
    var selectedFilter by remember { mutableStateOf("all") }
    var showClearDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "历史记录",
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
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空历史")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 过滤器
            ScrollableTabRow(
                selectedTabIndex = when (selectedFilter) {
                    "all" -> 0
                    "score_counter" -> 1
                    "spin_wheel" -> 2
                    else -> 0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    text = { Text("全部") }
                )
                Tab(
                    selected = selectedFilter == "score_counter",
                    onClick = { selectedFilter = "score_counter" },
                    text = { Text("游戏计分") }
                )
                Tab(
                    selected = selectedFilter == "spin_wheel",
                    onClick = { selectedFilter = "spin_wheel" },
                    text = { Text("幸运转盘") }
                )
            }
            
            // 历史列表
            val filteredHistory = if (selectedFilter == "all") {
                history
            } else {
                history.filter { it.gameType == selectedFilter }
            }
            
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "暂无历史记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        HistoryCard(
                            history = item,
                            onDelete = { viewModel.deleteHistory(item) }
                        )
                    }
                }
            }
        }
    }
    
    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空历史记录") },
            text = { Text("确定要清空所有历史记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun HistoryCard(
    history: GameHistory,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (history.gameType) {
                        "score_counter" -> Icons.Default.EmojiEvents
                        "spin_wheel" -> Icons.Outlined.Casino
                        else -> Icons.Outlined.History
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        text = history.playerName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (history.result.isNotEmpty()) {
                        Text(
                            text = history.result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (history.score > 0) {
                        Text(
                            text = "${history.score} 分",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = history.getShortTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
