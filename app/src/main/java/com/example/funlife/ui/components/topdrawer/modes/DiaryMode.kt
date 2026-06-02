// ═══════════════════════════════════════════════════════════════════════════
// DiaryMode.kt
// 模式 B：今天的一页日记本
//   · 顶部：日期 / 天气 / 今日心情统计
//   · 中部：今日发生事件流水（自动聚合 MoodEntry，预留 Habit/Bill 扩展点）
//   · 底部：一句话留言入口（点击跳转心情页快速记录）
//
// 数据获取：复用 MoodRepository.getAllMoods，仅 filter 当日，不动 DAO/Migration
// 适配：Spacing / Radius / TextSize / IconSize / rdp / rsp
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.repository.MoodRepository
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DiaryMode(userId: Long) {
    val ctx = LocalContext.current
    val today = remember { LocalDate.now() }
    val dateStr = today.toString()
    val scope = rememberCoroutineScope()

    // ★ 草稿持久化：每天一个独立 key，避免抽屉关闭/进程重启导致用户输入丢失
    val draftPrefs = remember { ctx.getSharedPreferences("diary_draft", android.content.Context.MODE_PRIVATE) }
    val draftKey = "draft_${userId}_${dateStr}"

    var todayMoods by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    // 初始值从 prefs 恢复（用户上次未完成的草稿）
    var quickNote by remember { mutableStateOf(draftPrefs.getString(draftKey, "") ?: "") }
    var saving by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // 重载今日心情列表（保存后调用）
    suspend fun reload() {
        if (userId <= 0L) return
        runCatching {
            val repo = MoodRepository(AppDatabase.getDatabase(ctx).moodDao())
            todayMoods = repo.getAllMoods(userId).first()
                .filter { it.date == dateStr }
                .sortedBy { it.timestamp }
        }
    }

    // 保存快速心情
    fun saveQuickNote() {
        val text = quickNote.trim()
        if (text.isBlank() || saving || userId <= 0L) return
        saving = true
        scope.launch {
            runCatching {
                val repo = MoodRepository(AppDatabase.getDatabase(ctx).moodDao())
                repo.insertMood(
                    MoodEntry(
                        userId = userId,
                        date = dateStr,
                        mood = "😌",
                        moodLevel = 3,
                        note = text,
                        timestamp = LocalDateTime.now().toString()
                    )
                )
                quickNote = ""
                draftPrefs.edit().remove(draftKey).apply()    // 提交成功后清除草稿
                reload()
            }
            saving = false
        }
    }

    // 草稿自动写盘：用户每输入一个字符就同步到 SharedPreferences
    // （比 onDispose 更稳：即使进程被杀也不丢；只在内容真的改变时才写）
    fun onQuickNoteChange(new: String) {
        val truncated = new.take(200)
        if (truncated == quickNote) return
        quickNote = truncated
        draftPrefs.edit().putString(draftKey, truncated).apply()
    }

    LaunchedEffect(userId, dateStr) {
        if (userId <= 0L) { loaded = true; return@LaunchedEffect }
        runCatching {
            val repo = MoodRepository(AppDatabase.getDatabase(ctx).moodDao())
            val all = repo.getAllMoods(userId).first()
            todayMoods = all.filter { it.date == dateStr }.sortedBy { it.timestamp }
        }
        loaded = true
    }

    // 时间线条目：当前只接入 mood；未来可往这个 list 里加 habit/bill 类型条目
    val timeline: List<DiaryEvent> = remember(todayMoods) {
        buildList {
            todayMoods.forEach { m ->
                add(
                    DiaryEvent(
                        time = parseTimeOnly(m.timestamp),
                        icon = m.mood,
                        title = "记下心情",
                        detail = if (m.note.isBlank()) "（未写备注）" else m.note,
                        accent = 0xFFFF8FB1
                    )
                )
            }
        }.sortedBy { it.time }
    }

    // 外层填满整个 page（消除 Pager 切换缝隙），卡片视觉用内层 padding 实现
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF8E7), Color(0xFFFFF0DC), Color(0xFFFCE4C4))
                )
            )
            .padding(horizontal = Spacing.md)
            .clip(RoundedCornerShape(Radius.xxl))
            .border(1.dp, Color(0xFFB8A78A).copy(alpha = 0.25f), RoundedCornerShape(Radius.xxl))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),                 // ★ IME 弹起时 LazyColumn 高度收缩
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // 顶部日期 + 装饰横线
            item("_header") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        today.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")),
                        fontSize = TextSize.title,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5D3D1F)
                    )
                    Text(
                        weekdayCn(today) + " · " + greetingByHour(),
                        fontSize = TextSize.tiny,
                        color = Color(0xFF8B6F4E),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .width(60.rdp)
                            .height(2.rdp)
                            .background(Color(0xFFB8A78A))
                    )
                }
            }

            // 统计气泡：今日心情数
            item("_stats") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    StatChip(emoji = "💭", value = "${todayMoods.size}", label = "条心情")
                    StatChip(emoji = "📝", value = "${timeline.size}", label = "条记录")
                    StatChip(emoji = "⏳", value = "${java.time.LocalTime.now().hour}h", label = "已度过")
                }
            }

            // 流水分隔线
            item("_divider") {
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFB8A78A).copy(alpha = 0.3f))
                )
                Spacer(Modifier.height(Spacing.xs))
            }

            // 空态
            if (loaded && timeline.isEmpty()) {
                item("_empty") {
                    EmptyDiaryBlock()
                }
            }

            // 时间线条目
            items(timeline, key = { it.time + it.title + it.detail.hashCode() }) { e ->
                DiaryEventRow(event = e)
            }

            // 输入区作为 LazyColumn 最后一项 —— 紧跟时间线，IME 弹起自动滚动到可见
            item("_quicknote") {
                Spacer(Modifier.height(Spacing.sm))
                DiaryQuickNoteBar(
                    text = quickNote,
                    onTextChange = { onQuickNoteChange(it) },     // ★ 写入 prefs
                    saving = saving,
                    focusRequester = focusRequester,
                    onSubmit = { saveQuickNote() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────
   日记快速笔记输入区（横线纸风格 + 发送按钮）
   ──────────────────────────────────────────────────────────── */
@Composable
private fun DiaryQuickNoteBar(
    text: String,
    onTextChange: (String) -> Unit,
    saving: Boolean,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val lineGap = 28.dp
    val lineColor = Color(0xFFB8A78A).copy(alpha = 0.55f)
    val accentLineColor = Color(0xFFE08070).copy(alpha = 0.55f)
    // 整个输入区直接坐在米黄纸面上（无白色外壳），上下分别用一道虚化分隔
    Column(
        modifier = modifier
            .bringIntoViewRequester(bringIntoView)
            // 顶部一道古朴的双线分隔（粗+细）
            .drawBehind {
                drawLine(
                    color = Color(0xFF8B6F4E).copy(alpha = 0.25f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.8f
                )
                drawLine(
                    color = Color(0xFF8B6F4E).copy(alpha = 0.15f),
                    start = Offset(0f, 4f),
                    end = Offset(size.width, 4f),
                    strokeWidth = 0.6f
                )
            }
            .padding(top = Spacing.md, bottom = Spacing.sm)
    ) {
        // 标题行：左 ✒ 字样 + 右"墨水"小圆点装饰；保存按钮（橙色圆）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                "✒",
                fontSize = 14.sp,
                color = Color(0xFF8B6F4E),
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                "现在的我想说……",
                fontSize = TextSize.sm,
                color = Color(0xFF8B6F4E),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            // 字数计 + 保存中状态
            Text(
                "${text.length} / 200" + if (saving) "  · 写入中…" else "",
                fontSize = 10.sp,
                color = Color(0xFF8B6F4E).copy(alpha = 0.55f),
                modifier = Modifier.padding(end = if (text.isNotBlank()) 8.dp else 0.dp)
            )
            if (text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFA486), Color(0xFFE07A5E))
                            )
                        )
                        .clickable(enabled = !saving) { onSubmit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "保存",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        // 横线纸输入区：背景透明，直接绘制装订红线 + 横线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clickable { focusRequester.requestFocus() }
                .drawBehind {
                    // 横线（避开第 0 行，从第 1 行开始）
                    val gapPx = lineGap.toPx()
                    var y = gapPx
                    while (y < size.height + 0.5f) {
                        drawLine(
                            color = lineColor,
                            start = Offset(28f, y),
                            end = Offset(size.width - 4f, y),
                            strokeWidth = 0.9f
                        )
                        y += gapPx
                    }
                    // 装订红线（左侧贯穿整个区域）
                    drawLine(
                        color = accentLineColor,
                        start = Offset(20f, 0f),
                        end = Offset(20f, size.height),
                        strokeWidth = 1.2f
                    )
                }
                .padding(start = 28.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    "在这里轻轻写下此刻心情，写完点右上发送收藏…",
                    fontSize = TextSize.sm,
                    color = Color(0xFF8B6F4E).copy(alpha = 0.50f),
                    fontStyle = FontStyle.Italic,
                    lineHeight = 28.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            scope.launch { bringIntoView.bringIntoView() }
                        }
                    },
                textStyle = TextStyle(
                    color = Color(0xFF5C4530),
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF8A65)),
                // ★ Default 而非 Done：让用户能用回车自由换行；保存只通过右上角发送按钮
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                maxLines = Int.MAX_VALUE       // 显式声明：内容超过 3 行时高度自动增长，没有上限
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 数据
// ─────────────────────────────────────────────────────────────────────────
private data class DiaryEvent(
    val time: String,            // "HH:mm"
    val icon: String,            // emoji 或 mood id（暂只显示文本，自定义图标后续可接 MoodIconView）
    val title: String,
    val detail: String,
    val accent: Long             // ARGB
)

// ─────────────────────────────────────────────────────────────────────────
// 子组件
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun StatChip(emoji: String, value: String, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(Color.White.copy(alpha = 0.65f))
            .border(1.dp, Color(0xFFB8A78A).copy(alpha = 0.35f), RoundedCornerShape(Radius.pill))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.rsp)
            Spacer(Modifier.width(4.dp))
            Text(value, fontSize = TextSize.sm, fontWeight = FontWeight.Black, color = Color(0xFF5D3D1F))
            Spacer(Modifier.width(3.dp))
            Text(label, fontSize = TextSize.tiny, color = Color(0xFF8B6F4E))
        }
    }
}

