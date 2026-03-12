// SpinWheelSettingsScreen.kt - 转盘设置页面（美化版）
package com.example.funlife.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.SpinWheelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelSettingsScreen(
    viewModel: SpinWheelViewModel,
    onNavigateBack: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenEditOptions: () -> Unit,
    onOpenMultiSpin: () -> Unit,
    onOpenHistoryFilter: () -> Unit,
    onOpenGuarantee: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenAnimation: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenModeManagement: () -> Unit
) {
    val showWeightVisualization by viewModel.showWeightVisualization.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎡", fontSize = 24.sp)
                        Text("转盘设置", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基础设置分组
            SettingsGroup(title = "基础设置", emoji = "⚙️") {
                SettingsCard(
                    title = "模式管理",
                    description = "切换和管理转盘模式",
                    icon = Icons.Default.Category,
                    iconColor = Color(0xFFFF6B9D),
                    onClick = onOpenModeManagement
                )
                
                SettingsCard(
                    title = "模板管理",
                    description = "保存和加载转盘模板",
                    icon = Icons.Default.Folder,
                    iconColor = Color(0xFFFFD93D),
                    onClick = onOpenTemplates
                )
                
                SettingsCard(
                    title = "编辑选项",
                    description = "自定义转盘选项和权重",
                    icon = Icons.Default.Edit,
                    iconColor = Color(0xFF4CAF50),
                    onClick = onOpenEditOptions
                )
            }
            
            // 权重可视化开关
            SettingsGroup(title = "显示设置", emoji = "👁️") {
                WeightVisualizationCard(
                    checked = showWeightVisualization,
                    onCheckedChange = { viewModel.toggleWeightVisualization() }
                )
            }
            
            // 抽取功能分组
            SettingsGroup(title = "抽取功能", emoji = "🎲") {
                SettingsCard(
                    title = "连抽功能",
                    description = "3/5/10连抽，享受折扣",
                    icon = Icons.Default.Repeat,
                    iconColor = Color(0xFF2196F3),
                    onClick = onOpenMultiSpin
                )
                
                SettingsCard(
                    title = "保底设置",
                    description = "设置选项保底次数",
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFFAB47BC),
                    onClick = onOpenGuarantee
                )
            }
            
            // 外观设置分组
            SettingsGroup(title = "外观设置", emoji = "🎨") {
                SettingsCard(
                    title = "主题设置",
                    description = "选择转盘主题颜色",
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFFFF9800),
                    onClick = onOpenTheme
                )
                
                SettingsCard(
                    title = "动画设置",
                    description = "开关各种动画效果",
                    icon = Icons.Default.Animation,
                    iconColor = Color(0xFF00BCD4),
                    onClick = onOpenAnimation
                )
            }
            
            // 数据管理分组
            SettingsGroup(title = "数据管理", emoji = "📊") {
                SettingsCard(
                    title = "历史筛选",
                    description = "筛选和导出历史记录",
                    icon = Icons.Default.FilterList,
                    iconColor = Color(0xFF9C27B0),
                    onClick = onOpenHistoryFilter
                )
                
                SettingsCard(
                    title = "统计数据",
                    description = "查看详细统计图表",
                    icon = Icons.Default.BarChart,
                    iconColor = Color(0xFFFF5722),
                    onClick = onOpenStats
                )
            }
            
            // 底部间距
            Spacer(Modifier.height(16.dp))
        }
    }
}

// 设置分组
@Composable
private fun SettingsGroup(
    title: String,
    emoji: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 分组标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        content()
    }
}

// 权重可视化卡片
@Composable
private fun WeightVisualizationCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.05f else 1f,
        animationSpec = tween(300)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (checked) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
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
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (checked) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD93D),
                                        Color(0xFFFF9800)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column {
                    Text(
                        "权重可视化",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        if (checked) "已开启 - 显示选项权重" else "显示选项权重大小",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD93D),
                    checkedTrackColor = Color(0xFFFFD93D).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 彩色图标容器
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                iconColor.copy(alpha = 0.8f),
                                iconColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 箭头图标
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
