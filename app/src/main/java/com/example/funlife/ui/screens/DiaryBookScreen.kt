// ═══════════════════════════════════════════════════════════════════════════
// DiaryBookScreen.kt — 古籍日记本（v55）
//
// 视觉结构（全屏沉浸，隐藏底栏）：
//   · 封面页（page 0）：藏青布面 + 烫金书名"岁时录" + 锡环 + "拖动开启"
//   · 内页（page 1..N）：单页布局，宣纸纹理，含日期 / 天气 / 标题 / 正文
//   · 翻页：使用 PageCurl 卷曲翻页（左右拖拽）
//   · 写作：点击右上"✎"按钮 → 弹出全屏编辑层（独立 TextField + 宋体）
//   · 目录：点击左上"☰"按钮 → 弹出月份索引（Sheet 风格）
//
// 数据流：
//   每本皮肤（bookSkinId）独立：日记 / 书名 / 署名互不相通
//   每页一篇（pageSlot = 页索引）；总页数 = VIP 本册页数
//   [封面] + [题辞] + [按页槽日记] + [空白待书] … + [卷终]
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.DiaryEntry
import com.example.funlife.data.model.VipLevel
import com.example.funlife.repository.DiaryRepository
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.ui.components.diarybook.PageCurl
import com.example.funlife.ui.components.diarybook.drawColophonOrnament
import com.example.funlife.ui.components.diarybook.drawCornerBrackets
import com.example.funlife.ui.components.diarybook.drawEmbossText
import com.example.funlife.ui.components.diarybook.drawMetallicFrame
import com.example.funlife.ui.components.diarybook.drawMiniCover
import com.example.funlife.ui.components.diarybook.drawSealStamp
import com.example.funlife.ui.components.diarybook.softGlow
import com.example.funlife.ui.components.diarybook.skin.BookCustomizationProvider
import com.example.funlife.ui.components.diarybook.skin.BookCustomizationSheet
import com.example.funlife.ui.components.diarybook.skin.BookSkinProvider
import com.example.funlife.ui.components.diarybook.skin.LocalBookSkin
import com.example.funlife.ui.components.diarybook.skin.SkinPickerSheet
import com.example.funlife.ui.components.diarybook.skin.rememberBookCustomization
import com.example.funlife.data.DiaryBookCustomizationStore
import com.example.funlife.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DiaryBookScreen(
    userId: Long,
    onBack: () -> Unit,
    /** 如果为 true，加载完后跳到今日页并自动打开编辑器。 */
    openEditorOnLaunch: Boolean = false
) {
    val ctx = LocalContext.current
    val repo = remember { DiaryRepository(AppDatabase.getDatabase(ctx).diaryDao()) }
    val scope = rememberCoroutineScope()

    // 本册总页数（VIP）+ 当前皮肤册日记（observe 在 BookSkinProvider 内按 skinId 订阅）
    var entries by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var bookPageCount by remember { mutableStateOf(1000) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        val vipDao = AppDatabase.getDatabase(ctx).userVipDao()
        val vip = vipDao.getUserVipSync(userId)
        val level = vip?.getCurrentVipLevel() ?: VipLevel.NORMAL
        bookPageCount = when (level) {
            VipLevel.NORMAL -> 1000
            VipLevel.VIP1   -> 5000
            VipLevel.VIP2   -> 10000
            VipLevel.VIP3, VipLevel.PERMANENT -> 50000
        }
    }

    // 当前页码（0 = 封面）
    var pageIndex by remember { mutableStateOf(0) }
    var jumpToPage by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var showSkinPicker by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var editingPageSlot by remember { mutableStateOf(2) }

    BookSkinProvider {
    BookCustomizationProvider(userId = userId) {
    val skin = LocalBookSkin.current
    val skinId = skin.id.raw

    LaunchedEffect(userId, skinId) {
        loaded = false
        pageIndex = 0
        jumpToPage = null
        repo.observeByBook(userId, skinId).collect {
            entries = it
            loaded = true
        }
    }

    val today = remember { LocalDate.now() }
    val frontMatterIdx = 1
    val firstEntryIdx = 2
    val backMatterIdx = bookPageCount - 1
    val totalPages = bookPageCount
    val firstBlankPageIdx = remember(entries, backMatterIdx) {
        (firstEntryIdx until backMatterIdx).firstOrNull { p ->
            entries.none { it.pageSlot == p }
        } ?: -1
    }

    fun entryAt(pageIdx: Int): DiaryEntry? = entries.find { it.pageSlot == pageIdx }
    fun isCover(pageIdx: Int) = pageIdx == 0
    fun isFrontMatter(pageIdx: Int) = pageIdx == frontMatterIdx
    fun isBackMatter(pageIdx: Int) = pageIdx == backMatterIdx
    fun isWritablePage(pageIdx: Int) = pageIdx in firstEntryIdx until backMatterIdx
    fun isTodayNewPage(pageIdx: Int) = pageIdx == firstBlankPageIdx && firstBlankPageIdx >= 0
    fun isBlankPage(pageIdx: Int) = isWritablePage(pageIdx) && !isTodayNewPage(pageIdx) && entryAt(pageIdx) == null

    var editingDate by remember { mutableStateOf(today) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editWeather by remember { mutableStateOf("") }
    var editMood by remember { mutableStateOf("") }

    fun openEditorFor(pageIdx: Int) {
        if (!isWritablePage(pageIdx)) return
        editingPageSlot = pageIdx
        val e = entryAt(pageIdx)
        editingDate = e?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
        editTitle = e?.title ?: ""
        editContent = e?.content ?: ""
        editWeather = e?.weather ?: ""
        editMood = e?.moodEmoji ?: ""
        showEditor = true
    }

    var autoEditorTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(loaded, openEditorOnLaunch, skinId, firstBlankPageIdx) {
        if (loaded && openEditorOnLaunch && !autoEditorTriggered) {
            val todayPage = entries.find { it.date == today.toString() }?.pageSlot
            val targetPage = todayPage ?: firstBlankPageIdx.coerceAtLeast(firstEntryIdx)
            pageIndex = targetPage.coerceIn(0, totalPages - 1)
            jumpToPage = pageIndex
            openEditorFor(pageIndex)
            autoEditorTriggered = true
        }
    }
    val customization = rememberBookCustomization()
    val defaultTitle = stringResource(R.string.diary_book_default_title)
    val defaultSubtitle = stringResource(R.string.diary_book_default_subtitle)
    val coverTitle = DiaryBookCustomizationStore.resolveTitle(customization, defaultTitle)
    val coverOwnerLine = DiaryBookCustomizationStore.resolveOwnerLine(customization, defaultSubtitle)
    val coverRefreshKey = "${skin.id.raw}|$coverTitle|$coverOwnerLine"
    val pageRefreshKey = remember(coverRefreshKey, skinId, entries) {
        val sig = entries.joinToString("|") { "${it.pageSlot}:${it.id}:${it.updatedAt}" }
        "$coverRefreshKey|$skinId|$sig"
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1410))) {
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("展卷中…", color = Color(0xFFCBB994), fontSize = 14.sp, letterSpacing = 4.sp)
            }
        } else {
        // 卷曲翻页核心
        PageCurl(
            pageCount = totalPages,
            onPageChange = { pageIndex = it.coerceIn(0, totalPages - 1) },
            jumpToPage = jumpToPage,
            onJumpConsumed = { jumpToPage = null },
            modifier = Modifier.fillMaxSize(),
            refreshKey = pageRefreshKey
        ) { pageIdx ->
            when {
                isCover(pageIdx) -> drawCoverPage(skin, coverTitle, coverOwnerLine)
                isFrontMatter(pageIdx) -> drawFrontMatter(skin, customization.ownerName)
                isBackMatter(pageIdx) -> drawBackMatter(skin, entries.size, customization.ownerName)
                isTodayNewPage(pageIdx) -> drawTodayNewPage(today, skin)
                else -> entryAt(pageIdx)?.let { drawDiaryPage(it, skin) }
                    ?: drawBlankPage(pageIdx, skin)
            }
        }

        // 顶部工具栏（仅非封面页显示）
        if (pageIndex > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFF5B4A36))
                }
                IconButton(onClick = { showCatalog = true }) {
                    Icon(Icons.Outlined.MenuBook, "目录", tint = Color(0xFF5B4A36))
                }
                IconButton(onClick = { showSkinPicker = true }) {
                    Icon(Icons.Outlined.ColorLens, "挑选皮肤", tint = Color(0xFF5B4A36))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = when {
                        isFrontMatter(pageIndex) -> "题  辞"
                        isBackMatter(pageIndex) -> "卷  终"
                        isTodayNewPage(pageIndex) -> "今  日"
                        else -> "第 ${pageIndex - 1} / ${totalPages - 2} 页"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF5B4A36).copy(alpha = 0.75f),
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { openEditorFor(pageIndex) }) {
                    Icon(Icons.Outlined.Edit, "写日记", tint = Color(0xFF5B4A36))
                }
            }
        } else {
            // 封面顶部：返回键 + 皮肤按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFFE8D9B0))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showCustomize = true }) {
                    Icon(Icons.Outlined.MenuBook, "刻印魔法书", tint = Color(0xFFE8D9B0))
                }
                IconButton(onClick = { showSkinPicker = true }) {
                    Icon(Icons.Outlined.ColorLens, "挑选皮肤", tint = Color(0xFFE8D9B0))
                }
            }
        }

        // 封面页中央"翻开"提示
        if (pageIndex == 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "← 向左拖动 · 翻开本书",
                    color = Color(0xFFE8D9B0).copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    letterSpacing = 4.sp
                )
            }
        }

        // 编辑器全屏覆盖
        if (showEditor) {
            DiaryEditor(
                date = editingDate,
                title = editTitle,
                content = editContent,
                weather = editWeather,
                moodEmoji = editMood,
                onTitleChange = { editTitle = it },
                onContentChange = { editContent = it },
                onWeatherChange = { editWeather = it },
                onMoodChange = { editMood = it },
                onCancel = { showEditor = false },
                onSave = {
                    scope.launch {
                        repo.saveOrUpdate(
                            DiaryEntry(
                                userId = userId,
                                bookSkinId = skinId,
                                pageSlot = editingPageSlot,
                                date = editingDate.toString(),
                                title = editTitle.trim(),
                                content = editContent.trim(),
                                weather = editWeather.trim().ifEmpty { null },
                                moodEmoji = editMood.trim().ifEmpty { null }
                            )
                        )
                        showEditor = false
                    }
                }
            )
        }

        // 皮肤选择底栏
        if (showSkinPicker) {
            SkinPickerSheet(
                userId = userId,
                onDismiss = { showSkinPicker = false },
            )
        }
        if (showCustomize) {
            BookCustomizationSheet(
                userId = userId,
                skinRawId = skin.id.raw,
                onDismiss = { showCustomize = false },
            )
        }

        // 目录浮层
        if (showCatalog) {
            CatalogSheet(
                entries = entries,
                firstBlankPageIdx = firstBlankPageIdx,
                onPick = { pageSlot ->
                    pageIndex = pageSlot.coerceIn(0, totalPages - 1)
                    jumpToPage = pageSlot
                    showCatalog = false
                },
                onJumpToNew = {
                    val target = if (firstBlankPageIdx >= 0) firstBlankPageIdx else firstEntryIdx
                    pageIndex = target
                    jumpToPage = target
                    showCatalog = false
                },
                onDismiss = { showCatalog = false }
            )
        }
        }  // else (loaded)
    }
    }  // BookCustomizationProvider
    }  // BookSkinProvider
}