@Composable
private fun DiaryEventRow(event: DiaryEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 时间
        Text(
            event.time,
            fontSize = TextSize.tiny,
            color = Color(0xFF8B6F4E),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.rdp)
        )
        Spacer(Modifier.width(Spacing.xs))
        // 图标圆点
        Box(
            modifier = Modifier
                .size(24.rdp)
                .clip(CircleShape)
                .background(Color(event.accent).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(event.icon, fontSize = 12.rsp)
        }
        Spacer(Modifier.width(Spacing.xs))
        // 标题 + 详情
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontSize = TextSize.sm,
                color = Color(0xFF5D3D1F),
                fontWeight = FontWeight.Black
            )
            Text(
                event.detail,
                fontSize = TextSize.tiny,
                color = Color(0xFF8B6F4E),
                fontStyle = FontStyle.Italic,
                lineHeight = 18.rsp
            )
        }
    }
}

@Composable
private fun EmptyDiaryBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📖", fontSize = 36.rsp)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "今天还没有记录哦",
            fontSize = TextSize.md,
            color = Color(0xFF5D3D1F),
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "去主页记一笔心情或打个卡，\n这页日记就会自动填满",
            fontSize = TextSize.tiny,
            color = Color(0xFF8B6F4E),
            lineHeight = 18.rsp,
            fontStyle = FontStyle.Italic
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 工具
// ─────────────────────────────────────────────────────────────────────────
private fun parseTimeOnly(timestamp: String): String = try {
    LocalDateTime.parse(timestamp).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: Exception) { "--:--" }

private fun weekdayCn(d: LocalDate): String = when (d.dayOfWeek) {
    java.time.DayOfWeek.MONDAY -> "周一"
    java.time.DayOfWeek.TUESDAY -> "周二"
    java.time.DayOfWeek.WEDNESDAY -> "周三"
    java.time.DayOfWeek.THURSDAY -> "周四"
    java.time.DayOfWeek.FRIDAY -> "周五"
    java.time.DayOfWeek.SATURDAY -> "周六"
    java.time.DayOfWeek.SUNDAY -> "周日"
}

private fun greetingByHour(): String = when (java.time.LocalTime.now().hour) {
    in 5..10 -> "晨光熹微"
    in 11..13 -> "正午时分"
    in 14..16 -> "午后悠然"
    in 17..19 -> "黄昏将至"
    in 20..22 -> "夜幕低垂"
    else -> "深夜独白"
}
