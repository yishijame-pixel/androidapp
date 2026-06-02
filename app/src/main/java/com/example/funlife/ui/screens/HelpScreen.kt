// HelpScreen.kt - 精致紧凑现代设计 v3
package com.example.funlife.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.funlife.utils.BatteryOptimizationHelper
import com.example.funlife.utils.OverlayBannerService
import com.example.funlife.utils.TestAlarmScheduler

/* ════════════════════════════════════════════════════════════════════════════
   帮助中心 v3 — 与设置页统一的简约现代风
   - 极简标题栏 56dp
   - 顶部权限概览大卡（进度环 + 一键修复）
   - 权限明细列表（Material 风格的列表项）
   - 测试 + FAQ
   ════════════════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var refreshTick by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val items = remember(refreshTick) { collectPermissionItems(context) }
    val grantedCount = items.count { it.granted }
    val totalCount = items.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HelpTopBar(onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 概览大卡
                OverviewCard(
                    granted = grantedCount,
                    total = totalCount,
                    onFixAll = {
                        items.filter { !it.granted }.forEach { it.requestAction() }
                        refreshTick++
                    },
                    onRefresh = { refreshTick++ }
                )

                // 权限明细
                SectionLabel("权限明细")
                Group {
                    items.forEachIndexed { idx, item ->
                        PermissionItemRow(item) { refreshTick++ }
                        if (idx < items.lastIndex) ItemDivider()
                    }
                }

                // 测试
                SectionLabel("测试工具")
                Group {
                    TestRow(context)
                }

                // FAQ
                SectionLabel("常见问题")
                Group {
                    FAQ_LIST.forEachIndexed { idx, faq ->
                        FaqRow(faq.question, faq.answer)
                        if (idx < FAQ_LIST.lastIndex) ItemDivider()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/* ──────────────── 顶部标题栏 ──────────────── */

@Composable
private fun HelpTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F8FA))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = Color(0xFF212121))
        }
        Text(
            "帮助中心",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF212121),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/* ──────────────── 概览大卡（进度 + 一键修复） ──────────────── */

@Composable
private fun OverviewCard(
    granted: Int,
    total: Int,
    onFixAll: () -> Unit,
    onRefresh: () -> Unit
) {
    val ratio = granted.toFloat() / total.coerceAtLeast(1)
    val allGood = granted == total
    val mainColor = when {
        ratio >= 0.95f -> Color(0xFF4CAF50)
        ratio >= 0.6f -> Color(0xFFFFA000)
        else -> Color(0xFFEF5350)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 状态徽章
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(mainColor.copy(alpha = 0.18f), mainColor.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (allGood) "✓" else "$granted",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = mainColor
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (allGood) "所有权限已就绪" else "$granted / $total 权限已就绪",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF212121)
                    )
                    Text(
                        if (allGood) "通知功能完全正常 🎉" else "还需要修复 ${total - granted} 项才能确保通知",
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(mainColor.copy(alpha = 0.7f), mainColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!allGood) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onFixAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC407A)),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("一键修复", fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("刷新状态", color = Color(0xFF424242), fontWeight = FontWeight.Black)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新检测", color = Color(0xFF424242), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/* ──────────────── 通用 Section / Group ──────────────── */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF9E9E9E),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column { content() }
    }
}

@Composable
private fun ItemDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(0.5.dp)
            .background(Color(0xFFEEEEEE))
    )
}

