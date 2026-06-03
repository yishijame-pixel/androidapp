// ═════════════════════════════════════════════════════════════════════════
// InboxScreen.kt
// 系统通知收件箱：列表展示所有通知
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.notifications.FunChannel
import com.example.funlife.notifications.InboxEntry
import com.example.funlife.notifications.InboxStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(
    onNavigateBack: () -> Unit,
    onDeepLink: (String) -> Unit
) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val entries = remember(tick) { InboxStore.getAll(ctx) }
    val unread = entries.count { !it.read }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 进入页面 → 全部标记已读
    LaunchedEffect(Unit) { InboxStore.markAllRead(ctx); tick++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF7FA), Color(0xFFFDF6F0))))
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
                    .size(40.dp).clip(CircleShape).background(Color.White)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF3D1F2C)) }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("通知收件箱", color = Color(0xFF1F2937), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    if (entries.isEmpty()) "暂无消息" else "共 ${entries.size} 条" + if (unread > 0) " · $unread 条未读" else "",
                    color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
            if (entries.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(36.dp).clip(CircleShape).background(Color.White)
                        .clickable { showClearConfirm = true },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Delete, null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp)) }
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Color(0xFFC8B0BB), modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("暂无系统通知", color = Color(0xFF1F2937), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("当有提醒、寄语、到期日时，都会出现在这里", color = Color(0xFF6B7280), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    SwipeToDeleteRow(
                        onDelete = { InboxStore.delete(ctx, entry.id); tick++ }
                    ) {
                        InboxRow(
                            entry = entry,
                            onClick = { entry.deepLink?.let { onDeepLink(it) } },
                            onDelete = { InboxStore.delete(ctx, entry.id); tick++ }
                        )
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空全部消息？") },
            text = { Text("已读和未读的消息都会被永久删除，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    InboxStore.clear(ctx); showClearConfirm = false; tick++
                }) { Text("清空", color = Color(0xFFE11D48)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun InboxRow(
    entry: InboxEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val ch = FunChannel.fromId(entry.channelId)
    val accent = channelAccent(ch)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 渠道图标徽章
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(ch?.emoji ?: "🔔", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title,
                    color = Color(0xFF1F2937),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!entry.read) {
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                entry.body,
                color = Color(0xFF4B5563),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(ch?.displayName ?: "通知", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text(formatTime(entry.timestamp), color = Color(0xFF9CA3AF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp).clip(CircleShape).background(Color(0xFFF3F4F6))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Delete, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp)) }
    }
}

/**
 * 向右滑动到一定阈值即删除。背景为红底 + 白色垃圾桶图标。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberDismissState(
        confirmValueChange = { value ->
            if (value == DismissValue.DismissedToEnd) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { distance -> distance * 0.45f }
    )
    SwipeToDismiss(
        state = state,
        directions = setOf(DismissDirection.StartToEnd),
        background = {
            val active = state.targetValue == DismissValue.DismissedToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF5252),
                                Color(0xFFFF1744).copy(alpha = if (active) 1f else 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("删除", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        },
        dismissContent = { content() }
    )
}

private fun channelAccent(ch: FunChannel?): Color = when (ch) {
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
    FunChannel.SOCIAL -> Color(0xFF2563EB)
    null -> Color(0xFF6B7280)
}

private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 60 * 60_000 -> "${diff / 60_000} 分钟前"
        diff < 24 * 60 * 60_000 -> "${diff / (60 * 60_000)} 小时前"
        diff < 7L * 24 * 60 * 60_000 -> "${diff / (24 * 60 * 60_000)} 天前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.CHINA).format(Date(ts))
    }
}