// ─────────────────────────────────────────────────────────────────────────
// 1. 封面页：与 3D 魔法书共用 drawMiniCover（支持用户定制书名 / 署名）
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawCoverPage(
    skin: BookSkin,
    bookTitle: String,
    ownerLine: String,
) {
    drawMiniCover(skin, bookTitle, ownerLine)
}

// ─────────────────────────────────────────────────────────────────────────
// 2. 日记内页（已有内容）：宣纸 + 日期 + 天气 + 标题 + 正文
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawDiaryPage(entry: DiaryEntry, skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette

    drawPaperBackground(w, h, skin)

    val nc = drawContext.canvas.nativeCanvas
    val ink = palette.ink.toArgb()
    val fade = palette.inkSoft.copy(alpha = 0.55f).toArgb()

    // 顶部：日期 + 天气
    val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: LocalDate.now()
    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日"))
    val weekStr = when (date.dayOfWeek.value) {
        1 -> "星期一"; 2 -> "星期二"; 3 -> "星期三"; 4 -> "星期四"
        5 -> "星期五"; 6 -> "星期六"; else -> "星期日"
    }

    val datePaint = android.graphics.Paint().apply {
        color = ink
        textSize = w * 0.045f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.LEFT
        letterSpacing = 0.15f
    }
    nc.drawText(dateStr, w * 0.10f, h * 0.13f, datePaint)

    val weekPaint = android.graphics.Paint(datePaint).apply {
        textSize = w * 0.032f
        color = fade
    }
    val metaParts = mutableListOf(weekStr)
    if (!entry.weather.isNullOrBlank()) metaParts += entry.weather
    if (!entry.moodEmoji.isNullOrBlank()) metaParts += entry.moodEmoji
    nc.drawText(metaParts.joinToString("  ·  "), w * 0.10f, h * 0.18f, weekPaint)

    // 日期下方装饰横线
    drawLine(
        color = palette.ruling.copy(alpha = 0.45f),
        start = Offset(w * 0.10f, h * 0.20f),
        end = Offset(w * 0.90f, h * 0.20f),
        strokeWidth = 0.7f
    )

    // 标题（若有）
    var contentStartY = h * 0.26f
    if (entry.title.isNotBlank()) {
        val titlePaint = android.graphics.Paint().apply {
            color = ink
            textSize = w * 0.058f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.LEFT
        }
        nc.drawText(entry.title, w * 0.10f, h * 0.245f, titlePaint)
        contentStartY = h * 0.30f
    }

    // 正文：自动换行
    val bodyPaint = android.graphics.Paint().apply {
        color = ink
        textSize = w * 0.042f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.LEFT
        letterSpacing = 0.08f
    }
    drawWrappedText(
        nc, entry.content, bodyPaint,
        x = w * 0.10f,
        topY = contentStartY,
        maxWidth = w * 0.80f,
        lineHeight = w * 0.072f,
        maxY = h * 0.90f
    )

    // 底部页脚日期序号（小字）
    val footerPaint = android.graphics.Paint().apply {
        color = fade
        textSize = w * 0.025f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.3f
    }
    nc.drawText(entry.date, w / 2f, h * 0.95f, footerPaint)
}

