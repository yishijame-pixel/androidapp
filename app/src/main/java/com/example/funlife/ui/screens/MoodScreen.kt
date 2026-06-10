// MoodScreen.kt — 心情时间河（重新设计版 · 2026-05）
//
// 设计理念：摒弃数据看板堆叠卡片的常规设计，整页只有两个层级：
//   1. 顶部 [今日心情Hero]：一行横滑 emoji，点击即开启快速记录抽屉；
//      右上角悬浮一颗连续打卡金徽章，常态可见的核心激励指标。
//   2. 主体 [时间河 Timeline]：垂直时间轴 + 渐变发光线 + 每条心情一个节点（节点大小=情绪强度 1-5），
//      右侧紧贴备注气泡（带左尖角）。同时间线即记录历史 + 周回顾 + 月概况。
//
// 严格遵循：
//   · docs/屏幕适配指南.md ：使用 Spacing / Radius / TextSize / IconSize / rdp / rsp / bottomTabContentPadding / ResponsiveDialogBox
//   · DEVELOPMENT_PRINCIPLES.md：复用现有 MoodViewModel，不动 DAO / Migration；Canvas radius 已 coerceAtLeast(1f)
package com.example.funlife.ui.screens

import com.example.funlife.resource.ResourceStore
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.data.MoodIcon
import com.example.funlife.data.MoodIconStore
import com.example.funlife.ui.components.MoodIconView
import com.example.funlife.ui.components.MoodIconManagerDialog
import com.example.funlife.ui.utils.IconSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.bottomTabContentPadding
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.viewmodel.MoodViewModel
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// emoji 在圆圈/方块内垂直居中的样式（去掉字体内置 padding + 行高居中）
private val EmojiCenteredStyle = androidx.compose.ui.text.TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

// ═════════════════════════════════════════════════════════════════════════
// 设计令牌（情绪 emoji 调色板）
// 注：原静态 MoodPalette 已迁移到 [MoodIconStore]（支持用户自定义上传图片）。
// 这里保留 MoodMeta 数据类与默认调色板（在 store 拿不到时的兜底）。
// ═════════════════════════════════════════════════════════════════════════
private data class MoodMeta(val color: Color, val label: String, val level: Int)

// 全局快照：由 MoodScreen 根 Composable 通过 SideEffect 注入，让非 Composable 调用点（如 forEach 内）也能访问
private var GLOBAL_MOOD_ICONS: List<MoodIcon> = MoodIconStore.defaults()

private val MoodPaletteFallback: Map<String, MoodMeta> = linkedMapOf(
    "🥰" to MoodMeta(Color(0xFFFF6F91), "超开心", 5),
    "😊" to MoodMeta(Color(0xFFFF8F4F), "开心",   5),
    "😃" to MoodMeta(Color(0xFFFFA726), "兴奋",   5),
    "🤗" to MoodMeta(Color(0xFFFFB74D), "温暖",   4),
    "😎" to MoodMeta(Color(0xFF7E57C2), "自信",   4),
    "😌" to MoodMeta(Color(0xFF26A69A), "平静",   3),
    "🤔" to MoodMeta(Color(0xFFAB7B3F), "思考",   3),
    "😶" to MoodMeta(Color(0xFF9E9E9E), "无语",   3),
    "😴" to MoodMeta(Color(0xFF9575CD), "困倦",   2),
    "🥱" to MoodMeta(Color(0xFF7986CB), "疲惫",   2),
    "😢" to MoodMeta(Color(0xFF42A5F5), "难过",   1),
    "😭" to MoodMeta(Color(0xFF1E88E5), "伤心",   1),
    "😡" to MoodMeta(Color(0xFFEF5350), "生气",   1),
    "😰" to MoodMeta(Color(0xFF607D8B), "焦虑",   2),
)

/** 通过 MoodEntry.mood（id 或 emoji）查找元信息。优先读全局自定义列表，否则回退到默认调色板。 */
private fun metaOf(idOrEmoji: String): MoodMeta {
    val ic = GLOBAL_MOOD_ICONS.firstOrNull { it.id == idOrEmoji }
    if (ic != null) return MoodMeta(Color(ic.color), ic.label, ic.level)
    return MoodPaletteFallback[idOrEmoji] ?: MoodMeta(Color(0xFFFF8F4F), "心情", 3)
}

/**
 * 渲染一个心情图标：自动判断是 emoji 还是用户上传图片。
 * 用来替换原先散落在各处的 Text(entry.mood, fontSize=...) 调用。
 */
@Composable
private fun MoodGlyph(
    idOrEmoji: String,
    emojiFontSize: TextUnit,
    imageSize: Dp,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    style: TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    val icon = GLOBAL_MOOD_ICONS.firstOrNull { it.id == idOrEmoji }
    if (icon != null && icon.isImage) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(icon.value.removePrefix("file://")))
                .crossfade(false)
                .build(),
            contentDescription = icon.label,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(imageSize).clip(CircleShape)
        )
    } else {
        Text(
            text = icon?.value ?: idOrEmoji,
            fontSize = emojiFontSize,
            textAlign = textAlign,
            style = style,
            modifier = modifier
        )
    }
}

private fun fmt(raw: String, pattern: String): String = try {
    LocalDate.parse(raw).format(DateTimeFormatter.ofPattern(pattern))
} catch (e: Exception) { raw }

