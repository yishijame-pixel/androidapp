// SettingsScreen.kt - 精致紧凑现代设计 v3
package com.example.funlife.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/* ════════════════════════════════════════════════════════════════════════════
   设计原则：
   - 极致紧凑：顶部栏 56dp，分区间距 18dp
   - 极简色彩：白底 + 7 种语义色图标 + 浅灰描边
   - 大圆角 22dp，无重阴影，靠描边和留白营造层级
   - 图标用方形圆角彩色背景容器（不是圆形），更现代
   ════════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCredits: () -> Unit = {},
) {
    val preferences by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 极简标题栏
            SettingsTopBar(onNavigateBack = onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 帮助中心横幅
                HelpBanner(onClick = onNavigateToHelp)

                // 通用偏好
                SettingsGroup(title = "通用") {
                    ToggleItem(
                        icon = Icons.Rounded.DarkMode,
                        accent = Color(0xFF5C6BC0),
                        title = "深色模式",
                        desc = "切换深色主题",
                        checked = preferences.isDarkMode,
                        onChange = viewModel::updateDarkMode
                    )
                    DividerLine()
                    ToggleItem(
                        icon = Icons.Rounded.Notifications,
                        accent = Color(0xFFEC407A),
                        title = "应用通知",
                        desc = "纪念日 & 日程提醒",
                        checked = preferences.enableNotifications,
                        onChange = viewModel::updateNotifications
                    )
                    DividerLine()
                    NavItem(
                        icon = Icons.Rounded.Notifications,
                        accent = Color(0xFFEC407A),
                        title = "通知中心",
                        desc = "分类开关 / 静默时段 / 推送时刻",
                        onClick = onNavigateToNotifications
                    )
                    DividerLine()
                    SliderItem(
                        icon = Icons.Rounded.Schedule,
                        accent = Color(0xFFFB8C00),
                        title = "提前提醒",
                        valueText = "${preferences.notificationDaysBefore} 天",
                        value = preferences.notificationDaysBefore.toFloat(),
                        range = 1f..30f,
                        onValueChange = {
                            viewModel.updatePreferences(
                                preferences.copy(notificationDaysBefore = it.toInt())
                            )
                        }
                    )
                }

                // 反馈
                SettingsGroup(title = "反馈") {
                    ToggleItem(
                        icon = Icons.Rounded.VolumeUp,
                        accent = Color(0xFF42A5F5),
                        title = "音效",
                        desc = "操作反馈音效",
                        checked = preferences.enableSound,
                        onChange = {
                            viewModel.updatePreferences(preferences.copy(enableSound = it))
                        }
                    )
                    DividerLine()
                    ToggleItem(
                        icon = Icons.Rounded.Vibration,
                        accent = Color(0xFF26A69A),
                        title = "震动",
                        desc = "操作触觉反馈",
                        checked = preferences.enableVibration,
                        onChange = {
                            viewModel.updatePreferences(preferences.copy(enableVibration = it))
                        }
                    )
                }

                // 游戏
                SettingsGroup(title = "游戏") {
                    SliderItem(
                        icon = Icons.Rounded.EmojiEvents,
                        accent = Color(0xFF7C4DFF),
                        title = "默认加分值",
                        valueText = "+${preferences.defaultScoreIncrement}",
                        value = preferences.defaultScoreIncrement.toFloat(),
                        range = 1f..10f,
                        onValueChange = viewModel::updateScoreIncrement.let { f ->
                            { f(it.toInt()) }
                        }
                    )
                }

                // 数据
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                var importResult by remember { mutableStateOf<com.example.funlife.utils.DataBackupManager.ImportResult?>(null) }
                var busyMsg by remember { mutableStateOf<String?>(null) }
                val pickImportFile = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    busyMsg = "正在导入…"
                    scope.launch {
                        runCatching {
                            val uid = com.example.funlife.utils.UserSessionManager(ctx).getCurrentUserId()
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.funlife.utils.DataBackupManager.importFromUri(ctx, uri, uid)
                            }
                        }.onSuccess {
                            busyMsg = null
                            importResult = it
                        }.onFailure {
                            busyMsg = null
                            android.util.Log.e("SettingsScreen", "导入失败", it)
                            android.widget.Toast.makeText(
                                ctx, "导入失败，请检查文件格式",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                SettingsGroup(title = "数据") {
                    ActionItem(
                        icon = Icons.Rounded.Upload,
                        accent = Color(0xFFFFA726),
                        title = "导出数据",
                        desc = "纪念日 / 心情 / 目标 / 习惯 / 倒数日 → JSON",
                        onClick = {
                            busyMsg = "正在导出…"
                            scope.launch {
                                runCatching {
                                    val uid = com.example.funlife.utils.UserSessionManager(ctx).getCurrentUserId()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        com.example.funlife.utils.DataBackupManager.exportToFile(ctx, uid)
                                    }
                                }.onSuccess { file ->
                                    busyMsg = null
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        ctx, "${ctx.packageName}.fileprovider", file
                                    )
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    ctx.startActivity(android.content.Intent.createChooser(send, "保存或分享备份"))
                                }.onFailure {
                                    busyMsg = null
                                    android.util.Log.e("SettingsScreen", "导出失败", it)
                                    android.widget.Toast.makeText(
                                        ctx, "导出失败，请重试",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                    DividerLine()
                    ActionItem(
                        icon = Icons.Rounded.Download,
                        accent = Color(0xFF42A5F5),
                        title = "导入数据",
                        desc = "从备份 JSON 恢复（追加，不覆盖现有数据）",
                        onClick = {
                            runCatching { pickImportFile.launch(arrayOf("application/json", "*/*")) }
                        }
                    )
                    DividerLine()
                    ActionItem(
                        icon = Icons.Rounded.BugReport,
                        accent = Color(0xFFEF5350),
                        title = "导出崩溃日志",
                        desc = "用于反馈问题",
                        onClick = {
                            val text = com.example.funlife.utils.CrashHandler.exportRecent(ctx)
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "${ctx.getString(com.example.funlife.R.string.app_name)} 崩溃反馈")
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            ctx.startActivity(android.content.Intent.createChooser(send, "分享崩溃日志"))
                        }
                    )
                }
                busyMsg?.let { msg ->
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = { Text("请稍候") },
                        text = { Text(msg) }
                    )
                }
                importResult?.let { r ->
                    AlertDialog(
                        onDismissRequest = { importResult = null },
                        confirmButton = {
                            TextButton(onClick = { importResult = null }) { Text("好的") }
                        },
                        title = { Text("导入完成") },
                        text = {
                            Text(
                                "已恢复：\n" +
                                "  纪念日 ${r.anniversaries} 条\n" +
                                "  心情 ${r.moods} 条\n" +
                                "  目标 ${r.goals} 条\n" +
                                "  习惯 ${r.habits} 条\n" +
                                "  倒数日 ${r.countdowns} 条"
                            )
                        }
                    )
                }

                // 关于
                SettingsGroup(title = "关于") {
                    StaticItem(
                        icon = Icons.Rounded.Info,
                        accent = Color(0xFF78909C),
                        title = "版本",
                        valueText = "v1.0.0"
                    )
                    DividerLine()
                    NavItem(
                        icon = Icons.Rounded.Copyright,
                        accent = Color(0xFF00897B),
                        title = "开源许可",
                        desc = "SuperTux 等第三方致谢",
                        onClick = onNavigateToCredits,
                    )
                    DividerLine()
                    ActionItem(
                        icon = Icons.Rounded.MenuBook,
                        accent = Color(0xFF8E24AA),
                        title = "使用指南",
                        desc = "应用功能说明",
                        onClick = onNavigateToHelp,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "${ctx.getString(com.example.funlife.R.string.app_name)} · 用心制作",
                    fontSize = 11.sp,
                    color = Color(0xFFBDBDBD),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/* ──────────────── 顶部极简标题栏 ──────────────── */

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    Surface(
        color = Color(0xFFF7F8FA),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF212121)
                )
            }
            Text(
                text = "设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF212121),
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/* ──────────────── 帮助中心横幅（小尺寸但精致） ──────────────── */

@Composable
private fun HelpBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFF8FA3),
                        Color(0xFFEC407A),
                        Color(0xFF9C27B0)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text("💡", fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "帮助中心",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        "HOT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEC407A)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "通知收不到？一键自检 + 修复",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White
        )
    }
}