// ─────────────────────────────────────────────────────────────────────────
// 3a. 前扉页（题辞）：竖排双栏 + 内框装饰 + 钤印
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawFrontMatter(skin: BookSkin, ownerName: String) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val foil = palette.foil.base
    val accent = palette.foil.accent
    drawPaperBackground(w, h, skin)

    val nc = drawContext.canvas.nativeCanvas
    val cx = w / 2f

    // 内框：双金边 + 四角护角
    val pad = (w * 0.11f).coerceAtLeast(8f)
    val frameSize = Size(w - pad * 2f, h * 0.58f)
    val frameTop = h * 0.16f
    drawMetallicFrame(Offset(pad, frameTop), frameSize, w * 0.005f, foil, accent, alpha = 0.55f)
    val pad2 = pad + w * 0.018f
    val innerSize = Size(w - pad2 * 2f, frameSize.height - w * 0.036f)
    val innerTop = frameTop + w * 0.018f
    drawMetallicFrame(Offset(pad2, innerTop), innerSize, w * 0.003f, foil, accent, alpha = 0.38f)
    drawCornerBrackets(foil, accent, pad + w * 0.006f, w * 0.055f, w * 0.004f)

    // 竖排双栏（右列先读）：字距紧凑，整体居中于内框
    val charSize = w * 0.072f
    val charStep = charSize * 1.16f
    val colGap = charSize * 2.05f
    val rightColX = cx + colGap / 2f
    val leftColX = cx - colGap / 2f
    val rightChars = "光阴荏苒"
    val leftChars = "且以此册志之"
    val maxLen = maxOf(rightChars.length, leftChars.length)
    val blockCenterY = innerTop + innerSize.height / 2f
    val blockHeight = (maxLen - 1) * charStep + charSize
    val blockTopBaseline = blockCenterY - blockHeight / 2f + charSize * 0.82f

    softGlow(Offset(cx, blockCenterY), colGap * 0.9f, palette.ink.copy(alpha = 0.35f), 0.12f)

    drawVerticalEpigraphColumn(
        chars = rightChars,
        cx = rightColX,
        topBaselineY = blockTopBaseline + (maxLen - rightChars.length) * charStep * 0.5f,
        textSize = charSize,
        step = charStep,
        base = palette.ink,
        accent = accent,
    )
    drawVerticalEpigraphColumn(
        chars = leftChars,
        cx = leftColX,
        topBaselineY = blockTopBaseline + (maxLen - leftChars.length) * charStep * 0.5f,
        textSize = charSize,
        step = charStep,
        base = palette.ink,
        accent = accent,
    )

    // 栏间竖线（极淡，暗示分栏）
    val ruleTop = blockTopBaseline - charSize * 0.35f
    val ruleBot = blockTopBaseline + (maxLen - 1) * charStep + charSize * 0.25f
    drawLine(
        color = foil.copy(alpha = 0.22f),
        start = Offset(cx, ruleTop),
        end = Offset(cx, ruleBot),
        strokeWidth = 0.6f,
    )

    // 副题：单行紧凑，置于内框下方
    val subY = frameTop + frameSize.height + h * 0.045f
    val subHalfW = w * 0.22f
    drawColophonOrnament(cx, subY - charSize * 0.55f, subHalfW, foil, w * 0.003f)
    val subPaint = android.graphics.Paint().apply {
        color = palette.inkSoft.copy(alpha = 0.82f).toArgb()
        textSize = w * 0.034f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.04f
    }
    val subText = ownerName.trim().ifBlank { "一时之事  ·  一册之录" }
    nc.drawText(subText, cx, subY, subPaint)

    // 右下角钤印「启」
    val stampSize = w * 0.088f
    drawSealStamp(
        center = Offset(w * 0.78f, h * 0.86f),
        sizePx = stampSize,
        sealColor = palette.seal,
        glyph = "启",
        backing = palette.paper,
    )
}