private fun weekdayCn(raw: String): String = try {
    when (LocalDate.parse(raw).dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"; DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
} catch (e: Exception) { "" }

private enum class MoodViewMode { TIMELINE, CALENDAR }

// 连续低落天数（最近含 1-2 等级的连续天数，用于驿站推送暖心彩蛋）
private fun computeLowStreak(moods: List<MoodEntry>): Int {
    if (moods.isEmpty()) return 0
    val byDate = moods.groupBy { it.date }
        .mapValues { (_, list) -> list.minOf { it.moodLevel } }
        .toSortedMap(compareByDescending { it })
    var n = 0
    var cur: LocalDate? = null
    for ((d, lvl) in byDate) {
        val date = try { LocalDate.parse(d) } catch (e: Exception) { continue }
        if (cur == null) { if (date != LocalDate.now() && date != LocalDate.now().minusDays(1)) break; cur = date }
        else if (date != cur.minusDays(1)) break else cur = date
        if (lvl <= 2) n++ else break
    }
    return n
}

private fun computeStreak(moods: List<MoodEntry>): Int {
    if (moods.isEmpty()) return 0
    val dates = moods.mapNotNull { try { LocalDate.parse(it.date) } catch (e: Exception) { null } }
        .distinct().sortedDescending()
    if (dates.isEmpty()) return 0
    val today = LocalDate.now()
    if (dates[0] != today && dates[0] != today.minusDays(1)) return 0
    var streak = 1
    var cur = dates[0]
    for (i in 1 until dates.size) {
        if (dates[i] == cur.minusDays(1)) { streak++; cur = dates[i] } else break
    }
    return streak
}

// ═════════════════════════════════════════════════════════════════════════
// 主屏
// ═════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    viewModel: MoodViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val moods by viewModel.moods.collectAsState()

    // 🎨 心情图标自定义：从 store 读取当前用户的图标列表
    val currentUserId = remember {
        com.example.funlife.utils.UserSessionManager(context).getCurrentUserId()
    }
    var moodIconsTick by remember { mutableStateOf(0) }
    val moodIcons = remember(moodIconsTick, currentUserId) {
        MoodIconStore.getAll(context, currentUserId)
    }
    // 将列表注入全局快照，让非 Composable 调用点（metaOf）能访问
    SideEffect { GLOBAL_MOOD_ICONS = moodIcons }
    var showMoodIconManager by remember { mutableStateOf(false) }

    // 待保存的 emoji（点击 emoji 后弹底部 sheet 输入备注）
    var pendingEmoji by remember { mutableStateOf<String?>(null) }
    var detailMood by remember { mutableStateOf<MoodEntry?>(null) }
    var deleteMood by remember { mutableStateOf<MoodEntry?>(null) }

    // ── 视图模式 / 筛选 / 弹窗入口 ──
    var viewMode by remember { mutableStateOf(MoodViewMode.TIMELINE) }
    var showFilter by remember { mutableStateOf(false) }
    var filterEmoji by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showStats by remember { mutableStateOf(false) }
    var showMailbox by remember { mutableStateOf(false) }
    var calendarMonthOffset by remember { mutableStateOf(0) }
    var calendarPickedDate by remember { mutableStateOf<String?>(null) }

    // ── 邮件箱（响应式：push/update 后自动刷新）──
    val mails by com.example.funlife.data.MoodMailStore.mailsFlow.collectAsState()
    val unreadCount by com.example.funlife.data.MoodMailStore.unreadFlow.collectAsState()
    val moodMailLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(moodMailLifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                com.example.funlife.data.MoodMailStore.refresh(context)
            }
        }
        moodMailLifecycle.lifecycle.addObserver(obs)
        onDispose { moodMailLifecycle.lifecycle.removeObserver(obs) }
    }
    // 心情更新 → 自动推送邮件（push 内部会自动刷新 Flow）
    LaunchedEffect(moods.size) {
        autoPushMoodMails(context, moods)
    }

    // 应用筛选 / 搜索（不改原始列表，仅渲染层）
    val filteredMoods = remember(moods, filterEmoji, searchQuery) {
        moods.filter { e ->
            (filterEmoji == null || e.mood == filterEmoji) &&
                (searchQuery.isBlank() || e.note.contains(searchQuery, ignoreCase = true) || e.tags.contains(searchQuery, ignoreCase = true))
        }
    }

    val bgBitmap: androidx.compose.ui.graphics.ImageBitmap? = remember {
        try {
            ResourceStore.openInputStream("login/xinq_1.png")?.use {
                android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        } catch (e: Exception) { null }
    }

    val streak = remember(moods) { computeStreak(moods) }
    val today = remember { LocalDate.now() }
    val todayMoods = remember(moods) { moods.filter { it.date == today.toString() } }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 全屏背景图 ──
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFE0EC), Color(0xFFFFF3E0), Color(0xFFE3F2FD))
                        )
                    )
            )
        }
        // 极浅暖白滤镜，提升内容可读性，但不抢背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.12f))
        )

        // ── 内容滚动层 ──
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = bottomTabContentPadding(
                top = Spacing.none,
                horizontal = Spacing.none
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.none)
        ) {
            item(key = "_topbar") {
                MoodTopBar(
                    onBack = onNavigateBack,
                    streak = streak,
                    filterActive = showFilter || filterEmoji != null || searchQuery.isNotBlank(),
                    unreadCount = unreadCount,
                    onToggleFilter = { showFilter = !showFilter },
                    onOpenStats = { showStats = true },
                    onOpenMailbox = { showMailbox = true }
                )
            }
            item(key = "_hero") {
                TodayMoodHero(
                    todayMoods = todayMoods,
                    icons = moodIcons,
                    onPickEmoji = { pendingEmoji = it },
                    onManageIcons = { showMoodIconManager = true }
                )
            }
            item(key = "_view_toggle") {
                ViewModeToggle(
                    viewMode = viewMode,
                    onToggle = {
                        viewMode = if (viewMode == MoodViewMode.TIMELINE) MoodViewMode.CALENDAR else MoodViewMode.TIMELINE
                    }
                )
            }
            if (showFilter) {
                item(key = "_filter") {
                    FilterBar(
                        moods = moods,
                        filterEmoji = filterEmoji,
                        onFilterEmoji = { filterEmoji = if (filterEmoji == it) null else it },
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onClear = { filterEmoji = null; searchQuery = "" }
                    )
                }
            }
            item(key = "_section_title") { TimelineSectionTitle(count = filteredMoods.size, viewMode = viewMode) }

            when (viewMode) {
                MoodViewMode.TIMELINE -> {
                    if (filteredMoods.isEmpty()) {
                        item(key = "_empty") { MoodEmptyState() }
                    } else {
                        itemsIndexed(filteredMoods, key = { _, e -> e.id }) { idx, entry ->
                            val prev = filteredMoods.getOrNull(idx - 1)
                            val next = filteredMoods.getOrNull(idx + 1)
                            TimelineNodeRow(
                                entry = entry,
                                prevEntry = prev,
                                nextEntry = next,
                                showDate = prev == null || prev.date != entry.date,
                                isFirst = idx == 0,
                                isLast = idx == filteredMoods.size - 1,
                                onTap = { detailMood = entry },
                                onDelete = { deleteMood = entry }
                            )
                        }
                    }
                }
                MoodViewMode.CALENDAR -> {
                    item(key = "_calendar") {
                        MoodCalendarView(
                            moods = moods,
                            monthOffset = calendarMonthOffset,
                            onMonthChange = { calendarMonthOffset += it },
                            onPickDate = { calendarPickedDate = it }
                        )
                    }
                }
            }
        }

    }

    // ───── 弹窗 ─────
    pendingEmoji?.let { emoji ->
        QuickRecordSheet(
            emoji = emoji,
            onDismiss = { pendingEmoji = null },
            onConfirm = { note ->
                val lvl = metaOf(emoji).level
                viewModel.addMood(emoji, lvl, note)
                pendingEmoji = null
            }
        )
    }
    detailMood?.let { entry ->
        MoodDetailDialog(
            entry = entry,
            onDismiss = { detailMood = null },
            onDelete = {
                detailMood = null
                deleteMood = entry
            }
        )
    }
    deleteMood?.let { entry ->
        DeleteMoodDialog(
            onConfirm = { viewModel.deleteMood(entry); deleteMood = null },
            onDismiss = { deleteMood = null }
        )
    }
    if (showStats) {
        MoodStatsDialog(moods = moods, onDismiss = { showStats = false })
    }
    if (showMoodIconManager) {
        MoodIconManagerDialog(
            userId = currentUserId,
            onDismiss = { showMoodIconManager = false },
            onChanged = { moodIconsTick++ }
        )
    }
    if (showMailbox) {
        MoodMailboxDialog(
            mails = mails,
            onDismiss = { showMailbox = false },
            onAccept = { id ->
                com.example.funlife.data.MoodMailStore.update(context, id) { it.copy(accepted = true) }
            },
            onMarkRead = { id ->
                com.example.funlife.data.MoodMailStore.update(context, id) { it.copy(read = true) }
            }
        )
    }
    calendarPickedDate?.let { date ->
        DayMoodsDialog(
            date = date,
            entries = moods.filter { it.date == date },
            onDismiss = { calendarPickedDate = null },
            onPickEntry = { calendarPickedDate = null; detailMood = it },
            onAddNew = { calendarPickedDate = null; pendingEmoji = "😊" }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 顶部返回栏（纯图标，无背景，留出背景图心情日记水印的空间）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MoodTopBar(
    onBack: () -> Unit,
    streak: Int = 0,
    filterActive: Boolean = false,
    unreadCount: Int = 0,
    onToggleFilter: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenMailbox: () -> Unit = {}
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusTop + Spacing.xs, start = Spacing.sm, end = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF1A1A1A),
                modifier = Modifier
                    .size(36.rdp)
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .padding(6.dp)
            )
            // 连续打卡 chip（紧跟返回按钮，不再悬浮覆盖右侧）
            if (streak > 0) {
                Spacer(Modifier.width(6.dp))
                StreakOrb(streak = streak)
            }
            Spacer(Modifier.weight(1f))
            // 右侧三图标
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarIcon(emoji = "🔍", active = filterActive, accent = Color(0xFFFF6F91), onClick = onToggleFilter)
                TopBarIcon(emoji = "📊", active = false, accent = Color(0xFF7E57C2), onClick = onOpenStats)
                TopBarIcon(emoji = "📬", active = unreadCount > 0, accent = Color(0xFFFFB300), badgeCount = unreadCount, onClick = onOpenMailbox)
            }
        }
        // 背景图自带"心情日记"水印的展示区
        Spacer(Modifier.height(110.rdp))
    }
}