/* ──────────────── 分组容器（带标题） ──────────────── */

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF9E9E9E),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color(0xFFEEEEEE)
            )
        ) {
            Column {
                content()
            }
        }
    }
}

/* ──────────────── 分隔线 ──────────────── */

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(0.5.dp)
            .background(Color(0xFFEEEEEE))
    )
}

/* ──────────────── 列表项：圆角方形图标容器 ──────────────── */

@Composable
private fun IconChip(
    icon: ImageVector,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

/* ──────────────── Nav 行（点击进入子页） ──────────────── */

@Composable
private fun NavItem(
    icon: ImageVector,
    accent: Color,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, accent = accent)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Text(desc, fontSize = 11.sp, color = Color(0xFF9E9E9E))
        }
        Text("›", fontSize = 22.sp, color = Color(0xFFBDBDBD), fontWeight = FontWeight.Bold)
    }
}

/* ──────────────── Toggle ──────────────── */

@Composable
private fun ToggleItem(
    icon: ImageVector,
    accent: Color,
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, accent = accent)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Text(desc, fontSize = 11.sp, color = Color(0xFF9E9E9E))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

/* ──────────────── Slider ──────────────── */

@Composable
private fun SliderItem(
    icon: ImageVector,
    accent: Color,
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = icon, accent = accent)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = (range.endInclusive - range.start - 1).toInt().coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.15f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/* ──────────────── Action（点击跳转） ──────────────── */

@Composable
private fun ActionItem(
    icon: ImageVector,
    accent: Color,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, accent = accent)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Text(desc, fontSize = 11.sp, color = Color(0xFF9E9E9E))
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD)
        )
    }
}

/* ──────────────── Static（只显示信息） ──────────────── */

@Composable
private fun StaticItem(
    icon: ImageVector,
    accent: Color,
    title: String,
    valueText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, accent = accent)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            valueText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9E9E)
        )
    }
}