/** 竖排题辞单栏：逐字凸压，字距固定，避免横排大 letterSpacing 导致散碎感。 */
private fun DrawScope.drawVerticalEpigraphColumn(
    chars: String,
    cx: Float,
    topBaselineY: Float,
    textSize: Float,
    step: Float,
    base: Color,
    accent: Color,
) {
    chars.forEachIndexed { i, ch ->
        drawEmbossText(
            text = ch.toString(),
            cx = cx,
            cy = topBaselineY + i * step,
            textSize = textSize,
            base = base,
            accent = accent,
            bold = true,
            shadowAlpha = 0.38f,
            highlightAlpha = 0.22f,
            letterSpacing = 0f,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 3b. 后尾页（卷终）：宣纸 + "卷终" + 总篇数 + 落款印
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawBackMatter(skin: BookSkin, entriesCount: Int, ownerName: String) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    drawPaperBackground(w, h, skin)

    val nc = drawContext.canvas.nativeCanvas

    // 主标题"卷终"
    val titlePaint = android.graphics.Paint().apply {
        color = palette.ink.toArgb()
        textSize = w * 0.16f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.8f
    }
    nc.drawText("卷  终", w / 2f, h * 0.42f, titlePaint)

    // 双横线
    drawLine(
        color = palette.foil.base.copy(alpha = 0.7f),
        start = Offset(w * 0.32f, h * 0.50f),
        end = Offset(w * 0.68f, h * 0.50f),
        strokeWidth = 1.2f
    )
    drawLine(
        color = palette.foil.base.copy(alpha = 0.4f),
        start = Offset(w * 0.36f, h * 0.515f),
        end = Offset(w * 0.64f, h * 0.515f),
        strokeWidth = 0.6f
    )

    // 统计文案
    val statPaint = android.graphics.Paint().apply {
        color = palette.inkSoft.toArgb()
        textSize = w * 0.040f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.3f
    }
    nc.drawText("此  册  共  录  $entriesCount  篇", w / 2f, h * 0.62f, statPaint)

    val hintPaint = android.graphics.Paint(statPaint).apply {
        textSize = w * 0.032f
        color = palette.inkSoft.copy(alpha = 0.7f).toArgb()
    }
    nc.drawText("愿  时  光  待  你  温  柔", w / 2f, h * 0.70f, hintPaint)

    // 落款印
    val stampSize = w * 0.085f
    val sx = w * 0.74f
    val sy = h * 0.84f
    drawRect(
        color = palette.seal.copy(alpha = 0.92f),
        topLeft = Offset(sx, sy),
        size = Size(stampSize, stampSize)
    )
    val stampPaint = android.graphics.Paint().apply {
        color = palette.paper.toArgb()
        textSize = stampSize * 0.55f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val stampGlyph = ownerName.trim().take(1).ifBlank { "终" }
    nc.drawText(stampGlyph, sx + stampSize / 2f, sy + stampSize * 0.72f, stampPaint)
}

// ─────────────────────────────────────────────────────────────────────────
// 3. 今日新页：邀请写作
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawTodayNewPage(today: LocalDate, skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette

    drawPaperBackground(w, h, skin)

    val nc = drawContext.canvas.nativeCanvas
    val ink = palette.ink.toArgb()
    val fade = palette.inkSoft.copy(alpha = 0.5f).toArgb()

    val dateStr = today.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日"))
    val datePaint = android.graphics.Paint().apply {
        color = ink
        textSize = w * 0.045f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.LEFT
        letterSpacing = 0.15f
    }
    nc.drawText(dateStr, w * 0.10f, h * 0.13f, datePaint)

    drawLine(
        color = palette.ruling.copy(alpha = 0.45f),
        start = Offset(w * 0.10f, h * 0.18f),
        end = Offset(w * 0.90f, h * 0.18f),
        strokeWidth = 0.7f
    )

    // 中央邀请文字
    val invitePaint = android.graphics.Paint().apply {
        color = fade
        textSize = w * 0.055f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.3f
    }
    nc.drawText("今  日  尚  无  字", w / 2f, h * 0.50f, invitePaint)

    val hintPaint = android.graphics.Paint().apply {
        color = fade
        textSize = w * 0.034f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.2f
    }
    nc.drawText("点  击  右  上  角  ✎  落  笔", w / 2f, h * 0.56f, hintPaint)
}

// ─────────────────────────────────────────────────────────────────────────
// 3c. 空白待书页（VIP 本册页数填充，未到卷终前的留白）
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawBlankPage(pageIdx: Int, skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette

    drawPaperBackground(w, h, skin)

    // 横线稿纸
    val lineTop = h * 0.22f
    val lineBot = h * 0.88f
    val step = (lineBot - lineTop) / 14f
    for (i in 0..14) {
        val y = lineTop + step * i
        drawLine(
            color = palette.ruling.copy(alpha = 0.28f),
            start = Offset(w * 0.10f, y),
            end = Offset(w * 0.90f, y),
            strokeWidth = 0.6f
        )
    }

    val nc = drawContext.canvas.nativeCanvas
    val pageNum = pageIdx - 1
    val numPaint = android.graphics.Paint().apply {
        color = palette.inkSoft.copy(alpha = 0.35f).toArgb()
        textSize = w * 0.030f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.2f
    }
    nc.drawText("—  $pageNum  —", w / 2f, h * 0.94f, numPaint)
}

// ─────────────────────────────────────────────────────────────────────────
// 共享：宣纸纸面（米黄渐变 + 织物斑点 + 边缘暗角 + 装订侧细线）
// ─────────────────────────────────────────────────────────────────────────
private fun DrawScope.drawPaperBackground(w: Float, h: Float, skin: BookSkin) {
    val palette = skin.palette
    val materials = skin.materials
    val basePaper = palette.paper
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                basePaper,
                basePaper.lerpDarken(0.06f),
                basePaper.lerpDarken(0.12f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h)
    )
    // 纸张斑点
    val noiseCount = (materials.paperNoiseCount / 8).coerceAtLeast(50)
    for (i in 0 until noiseCount) {
        val sx = ((i * 9301 + 49297) % 233280) / 233280f
        val sy = ((i * 12289 + 33191) % 233280) / 233280f
        drawCircle(
            color = palette.paperFiber.copy(alpha = materials.paperNoiseAlpha),
            radius = 0.7f,
            center = Offset(w * sx, h * sy)
        )
    }
    // 边缘暗角
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, palette.ruling.copy(alpha = 0.18f)),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.7f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h)
    )
    // 左侧装订线（深色）
    drawRect(
        color = palette.ruling.copy(alpha = 0.18f),
        topLeft = Offset(0f, 0f),
        size = Size(w * 0.06f, h)
    )
    drawLine(
        color = palette.ruling.copy(alpha = 0.55f),
        start = Offset(w * 0.075f, 0f),
        end = Offset(w * 0.075f, h),
        strokeWidth = 0.8f
    )
}

