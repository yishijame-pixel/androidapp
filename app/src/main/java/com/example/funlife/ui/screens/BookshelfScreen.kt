// BookshelfScreen.kt — 方案 F · 人生书架（v52 重新设计）
//
// 设计语言：
//   - 沉浸式米色背景，渐变到暖橙，营造"图书馆/羊皮纸"质感
//   - Hero 区：今年读完计数 + 金色"AI 年鉴" CTA 大按钮，渐变 + 阴影
//   - 书目卡片：左侧色条按评分上色 / 书名加粗大字 / 灰色作者 / 星行 + 日期 / 标签 chip
//   - 编辑器改用 ModalBottomSheet，更轻盈
//   - 空状态：插画式 emoji + 引导
//   - 隐藏底部 nav bar（已在 MainActivity 配置）
package com.example.funlife.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.Book
import com.example.funlife.viewmodel.BookViewModel
import com.example.funlife.viewmodel.YearbookUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
// 配色（沉浸式书页主题）
// ═══════════════════════════════════════════════════════════════
private val PageBgTop      = Color(0xFFFFF8EC)   // 顶部象牙白
private val PageBgBottom   = Color(0xFFF7E9D2)   // 底部暖米黄
private val InkPrimary     = Color(0xFF3E2723)   // 墨黑棕
private val InkSecondary   = Color(0xFF8D6E63)   // 暖灰棕
private val InkMuted       = Color(0xFFBCAAA4)
private val GoldStart      = Color(0xFFFFB300)
private val GoldMid        = Color(0xFFFF8F00)
private val GoldEnd        = Color(0xFFD84315)
private val AccentTagBg    = Color(0xFFFFF0E0)
private val CardBg         = Color(0xFFFFFCF5)

// ═══════════════════════════════════════════════════════════════
// 主页面
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    userId: Long,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val vm: BookViewModel = viewModel(
        key = "BookViewModel_$userId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                BookViewModel(app, userId) as T
        }
    )
    val books by vm.books.collectAsState()
    val total by vm.totalCount.collectAsState()
    val yearbookState by vm.yearbook.collectAsState()

    var editing by remember { mutableStateOf<Book?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Book?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageBgTop, PageBgBottom)))
    ) {
        // 顶栏 + 内容
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // 自定义顶栏
            TopBar(onBack = onNavigateBack, total = total)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp, end = 18.dp,
                    top = 6.dp, bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { HeroCard(yearCount = booksFinishedThisYear(books), total = total, onGenerate = { vm.generateYearbook() }) }

                if (books.isEmpty()) {
                    item { EmptyState() }
                } else {
                    item {
                        Text(
                            "我的书架",
                            color = InkPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    items(books, key = { it.id }) { b ->
                        BookCard(
                            book = b,
                            onClick = { editing = b; showEditor = true },
                            onDelete = { pendingDelete = b }
                        )
                    }
                }
            }
        }

        // 浮动添加按钮（自定义 - 比 FAB 更精致）
        FloatingAddButton(
            onClick = { editing = null; showEditor = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 32.dp)
                .navigationBarsPadding()
        )
    }

    // ─── 编辑器（底部 Sheet）
    if (showEditor) {
        BookEditorSheet(
            initial = editing,
            onDismiss = { showEditor = false; editing = null },
            onSave = { title, author, rating, finishedAt, note, fav, tags ->
                vm.saveBook(editing, title, author, rating, finishedAt, note, fav, tags)
                showEditor = false; editing = null
            }
        )
    }

    // ─── 删除确认
    pendingDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除「${b.title}」？") },
            text = { Text("删除后笔记和摘抄无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteBook(b); pendingDelete = null }) {
                    Text("删除", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
            containerColor = CardBg
        )
    }

    // ─── 年鉴对话框
    YearbookDialog(state = yearbookState, onDismiss = { vm.resetYearbook() })
}

