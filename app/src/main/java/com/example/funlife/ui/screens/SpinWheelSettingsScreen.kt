// SpinWheelSettingsScreen.kt - 转盘设置页面
package com.example.funlife.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("转盘设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 模式管理
            SettingsCard(
                title = "模式管理",
                description = "切换和管理转盘模式",
                icon = Icons.Default.Category,
                onClick = onOpenModeManagement
            )
            
            // 模板管理
            SettingsCard(
                title = "模板管理",
                description = "保存和加载转盘模板",
                icon = Icons.Default.Folder,
                onClick = onOpenTemplates
            )
            
            // 编辑选项
            SettingsCard(
                title = "编辑选项",
                description = "自定义转盘选项和权重",
                icon = Icons.Default.Edit,
                onClick = onOpenEditOptions
            )
            
            // 权重可视化
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.toggleWeightVisualization() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "权重可视化",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "显示选项权重大小",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showWeightVisualization,
                        onCheckedChange = { viewModel.toggleWeightVisualization() }
                    )
                }
            }
            
            // 连抽功能
            SettingsCard(
                title = "连抽功能",
                description = "3/5/10连抽，享受折扣",
                icon = Icons.Default.Repeat,
                onClick = onOpenMultiSpin
            )
            
            // 保底设置
            SettingsCard(
                title = "保底设置",
                description = "设置选项保底次数",
                icon = Icons.Default.Shield,
                onClick = onOpenGuarantee
            )
            
            Divider()
            
            // 主题设置
            SettingsCard(
                title = "主题设置",
                description = "选择转盘主题颜色",
                icon = Icons.Default.Palette,
                onClick = onOpenTheme
            )
            
            // 动画设置
            SettingsCard(
                title = "动画设置",
                description = "开关各种动画效果",
                icon = Icons.Default.Animation,
                onClick = onOpenAnimation
            )
            
            Divider()
            
            // 历史筛选
            SettingsCard(
                title = "历史筛选",
                description = "筛选和导出历史记录",
                icon = Icons.Default.FilterList,
                onClick = onOpenHistoryFilter
            )
            
            // 统计数据
            SettingsCard(
                title = "统计数据",
                description = "查看详细统计图表",
                icon = Icons.Default.BarChart,
                onClick = onOpenStats
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
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
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