@Composable
private fun TopBarIcon(emoji: String, active: Boolean, accent: Color, badgeCount: Int = 0, onClick: () -> Unit) {
    Box(modifier = Modifier.size(38.rdp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(if (active) 6.dp else 3.dp, CircleShape, spotColor = accent)
                .clip(CircleShape)
                .background(
                    if (active)
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                    else
                        Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.9f)))
                )
                .border(1.dp, accent.copy(alpha = if (active) 0.0f else 0.45f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 16.rsp, textAlign = TextAlign.Center, style = EmojiCenteredStyle)
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(16.rdp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (badgeCount > 9) "9+" else "$badgeCount",
                    fontSize = 9.rsp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// 视图切换胶囊（独立保留）
@Composable
private fun ViewModeToggle(viewMode: MoodViewMode, onToggle: () -> Unit) {
    val (icon, label) = when (viewMode) {
        MoodViewMode.TIMELINE -> "📅" to "切换月历"
        MoodViewMode.CALENDAR -> "🌊" to "切换时间河"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .height(38.rdp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(Color.White.copy(alpha = 0.85f))
            .border(1.dp, Color(0xFFFFB7C9), RoundedCornerShape(Radius.pill))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 14.rsp)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = TextSize.xs, fontWeight = FontWeight.Black, color = Color(0xFF8B3F58))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 今日心情 Hero —— 一句问候 + 横滑 emoji 快选 + 今日已选状态
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun TodayMoodHero(
    todayMoods: List<MoodEntry>,
    icons: List<MoodIcon>,
    onPickEmoji: (String) -> Unit,
    onManageIcons: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val hour = remember { java.time.LocalTime.now().hour }
    val greet = remember(hour) {
        when (hour) {
            in 5..10 -> "早安，今天先记一笔吧"
            in 11..13 -> "午后好，此刻心情如何？"
            in 14..17 -> "下午好，记一记此刻"
            in 18..21 -> "晚上好，今天过得怎么样？"
            else -> "夜深了，留一句心情吧"
        }
    }
    val lastMood = todayMoods.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm)
    ) {
        Column {
            // 问候语
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧色条
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.rdp)
                        .clip(RoundedCornerShape(Radius.xs))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFF6B9D), Color(0xFFFF9472)))
                        )
                )
                Spacer(Modifier.width(Spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        greet,
                        fontSize = TextSize.title,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D1F2C)
                    )
                    Text(
                        today.format(DateTimeFormatter.ofPattern("M 月 d 日 · ")) + weekdayCn(today.toString()),
                        fontSize = TextSize.xs,
                        color = Color(0xFF8B5670),
                        fontWeight = FontWeight.Bold
                    )
                }
                // 今日已记状态
                if (lastMood != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(metaOf(lastMood.mood).color.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MoodGlyph(
                                idOrEmoji = lastMood.mood,
                                emojiFontSize = 12.rsp,
                                imageSize = 14.rdp
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "今日 ${todayMoods.size} 条",
                                fontSize = TextSize.tiny,
                                color = metaOf(lastMood.mood).color,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            // emoji 横滑快选（横向贴近屏幕边）
            EmojiQuickRow(icons = icons, onPickEmoji = onPickEmoji, onManage = onManageIcons)
            Spacer(Modifier.height(2.dp))
            Text(
                "点 emoji 即可快速记录 · 可补备注",
                fontSize = TextSize.tiny,
                color = Color(0xFFAD6584),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = Spacing.md + 2.dp)
            )
        }
    }
}

@Composable
private fun EmojiQuickRow(
    icons: List<MoodIcon>,
    onPickEmoji: (String) -> Unit,
    onManage: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        items(icons, key = { it.id }) { ic ->
            EmojiQuickChip(icon = ic, onClick = { onPickEmoji(ic.id) })
        }
        // 末尾的「管理」入口：长按某项也可以直接打开编辑（暂未做长按，先用按钮）
        item(key = "__manage__") {
            ManageMoodIconChip(onClick = onManage)
        }
    }
}

@Composable
private fun ManageMoodIconChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.rdp)
            .shadow(4.dp, CircleShape, spotColor = Color(0xFFFF6F91))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFE5EC), Color(0xFFFCE4EC))
                )
            )
            .border(1.5.dp, Color(0xFFFF6F91).copy(alpha = 0.55f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("＋", color = Color(0xFFFF6F91), fontSize = 22.rsp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EmojiQuickChip(icon: MoodIcon, onClick: () -> Unit) {
    val meta = MoodMeta(Color(icon.color), icon.label, icon.level)
    Box(
        modifier = Modifier
            .size(48.rdp)
            .shadow(4.dp, CircleShape, spotColor = meta.color)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f),
                        meta.color.copy(alpha = 0.28f)
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        MoodIconView(icon = icon, iconSize = 44.rdp, emojiFontSize = 22.rsp)
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 右上角悬浮：连续打卡金徽章
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun StreakOrb(streak: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(Radius.pill), spotColor = Color(0xFFFF7043))
            .clip(RoundedCornerShape(Radius.pill))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD66B),
                        Color(0xFFFF8F4F),
                        Color(0xFFFF5E62)
                    )
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(Radius.pill))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 14.rsp)
            Spacer(Modifier.width(3.dp))
            Text(
                "$streak 天",
                fontSize = TextSize.xs,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 时间线标题
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun TimelineSectionTitle(count: Int, viewMode: MoodViewMode = MoodViewMode.TIMELINE) {
    val (icon, title) = when (viewMode) {
        MoodViewMode.TIMELINE -> "🌊" to "心情时间河"
        MoodViewMode.CALENDAR -> "📅" to "心情月历"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.rsp)
        Spacer(Modifier.width(Spacing.xs))
        Text(
            title,
            fontSize = TextSize.title,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3D1F2C)
        )
        Spacer(Modifier.weight(1f))
        if (viewMode == MoodViewMode.TIMELINE && count > 0) {
            Text(
                "$count 朵心情",
                fontSize = TextSize.xs,
                color = Color(0xFF8B5670),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 时间河节点 —— 整页核心视觉
//
// 布局：[左 72dp：日期 + 时间轴线 + 节点 emoji 圆] [右：备注气泡（如有）]
// 时间轴线：上下贯穿 row 全高，渐变色，节点处用 emoji 圆覆盖；
// 节点大小 = 心情等级（38 / 44 / 50 / 56 / 60.rdp）
// 同一天多条：左侧日期只在第一条显示
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun TimelineNodeRow(
    entry: MoodEntry,
    prevEntry: MoodEntry?,
    nextEntry: MoodEntry?,
    showDate: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val meta = metaOf(entry.mood)
    val nodeSize = when (meta.level) {
        5 -> 56.rdp
        4 -> 50.rdp
        3 -> 44.rdp
        2 -> 40.rdp
        else -> 38.rdp
    }
    val prevColor = prevEntry?.let { metaOf(it.mood).color } ?: meta.color
    val nextColor = nextEntry?.let { metaOf(it.mood).color } ?: meta.color

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = Spacing.md, vertical = 5.dp)
    ) {
        // ── 左侧：日期标 + 轴线 + 节点圆 ──
        Box(modifier = Modifier.width(80.rdp).fillMaxHeight()) {
            // 时间轴上半段
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(3.dp)
                    .fillMaxHeight(0.5f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isFirst) Color.Transparent else prevColor.copy(alpha = 0.6f),
                                meta.color.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            // 时间轴下半段
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(3.dp)
                    .fillMaxHeight(0.5f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                meta.color.copy(alpha = 0.85f),
                                if (isLast) Color.Transparent else nextColor.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            // 节点 emoji 圆（3 层光晕）
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(nodeSize + 20.rdp),
                contentAlignment = Alignment.Center
            ) {
                // 最外层柔光晕
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    meta.color.copy(alpha = 0.32f),
                                    meta.color.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // 中层彩色环（纯彩色玻璃，不再用白）
                Box(
                    modifier = Modifier
                        .size(nodeSize + 8.rdp)
                        .clip(CircleShape)
                        .background(meta.color.copy(alpha = 0.22f))
                        .border(1.5.dp, meta.color.copy(alpha = 0.55f), CircleShape)
                )
                // 内层 emoji 圆玻璃体
                Box(
                    modifier = Modifier
                        .size(nodeSize)
                        .shadow(8.dp, CircleShape, spotColor = meta.color)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White,
                                    meta.color.copy(alpha = 0.18f),
                                    meta.color.copy(alpha = 0.4f)
                                )
                            )
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onTap() },
                    contentAlignment = Alignment.Center
                ) {
                    MoodGlyph(
                        idOrEmoji = entry.mood,
                        emojiFontSize = (nodeSize.value * 0.58f).sp,
                        imageSize = (nodeSize.value * 0.78f).dp,
                        textAlign = TextAlign.Center,
                        style = EmojiCenteredStyle
                    )
                }
            }
            // 日期标签——移动到节点圆下方，不遮挡 emoji
            if (showDate) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (nodeSize.value / 2 + 18).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(3.dp, RoundedCornerShape(Radius.pill), spotColor = meta.color)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(meta.color, meta.color.copy(alpha = 0.78f))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(Radius.pill))
                            .padding(horizontal = 9.dp, vertical = 2.dp)
                    ) {
                        Text(
                            fmt(entry.date, "M.d"),
                            fontSize = TextSize.tiny,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(2.dp))
        // ── 右侧：备注气泡 ──
        NoteBubble(
            entry = entry,
            meta = meta,
            onTap = onTap,
            onDelete = onDelete,
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun NoteBubble(
    entry: MoodEntry,
    meta: MoodMeta,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 左侧带尖角的气泡——加大尖角
    val bubbleShape = remember(meta.color) {
        GenericShape { size, _ ->
            val r = 30f
            val tail = 20f          // 尖角宽（变大）
            val tailH = 28f         // 尖角高（变大）
            val tailTop = size.height / 2 - tailH / 2
            val w = size.width
            val h = size.height
            moveTo(tail + r, 0f)
            lineTo(w - r, 0f)
            quadraticBezierTo(w, 0f, w, r)
            lineTo(w, h - r)
            quadraticBezierTo(w, h, w - r, h)
            lineTo(tail + r, h)
            quadraticBezierTo(tail, h, tail, h - r)
            lineTo(tail, tailTop + tailH)
            // 尖角平滑向左
            quadraticBezierTo(tail / 2, tailTop + tailH * 0.7f, 0f, tailTop + tailH / 2)
            quadraticBezierTo(tail / 2, tailTop + tailH * 0.3f, tail, tailTop)
            lineTo(tail, r)
            quadraticBezierTo(tail, 0f, tail + r, 0f)
            close()
        }
    }
    val hasNote = entry.note.isNotBlank()

    Box(
        modifier = modifier
            .heightIn(min = 68.rdp)
            .clip(bubbleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        meta.color.copy(alpha = 0.38f),
                        meta.color.copy(alpha = 0.22f),
                        meta.color.copy(alpha = 0.32f)
                    )
                )
            )
            .border(1.dp, meta.color.copy(alpha = 0.45f), bubbleShape)
            .clickable { onTap() }
    ) {
        // 右侧装饰大 emoji 水印（半透明，文化层次）
        MoodGlyph(
            idOrEmoji = entry.mood,
            emojiFontSize = 64.rsp,
            imageSize = 64.rdp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .graphicsLayer {
                    alpha = 0.12f
                    rotationZ = -10f
                }
        )
        // 右上角雅致删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp, top = 6.dp)
                .size(24.rdp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = meta.color.copy(alpha = 0.85f),
                modifier = Modifier.size(13.rdp)
            )
        }
        // 主体内容
        Column(
            modifier = Modifier
                .padding(start = 26.dp, end = 36.dp, top = Spacing.xs, bottom = Spacing.xs)
                .fillMaxWidth()
        ) {
            // 顶行：标签 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(Radius.pill), spotColor = meta.color)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(
                            Brush.horizontalGradient(
                                listOf(meta.color, meta.color.copy(alpha = 0.78f))
                            )
                        )
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(meta.label, fontSize = TextSize.tiny, color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(Spacing.xs))
                // 时间 with small dot icon
                Text("⏱", fontSize = 9.rsp)
                Spacer(Modifier.width(3.dp))
                Text(
                    timeOnly(entry.timestamp),
                    fontSize = TextSize.tiny,
                    color = Color(0xFF6B2D3F),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            // 备注
            if (hasNote) {
                Text(
                    entry.note,
                    fontSize = TextSize.sm,
                    color = Color(0xFF2B1320),
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.rsp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✍️", fontSize = 11.rsp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "点击补充心情细节",
                        fontSize = TextSize.tiny,
                        color = Color(0xFF8B5670),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer { alpha = 0.85f }
                    )
                }
            }
        }
    }
}

