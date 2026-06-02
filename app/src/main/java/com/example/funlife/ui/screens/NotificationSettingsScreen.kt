// ═════════════════════════════════════════════════════════════════════════
// NotificationSettingsScreen.kt
// 企业级通知中心 — 用户设置页
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.notifications.DailyDigestKind
import com.example.funlife.notifications.DailyDigestScheduler
import com.example.funlife.notifications.FunChannel
import com.example.funlife.notifications.NotificationPrefs

@Composable
fun NotificationSettingsScreen(onNavigateBack: () -> Unit) {
    val ctx = LocalContext.current

    // 用 tick 触发重组（SharedPreferences 写入后）
    var tick by remember { mutableStateOf(0) }
    fun bump() { tick++ }

    val global = remember(tick) { NotificationPrefs.isGlobalEnabled(ctx) }
    val quietEnabled = remember(tick) { NotificationPrefs.isQuietHoursEnabled(ctx) }
    val quietFrom = remember(tick) { NotificationPrefs.getQuietFromMin(ctx) }
    val quietTo = remember(tick) { NotificationPrefs.getQuietToMin(ctx) }
    val dedup = remember(tick) { NotificationPrefs.isDedupEnabled(ctx) }
    val openApp = remember(tick) { NotificationPrefs.isOpenAppEnabled(ctx) }
    val engagement = remember(tick) { NotificationPrefs.isEngagementEnabled(ctx) }
    val goalT = remember(tick) { NotificationPrefs.getGoalTime(ctx) }
    val habitT = remember(tick) { NotificationPrefs.getHabitTime(ctx) }
    val moodT = remember(tick) { NotificationPrefs.getMoodTime(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFF7FA), Color(0xFFFDF6F0)))
            )
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 36.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3D1F2C)) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("通知中心", color = Color(0xFF1F2937), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("掌控每一条提醒", color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ⚠️ 系统级通知开关检测：被禁用时一切应用内开关都无效
            val notifMgr = remember { androidx.core.app.NotificationManagerCompat.from(ctx) }
            val systemEnabled = remember(tick) { notifMgr.areNotificationsEnabled() }
            if (!systemEnabled) {
                SystemNotifBlockedBanner(
                    onJumpSettings = {
                        runCatching {
                            val i = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            ctx.startActivity(i)
                        }
                    },
                    onRecheck = { bump() }
                )
            }

            // 总开关
            SettingsCard {
                ToggleRow(
                    emoji = "🔔",
                    title = "应用通知总开关",
                    desc = "关闭后所有 App 通知都不再发送",
                    checked = global,
                    accent = Color(0xFFEC407A),
                    onChange = {
                        NotificationPrefs.setGlobalEnabled(ctx, it)
                        bump()
                    }
                )
            }

            // 分类开关
            SettingsCard {
                SectionTitle("通知分类")
                FunChannel.values().forEach { ch ->
                    val enabled = remember(tick, ch) { NotificationPrefs.isChannelEnabled(ctx, ch) }
                    ToggleRow(
                        emoji = ch.emoji,
                        title = ch.displayName,
                        desc = ch.description,
                        checked = enabled && global,
                        accent = channelColor(ch),
                        enabled = global,
                        onChange = {
                            NotificationPrefs.setChannelEnabled(ctx, ch, it)
                            bump()
                        }
                    )
                }
            }

            // 每日推送时刻
            SettingsCard {
                SectionTitle("每日推送时间")
                TimeRow("🎯 目标进度", goalT, enabled = global) { h, m ->
                    NotificationPrefs.setGoalTime(ctx, h, m)
                    DailyDigestScheduler.schedule(ctx, DailyDigestKind.GOAL)
                    bump()
                }
                TimeRow("✅ 习惯打卡", habitT, enabled = global) { h, m ->
                    NotificationPrefs.setHabitTime(ctx, h, m)
                    DailyDigestScheduler.schedule(ctx, DailyDigestKind.HABIT)
                    bump()
                }
                TimeRow("💌 心情邮箱", moodT, enabled = global) { h, m ->
                    NotificationPrefs.setMoodTime(ctx, h, m)
                    DailyDigestScheduler.schedule(ctx, DailyDigestKind.MOOD)
                    bump()
                }
            }

            // 静默时段
            SettingsCard {
                ToggleRow(
                    emoji = "🌙",
                    title = "夜间静默时段",
                    desc = "默认 22:00 - 08:00 不打扰（强提醒除外）",
                    checked = quietEnabled,
                    accent = Color(0xFF6366F1),
                    onChange = {
                        NotificationPrefs.setQuietHoursEnabled(ctx, it)
                        bump()
                    }
                )
                if (quietEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeChip("起", quietFrom, modifier = Modifier.weight(1f)) { h, m ->
                            NotificationPrefs.setQuietRange(ctx, h * 60 + m, quietTo); bump()
                        }
                        TimeChip("止", quietTo, modifier = Modifier.weight(1f)) { h, m ->
                            NotificationPrefs.setQuietRange(ctx, quietFrom, h * 60 + m); bump()
                        }
                    }
                }
            }

            // 其他策略
            SettingsCard {
                SectionTitle("智能策略")
                ToggleRow(
                    emoji = "🛡️",
                    title = "低频不打扰",
                    desc = "同类通知 24 小时内只推一次",
                    checked = dedup,
                    accent = Color(0xFF10B981),
                    onChange = { NotificationPrefs.setDedupEnabled(ctx, it); bump() }
                )
                ToggleRow(
                    emoji = "🌅",
                    title = "每日打开摘要",
                    desc = "每天首次打开 App 时推送今日提醒摘要",
                    checked = openApp,
                    accent = Color(0xFFF59E0B),
                    onChange = { NotificationPrefs.setOpenAppEnabled(ctx, it); bump() }
                )
                ToggleRow(
                    emoji = "🌱",
                    title = "空模块拉活提醒",
                    desc = "心情/纪念日/目标/习惯/倒数日 还没添加时，每模块每 24 小时最多提醒一次",
                    checked = engagement,
                    accent = Color(0xFF8B5CF6),
                    onChange = { NotificationPrefs.setEngagementEnabled(ctx, it); bump() }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── 组件 ──

@Composable
private fun SystemNotifBlockedBanner(
    onJumpSettings: () -> Unit,
    onRecheck: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3E0))
            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "系统已禁用本应用通知",
                color = Color(0xFFE65100),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "纪念日、心情提醒、目标 等所有提醒将无法到达。建议前往系统设置开启通知。",
            color = Color(0xFF6D4C00),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onJumpSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
            ) { Text("前往系统设置", color = Color.White) }
            OutlinedButton(onClick = onRecheck) { Text("已开启，重新检测") }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Color(0xFF6B7280),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun ToggleRow(
    emoji: String,
    title: String,
    desc: String,
    checked: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFF1F2937), fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(desc, color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB)
            )
        )
    }
}

