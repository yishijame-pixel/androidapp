// ═════════════════════════════════════════════════════════════════════════
// GoalExtras.kt
// 目标 / 倒数日扩展 UI：里程碑 + 打卡 + 看板 + 成就墙 + 详情 + 分享 + 提醒
// 严格遵循 docs/屏幕适配指南.md & DEVELOPMENT_PRINCIPLES.md
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as ANCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.GoalAchievementStore
import com.example.funlife.data.GoalCheckInStore
import com.example.funlife.data.Milestone
import com.example.funlife.data.MilestoneStore
import com.example.funlife.data.CountdownReminderStore
import com.example.funlife.data.model.Countdown
import com.example.funlife.data.model.Goal
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.utils.CountdownReminderScheduler
import com.example.funlife.utils.UserSessionManager
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════
// 用户ID hook
// ═════════════════════════════════════════════════════════════════════════
@Composable
internal fun rememberCurrentUserId(): Long {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrNull()?.takeIf { it > 0 } ?: 0L
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 排序 & 筛选状态
// ═════════════════════════════════════════════════════════════════════════
internal enum class GoalSortMode(val label: String) {
    DEADLINE("按截止"),
    PROGRESS("按进度"),
    CREATED("按创建")
}

internal data class GoalFilterState(
    val query: String = "",
    val sort: GoalSortMode = GoalSortMode.CREATED,
    val category: String? = null
)

internal fun applyFilter(goals: List<Goal>, f: GoalFilterState): List<Goal> {
    val q = f.query.trim()
    val list = goals.filter { g ->
        val byCat = f.category == null || g.category == f.category
        val byQ = q.isEmpty() ||
            g.title.contains(q, ignoreCase = true) ||
            g.description.contains(q, ignoreCase = true) ||
            g.category.contains(q, ignoreCase = true)
        byCat && byQ
    }
    return when (f.sort) {
        GoalSortMode.DEADLINE -> list.sortedBy { it.targetDate ?: "9999-99-99" }
        GoalSortMode.PROGRESS -> list.sortedByDescending { it.progress }
        GoalSortMode.CREATED -> list.sortedByDescending { it.createdAt }
    }
}

internal fun applyCountdownFilter(items: List<Countdown>, f: GoalFilterState): List<Countdown> {
    val q = f.query.trim()
    return items.filter { c ->
        val byCat = f.category == null || c.category == f.category
        val byQ = q.isEmpty() ||
            c.title.contains(q, ignoreCase = true) ||
            c.note.contains(q, ignoreCase = true) ||
            c.category.contains(q, ignoreCase = true)
        byCat && byQ
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 顶部 工具栏组件：搜索框 + 排序 + 分类筛选
// ═════════════════════════════════════════════════════════════════════════
@Composable
internal fun FilterSearchBar(
    state: GoalFilterState,
    onState: (GoalFilterState) -> Unit,
    categories: List<Pair<String, Color>>,   // (name, accent)
    showSort: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 搜索框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.rdp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.pill))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFFAD6584), modifier = Modifier.size(16.rdp))
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.query.isEmpty()) {
                            Text("搜索目标 / 倒数日…", color = Color(0xFFC9A9BC), fontSize = TextSize.sm)
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = { onState(state.copy(query = it.take(40))) },
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFFFF6F91)),
                            textStyle = TextStyle(color = Color(0xFF3D1F2C), fontSize = TextSize.sm, fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (state.query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(22.rdp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE4EC))
                                .clickable { onState(state.copy(query = "")) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(12.rdp)) }
                    }
                }
            }
            if (showSort) {
                Spacer(Modifier.width(6.dp))
                // 排序循环按钮
                Box(
                    modifier = Modifier
                        .height(40.rdp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.pill))
                        .clickable {
                            val next = when (state.sort) {
                                GoalSortMode.CREATED -> GoalSortMode.DEADLINE
                                GoalSortMode.DEADLINE -> GoalSortMode.PROGRESS
                                GoalSortMode.PROGRESS -> GoalSortMode.CREATED
                            }
                            onState(state.copy(sort = next))
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⇅ ${state.sort.label}", fontSize = TextSize.tiny, color = Color(0xFF8B5670), fontWeight = FontWeight.Black)
                }
            }
        }
        // 分类 chip 行
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(label = "全部", color = Color(0xFFFF6F91), active = state.category == null) {
                    onState(state.copy(category = null))
                }
                categories.forEach { (name, c) ->
                    FilterChip(label = name, color = c, active = state.category == name) {
                        onState(state.copy(category = if (state.category == name) null else name))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, color: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (active) color else Color.White)
            .border(1.dp, if (active) color else color.copy(alpha = 0.45f), RoundedCornerShape(Radius.pill))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = TextSize.tiny, color = if (active) Color.White else color, fontWeight = FontWeight.Black)
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 里程碑 Section（嵌入 GoalCard 的展开内容）
// ═════════════════════════════════════════════════════════════════════════
@Composable
internal fun MilestoneSection(
    goal: Goal,
    accent: Color,
    onProgressFromMilestones: (Int) -> Unit,
    onCheckedInToday: () -> Unit
) {
    val ctx = LocalContext.current
    val userId = rememberCurrentUserId()
    var items by remember(goal.id) { mutableStateOf(MilestoneStore.load(ctx, userId, goal.id)) }
    var newText by remember { mutableStateOf("") }
    var checkedToday by remember(goal.id) { mutableStateOf(GoalCheckInStore.isCheckedToday(ctx, userId, goal.id)) }
    val checkInDays = remember(goal.id) { GoalCheckInStore.load(ctx, userId, goal.id).size }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        // 标题 + 打卡按钮
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📌 里程碑 / 打卡", fontSize = TextSize.tiny, color = accent, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(
                        if (checkedToday) Color(0xFF66BB6A).copy(alpha = 0.18f)
                        else Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f))).run { accent } .let { _ ->
                            // 使用 brush 渲染：单色背景容器即可，详见下方 Modifier 链
                            accent.copy(alpha = 0.0f)
                        }
                    )
                    .then(
                        if (checkedToday) Modifier
                        else Modifier.background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f))))
                    )
                    .clickable(enabled = !checkedToday) {
                        val ok = GoalCheckInStore.checkInToday(ctx, userId, goal.id)
                        if (ok) {
                            checkedToday = true
                            onCheckedInToday()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    if (checkedToday) "✓ 今日已打卡 · 累计 ${checkInDays + 1}天"
                    else "📍 今日打卡 · 累计 ${checkInDays}天",
                    fontSize = TextSize.tiny,
                    color = if (checkedToday) Color(0xFF2E7D32) else Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // 列表
        items.forEach { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.rdp)
                        .clip(CircleShape)
                        .background(if (m.done) accent else Color.White)
                        .border(1.5.dp, accent, CircleShape)
                        .clickable {
                            val updated = items.map { if (it.id == m.id) it.copy(done = !it.done, doneAt = if (!it.done) LocalDate.now().toString() else null) else it }
                            items = updated
                            MilestoneStore.save(ctx, userId, goal.id, updated)
                            // 同步进度
                            val frac = MilestoneStore.completionFraction(ctx, userId, goal.id)
                            if (frac != null) onProgressFromMilestones((frac * 100).toInt())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (m.done) Text("✓", color = Color.White, fontSize = 11.rsp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    m.text,
                    fontSize = TextSize.xs,
                    color = if (m.done) Color(0xFFA08AB5) else Color(0xFF3D1F2C),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(20.rdp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.10f))
                        .clickable {
                            val updated = items.filter { it.id != m.id }
                            items = updated
                            MilestoneStore.save(ctx, userId, goal.id, updated)
                            val frac = MilestoneStore.completionFraction(ctx, userId, goal.id)
                            if (frac != null) onProgressFromMilestones((frac * 100).toInt())
                        },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color(0xFFE53935), fontSize = 11.rsp, fontWeight = FontWeight.Black) }
            }
        }
        if (items.size < 10) {
            // 新增输入
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.rdp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Color(0xFFFFF6F1))
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(Radius.pill))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (newText.isEmpty()) Text("加一个小步骤…", color = Color(0xFFC9A9BC), fontSize = TextSize.tiny)
                    BasicTextField(
                        value = newText,
                        onValueChange = { newText = it.take(40) },
                        singleLine = true,
                        cursorBrush = SolidColor(accent),
                        textStyle = TextStyle(color = Color(0xFF3D1F2C), fontSize = TextSize.tiny, fontWeight = FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .height(34.rdp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f))))
                        .clickable(enabled = newText.isNotBlank()) {
                            val updated = items + Milestone(UUID.randomUUID().toString(), newText.trim(), false)
                            items = updated
                            MilestoneStore.save(ctx, userId, goal.id, updated)
                            newText = ""
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋ 添加", color = Color.White, fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 数据看板对话框
// ═════════════════════════════════════════════════════════════════════════
@Composable
internal fun GoalDashboardDialog(
    activeGoals: List<Goal>,
    completedGoals: List<Goal>,
    countdowns: List<Countdown>,
    categoryColorOf: (String) -> Color,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val userId = rememberCurrentUserId()
    val streak = remember { GoalCheckInStore.globalStreak(ctx, userId) }
    val allCheckIns = remember { GoalCheckInStore.loadAll(ctx, userId) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
            ) {
                // 头部
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.rdp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFFFF6F91)))
                        )
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📊", fontSize = 26.rsp)
                        Spacer(Modifier.width(8.dp))
                        Text("数据看板", color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.headline, letterSpacing = 1.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.rdp).align(Alignment.TopEnd).padding(Spacing.sm)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.28f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.rdp)) }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 540.rdp)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.md)
                ) {
                    // 顶层统计
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashStat("$streak", "连签天数", Color(0xFFFF6F91), Modifier.weight(1f))
                        DashStat("${activeGoals.size}", "进行中", Color(0xFFFF8F4F), Modifier.weight(1f))
                        DashStat("${completedGoals.size}", "已达成", Color(0xFF66BB6A), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.md))
                    // 完成率环形图
                    DashSection(title = "总体完成率") {
                        val total = activeGoals.size + completedGoals.size
                        val rate = if (total == 0) 0f else completedGoals.size.toFloat() / total
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CompletionRing(rate = rate, modifier = Modifier.size(110.rdp))
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("已完成 ${completedGoals.size} / $total", fontSize = TextSize.sm, color = Color(0xFF3D1F2C), fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${(rate * 100).toInt()}%",
                                    fontSize = 28.rsp,
                                    color = Color(0xFFFF6F91),
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (total == 0) "还没有目标，去新增一个吧 ✨"
                                    else if (rate >= 0.8f) "完成率优秀，继续保持！"
                                    else if (rate >= 0.4f) "稳步推进，再加把劲！"
                                    else "起步阶段，先小步快跑～",
                                    fontSize = TextSize.tiny,
                                    color = Color(0xFFAD6584),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    // 分类完成柱状图
                    DashSection(title = "分类完成情况") {
                        val byCat = (activeGoals + completedGoals).groupBy { it.category }
                        if (byCat.isEmpty()) {
                            EmptyHint("暂无分类数据")
                        } else {
                            byCat.entries.sortedByDescending { it.value.size }.forEach { (cat, list) ->
                                val done = list.count { it.isCompleted }
                                val frac = done.toFloat() / list.size
                                CategoryProgressRow(name = cat, done = done, total = list.size, frac = frac, color = categoryColorOf(cat))
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    // 30 天打卡热力
                    DashSection(title = "近 30 天打卡热力") {
                        StreakHeatmap30(allCheckIns)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    // 倒数日临近
                    DashSection(title = "即将到达的倒数日") {
                        val nearest = countdowns
                            .mapNotNull { c ->
                                val d = runCatching { c.getDaysRemaining() }.getOrNull() ?: return@mapNotNull null
                                c to d
                            }
                            .filter { it.second >= 0 }
                            .sortedBy { it.second }
                            .take(3)
                        if (nearest.isEmpty()) {
                            EmptyHint("没有未来的倒数日")
                        } else {
                            nearest.forEach { (c, d) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(c.icon, fontSize = 18.rsp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(c.title, fontSize = TextSize.sm, color = Color(0xFF3D1F2C), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${d}天", fontSize = TextSize.sm, color = Color(0xFFFF6F91), fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                }
            }
        }
    }
}

@Composable
private fun DashStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.lg))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.rsp, color = color, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = TextSize.tiny, color = Color(0xFF8B5670), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DashSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Color.White)
            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.lg))
            .padding(Spacing.sm)
    ) {
        Text(title, fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = TextSize.tiny, color = Color(0xFFAD6584), fontWeight = FontWeight.Medium) }
}