/** Color 工具：朝深色方向 lerp，避免引入新颜色字段。 */
private fun Color.lerpDarken(amount: Float): Color = Color(
    red   = (red   * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue  = (blue  * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha
)

/** Native canvas 自动换行画长文（按字符宽度估算，遇 \n 强制换行） */
private fun drawWrappedText(
    nc: android.graphics.Canvas,
    text: String,
    paint: android.graphics.Paint,
    x: Float,
    topY: Float,
    maxWidth: Float,
    lineHeight: Float,
    maxY: Float
) {
    if (text.isBlank()) return
    var y = topY
    text.split("\n").forEach { paragraph ->
        var line = StringBuilder()
        for (c in paragraph) {
            line.append(c)
            val lw = paint.measureText(line.toString())
            if (lw > maxWidth) {
                line.deleteCharAt(line.length - 1)
                if (y > maxY) return
                nc.drawText(line.toString(), x, y, paint)
                y += lineHeight
                line = StringBuilder().append(c)
            }
        }
        if (line.isNotEmpty()) {
            if (y > maxY) return
            nc.drawText(line.toString(), x, y, paint)
            y += lineHeight
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 编辑器（全屏覆盖，宣纸底，宋体大字写日记）
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun DiaryEditor(
    date: LocalDate,
    title: String,
    content: String,
    weather: String,
    moodEmoji: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onWeatherChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1E5C4))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 36.dp)) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Outlined.Close, "取消", tint = Color(0xFF5B4A36))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")),
                    fontSize = 14.sp,
                    color = Color(0xFF5B4A36),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFB23A48))
                        .clickable(onClick = onSave)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("落  墨", color = Color.White, fontSize = 13.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 元数据（天气 + 心情，单行小输入）
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("天气", fontSize = 12.sp, color = Color(0xFF5B4A36).copy(alpha = 0.7f), letterSpacing = 2.sp)
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = weather,
                    onValueChange = { if (it.length <= 8) onWeatherChange(it) },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF3D2A1F),
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color(0xFFB23A48)),
                    modifier = Modifier.width(80.dp).padding(end = 16.dp)
                )
                Text("心情", fontSize = 12.sp, color = Color(0xFF5B4A36).copy(alpha = 0.7f), letterSpacing = 2.sp)
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = moodEmoji,
                    onValueChange = { if (it.length <= 4) onMoodChange(it) },
                    textStyle = TextStyle(fontSize = 18.sp, color = Color(0xFF3D2A1F)),
                    cursorBrush = SolidColor(Color(0xFFB23A48)),
                    modifier = Modifier.width(48.dp)
                )
            }

            // 标题
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = TextStyle(
                    fontSize = 26.sp,
                    color = Color(0xFF3D2A1F),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                cursorBrush = SolidColor(Color(0xFFB23A48)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            "为今日起一个题…",
                            color = Color(0xFF8B6F4E).copy(alpha = 0.45f),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                    inner()
                }
            )

            // 装饰线
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(Color(0xFF8B6F4E).copy(alpha = 0.45f))
            )

            // 正文（可滚动）
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    color = Color(0xFF3D2A1F),
                    lineHeight = 28.sp,
                    letterSpacing = 1.sp
                ),
                cursorBrush = SolidColor(Color(0xFFB23A48)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                decorationBox = { inner ->
                    if (content.isEmpty()) {
                        Text(
                            "今日所见所感，落于此处…",
                            color = Color(0xFF8B6F4E).copy(alpha = 0.40f),
                            fontSize = 17.sp,
                            lineHeight = 28.sp
                        )
                    }
                    inner()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 目录 Sheet：按月分组列出已有日记，点击跳转
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun CatalogSheet(
    entries: List<DiaryEntry>,
    firstBlankPageIdx: Int,
    onPick: (pageSlot: Int) -> Unit,
    onJumpToNew: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(280.dp)
                .background(Color(0xFFF1E5C4))
                .clickable(enabled = false, onClick = {})
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "目  录",
                fontSize = 22.sp,
                color = Color(0xFF3D2A1F),
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${entries.size} 篇",
                fontSize = 12.sp,
                color = Color(0xFF8B6F4E).copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(20.dp))

            // 按月分组
            val byMonth = entries.groupBy { it.date.substring(0, 7) }
            byMonth.toSortedMap(compareByDescending { it }).forEach { (ym, list) ->
                Text(
                    text = ym.replace("-", " . "),
                    fontSize = 13.sp,
                    color = Color(0xFFB23A48),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                list.sortedBy { it.pageSlot }.forEach { e ->
                    val short = if (e.title.isNotBlank()) e.title else "无题"
                    Text(
                        text = "  ${e.date.substring(8)}    $short",
                        fontSize = 14.sp,
                        color = Color(0xFF3D2A1F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(e.pageSlot) }
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // 下一空白页
            if (firstBlankPageIdx >= 0) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFB23A48).copy(alpha = 0.85f))
                        .clickable(onClick = onJumpToNew)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "今  日  落  笔",
                        color = Color.White,
                        fontSize = 14.sp,
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