// 备注可滚动帕——限高 + 内容多时垂直滚动 + 能看到字符计数
@Composable
private fun ScrollableNoteBox(text: String, accent: Color) {
    val charCount = text.length
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.08f),
                        Color(0xFFFAF8F9),
                        accent.copy(alpha = 0.06f)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(Radius.lg))
    ) {
        // 左上装饰引号
        Text(
            "“",
            fontSize = 32.rsp,
            color = accent.copy(alpha = 0.35f),
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = (-4).dp)
        )
        // 右下装饰引号
        Text(
            "”",
            fontSize = 32.rsp,
            color = accent.copy(alpha = 0.35f),
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = 20.dp)
        )
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            // 限高 220dp，内容多则滚动
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.rdp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text,
                    fontSize = TextSize.sm,
                    color = Color(0xFF2B1320),
                    lineHeight = 22.rsp,
                    fontWeight = FontWeight.Medium
                )
            }
            // 底部微分割 + 字符数
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(accent.copy(alpha = 0.18f))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$charCount 字",
                    fontSize = TextSize.tiny,
                    color = accent.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

private fun timeOnly(ts: String): String = try {
    // timestamp 形如 yyyy-MM-ddTHH:mm:ss
    val t = ts.substringAfter('T', "")
    if (t.length >= 5) t.substring(0, 5) else ""
} catch (e: Exception) { "" }