@Composable
private fun CompletionRing(rate: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.14f
            val pad = stroke / 2f
            // 底环
            drawArc(
                color = Color(0xFFFFE9EF),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2, size.height - pad * 2),
                style = Stroke(width = stroke)
            )
            // 进度
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F), Color(0xFFFF6F91))),
                startAngle = -90f,
                sweepAngle = 360f * rate.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2, size.height - pad * 2),
                style = Stroke(width = stroke)
            )
        }
    }
}

@Composable
private fun CategoryProgressRow(name: String, done: Int, total: Int, frac: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontSize = TextSize.tiny, color = color, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("$done / $total", fontSize = TextSize.tiny, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(color.copy(alpha = 0.15f))
        ) {
            if (frac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun StreakHeatmap30(checkIns: Set<String>) {
    val today = LocalDate.now()
    val days = (0..29).map { today.minusDays((29 - it).toLong()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        days.forEach { d ->
            val on = d.toString() in checkIns
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.rdp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (on) Color(0xFFFF6F91) else Color(0xFFFFE9EF))
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("30天前", fontSize = TextSize.tiny, color = Color(0xFFAD6584))
        Spacer(Modifier.weight(1f))
        Text("今天", fontSize = TextSize.tiny, color = Color(0xFFAD6584))
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 成就墙对话框
// ═════════════════════════════════════════════════════════════════════════
@Composable
internal fun AchievementWallDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val userId = rememberCurrentUserId()
    val list = remember { GoalAchievementStore.load(ctx, userId) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.rdp)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6F91))))
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆", fontSize = 26.rsp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("成就墙", color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.headline, letterSpacing = 1.sp)
                            Text("已解锁 ${list.count { it.unlocked }} / ${list.size}", color = Color.White.copy(alpha = 0.85f), fontSize = TextSize.tiny, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier.size(32.rdp).align(Alignment.TopEnd).padding(Spacing.sm)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.28f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.rdp)) }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 520.rdp)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.md)
                ) {
                    list.forEach { a ->
                        AchievementRow(a)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(a: com.example.funlife.data.Achievement) {
    val unlocked = a.unlocked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(if (unlocked) Color(0xFFFFF8E1) else Color(0xFFF5F0F2))
            .border(1.dp, if (unlocked) Color(0xFFFFB300) else Color(0xFFE5DEE2), RoundedCornerShape(Radius.lg))
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.rdp)
                .clip(CircleShape)
                .background(if (unlocked) Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300))) else Brush.linearGradient(listOf(Color(0xFFE5DEE2), Color(0xFFCFC4CB)))),
            contentAlignment = Alignment.Center
        ) {
            Text(a.emoji, fontSize = 22.rsp)
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(a.title, fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = if (unlocked) Color(0xFF6D4C00) else Color(0xFF8B7A82))
            Spacer(Modifier.height(2.dp))
            Text(a.desc, fontSize = TextSize.tiny, color = Color(0xFFAD6584), fontWeight = FontWeight.Medium)
        }
        if (unlocked) {
            Text("✓", fontSize = 18.rsp, color = Color(0xFF66BB6A), fontWeight = FontWeight.Black)
        } else {
            Text("🔒", fontSize = 14.rsp)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 倒数日详情对话框：精确倒数 + 占比环 + 编辑/分享/提醒/删除
// ═════════════════════════════════════════════════════════════════════════
@Composable
internal fun CountdownDetailDialog(
    countdown: Countdown,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    parseColor: (String) -> Color
) {
    val ctx = LocalContext.current
    val userId = rememberCurrentUserId()
    val accent = parseColor(countdown.color)

    // 实时倒数（秒级）
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(countdown.id) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val targetMs = remember(countdown.targetDate) {
        runCatching {
            LocalDate.parse(countdown.targetDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull() ?: 0L
    }
    val createdMs = remember(countdown.createdAt) {
        runCatching {
            LocalDate.parse(countdown.createdAt).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull() ?: nowMs
    }
    val diff = targetMs - nowMs
    val absSec = kotlin.math.abs(diff) / 1000
    val days = absSec / 86400
    val hours = (absSec % 86400) / 3600
    val mins = (absSec % 3600) / 60
    val secs = absSec % 60
    val isFuture = diff > 0
    val isToday = (diff in 0..86_400_000L) || (diff in -86_400_000L..0L && days == 0L)

    // 占比进度（0..1）：从 createdAt 到 targetDate 的过去比例
    val ratio = remember(nowMs, targetMs, createdMs) {
        val total = (targetMs - createdMs).coerceAtLeast(1)
        ((nowMs - createdMs).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    // 当前提醒
    var reminderAt by remember { mutableStateOf(CountdownReminderStore.get(ctx, userId, countdown.id)) }

    val ink = Color(0xFF1F2937)
    val muted = Color(0xFF6B7280)
    val divider = Color(0xFFEFEAEE)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color.White)
                    .border(1.dp, divider, RoundedCornerShape(Radius.xxl))
            ) {
                // 顶部彩色细条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.rdp)
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.55f))))
                )
                // 头部：图标 + 标题 + 关闭
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.rdp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(accent.copy(alpha = 0.10f))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(Radius.md)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (countdown.icon.startsWith("file:") || countdown.icon.startsWith("/")) {
                            val path = countdown.icon.removePrefix("file:")
                            coil.compose.AsyncImage(
                                model = java.io.File(path),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(36.rdp).clip(RoundedCornerShape(Radius.sm))
                            )
                        } else {
                            Text(countdown.icon.ifBlank { "🎈" }, fontSize = 22.rsp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            countdown.title,
                            color = ink,
                            fontSize = TextSize.title,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Radius.pill))
                                    .background(accent.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    countdown.category,
                                    color = accent,
                                    fontSize = TextSize.tiny,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                countdown.targetDate ?: "",
                                color = muted,
                                fontSize = TextSize.tiny,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.rdp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, null, tint = ink, modifier = Modifier.size(16.rdp)) }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(divider))

                // Hero 区：巨型日数 + 进度环
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg, bottom = Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(196.rdp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = size.minDimension * 0.07f
                            val pad = stroke / 2f
                            drawArc(
                                color = accent.copy(alpha = 0.10f),
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                topLeft = Offset(pad, pad),
                                size = Size(size.width - pad * 2, size.height - pad * 2),
                                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.55f), accent)),
                                startAngle = -90f,
                                sweepAngle = 360f * ratio,
                                useCenter = false,
                                topLeft = Offset(pad, pad),
                                size = Size(size.width - pad * 2, size.height - pad * 2),
                                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isToday) "就是今天" else if (isFuture) "距目标还有" else "已过去",
                                color = muted,
                                fontSize = TextSize.tiny,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            if (isToday) {
                                Text("Today", color = accent, fontSize = 48.rsp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                            } else {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$days", color = ink, fontSize = 72.rsp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("天", color = ink, fontSize = TextSize.title, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "已过 ${(ratio * 100).toInt()}%",
                                color = accent,
                                fontSize = TextSize.tiny,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // 时分秒
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeStat("$hours", "小时", accent, modifier = Modifier.weight(1f))
                    TimeStat("$mins", "分", accent, modifier = Modifier.weight(1f))
                    TimeStat("$secs", "秒", accent, modifier = Modifier.weight(1f))
                }

                if (countdown.note.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Color(0xFFFAF7F8))
                            .border(1.dp, divider, RoundedCornerShape(Radius.md))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.rdp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            countdown.note,
                            color = ink,
                            fontSize = TextSize.xs,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.rsp
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // 提醒卡
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(Color(0xFFFAF7F8))
                        .border(1.dp, divider, RoundedCornerShape(Radius.md))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.rdp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = accent, modifier = Modifier.size(18.rdp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("到点提醒", color = ink, fontSize = TextSize.xs, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            reminderAt?.let { ts ->
                                val ld = java.time.Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDateTime()
                                "已设置 · " + ld.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                            } ?: "未设置 · 点击右侧设置",
                            color = muted,
                            fontSize = TextSize.tiny,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    SmallPillBtn(
                        label = if (reminderAt == null) "设置" else "修改",
                        bg = accent,
                        fg = Color.White
                    ) {
                        pickReminderTime(ctx, countdown.targetDate) { triggerMs ->
                            CountdownReminderStore.set(ctx, userId, countdown.id, triggerMs)
                            CountdownReminderScheduler.schedule(ctx, countdown.id, countdown.title, countdown.note, triggerMs)
                            reminderAt = triggerMs
                        }
                    }
                    if (reminderAt != null) {
                        Spacer(Modifier.width(6.dp))
                        SmallPillBtn(
                            label = "清除",
                            bg = Color(0xFFF3F4F6),
                            fg = ink
                        ) {
                            CountdownReminderStore.clear(ctx, userId, countdown.id)
                            CountdownReminderScheduler.cancel(ctx, countdown.id)
                            reminderAt = null
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.md))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(divider))

                // 底部操作行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailActionVectorBtn(
                        icon = Icons.Default.Edit,
                        label = "编辑",
                        bg = Color(0xFFF3F4F6),
                        fg = ink,
                        modifier = Modifier.weight(1f),
                        onClick = onEdit
                    )
                    DetailActionVectorBtn(
                        icon = Icons.Default.Share,
                        label = "分享卡",
                        bg = accent,
                        fg = Color.White,
                        modifier = Modifier.weight(1.3f)
                    ) { shareCountdownCard(ctx, countdown, accent) }
                    DetailActionVectorBtn(
                        icon = Icons.Default.Delete,
                        label = null,
                        bg = Color(0xFFFFF1F2),
                        fg = Color(0xFFE11D48),
                        modifier = Modifier.weight(0.55f),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeStat(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(Color(0xFFFAF7F8))
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(Radius.md))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = accent, fontSize = 22.rsp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
        Text(label, color = Color(0xFF6B7280), fontSize = TextSize.tiny, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallPillBtn(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = fg, fontSize = TextSize.tiny, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun TimeUnit(value: String, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontSize = 18.rsp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = TextSize.tiny)
        }
    }
}

@Composable
private fun TextChipBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(Color.White.copy(alpha = 0.85f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = Color(0xFF3D1F2C), fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DetailActionVectorBtn(
    icon: ImageVector,
    label: String?,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.rdp)
            .shadow(3.dp, RoundedCornerShape(Radius.pill), spotColor = Color.Black.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.rdp))
        if (!label.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(label, color = fg, fontSize = TextSize.sm, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun DetailActionBtn(label: String, bg: Color, fg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.rdp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = TextSize.sm, fontWeight = FontWeight.Black)
    }
}

// ─── 选时间 ───
private fun pickReminderTime(ctx: Context, targetDate: String?, onPick: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply {
        runCatching {
            val d = LocalDate.parse(targetDate)
            set(Calendar.YEAR, d.year)
            set(Calendar.MONTH, d.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, d.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
    }
    DatePickerDialog(
        ctx,
        { _, y, m, d ->
            TimePickerDialog(
                ctx,
                { _, h, min ->
                    val c = Calendar.getInstance().apply {
                        set(y, m, d, h, min, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (c.timeInMillis > System.currentTimeMillis()) {
                        onPick(c.timeInMillis)
                    } else {
                        android.widget.Toast.makeText(ctx, "提醒时间需在未来", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// ─── 分享卡片：用 android.graphics 绘制纯位图，存缓存目录后通过 FileProvider 分享 ───
private fun shareCountdownCard(ctx: Context, c: Countdown, accent: Color) {
    runCatching {
        val w = 1080
        val h = 1620
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = ANCanvas(bmp)
        // 渐变底
        val accentInt = accent.toArgb()
        val accentLight = Color(accent.red, accent.green, accent.blue, 0.78f).toArgb()
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), accentInt, accentLight, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        // 装饰大圆
        val decor = Paint().apply { isAntiAlias = true; color = 0x33FFFFFF }
        canvas.drawCircle(w * 0.85f, h * 0.10f, w * 0.40f, decor)
        canvas.drawCircle(w * 0.10f, h * 0.85f, w * 0.30f, decor)

        val whiteBold = Paint().apply {
            isAntiAlias = true; color = 0xFFFFFFFF.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        // emoji
        whiteBold.textSize = 200f
        canvas.drawText(c.icon, w / 2f, 360f, whiteBold)
        // 标题
        whiteBold.textSize = 88f
        val title = if (c.title.length > 14) c.title.take(13) + "…" else c.title
        canvas.drawText(title, w / 2f, 500f, whiteBold)
        // 大数字
        val days = runCatching { c.getDaysRemaining() }.getOrNull()
        val isFuture = (days ?: 0L) >= 0
        val absDays = kotlin.math.abs(days ?: 0L)
        whiteBold.textSize = 360f
        canvas.drawText("$absDays", w / 2f, 1020f, whiteBold)
        whiteBold.textSize = 64f
        val daysLabel = if (isFuture) "天后到达" else "天前已过"
        canvas.drawText(daysLabel, w / 2f, 1110f, whiteBold)
        // 日期
        whiteBold.textSize = 52f
        whiteBold.alpha = 200
        canvas.drawText("📅 ${c.targetDate}", w / 2f, 1230f, whiteBold)
        // 备注
        if (c.note.isNotBlank()) {
            whiteBold.textSize = 44f
            whiteBold.alpha = 220
            val note = if (c.note.length > 20) c.note.take(19) + "…" else c.note
            canvas.drawText("💬 $note", w / 2f, 1330f, whiteBold)
        }
        // 落款
        whiteBold.textSize = 36f
        whiteBold.alpha = 180
        canvas.drawText("via FunLife · 倒数日", w / 2f, h - 80f, whiteBold)

        // 写入缓存
        val dir = File(ctx.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(dir, "countdown_${c.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
        bmp.recycle()

        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${c.icon} ${c.title} · 距 ${c.targetDate} ${if (isFuture) "还有" else "已过"} ${absDays} 天")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "分享倒数日").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }.onFailure {
        android.util.Log.e("GoalExtras", "分享失败", it)
        android.widget.Toast.makeText(ctx, "分享失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
    }
}
