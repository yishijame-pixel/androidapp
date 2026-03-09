// MoodScreen.kt - 完善版心情日记屏幕
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.viewmodel.MoodViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    viewModel: MoodViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val moods by viewModel.moods.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, "添加") },
                text = { Text("记录心情") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        if (moods.isEmpty()) {
            EmptyMoodState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部统计卡片
                item {
                    MoodOverviewCard(moods)
                }
                
                // 心情记录列表
                items(moods, key = { it.id }) { mood ->
                    EnhancedMoodCard(
                        mood = mood,
                        onDelete = { viewModel.deleteMood(mood) }
                    )
                }
            }
        }
    }
    
    if (showDialog) {
        AddMoodDialog(
            onDismiss = { showDialog = false },
            onConfirm = { mood, level, note ->
                viewModel.addMood(mood, level, note)
                showDialog = false
            }
        )
    }
}

@Composable
fun EmptyMoodState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // 动画图标
            var scale by remember { mutableStateOf(1f) }
            LaunchedEffect(Unit) {
                while (true) {
                    animate(1f, 1.2f, animationSpec = tween(1000)) { value, _ -> scale = value }
                    animate(1.2f, 1f, animationSpec = tween(1000)) { value, _ -> scale = value }
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
                Text("😊", fontSize = (60 * scale).sp)
            }
            
            Text(
                "还没有心情记录",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "点击下方按钮记录今天的心情\n记录每一天的情绪变化",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MoodOverviewCard(moods: List<MoodEntry>) {
    val totalMoods = moods.size
    val recentMoods = moods.take(7)
    
    // 统计心情分布
    val moodCounts = moods.groupingBy { it.mood }.eachCount()
    val mostFrequentMood = moodCounts.maxByOrNull { it.value }?.key ?: "😊"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 24.sp)
                Text(
                    "心情概览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MoodStatItem("📝", "总记录", "$totalMoods 条")
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                MoodStatItem("😊", "最常心情", mostFrequentMood)
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                MoodStatItem("📅", "最近7天", "${recentMoods.size} 条")
            }
        }
    }
}

@Composable
fun MoodStatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EnhancedMoodCard(mood: MoodEntry, onDelete: () -> Unit) {
    // 根据心情选择颜色
    val moodColor = when (mood.mood) {
        "😊", "😃" -> Color(0xFF4ECDC4)
        "😐" -> Color(0xFFFFD700)
        "😢" -> Color(0xFF3498DB)
        "😡" -> Color(0xFFE74C3C)
        else -> Color(0xFF4ECDC4)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 背景装饰
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                moodColor.copy(alpha = 0.15f),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 心情图标
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        moodColor,
                                        moodColor.copy(alpha = 0.8f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mood.mood, fontSize = 32.sp)
                    }
                    
                    // 日期和备注
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 格式化日期显示
                        val date = try {
                            LocalDate.parse(mood.date)
                            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                            LocalDate.parse(mood.date).format(formatter)
                        } catch (e: Exception) {
                            mood.date
                        }
                        
                        Text(
                            date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (mood.note.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = moodColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    mood.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
                
                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MoodCard(mood: MoodEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(mood.mood, fontSize = 40.sp)
                Column {
                    Text(
                        mood.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (mood.note.isNotEmpty()) {
                        Text(
                            mood.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String) -> Unit
) {
    var selectedMood by remember { mutableStateOf("😊") }
    var selectedLevel by remember { mutableStateOf(3) }
    var note by remember { mutableStateOf("") }
    
    val moods = listOf(
        "😊" to "开心",
        "😃" to "兴奋",
        "😐" to "平静",
        "😢" to "难过",
        "😡" to "生气"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "记录心情",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "今天的心情",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        moods.forEachIndexed { index, (emoji, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedMood = emoji
                                        selectedLevel = index + 1
                                    }
                                    .background(
                                        if (selectedMood == emoji)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            Color.Transparent
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    emoji,
                                    fontSize = if (selectedMood == emoji) 40.sp else 32.sp
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedMood == emoji)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（选填）") },
                    placeholder = { Text("记录今天发生的事情...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = { onConfirm(selectedMood, selectedLevel, note) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