@Composable
private fun IconChip(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

/* ──────────────── 权限项 ──────────────── */

@Composable
private fun PermissionItemRow(item: PermissionItem, onChanged: () -> Unit) {
    val context = LocalContext.current
    val isManual = item.manualConfirmKey != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = item.icon, accent = item.accent)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(item.granted, isManual)
                }
                Text(
                    item.description,
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (!item.granted) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        item.requestAction()
                        onChanged()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = item.accent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("去设置", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // 手动确认行
        if (isManual && item.manualConfirmKey != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (item.granted) Color(0xFFE8F5E9) else Color(0xFFFAFAFA)
                    )
                    .clickable {
                        toggleManualConfirm(context, item.manualConfirmKey, !item.granted)
                        onChanged()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (item.granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (item.granted) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (item.granted) "已手动设置完成（点击撤销）" else "设置完成后点这里确认",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.granted) Color(0xFF2E7D32) else Color(0xFF616161)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(granted: Boolean, isManual: Boolean) {
    val (bg, text) = when {
        granted -> Color(0xFF4CAF50) to "已就绪"
        isManual -> Color(0xFF9E9E9E) to "需手动"
        else -> Color(0xFFFFA000) to "未开启"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(text, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Black)
    }
}

/* ──────────────── 测试按钮行 ──────────────── */

@Composable
private fun TestRow(context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                TestAlarmScheduler.scheduleIn(context, 30)
                android.widget.Toast.makeText(
                    context,
                    "✅ 30秒后将触发，请立即返回桌面并清后台",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = Icons.Rounded.NotificationsActive, accent = Color(0xFFFF5722))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("立即测试通知", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Text(
                "30秒后触发，可验证后台通知是否正常",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFF5722))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("30秒测试", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

/* ──────────────── FAQ ──────────────── */

@Composable
private fun FaqRow(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF9E9E9E)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF7F8FA))
                    .padding(12.dp)
            ) {
                Text(
                    text = answer,
                    fontSize = 12.sp,
                    color = Color(0xFF424242),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/* ──────────────── 数据：权限项 ──────────────── */

private data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val granted: Boolean,
    val requestAction: () -> Unit,
    val manualConfirmKey: String? = null
)

private const val MANUAL_CONFIRM_PREFS = "help_manual_perm_confirm"

private fun collectPermissionItems(context: Context): List<PermissionItem> {
    val list = mutableListOf<PermissionItem>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        list += PermissionItem(
            title = "推送通知",
            description = "Android 13+ 必需，否则无法显示任何通知",
            icon = Icons.Rounded.Notifications,
            accent = Color(0xFFEC407A),
            granted = granted,
            requestAction = {
                if (context is android.app.Activity) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        context,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                } else BatteryOptimizationHelper.openAppNotificationSettings(context)
            }
        )
    }

    list += PermissionItem(
        title = "忽略电池优化",
        description = "防止系统杀掉后台闹钟任务",
        icon = Icons.Rounded.BatteryFull,
        accent = Color(0xFF66BB6A),
        granted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context),
        requestAction = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        list += PermissionItem(
            title = "精确闹钟",
            description = "Android 12+ 让闹钟精确到秒触发",
            icon = Icons.Rounded.Schedule,
            accent = Color(0xFFFB8C00),
            granted = am.canScheduleExactAlarms(),
            requestAction = {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    ).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("HelpScreen", "精确闹钟权限请求失败", e)
                }
            }
        )
    }

    if (Build.VERSION.SDK_INT >= 34) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        list += PermissionItem(
            title = "全屏通知",
            description = "Android 14+ 让通知锁屏强制弹出",
            icon = Icons.Rounded.Fullscreen,
            accent = Color(0xFF7C4DFF),
            granted = try { nm.canUseFullScreenIntent() } catch (_: Exception) { false },
            requestAction = {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                    ).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("HelpScreen", "全屏通知权限请求失败", e)
                }
            }
        )
    }

    list += PermissionItem(
        title = "应用悬浮窗",
        description = "在其他应用上层显示提醒卡片",
        icon = Icons.Rounded.PictureInPicture,
        accent = Color(0xFF42A5F5),
        granted = OverlayBannerService.hasOverlayPermission(context),
        requestAction = { OverlayBannerService.requestOverlayPermission(context) }
    )

    val prefs = context.getSharedPreferences(MANUAL_CONFIRM_PREFS, Context.MODE_PRIVATE)

    list += PermissionItem(
        title = "自启动管理",
        description = "国产 ROM (MIUI/华为/OPPO) 默认禁止",
        icon = Icons.Rounded.RestartAlt,
        accent = Color(0xFF26A69A),
        granted = prefs.getBoolean("autostart_confirmed", false),
        requestAction = { BatteryOptimizationHelper.openAutoStartSettings(context) },
        manualConfirmKey = "autostart_confirmed"
    )

    list += PermissionItem(
        title = "通知重要性",
        description = "调到「重要」或「紧急」才会弹出悬浮通知",
        icon = Icons.Rounded.PriorityHigh,
        accent = Color(0xFFEF5350),
        granted = prefs.getBoolean("importance_confirmed", false),
        requestAction = { BatteryOptimizationHelper.openAppNotificationSettings(context) },
        manualConfirmKey = "importance_confirmed"
    )

    return list
}

private fun toggleManualConfirm(context: Context, key: String, value: Boolean) {
    context.getSharedPreferences(MANUAL_CONFIRM_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(key, value).apply()
}

/* ──────────────── 数据：FAQ ──────────────── */

private data class Faq(val question: String, val answer: String)

private val FAQ_LIST = listOf(
    Faq(
        "为什么收不到纪念日通知？",
        """
            最常见的 3 个原因：
            
            ① 「通知重要性」是「默认」 — 必须调到「重要」或「紧急」才会弹出悬浮通知。
            
            ② 「悬浮通知」开关关闭 — 进入通知设置打开该开关。
            
            ③ MIUI/华为/OPPO 默认禁止「自启动」 — App 在桌面时被杀，闹钟无法触发。
            
            💡 设置好后，用上方「30秒测试」按钮验证。
        """.trimIndent()
    ),
    Faq(
        "通知发出来了但没有声音/震动？",
        """
            进入「通知重要性」按钮跳转的设置页：
            
            ① 确认「发声」开关已开
            ② 确认「振动」开关已开
            ③ 「声音」选项不是「无」
            ④ 手机不在「静音」或「勿扰」模式
        """.trimIndent()
    ),
    Faq(
        "锁屏时通知不显示？",
        """
            ① 在通知设置页打开「锁屏通知」开关
            ② 授予「全屏通知」权限（Android 14+ 必需）
            ③ 「通知重要性」调到「紧急」级别
        """.trimIndent()
    ),
    Faq(
        "清后台 / 关机后通知就不响？",
        """
            ① 必须开启「自启动」管理（国产 ROM 默认关闭）
            ② 必须开启「忽略电池优化」
            ③ MIUI 还需在「最近任务页」长按本应用，点「🔒 锁定」防止清后台
        """.trimIndent()
    ),
    Faq(
        "「通知重要性」设置在哪里？",
        """
            MIUI 路径：
            设置 → 应用 → 应用管理 → FunLife → 通知管理 → 通知类别 → 选「纪念日提醒」 → 重要性 → 选「紧急」
            
            原生 Android：
            设置 → 应用 → FunLife → 通知 → 选某通知类别 → 重要性
            
            或直接点上方「通知重要性」按钮跳转。
        """.trimIndent()
    ),
    Faq(
        "一键修复后还是没全部开启？",
        """
            部分权限（如「通知重要性」「自启动」）受系统保护，App 无法代为开启。点击「去设置」按钮会跳转到设置页，你需要：
            
            ① 跳转后手动打开对应开关
            ② 返回 App，会自动刷新状态
            ③ 对于无法 API 检测的项，点击「设置完成后点这里确认」标记
        """.trimIndent()
    )
)
