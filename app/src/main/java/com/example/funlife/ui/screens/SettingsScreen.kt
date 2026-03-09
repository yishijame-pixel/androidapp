// SettingsScreen.kt - 设置屏幕
package com.example.funlife.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val preferences by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "设置",
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 外观设置
        SettingsSection(title = "外观设置") {
            SettingsSwitchItem(
                icon = Icons.Default.DarkMode,
                title = "深色模式",
                description = "启用深色主题",
                checked = preferences.isDarkMode,
                onCheckedChange = { viewModel.updateDarkMode(it) }
            )
        }
        
        // 通知设置
        SettingsSection(title = "通知设置") {
            SettingsSwitchItem(
                icon = Icons.Default.Notifications,
                title = "启用通知",
                description = "接收纪念日提醒",
                checked = preferences.enableNotifications,
                onCheckedChange = { viewModel.updateNotifications(it) }
            )
            
            if (preferences.enableNotifications) {
                SettingsSliderItem(
                    icon = Icons.Default.Schedule,
                    title = "提前提醒",
                    description = "提前 ${preferences.notificationDaysBefore} 天提醒",
                    value = preferences.notificationDaysBefore.toFloat(),
                    valueRange = 1f..30f,
                    onValueChange = { 
                        viewModel.updatePreferences(
                            preferences.copy(notificationDaysBefore = it.toInt())
                        )
                    }
                )
            }
        }
        
        // 游戏设置
        SettingsSection(title = "游戏设置") {
            SettingsSliderItem(
                icon = Icons.Default.Add,
                title = "默认加分值",
                description = "每次加分 ${preferences.defaultScoreIncrement} 分",
                value = preferences.defaultScoreIncrement.toFloat(),
                valueRange = 1f..10f,
                onValueChange = { viewModel.updateScoreIncrement(it.toInt()) }
            )
            
            SettingsSwitchItem(
                icon = Icons.Default.VolumeUp,
                title = "音效",
                description = "启用音效反馈",
                checked = preferences.enableSound,
                onCheckedChange = { 
                    viewModel.updatePreferences(preferences.copy(enableSound = it))
                }
            )
            
            SettingsSwitchItem(
                icon = Icons.Default.Vibration,
                title = "震动",
                description = "启用震动反馈",
                checked = preferences.enableVibration,
                onCheckedChange = { 
                    viewModel.updatePreferences(preferences.copy(enableVibration = it))
                }
            )
        }
        
        // 数据管理
        SettingsSection(title = "数据管理") {
            SettingsSwitchItem(
                icon = Icons.Default.Backup,
                title = "自动备份",
                description = "自动备份应用数据",
                checked = preferences.autoBackup,
                onCheckedChange = { 
                    viewModel.updatePreferences(preferences.copy(autoBackup = it))
                }
            )
            
            SettingsButtonItem(
                icon = Icons.Default.CloudUpload,
                title = "导出数据",
                description = "导出所有数据到文件",
                onClick = { /* TODO: 实现导出功能 */ }
            )
            
            SettingsButtonItem(
                icon = Icons.Default.CloudDownload,
                title = "导入数据",
                description = "从文件导入数据",
                onClick = { /* TODO: 实现导入功能 */ }
            )
        }
        
        // 关于
        SettingsSection(title = "关于") {
            SettingsInfoItem(
                icon = Icons.Default.Info,
                title = "版本",
                value = "1.0.0"
            )
            
            SettingsButtonItem(
                icon = Icons.Default.Description,
                title = "使用说明",
                description = "查看应用使用指南",
                onClick = { /* TODO: 显示使用说明 */ }
            )
        }
    }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start - 1).toInt()
        )
    }
}

@Composable
fun SettingsButtonItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SettingsInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