@Composable
private fun TimeRow(
    title: String,
    time: Pair<Int, Int>,
    enabled: Boolean,
    onPick: (Int, Int) -> Unit
) {
    val ctx = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color(0xFF1F2937), fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) Color(0xFFF3F4F6) else Color(0xFFF9FAFB))
                .clickable(enabled = enabled) {
                    TimePickerDialog(ctx, { _, h, m -> onPick(h, m) }, time.first, time.second, true).show()
                }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                String.format("%02d:%02d", time.first, time.second),
                color = if (enabled) Color(0xFF1F2937) else Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TimeChip(label: String, totalMin: Int, modifier: Modifier = Modifier, onPick: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val h = totalMin / 60
    val m = totalMin % 60
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .clickable { TimePickerDialog(ctx, { _, hh, mm -> onPick(hh, mm) }, h, m, true).show() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Bedtime, null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(6.dp))
        Text(String.format("%02d:%02d", h, m), color = Color(0xFF1F2937), fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

private fun channelColor(ch: FunChannel): Color = when (ch) {
    FunChannel.ANNIVERSARY -> Color(0xFFEC407A)
    FunChannel.COUNTDOWN -> Color(0xFFFF8F4F)
    FunChannel.GOAL -> Color(0xFFFB8C00)
    FunChannel.HABIT -> Color(0xFF26A69A)
    FunChannel.MOOD -> Color(0xFF7E57C2)
    FunChannel.OPEN_APP -> Color(0xFFF59E0B)
    FunChannel.WEEKLY -> Color(0xFF42A5F5)
    FunChannel.SYSTEM -> Color(0xFF6B7280)
    FunChannel.BOOKKEEPING -> Color(0xFFFFA726)
    FunChannel.LETTER -> Color(0xFFB39DDB)
}