// ═══════════════════════════════════════════════════════════════
// 顶栏
// ═══════════════════════════════════════════════════════════════
@Composable
private fun TopBar(onBack: () -> Unit, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "返回", tint = InkPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text("📚 人生书架", color = InkPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "记录每一本读过的书",
                color = InkSecondary, fontSize = 11.sp
            )
        }
        Surface(
            color = AccentTagBg, shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text("$total 本",
                color = GoldEnd, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Hero 区：年度统计 + AI 年鉴 CTA
// ═══════════════════════════════════════════════════════════════
@Composable
private fun HeroCard(yearCount: Int, total: Int, onGenerate: () -> Unit) {
    // 金色光晕呼吸动画
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = GoldEnd.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFF7E1), Color(0xFFFFE0B2), Color(0xFFFFCCBC))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$yearCount",
                color = InkPrimary, fontSize = 56.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("今年读完", color = InkSecondary, fontSize = 13.sp)
                Text("总计 $total 本", color = InkMuted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // 金色 CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = GoldEnd)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(GoldStart, GoldMid, GoldEnd)))
                .clickable(onClick = onGenerate)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null,
                    tint = Color.White.copy(alpha = pulse),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("生成我的 AI 阅读年鉴",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("VIP3", color = Color.White,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 书目卡片
// ═══════════════════════════════════════════════════════════════
@Composable
private fun BookCard(book: Book, onClick: () -> Unit, onDelete: () -> Unit) {
    val ratingColor = when (book.rating) {
        5 -> Color(0xFFD32F2F)   // 5 星：红
        4 -> Color(0xFFF57C00)   // 4 星：橙
        3 -> Color(0xFFFBC02D)   // 3 星：黄
        2 -> Color(0xFF7CB342)   // 2 星：绿
        1 -> Color(0xFF607D8B)   // 1 星：灰蓝
        else -> InkMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
    ) {
        // 左侧色条（按评分着色）
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(ratingColor)
        )

        Column(Modifier.weight(1f).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    book.title,
                    color = InkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = InkMuted, modifier = Modifier.size(18.dp))
                }
            }
            if (book.author.isNotBlank()) {
                Text(book.author, color = InkSecondary, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRow(book.rating, size = 14.dp)
                if (book.finishedAt > 0L) {
                    Text(
                        "  ·  " + SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(Date(book.finishedAt)),
                        color = InkMuted, fontSize = 11.sp
                    )
                } else {
                    Text("  ·  阅读中", color = Color(0xFFFF8F00), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            if (book.note.isNotBlank()) {
                Text(
                    book.note.take(70) + if (book.note.length > 70) "…" else "",
                    color = InkSecondary, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (book.tags.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                TagRow(book.tags)
            }
        }
    }
}

@Composable
private fun StarRow(rating: Int, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Row {
        repeat(5) { i ->
            if (i < rating) Icon(Icons.Filled.Star, null, tint = GoldMid, modifier = Modifier.size(size))
            else Icon(Icons.Outlined.Star, null, tint = InkMuted, modifier = Modifier.size(size))
        }
    }
}

@Composable
private fun TagRow(tagsRaw: String) {
    val tags = tagsRaw.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }.take(4)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.forEach { t ->
            Surface(
                color = AccentTagBg, shape = RoundedCornerShape(20.dp)
            ) {
                Text("# $t",
                    color = GoldEnd, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 空状态
// ═══════════════════════════════════════════════════════════════
@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(110.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.MenuBook, null, tint = GoldMid, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("书架还空空的",
            color = InkPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("点击右下角 ＋ 记录你的第一本书",
            color = InkSecondary, fontSize = 12.sp)
    }
}

// ═══════════════════════════════════════════════════════════════
// 浮动添加按钮（自定义渐变 + 阴影）
// ═══════════════════════════════════════════════════════════════
@Composable
private fun FloatingAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(12.dp, CircleShape, spotColor = GoldEnd)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(GoldStart, GoldEnd)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "添加", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// 编辑器 ModalBottomSheet
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookEditorSheet(
    initial: Book?,
    onDismiss: () -> Unit,
    onSave: (title: String, author: String, rating: Int, finishedAt: Long, note: String, fav: String, tags: String) -> Unit,
) {
    var title by rememberSaveable(initial) { mutableStateOf(initial?.title ?: "") }
    var author by rememberSaveable(initial) { mutableStateOf(initial?.author ?: "") }
    var rating by rememberSaveable(initial) { mutableStateOf(initial?.rating ?: 0) }
    var note by rememberSaveable(initial) { mutableStateOf(initial?.note ?: "") }
    var fav by rememberSaveable(initial) { mutableStateOf(initial?.favoriteQuote ?: "") }
    var tags by rememberSaveable(initial) { mutableStateOf(initial?.tags ?: "") }
    val finished by rememberSaveable(initial) {
        mutableStateOf(initial?.finishedAt?.takeIf { it > 0 } ?: System.currentTimeMillis())
    }
    var markAsFinished by rememberSaveable(initial) {
        mutableStateOf(initial?.finishedAt?.let { it > 0 } ?: true)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PageBgTop,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InkMuted) }
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial == null) "✨ 添加一本书" else "✏️ 编辑书目",
                color = InkPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            FieldLabel("书名 *")
            BookTextField(value = title, onValueChange = { title = it.take(120) }, singleLine = true)

            FieldLabel("作者")
            BookTextField(value = author, onValueChange = { author = it.take(80) }, singleLine = true)

            FieldLabel("评分（点击星星打分）")
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    IconButton(onClick = { rating = if (rating == i + 1) 0 else i + 1 }) {
                        if (i < rating) Icon(Icons.Filled.Star, null, tint = GoldMid, modifier = Modifier.size(28.dp))
                        else Icon(Icons.Outlined.Star, null, tint = InkMuted, modifier = Modifier.size(28.dp))
                    }
                }
                if (rating > 0) {
                    Text("$rating 星",
                        color = GoldEnd, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = markAsFinished, onCheckedChange = { markAsFinished = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = GoldMid)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (markAsFinished) "已标记为读完" else "还在阅读中",
                    color = InkPrimary, fontSize = 13.sp
                )
            }

            FieldLabel("我的心得（最多 4000 字）")
            BookTextField(
                value = note, onValueChange = { note = it.take(4000) },
                minLines = 3, maxLines = 6,
                placeholder = "这本书带给我什么..."
            )

            FieldLabel("最爱的一句话")
            BookTextField(
                value = fav, onValueChange = { fav = it.take(800) },
                minLines = 2, maxLines = 4,
                placeholder = "摘抄一段最打动你的文字"
            )

            FieldLabel("标签（逗号分隔）")
            BookTextField(
                value = tags, onValueChange = { tags = it.take(200) },
                singleLine = true,
                placeholder = "成长, 小说, 推荐"
            )

            // 保存按钮
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (title.isNotBlank())
                            Brush.horizontalGradient(listOf(GoldStart, GoldEnd))
                        else Brush.horizontalGradient(listOf(InkMuted, InkMuted))
                    )
                    .clickable(enabled = title.isNotBlank()) {
                        onSave(
                            title.trim(), author.trim(), rating,
                            if (markAsFinished) finished else 0L,
                            note, fav, tags.trim()
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (initial == null) "📖 收入书架" else "保存修改",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = InkSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines, maxLines = maxLines,
        placeholder = { Text(placeholder, color = InkMuted) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldMid,
            unfocusedBorderColor = InkMuted,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg,
            cursorColor = GoldEnd,
            focusedTextColor = InkPrimary,
            unfocusedTextColor = InkPrimary,
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ═══════════════════════════════════════════════════════════════
// 年鉴对话框
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearbookDialog(state: YearbookUiState, onDismiss: () -> Unit) {
    when (state) {
        is YearbookUiState.Generating -> {
            AlertDialog(
                onDismissRequest = { /* 不允许关闭 */ },
                title = { Text("AI 正在为你撰写年鉴…", color = InkPrimary) },
                text = {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldEnd)
                    }
                },
                confirmButton = {},
                containerColor = CardBg
            )
        }
        is YearbookUiState.Ready -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        if (state.fromCloud) "🪄 ${state.stats.year} 年阅读年鉴" else "📒 ${state.stats.year} 年阅读小结",
                        color = InkPrimary, fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        if (!state.fromCloud) {
                            Text(
                                "云端 AI 暂时不可用，已用本地模板生成。",
                                color = Color(0xFFD32F2F), fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Text(
                            state.text,
                            color = InkPrimary, fontSize = 14.sp, lineHeight = 22.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("收下", color = GoldEnd) }
                },
                containerColor = CardBg
            )
        }
        is YearbookUiState.NotVip3 -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("🔒 VIP3 专享", color = InkPrimary) },
                text = { Text(state.message, color = InkSecondary) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("好的", color = GoldEnd) }
                },
                containerColor = CardBg
            )
        }
        is YearbookUiState.Empty -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("还没有完成任何书", color = InkPrimary) },
                text = { Text("${state.year} 年还没有标记为「读完」的书。先记录几本再来生成年鉴吧 ✨", color = InkSecondary) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("好的", color = GoldEnd) }
                },
                containerColor = CardBg
            )
        }
        else -> Unit
    }
}

// ═══════════════════════════════════════════════════════════════
// 工具
// ═══════════════════════════════════════════════════════════════
private fun booksFinishedThisYear(books: List<Book>): Int {
    val cal = java.util.Calendar.getInstance()
    val thisYear = cal.get(java.util.Calendar.YEAR)
    return books.count { b ->
        if (b.finishedAt <= 0L) false
        else {
            cal.timeInMillis = b.finishedAt
            cal.get(java.util.Calendar.YEAR) == thisYear
        }
    }
}