// ═════════════════════════════════════════════════════════════════════════
// 空状态 —— 时间河（精致版：流光河面 + 漂浮心情瓶 + 呼吸提示）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MoodEmptyState() {
    // 漂浮 / 河面流光动画
    val infinite = rememberInfiniteTransition(label = "river")
    val floatY by infinite.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.lg)
            .heightIn(min = 240.rdp)
            .shadow(14.dp, RoundedCornerShape(Radius.xxl), spotColor = Color(0xFFFF9EBC))
            .clip(RoundedCornerShape(Radius.xxl))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE4EC),
                        Color(0xFFFFF1F2),
                        Color(0xFFFFF8E7),
                        Color(0xFFE8F5FF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.85f), Color(0xFFFFD9E2))
                ),
                shape = RoundedCornerShape(Radius.xxl)
            )
    ) {
        // ── 顶部漂浮装饰星 ──
        Text("🌸", fontSize = 14.rsp, modifier = Modifier
            .align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp)
            .graphicsLayer { alpha = 0.7f; translationY = floatY })
        Text("⭐", fontSize = 13.rsp, modifier = Modifier
            .align(Alignment.TopEnd).padding(end = 18.dp, top = 18.dp)
            .graphicsLayer { alpha = 0.65f; translationY = -floatY })
        Text("✨", fontSize = 12.rsp, modifier = Modifier
            .align(Alignment.CenterStart).padding(start = 10.dp)
            .graphicsLayer { alpha = 0.55f; translationY = floatY * 1.2f })
        Text("💫", fontSize = 13.rsp, modifier = Modifier
            .align(Alignment.CenterEnd).padding(end = 12.dp)
            .graphicsLayer { alpha = 0.55f; translationY = -floatY * 1.2f })

        // ── 河面波纹 SVG（Canvas 绘制流光）──
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            // 三层正弦波，相位错开形成流动感
            for (layer in 0..2) {
                val baseY = h * (0.45f + layer * 0.18f)
                val amp = 6f - layer * 1.5f
                val phase = shimmer * 2f * Math.PI.toFloat() + layer * 1.1f
                val path = Path().apply {
                    moveTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        val y = baseY + kotlin.math.sin(((x / w) * 4f * Math.PI.toFloat() + phase).toDouble()).toFloat() * amp
                        lineTo(x, y)
                        x += 6f
                    }
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFB7CE).copy(alpha = 0.18f - layer * 0.05f),
                            Color(0xFFFFD6E2).copy(alpha = 0.30f - layer * 0.08f)
                        )
                    )
                )
            }
        }

        // ── 中央内容 ──
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 大波浪 + 🕊️ 飞鸟 —— 治愈空灵，圆内 Canvas 自绘双层波浪，飞鸟漂浮其上
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        translationY = floatY
                        scaleX = pulse; scaleY = pulse
                    }
                    .shadow(14.dp, CircleShape, spotColor = Color(0xFF7EC8E3))
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFBEC),  // 顶部淡奶白（天空感）
                                Color(0xFFE8F5FF),
                                Color(0xFFB8E0F6),  // 浅蓝水面
                                Color(0xFF7EC8E3)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.92f), CircleShape)
            ) {
                // 圆内底部双层波浪
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    for (layer in 0..1) {
                        val baseY = h * (0.62f + layer * 0.14f)
                        val amp = 5f - layer * 1.5f
                        val phase = shimmer * 2f * Math.PI.toFloat() + layer * 1.4f
                        val path = Path().apply {
                            moveTo(0f, baseY)
                            var x = 0f
                            while (x <= w) {
                                val y = baseY + kotlin.math.sin(((x / w) * 3.5f * Math.PI.toFloat() + phase).toDouble()).toFloat() * amp
                                lineTo(x, y); x += 4f
                            }
                            lineTo(w, h); lineTo(0f, h); close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF7EC8E3).copy(alpha = 0.55f - layer * 0.15f),
                                    Color(0xFF4FA3D1).copy(alpha = 0.85f - layer * 0.20f)
                                )
                            )
                        )
                    }
                }

                // 飞鸟（向上微漂浮）
                Text(
                    "🕊️",
                    fontSize = 36.rsp,
                    style = EmojiCenteredStyle,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .graphicsLayer { translationY = floatY * 0.7f }
                )

                // 一两颗散落星点
                Text(
                    "✨",
                    fontSize = 12.rsp,
                    style = EmojiCenteredStyle,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 10.dp)
                        .graphicsLayer { alpha = 0.75f; translationY = -floatY * 0.6f }
                )
                Text(
                    "✨",
                    fontSize = 10.rsp,
                    style = EmojiCenteredStyle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 18.dp, top = 22.dp)
                        .graphicsLayer { alpha = 0.65f; translationY = floatY * 0.6f }
                )
            }

            Spacer(Modifier.height(2.dp))

            // 标题
            Text(
                "时间河还很安静",
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE53A6B)
            )

            // 副标题
            Text(
                "选一个 emoji，让第一朵心情漂下来吧",
                fontSize = TextSize.xs,
                color = Color(0xFFAD6584),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // 提示胶囊 —— 引导向上滑动
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color.White.copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.pill))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👆", fontSize = 12.rsp, style = EmojiCenteredStyle)
                Text(
                    "点上方 emoji 开始记录",
                    fontSize = TextSize.tiny,
                    color = Color(0xFFE53A6B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 快速记录抽屉 —— 从底部弹出
// ═════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickRecordSheet(
    emoji: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    val meta = metaOf(emoji)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            contentAlignment = Alignment.BottomCenter
        ) {
            ResponsiveDialogBox {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.xxl))
                        .background(Color.White)
                ) {
                    // 顶部 Hero 色块
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(meta.color, meta.color.copy(alpha = 0.7f))
                                )
                            )
                            .padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // emoji 大圆
                            Box(
                                modifier = Modifier
                                    .size(56.rdp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                MoodGlyph(idOrEmoji = emoji, emojiFontSize = 30.rsp, imageSize = 48.rdp)
                            }
                            Spacer(Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    meta.label,
                                    fontSize = TextSize.headline,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    "等级 ${meta.level} / 5",
                                    fontSize = TextSize.xs,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.rdp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                            }
                        }
                    }
                    // 备注输入
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            "说点什么？（可选）",
                            fontSize = TextSize.sm,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF616161),
                            modifier = Modifier.padding(bottom = Spacing.xs)
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { if (it.length <= 200) note = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("发生了什么有趣的事…", fontSize = TextSize.sm) },
                            shape = RoundedCornerShape(Radius.md),
                            minLines = 2,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = meta.color,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        Text(
                            "${note.length} / 200",
                            fontSize = TextSize.tiny,
                            color = Color(0xFFBDBDBD),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.xxs),
                            textAlign = TextAlign.End
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            // 跳过备注按钮
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.rdp)
                                    .clip(RoundedCornerShape(Radius.pill))
                                    .background(Color(0xFFF5F5F5))
                                    .clickable { onConfirm("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("直接保存", color = Color(0xFF616161), fontWeight = FontWeight.Black, fontSize = TextSize.sm)
                            }
                            // 保存（带备注）
                            Box(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(46.rdp)
                                    .shadow(8.dp, RoundedCornerShape(Radius.pill), spotColor = meta.color)
                                    .clip(RoundedCornerShape(Radius.pill))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(meta.color, meta.color.copy(alpha = 0.78f))
                                        )
                                    )
                                    .clickable { onConfirm(note.trim()) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "保存心情",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = TextSize.body,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 详情对话框
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MoodDetailDialog(entry: MoodEntry, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val meta = metaOf(entry.mood)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
            ) {
                // ── Hero 头部：渐变 + 装饰气泡 + 标签 + 关闭 + 大 emoji 圆 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.rdp)
                        .background(
                            Brush.linearGradient(
                                listOf(meta.color, meta.color.copy(alpha = 0.78f), meta.color.copy(alpha = 0.92f))
                            )
                        )
                ) {
                    // 装饰圆点（柔和泡泡感）
                    Box(
                        modifier = Modifier
                            .size(110.rdp)
                            .align(Alignment.TopStart)
                            .offset(x = (-30).rdp, y = (-30).rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                    Box(
                        modifier = Modifier
                            .size(70.rdp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 20.rdp, y = 20.rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Box(
                        modifier = Modifier
                            .size(34.rdp)
                            .align(Alignment.CenterStart)
                            .offset(x = 60.rdp, y = (-20).rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )

                    // 关闭按钮（右上）
                    Box(
                        modifier = Modifier
                            .size(32.rdp)
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                    }

                    // 标签胶囊（左上）
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.md)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color.White.copy(alpha = 0.28f))
                            .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(Radius.pill))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "✦ ${meta.label}",
                            fontSize = TextSize.xs,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    // 中心大 emoji 圆（带白圈光晕）
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 44.rdp)
                            .size(96.rdp)
                            .shadow(12.dp, CircleShape, spotColor = meta.color)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, meta.color.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MoodGlyph(idOrEmoji = entry.mood, emojiFontSize = 56.rsp, imageSize = 84.rdp)
                    }
                }
                Spacer(Modifier.height(54.rdp))

                // ── 情绪强度点阵 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "情绪强度",
                        fontSize = TextSize.tiny,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    repeat(5) { i ->
                        val active = i < meta.level
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) meta.color else Color(0xFFE0E0E0))
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // ── 日期块 ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        fmt(entry.date, "yyyy 年 M 月 d 日"),
                        fontSize = TextSize.title,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(meta.color.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                weekdayCn(entry.date),
                                fontSize = TextSize.tiny,
                                color = meta.color,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "⏱ " + timeOnly(entry.timestamp),
                            fontSize = TextSize.xs,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ── 备注块 ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                ) {
                    if (entry.note.isNotBlank()) {
                        ScrollableNoteBox(text = entry.note, accent = meta.color)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(Color(0xFFF6F6F8))
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✍️  这条心情没有留言",
                                fontSize = TextSize.xs,
                                color = Color(0xFFAAAAAA),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── 按钮区 ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.lg)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.rdp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color(0xFFFFF1F2))
                            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(Radius.pill))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(IconSize.sm)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("删除", color = Color(0xFFE53935), fontWeight = FontWeight.Black, fontSize = TextSize.sm)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(46.rdp)
                            .shadow(6.dp, RoundedCornerShape(Radius.pill), spotColor = meta.color)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(meta.color, meta.color.copy(alpha = 0.78f), meta.color)
                                )
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✦ 收起 ✦",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = TextSize.sm,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 删除确认
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun DeleteMoodDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color.White)
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.rdp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFE53935), modifier = Modifier.size(IconSize.lg))
                }
                Text("确认删除？", fontSize = TextSize.title, fontWeight = FontWeight.Black, color = Color(0xFF1A1A1A))
                Text("删除后无法恢复", fontSize = TextSize.xs, color = Color(0xFF9E9E9E), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.rdp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color(0xFFF5F5F5))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("再想想", color = Color(0xFF616161), fontWeight = FontWeight.Black, fontSize = TextSize.sm)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.rdp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFC62828)))
                            )
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("删除", color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.sm)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 筛选栏：emoji 横滑 + 关键字搜索
// ═════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    moods: List<MoodEntry>,
    filterEmoji: String?,
    onFilterEmoji: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val emojis = remember(moods) { moods.map { it.mood }.distinct() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("搜索备注 / 标签", fontSize = TextSize.sm, color = Color(0xFFB69BAA)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.rdp),
            shape = RoundedCornerShape(Radius.pill),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF6F91),
                unfocusedBorderColor = Color(0xFFFFB7C9),
                focusedContainerColor = Color.White.copy(alpha = 0.92f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.85f)
            )
        )
        if (emojis.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = Spacing.md)
            ) {
                items(emojis) { e ->
                    val active = filterEmoji == e
                    val meta = metaOf(e)
                    val iconObj = GLOBAL_MOOD_ICONS.firstOrNull { it.id == e }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(meta.color, meta.color.copy(alpha = 0.78f)))
                                else Brush.horizontalGradient(listOf(Color.White, Color.White.copy(alpha = 0.85f)))
                            )
                            .border(1.dp, meta.color.copy(alpha = if (active) 0.0f else 0.5f), RoundedCornerShape(Radius.pill))
                            .clickable { onFilterEmoji(e) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MoodGlyph(idOrEmoji = e, emojiFontSize = 14.rsp, imageSize = 16.rdp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                meta.label,
                                fontSize = TextSize.tiny,
                                color = if (active) Color.White else meta.color,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                if (filterEmoji != null || searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(Color(0xFFFFEBEE))
                                .clickable { onClear() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("✕ 清除", fontSize = TextSize.tiny, color = Color(0xFFE53935), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 心情月历视图（月份导航 + 7×6 格子，每格主导 emoji + 等级颜色）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MoodCalendarView(
    moods: List<MoodEntry>,
    monthOffset: Int,
    onMonthChange: (Int) -> Unit,
    onPickDate: (String) -> Unit
) {
    val baseMonth = remember { YearMonth.now() }
    val ym = remember(monthOffset) { baseMonth.plusMonths(monthOffset.toLong()) }
    val firstDow = ym.atDay(1).dayOfWeek.value % 7  // 周日=0
    val daysInMonth = ym.lengthOfMonth()
    // 每天主导 emoji（取等级最高的一条）
    val byDate = remember(moods, ym) {
        moods.filter {
            try { val d = LocalDate.parse(it.date); d.year == ym.year && d.month == ym.month } catch (e: Exception) { false }
        }.groupBy { it.date }
            .mapValues { (_, list) -> list.maxByOrNull { it.moodLevel } }
    }
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .clip(RoundedCornerShape(Radius.xl))
            .background(Color.White.copy(alpha = 0.88f))
            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.xl))
            .padding(Spacing.sm)
    ) {
        // 月份导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.rdp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE))
                    .clickable { onMonthChange(-1) },
                contentAlignment = Alignment.Center
            ) { Text("◀", fontSize = 12.rsp, color = Color(0xFFE53935), fontWeight = FontWeight.Black) }
            Spacer(Modifier.weight(1f))
            Text(
                "${ym.year} 年 ${ym.monthValue} 月",
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3D1F2C)
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.rdp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE))
                    .clickable { onMonthChange(1) },
                contentAlignment = Alignment.Center
            ) { Text("▶", fontSize = 12.rsp, color = Color(0xFFE53935), fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.height(Spacing.xs))
        // 周标题
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    it,
                    fontSize = TextSize.tiny,
                    color = Color(0xFF8B5670),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 6 行 × 7 列格子
        var dayIdx = 1 - firstDow
        repeat(6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    val d = dayIdx
                    val date = if (d in 1..daysInMonth) ym.atDay(d) else null
                    val key = date?.toString()
                    val entry = key?.let { byDate[it] }
                    val isToday = date == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(
                                when {
                                    entry != null -> metaOf(entry.mood).color.copy(alpha = 0.22f)
                                    date != null -> Color(0xFFFFFAFB)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                if (isToday) 1.5.dp else 0.5.dp,
                                if (isToday) Color(0xFFFF6F91) else Color(0xFFFFE0E8),
                                RoundedCornerShape(Radius.md)
                            )
                            .let { if (date != null) it.clickable { onPickDate(date.toString()) } else it },
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$d",
                                    fontSize = TextSize.tiny,
                                    color = if (isToday) Color(0xFFE53935) else Color(0xFF6B4F5A),
                                    fontWeight = FontWeight.Black
                                )
                                if (entry != null) {
                                    MoodGlyph(
                                        idOrEmoji = entry.mood,
                                        emojiFontSize = 14.rsp,
                                        imageSize = 18.rdp,
                                        textAlign = TextAlign.Center,
                                        style = EmojiCenteredStyle
                                    )
                                }
                            }
                        }
                    }
                    dayIdx++
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        // 图例
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "💡 点击有 emoji 的格子查看当日心情",
                fontSize = TextSize.tiny,
                color = Color(0xFF8B5670),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 当日心情列表（点击日历某天弹出）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun DayMoodsDialog(
    date: String,
    entries: List<MoodEntry>,
    onDismiss: () -> Unit,
    onPickEntry: (MoodEntry) -> Unit,
    onAddNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
                    .padding(Spacing.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            fmt(date, "yyyy 年 M 月 d 日"),
                            fontSize = TextSize.title,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            weekdayCn(date) + " · ${entries.size} 条记录",
                            fontSize = TextSize.xs,
                            color = Color(0xFF8B5670),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.rdp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(IconSize.sm)) }
                }
                Spacer(Modifier.height(Spacing.sm))
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Color(0xFFFFF1F2))
                            .padding(Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "这一天还没有心情记录 ✨",
                            fontSize = TextSize.sm,
                            color = Color(0xFFAD6584),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        entries.forEach { entry ->
                            val meta = metaOf(entry.mood)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Radius.lg))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(meta.color.copy(alpha = 0.18f), meta.color.copy(alpha = 0.08f))
                                        )
                                    )
                                    .border(1.dp, meta.color.copy(alpha = 0.35f), RoundedCornerShape(Radius.lg))
                                    .clickable { onPickEntry(entry) }
                                    .padding(Spacing.sm)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MoodGlyph(idOrEmoji = entry.mood, emojiFontSize = 22.rsp, imageSize = 28.rdp)
                                    Spacer(Modifier.width(Spacing.sm))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${meta.label} · " + timeOnly(entry.timestamp),
                                            fontSize = TextSize.sm,
                                            color = meta.color,
                                            fontWeight = FontWeight.Black
                                        )
                                        if (entry.note.isNotBlank()) {
                                            Text(
                                                entry.note,
                                                fontSize = TextSize.xs,
                                                color = Color(0xFF6B4F5A),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 心情数据看板 —— 全屏 Dialog
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MoodStatsDialog(moods: List<MoodEntry>, onDismiss: () -> Unit) {
    val today = remember { LocalDate.now() }
    val ym = remember { YearMonth.now() }
    val monthMoods = remember(moods) {
        moods.filter {
            try { val d = LocalDate.parse(it.date); d.year == ym.year && d.month == ym.month } catch (e: Exception) { false }
        }
    }
    val totalMonth = monthMoods.size
    val recordedDays = monthMoods.map { it.date }.distinct().size
    val avgLevel = if (monthMoods.isNotEmpty()) monthMoods.sumOf { it.moodLevel } / monthMoods.size.toDouble() else 0.0
    val happyCount = monthMoods.count { it.moodLevel >= 4 }
    val happyRate = if (monthMoods.isNotEmpty()) (happyCount * 100 / monthMoods.size) else 0
    val streak = computeStreak(moods)
    val lowStreak = computeLowStreak(moods)
    // Top3 emoji
    val top3 = remember(moods) {
        moods.groupingBy { it.mood }.eachCount().entries
            .sortedByDescending { it.value }.take(3)
    }
    // 近 30 天波形数据：每天平均等级
    val last30 = remember(moods) {
        (29 downTo 0).map { i ->
            val date = today.minusDays(i.toLong()).toString()
            val list = moods.filter { it.date == date }
            if (list.isEmpty()) 0.0 else list.sumOf { it.moodLevel } / list.size.toDouble()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero 头
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.rdp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6F91), Color(0xFFFF9472), Color(0xFFFFB347))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.rdp)
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = Spacing.lg)
                    ) {
                        Text("📊 心情数据看板", fontSize = TextSize.headline, fontWeight = FontWeight.Black, color = Color.White)
                        Text(
                            "${ym.year} 年 ${ym.monthValue} 月 · 第 ${today.dayOfMonth} 天",
                            fontSize = TextSize.xs,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (moods.isEmpty()) {
                        MoodStatsEmptyState(onDismiss = onDismiss)
                    } else {
                    // 4 个数据徽章
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        StatsCardBadge("📝", "$totalMonth", "本月记录", Color(0xFFFF6F91), Modifier.weight(1f))
                        StatsCardBadge("📅", "$recordedDays", "记录天数", Color(0xFF7E57C2), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        StatsCardBadge("😊", "$happyRate%", "好心情率", Color(0xFFFFB347), Modifier.weight(1f))
                        StatsCardBadge("🔥", "$streak 天", "连续打卡", Color(0xFFFF5E62), Modifier.weight(1f))
                    }

                    // 近30天波形
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.xl))
                            .padding(Spacing.md)
                    ) {
                        Text("近 30 天心情波形", fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
                        Spacer(Modifier.height(Spacing.xs))
                        Canvas(modifier = Modifier.fillMaxWidth().height(70.rdp)) {
                            val w = size.width
                            val h = size.height
                            val step = w / (last30.size - 1).coerceAtLeast(1)
                            val path = Path()
                            last30.forEachIndexed { i, v ->
                                val x = i * step
                                val y = h - (v / 5.0).toFloat().coerceIn(0f, 1f) * h
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFFB347))),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round)
                            )
                            // 0 线
                            drawLine(
                                color = Color(0xFFFFE0E8),
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                strokeWidth = 1f
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("30 天前", fontSize = TextSize.tiny, color = Color(0xFFAD6584), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("今日", fontSize = TextSize.tiny, color = Color(0xFFAD6584), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Top 3 心情
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.xl))
                            .padding(Spacing.md)
                    ) {
                        Text("最常出现 Top 3", fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
                        Spacer(Modifier.height(Spacing.xs))
                        if (top3.isEmpty()) {
                            Text("暂无数据", fontSize = TextSize.xs, color = Color(0xFFAD6584))
                        } else {
                            val maxCount = top3.first().value
                            top3.forEachIndexed { idx, e ->
                                val meta = metaOf(e.key)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("#${idx + 1}", fontSize = TextSize.tiny, color = meta.color, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.width(6.dp))
                                    Text(e.key, fontSize = 18.rsp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(meta.label, fontSize = TextSize.xs, color = Color(0xFF6B4F5A), fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(Radius.pill))
                                            .background(Color(0xFFFFF1F2))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(e.value.toFloat() / maxCount.toFloat())
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(Radius.pill))
                                                .background(
                                                    Brush.horizontalGradient(listOf(meta.color, meta.color.copy(alpha = 0.7f)))
                                                )
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("${e.value}", fontSize = TextSize.xs, color = meta.color, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // 状态摘要
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFF1F2), Color(0xFFFFFBF7))
                                )
                            )
                            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.xl))
                            .padding(Spacing.md)
                    ) {
                        Text("📌 本月状态", fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "平均情绪等级 ${"%.1f".format(avgLevel)} / 5",
                            fontSize = TextSize.xs,
                            color = Color(0xFF6B4F5A),
                            fontWeight = FontWeight.Medium
                        )
                        if (lowStreak >= 2) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "💗 检测到连续 $lowStreak 天低落，记得照顾自己",
                                fontSize = TextSize.xs,
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold
                            )
                        } else if (streak >= 3) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "🌟 已连续打卡 $streak 天，坚持就是胜利！",
                                fontSize = TextSize.xs,
                                color = Color(0xFFFFB347),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 收起按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.rdp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF9472)))
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦ 收起 ✦", color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.sm, letterSpacing = 2.sp)
                    }
                    } // end else (moods non-empty)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 数据看板空状态 —— 美化版（独立 Composable，避免空状态早返回破坏 Compose 槽表）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun ColumnScope.MoodStatsEmptyState(onDismiss: () -> Unit) {
    // 渐变光晕主卡
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFF1F2), Color(0xFFFFE9F0), Color(0xFFFFFBF7))
                )
            )
            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.xl))
            .padding(vertical = 28.dp, horizontal = Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 大徽章
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(10.dp, CircleShape, spotColor = Color(0xFFFF6F91))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF8FA6), Color(0xFFFF6F91), Color(0xFFFF9472))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 48.rsp, textAlign = TextAlign.Center, style = EmojiCenteredStyle)
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                "数据看板还在等待你",
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3D1F2C)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "记录第一条心情，这里就会绽放\n你的情绪波形 · Top3 排行 · 月度概览 ✨",
                fontSize = TextSize.xs,
                color = Color(0xFFAD6584),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 18.rsp
            )

            // 装饰条 —— 用 emoji 暗示未来内容
            Spacer(Modifier.height(Spacing.md))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "📝" to Color(0xFFFF6F91),
                    "📅" to Color(0xFF7E57C2),
                    "😊" to Color(0xFFFFB347),
                    "🔥" to Color(0xFFFF5E62)
                ).forEach { (e, c) ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(c.copy(alpha = 0.12f))
                            .border(1.dp, c.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = 18.rsp, style = EmojiCenteredStyle)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(Spacing.sm))

    // 主行动按钮 —— 跳去记录
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.rdp)
            .shadow(8.dp, RoundedCornerShape(Radius.pill), spotColor = Color(0xFFFF6F91))
            .clip(RoundedCornerShape(Radius.pill))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF9472), Color(0xFFFFB347)))
            )
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✨", fontSize = 18.rsp, style = EmojiCenteredStyle)
            Text(
                "去记录第一条心情",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = TextSize.sm,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StatsCardBadge(emoji: String, value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(Radius.xl), spotColor = accent)
            .clip(RoundedCornerShape(Radius.xl))
            .background(
                Brush.linearGradient(
                    listOf(Color.White, accent.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(Radius.xl))
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 渐变 emoji 圆头
            Box(
                modifier = Modifier
                    .size(38.rdp)
                    .shadow(4.dp, CircleShape, spotColor = accent)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MoodGlyph(
                    idOrEmoji = emoji,
                    emojiFontSize = 18.rsp,
                    imageSize = 30.rdp,
                    textAlign = TextAlign.Center,
                    style = EmojiCenteredStyle
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    value,
                    fontSize = 20.rsp,
                    color = accent,
                    fontWeight = FontWeight.Black,
                    style = EmojiCenteredStyle
                )
                Text(
                    label,
                    fontSize = TextSize.tiny,
                    color = Color(0xFF8B5670),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 心情驿站 —— 暖心彩蛋卡 / 鼓励奖章（保留废弃，仅留 CheerCard 数据类）
// ═════════════════════════════════════════════════════════════════════════
@Suppress("unused")
@Composable
private fun MoodCheerDialog_DEPRECATED(streak: Int, lowDays: Int, onDismiss: () -> Unit) {
    val (emoji, title, body, accent) = remember(streak, lowDays) {
        when {
            lowDays >= 3 -> CheerCard("🌷", "亲爱的，请抱抱自己", "连续 $lowDays 天有低落记录。要相信，再黑的夜也会黎明。给自己一杯热茶，深呼吸 3 次，世界没那么糟。", Color(0xFFFF6F91))
            lowDays == 2 -> CheerCard("☕", "暖一杯茶，慢慢来", "情绪有起伏很正常。今天试着记录一件小确幸吧 ✨", Color(0xFFFFB347))
            streak >= 7 -> CheerCard("🏅", "坚持周勋章", "已连续打卡 $streak 天 ! 你比 90% 的用户更懂自己的内心节奏。", Color(0xFFFFB300))
            streak >= 3 -> CheerCard("🌟", "习惯萌芽", "已连续 $streak 天打卡，你正在养成觉察情绪的好习惯。", Color(0xFF7E57C2))
            else -> CheerCard("💌", "今日心情邮件", "无论今天感觉如何，记录下来本身就是一份对自己的温柔。", Color(0xFFFF8F4F))
        }
    }
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
                        .height(140.rdp)
                        .background(
                            Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                        )
                ) {
                    // 装饰圆
                    Box(
                        modifier = Modifier
                            .size(90.rdp)
                            .align(Alignment.TopStart)
                            .offset(x = (-20).rdp, y = (-20).rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(60.rdp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 18.rdp, y = 18.rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Box(
                        modifier = Modifier
                            .size(28.rdp)
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 36.rdp)
                            .size(78.rdp)
                            .shadow(10.dp, CircleShape, spotColor = accent)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        MoodGlyph(idOrEmoji = emoji, emojiFontSize = 42.rsp, imageSize = 64.rdp)
                    }
                }
                Spacer(Modifier.height(46.rdp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = TextSize.title, fontWeight = FontWeight.Black, color = Color(0xFF1A1A1A))
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        body,
                        fontSize = TextSize.sm,
                        color = Color(0xFF6B4F5A),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.rsp
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.rdp)
                            .shadow(6.dp, RoundedCornerShape(Radius.pill), spotColor = accent)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("收下温暖 ❤", color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.sm, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
        }
    }
}

private data class CheerCard(val emoji: String, val title: String, val body: String, val accent: Color)

// ═════════════════════════════════════════════════════════════════════════
// 心情邮件 —— 自动推送规则
// 每条规则有冷却时间（避免同类型反复推送）：
//   - low_3d / low_2d / low_1d：连续低落
//   - streak_30 / streak_7 / streak_3：连续打卡里程碑
//   - first_record：首次记录欢迎
//   - daily_no_record：今日尚未记录（晚上 20 点后才推）
// ═════════════════════════════════════════════════════════════════════════
private fun autoPushMoodMails(ctx: android.content.Context, moods: List<MoodEntry>) {
    val now = System.currentTimeMillis()
    val store = com.example.funlife.data.MoodMailStore
    val cooldownMs = 24L * 3600_000L  // 同类型 24 小时只推一次

    fun pushIfReady(type: String, mail: () -> com.example.funlife.data.MoodMail) {
        if (now - store.lastPushAt(ctx, type) >= cooldownMs) {
            store.push(ctx, mail())
        }
    }

    // 首次记录欢迎
    if (moods.isNotEmpty()) {
        pushIfReady("welcome") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_welcome", type = "welcome", emoji = "💌",
                title = "欢迎来到心情驿站",
                body = "亲爱的旅人：\n\n看见你愿意把心情写下来，我很开心。这里会成为只属于你的小树洞——所有秘密都安全地藏在你手机里。\n\n慢慢来，不必急着开心，也不必假装坚强。每一种情绪都值得被温柔接住。\n\n— 你的小邮差",
                accentHex = 0xFFFF8F4F, createdAt = now
            )
        }
    }

    val streak = computeStreak(moods)
    when {
        streak >= 30 -> pushIfReady("streak_30") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_s30", type = "streak_30", emoji = "🏆",
                title = "30 天黄金勋章",
                body = "你已经连续记录 30 天了！\n\n这意味着你正在用最认真的方式爱自己。研究显示，坚持自我觉察的人，应对压力的能力比常人高出 67%。\n\n请收下这枚黄金勋章 🏆，它会一直亮在你的驿站里。",
                accentHex = 0xFFFFB300, createdAt = now
            )
        }
        streak >= 7 -> pushIfReady("streak_7") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_s7", type = "streak_7", emoji = "🏅",
                title = "周勋章已点亮",
                body = "连续 7 天的觉察日记 ✨\n\n你比 90% 的用户更懂自己的节奏。送你一句话：\n\n\"我们无法选择风浪，但能选择何时升帆。\"\n\n继续航行吧，captain ⛵",
                accentHex = 0xFFFF8F4F, createdAt = now
            )
        }
        streak >= 3 -> pushIfReady("streak_3") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_s3", type = "streak_3", emoji = "🌟",
                title = "习惯正在萌芽",
                body = "已连续 $streak 天打卡。\n\n据说一个习惯成型需要 21 天，你已经在 1/7 的路上了——而且最难的开头部分，已经被你迈过。\n\n继续闪耀 🌟",
                accentHex = 0xFF7E57C2, createdAt = now
            )
        }
    }

    val low = computeLowStreak(moods)
    when {
        low >= 3 -> pushIfReady("low_3d") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_l3", type = "low_3d", emoji = "🌷",
                title = "亲爱的，请抱抱自己",
                body = "连续 $low 天检测到你的情绪偏低。\n\n请允许我递给你一杯热茶 ☕。\n\n• 试试 4-7-8 呼吸：吸气 4 秒，屏息 7 秒，呼气 8 秒，重复 3 次。\n• 给身边一位朋友发一条「最近怎么样？」\n• 走到窗边，闭眼听 1 分钟外面的声音。\n\n再黑的夜，也有黎明。我会在邮箱里继续陪你。",
                accentHex = 0xFFFF6F91, createdAt = now
            )
        }
        low == 2 -> pushIfReady("low_2d") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_l2", type = "low_2d", emoji = "☕",
                title = "暖一杯茶，慢慢来",
                body = "情绪有起伏很正常。\n\n今天试着记录一件「小确幸」吧——\n\n· 中午多睡的 5 分钟\n· 路边突然飘来的桂花香\n· 一首听过几百遍仍想跟唱的歌\n\n积少成多，光会回来。",
                accentHex = 0xFFFFB347, createdAt = now
            )
        }
    }

    // 今日还没有记录（仅当本地时间 ≥ 20:00 推一次）
    val today = LocalDate.now().toString()
    val todayCount = moods.count { it.date == today }
    val hour = java.time.LocalTime.now().hour
    if (todayCount == 0 && hour >= 20 && moods.isNotEmpty()) {
        pushIfReady("evening_remind") {
            com.example.funlife.data.MoodMail(
                id = "m_${now}_ev", type = "evening_remind", emoji = "🌙",
                title = "今晚的你，怎么样？",
                body = "夜色温柔。\n\n今天还没有给自己留一句话呢。无论是 🥰 还是 😮‍💨，都来记一笔吧——\n\n这是你和自己最后的私语时间。",
                accentHex = 0xFF7E57C2, createdAt = now
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 心情邮箱 Dialog —— 列表视图：未签收信封 / 已签收 / 已读
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MoodMailboxDialog(
    mails: List<com.example.funlife.data.MoodMail>,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onMarkRead: (String) -> Unit
) {
    var openedMail by remember { mutableStateOf<com.example.funlife.data.MoodMail?>(null) }

    // 当前打开的信件如果列表里更新了，要同步显示新的状态
    val currentOpened = openedMail?.id?.let { id -> mails.find { it.id == id } } ?: openedMail

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
                        .height(110.rdp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFB300), Color(0xFFFF8F4F), Color(0xFFFF6F91))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.rdp)
                            .align(Alignment.TopStart)
                            .offset(x = (-15).rdp, y = (-15).rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(50.rdp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 15.rdp, y = 15.rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(32.rdp)
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .clickable {
                                if (currentOpened != null) openedMail = null else onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentOpened != null) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                        } else {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(IconSize.sm))
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = Spacing.lg)
                    ) {
                        Text(
                            if (currentOpened != null) "💌 ${currentOpened.title}" else "📬 心情邮箱",
                            fontSize = TextSize.headline,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (currentOpened != null) fmt(java.time.LocalDate.ofEpochDay(currentOpened.createdAt / 86400000L).toString(), "yyyy.M.d")
                            else "${mails.size} 封信件 · ${mails.count { !it.read }} 封未读",
                            fontSize = TextSize.xs,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 内容区
                if (currentOpened != null) {
                    MailDetailView(
                        mail = currentOpened,
                        onAccept = { onAccept(currentOpened.id); onMarkRead(currentOpened.id) },
                        onMarkRead = { if (!currentOpened.read) onMarkRead(currentOpened.id) }
                    )
                } else {
                    MailListView(
                        mails = mails,
                        onPick = { mail ->
                            openedMail = mail
                            // 已签收的信件——点击即标记为已读
                            if (mail.accepted && !mail.read) onMarkRead(mail.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MailListView(
    mails: List<com.example.funlife.data.MoodMail>,
    onPick: (com.example.funlife.data.MoodMail) -> Unit
) {
    if (mails.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 56.rsp)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "邮箱空空如也",
                    fontSize = TextSize.title,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D1F2C)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "继续记录心情，邮差会按你的状态\n递来温暖卡片 ✨",
                    fontSize = TextSize.xs,
                    color = Color(0xFFAD6584),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.rsp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.rdp),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            items(mails, key = { it.id }) { mail -> MailEnvelopeCard(mail = mail, onClick = { onPick(mail) }) }
        }
    }
}

@Composable
private fun MailEnvelopeCard(
    mail: com.example.funlife.data.MoodMail,
    onClick: () -> Unit
) {
    val accent = Color(mail.accentHex)
    val sealed = !mail.accepted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(
                if (sealed)
                    Brush.linearGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.08f)))
                else
                    Brush.linearGradient(listOf(Color.White, Color(0xFFFFFBF7)))
            )
            .border(1.dp, accent.copy(alpha = if (sealed) 0.45f else 0.25f), RoundedCornerShape(Radius.lg))
            .clickable { onClick() }
            .padding(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 信封图标
            Box(
                modifier = Modifier
                    .size(48.rdp)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(Radius.md)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (sealed) "✉️" else mail.emoji, fontSize = 22.rsp)
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mail.title,
                        fontSize = TextSize.sm,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D1F2C),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!mail.read) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935))
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (sealed) "✨ 来自驿站的新邮件 · 点击签收"
                    else mail.body.take(30) + if (mail.body.length > 30) "…" else "",
                    fontSize = TextSize.tiny,
                    color = if (sealed) accent else Color(0xFF8B5670),
                    fontWeight = if (sealed) FontWeight.Black else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    relativeTimeOf(mail.createdAt),
                    fontSize = TextSize.tiny,
                    color = Color(0xFFAD6584),
                    fontWeight = FontWeight.Medium
                )
            }
            // 状态标签
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(
                        when {
                            sealed -> accent
                            !mail.read -> Color(0xFFFFB300)
                            else -> Color(0xFFE0E0E0)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    when {
                        sealed -> "签收"
                        !mail.read -> "未读"
                        else -> "已读"
                    },
                    fontSize = TextSize.tiny,
                    color = if (sealed || !mail.read) Color.White else Color(0xFF757575),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun MailDetailView(
    mail: com.example.funlife.data.MoodMail,
    onAccept: () -> Unit,
    onMarkRead: () -> Unit
) {
    val accent = Color(mail.accentHex)
    val sealed = !mail.accepted

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.rdp)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        if (sealed) {
            // 未签收：显示密封信封 + 签收按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f)))
                    )
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(Radius.xl))
                    .padding(vertical = 28.dp, horizontal = Spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✉️", fontSize = 64.rsp)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "一封未拆封的信",
                        fontSize = TextSize.title,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D1F2C)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "邮差刚刚送来，签收后即可阅读",
                        fontSize = TextSize.xs,
                        color = Color(0xFFAD6584),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.rdp)
                    .shadow(8.dp, RoundedCornerShape(Radius.pill), spotColor = accent)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(
                        Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f), accent))
                    )
                    .clickable { onAccept() },
                contentAlignment = Alignment.Center
            ) {
                Text("✦ 签 收 ✦", color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.sm, letterSpacing = 4.sp)
            }
        } else {
            // 已签收：展示完整信件
            LaunchedEffect(mail.id) { onMarkRead() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.rdp)
                        .shadow(6.dp, CircleShape, spotColor = accent)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, accent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mail.emoji, fontSize = 28.rsp)
                }
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text(mail.title, fontSize = TextSize.title, fontWeight = FontWeight.Black, color = Color(0xFF1A1A1A))
                    Text(
                        "from 心情驿站邮差 · " + relativeTimeOf(mail.createdAt),
                        fontSize = TextSize.tiny,
                        color = Color(0xFFAD6584),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            // 信纸
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.06f), Color(0xFFFFFBF7))
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(Radius.lg))
                    .padding(Spacing.md)
            ) {
                Text(
                    mail.body,
                    fontSize = TextSize.sm,
                    color = Color(0xFF3D1F2C),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.rsp
                )
            }
        }
    }
}

private fun relativeTimeOf(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3600_000} 小时前"
        diff < 7L * 86_400_000 -> "${diff / 86_400_000} 天前"
        else -> {
            val date = java.util.Date(millis)
            java.text.SimpleDateFormat("M.d", java.util.Locale.CHINA).format(date)
        }
    }
}
