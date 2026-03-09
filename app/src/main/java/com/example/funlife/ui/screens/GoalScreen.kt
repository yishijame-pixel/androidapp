// GoalScreen.kt - 完善版目标管理屏幕
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
import com.example.funlife.data.model.Countdown
import com.example.funlife.viewmodel.GoalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    viewModel: GoalViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val countdowns by viewModel.countdowns.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, "添加") },
                text = { Text("添加倒数日") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        if (countdowns.isEmpty()) {
            EmptyGoalState(Modifier.padding(padding))
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
                    GoalOverviewCard(countdowns)
                }
                
                // 倒数日列表
                items(countdowns, key = { it.id }) { countdown ->
                    EnhancedCountdownCard(
                        countdown = countdown,
                        onDelete = { viewModel.deleteCountdown(countdown) }
                    )
                }
            }
        }
    }
    
    if (showDialog) {
        AddCountdownDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, date, icon, color ->
                viewModel.addCountdown(title, date, "生活", icon, color)
                showDialog = false
            }
        )
    }
}

@Composable
fun EmptyGoalState(modifier: Modifier = Modifier) {
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
                Text("🎯", fontSize = (60 * scale).sp)
            }
            
            Text(
                "还没有倒数日",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "点击下方按钮添加重要日期\n记录生活中的重要时刻",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GoalOverviewCard(countdowns: List<Countdown>) {
    val totalCountdowns = countdowns.size
    val upcomingCountdowns = countdowns.count { it.getDaysRemaining() >= 0 }
    val closestCountdown = countdowns
        .filter { it.getDaysRemaining() >= 0 }
        .minByOrNull { it.getDaysRemaining() }
    
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
                    "目标概览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                GoalStatItem("📝", "总目标", "$totalCountdowns 个")
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                GoalStatItem("⏰", "进行中", "$upcomingCountdowns 个")
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                GoalStatItem(
                    "🔥",
                    "最近目标",
                    closestCountdown?.let { "${it.getDaysRemaining()}天" } ?: "无"
                )
            }
        }
    }
}

@Composable
fun GoalStatItem(icon: String, label: String, value: String) {
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
fun EnhancedCountdownCard(countdown: Countdown, onDelete: () -> Unit) {
    val daysRemaining = countdown.getDaysRemaining()
    val color = Color(android.graphics.Color.parseColor(countdown.color))
    
    // 根据剩余天数选择状态颜色
    val statusColor = when {
        daysRemaining < 0 -> Color(0xFF95A5A6) // 已过期 - 灰色
        daysRemaining == 0L -> Color(0xFFE74C3C) // 今天 - 红色
        daysRemaining <= 7 -> Color(0xFFFF6B35) // 一周内 - 橙色
        daysRemaining <= 30 -> Color(0xFFFFD700) // 一月内 - 金色
        else -> Color(0xFF4ECDC4) // 更远 - 青色
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
                                color.copy(alpha = 0.15f),
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
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        color,
                                        color.copy(alpha = 0.8f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(countdown.icon, fontSize = 32.sp)
                    }
                    
                    // 标题和倒计时
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            countdown.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // 倒计时显示
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when {
                                        daysRemaining > 0 -> "⏰"
                                        daysRemaining == 0L -> "🎉"
                                        else -> "✓"
                                    },
                                    fontSize = 16.sp
                                )
                                Text(
                                    when {
                                        daysRemaining > 0 -> "还有 $daysRemaining 天"
                                        daysRemaining == 0L -> "就是今天！"
                                        else -> "已过去 ${-daysRemaining} 天"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                        
                        // 日期显示
                        Text(
                            try {
                                val date = LocalDate.parse(countdown.targetDate)
                                val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                                date.format(formatter)
                            } catch (e: Exception) {
                                countdown.targetDate
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
fun CountdownCard(countdown: Countdown, onDelete: () -> Unit) {
    val daysRemaining = countdown.getDaysRemaining()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(countdown.color)).copy(alpha = 0.1f)
        )
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(countdown.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(countdown.icon, fontSize = 24.sp)
                }
                
                Column {
                    Text(
                        countdown.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            daysRemaining > 0 -> "还有 $daysRemaining 天"
                            daysRemaining == 0L -> "就是今天！"
                            else -> "已过去 ${-daysRemaining} 天"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedIcon by remember { mutableStateOf("🎯") }
    var selectedColor by remember { mutableStateOf("#4ECDC4") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val icons = listOf("🎯", "🎓", "✈️", "💼", "🎂", "💍", "🏆", "📅", "🎉", "💝", "🏠", "🚗")
    val colors = listOf(
        "#4ECDC4", "#FF6B9D", "#FFD700", "#9B59B6",
        "#FF6B35", "#3498DB", "#2ECC71", "#E74C3C"
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
                    "添加倒数日",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("例如：毕业典礼") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 日期选择按钮
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedDate?.let {
                            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                            it.format(formatter)
                        } ?: "选择日期"
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "选择图标",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(icons) { icon ->
                            FilterChip(
                                selected = selectedIcon == icon,
                                onClick = { selectedIcon = icon },
                                label = { Text(icon, fontSize = 24.sp) },
                                shape = CircleShape
                            )
                        }
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "选择颜色",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(if (selectedColor == color) 48.dp else 40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
                
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
                        onClick = {
                            if (title.isNotBlank() && selectedDate != null) {
                                onConfirm(title, selectedDate.toString(), selectedIcon, selectedColor)
                            }
                        },
                        enabled = title.isNotBlank() && selectedDate != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
    
    // 日期选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
