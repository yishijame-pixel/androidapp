// ═══════════════════════════════════════════════════════════════════════════
// DiaryHubScreen.kt — 日记本"中心页"（非全屏）
//
// · 主页 → 点击"日记本" 进入这里（保留底栏，与其他功能页一致）
// · 中央：3D 魔法书 widget（点击进入全屏阅读）
// · 上方：标题 / 副标题（皮肤气质文案）
// · 下方：统计 + 三个动作（写今日 / 翻阅本卷 / 切皮）
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.funlife.R
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.VipLevel
import com.example.funlife.data.skin.SkinModule
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.repository.DiaryRepository
import com.example.funlife.ui.components.diarybook.BookStageAmbient
import com.example.funlife.ui.components.diarybook.MagicBook3DWidget
import com.example.funlife.ui.components.diarybook.StageSubtitle
import com.example.funlife.ui.components.diarybook.StageTitle
import com.example.funlife.ui.components.diarybook.drawMiniCover
import com.example.funlife.ui.components.diarybook.skin.BookCustomizationProvider
import com.example.funlife.ui.components.diarybook.skin.BookCustomizationSheet
import com.example.funlife.ui.components.diarybook.skin.BookSkinProvider
import com.example.funlife.ui.components.diarybook.skin.LocalBookSkin
import com.example.funlife.ui.components.diarybook.skin.SkinPickerSheet
import com.example.funlife.ui.components.diarybook.skin.rememberBookCustomization
import com.example.funlife.data.DiaryBookCustomizationStore
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DiaryHubScreen(
    userId: Long,
    onBack: () -> Unit,
    onOpenFullReader: () -> Unit,
    onOpenWriteToday: () -> Unit
) {
    val ctx = LocalContext.current
    val repo = remember { DiaryRepository(AppDatabase.getDatabase(ctx).diaryDao()) }
    val vipDao = remember { AppDatabase.getDatabase(ctx).userVipDao() }

    var totalCount by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var hasTodayEntry by remember { mutableStateOf(false) }
    // 本册总页数（影响魔法书可视厚度）
    var bookPageCount by remember { mutableStateOf(1000) }

    LaunchedEffect(userId) {
        val vip = vipDao.getUserVipSync(userId)
        val level = vip?.getCurrentVipLevel() ?: VipLevel.NORMAL
        bookPageCount = when (level) {
            VipLevel.NORMAL -> 1000
            VipLevel.VIP1   -> 5000
            VipLevel.VIP2   -> 10000
            VipLevel.VIP3, VipLevel.PERMANENT -> 50000
        }
    }

    BookSkinProvider {
        BookCustomizationProvider(userId = userId) {
        val skin = LocalBookSkin.current
        val skinId = skin.id.raw

        LaunchedEffect(userId, skinId) {
            repo.observeByBook(userId, skinId).collect { all ->
                totalCount = all.size
                val today = LocalDate.now()
                hasTodayEntry = all.any { it.date == today.toString() }
                val days = all.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
                    .toSortedSet().toList()
                var s = 0
                var cursor = today
                if (!hasTodayEntry && days.contains(today.minusDays(1))) {
                    cursor = today.minusDays(1)
                }
                while (days.contains(cursor)) {
                    s++
                    cursor = cursor.minusDays(1)
                }
                streak = s
            }
        }

        val stage = com.example.funlife.ui.components.diarybook.bookStageThemeFor(skin.id.raw)
        var showSkinPicker by remember { mutableStateOf(false) }
        var showCustomize by remember { mutableStateOf(false) }

        // 翻开书动画：点击 widget / 翻阅按钮 → openProgress 0→1（550ms）→ 跳转
        val openProgress = remember { androidx.compose.animation.core.Animatable(0f) }
        val scope = rememberCoroutineScope()
        val launchOpen: () -> Unit = remember(onOpenFullReader) {
            {
                if (!openProgress.isRunning) {
                    scope.launch {
                        openProgress.animateTo(
                            1f,
                            animationSpec = tween(550, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        )
                        onOpenFullReader()
                        // 跳转后异步重置，下次回来 widget 是常态
                        openProgress.snapTo(0f)
                    }
                }
            }
        }

        // 整体背景：深邃剧场——跟随皮肤 600ms 交叉过渡，避免切皮"硬色块"
        val animSpec = tween<Color>(durationMillis = 600)
        val bgTop by animateColorAsState(
            targetValue = stage.bgTop, animationSpec = animSpec, label = "bgTop"
        )
        val bgMid by animateColorAsState(
            targetValue = stage.bgMid, animationSpec = animSpec, label = "bgMid"
        )
        val bgBot by animateColorAsState(
            targetValue = stage.bgBot, animationSpec = animSpec, label = "bgBot"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(bgTop, bgMid, bgBot))
                )
        ) {
            BookStageAmbient(stage = stage, skinRawId = skin.id.raw)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()        // 顶部自适应避让状态栏
                    .navigationBarsPadding()    // 底部自适应避让导航条 / 手势条
            ) {
                // 顶栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = stage.icon)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showCustomize = true }) {
                        Icon(Icons.Outlined.MenuBook, "刻印魔法书", tint = stage.icon)
                    }
                    IconButton(onClick = { showSkinPicker = true }) {
                        Icon(Icons.Outlined.ColorLens, "挑选皮肤", tint = stage.icon)
                    }
                }

                StageTitle(
                    text = stringResource(skin.meta.displayNameRes),
                    stage = stage,
                )
                StageSubtitle(
                    text = stringResource(skin.meta.descriptionRes),
                    stage = stage,
                )

                Spacer(Modifier.height(4.dp))

                // 中央：魔法书 —— 书本身固定尺寸保持精致，特效层 fillMaxSize 铺满父容器
                MagicBook3DWidget(
                    skin = skin,
                    pageCount = bookPageCount,
                    widthDp = 210.dp,
                    heightDp = 290.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    openProgress = openProgress.value,
                    onClick = launchOpen
                )

                // 皮肤缩略图条
                SkinThumbRail(currentSkinId = skin.id.raw, stage = stage)

                Spacer(Modifier.height(14.dp))

                StatsOrnamentRow(
                    totalCount = totalCount,
                    streak = streak,
                    hasTodayEntry = hasTodayEntry,
                    stage = stage,
                )

                Spacer(Modifier.height(14.dp))

                // 双按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubAction(
                        icon = Icons.Outlined.Edit,
                        label = if (hasTodayEntry) "修改今日" else "写下今日",
                        gradient = Brush.horizontalGradient(
                            listOf(stage.primaryStart, stage.primaryEnd)
                        ),
                        fg = stage.primaryText,
                        glow = stage.halo,
                        onClick = onOpenWriteToday,
                        modifier = Modifier.weight(1f)
                    )
                    HubAction(
                        icon = Icons.Outlined.AutoStories,
                        label = "翻阅本卷",
                        gradient = Brush.horizontalGradient(
                            listOf(
                                stage.halo.copy(alpha = 0.18f),
                                stage.halo.copy(alpha = 0.10f)
                            )
                        ),
                        fg = stage.title,
                        glow = stage.halo,
                        outlined = true,
                        onClick = launchOpen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            if (showSkinPicker) {
                SkinPickerSheet(onDismiss = { showSkinPicker = false })
            }
            if (showCustomize) {
                BookCustomizationSheet(
                    userId = userId,
                    skinRawId = skin.id.raw,
                    onDismiss = { showCustomize = false },
                )
            }
        }
        }
    }
}

@Composable
private fun StatsOrnamentRow(
    totalCount: Int,
    streak: Int,
    hasTodayEntry: Boolean,
    stage: com.example.funlife.ui.components.diarybook.BookStageTheme,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, stage.halo.copy(alpha = 0.35f)),
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatItem(label = "总篇数", value = "$totalCount", stage = stage)
            StatItem(label = "连续天", value = "$streak", stage = stage)
            StatItem(
                label = "今日",
                value = if (hasTodayEntry) "已写" else "未写",
                stage = stage,
                highlight = hasTodayEntry,
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(stage.halo.copy(alpha = 0.35f), Color.Transparent),
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    stage: com.example.funlife.ui.components.diarybook.BookStageTheme,
    highlight: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) stage.haloCore else stage.statValue,
            letterSpacing = 1.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = stage.halo.copy(alpha = 0.5f),
                    offset = Offset(0f, 0f),
                    blurRadius = 16f,
                )
            )
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = stage.statLabel,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun HubAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    gradient: Brush,
    fg: Color,
    glow: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "hub_action_scale",
    )
    Row(
        modifier = modifier
            .scale(scale)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .then(
                if (outlined) Modifier.border(
                    1.2.dp, glow.copy(alpha = if (pressed) 0.75f else 0.55f),
                    RoundedCornerShape(25.dp),
                ) else Modifier
            )
            .background(gradient, RoundedCornerShape(25.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}

/**
 * 水平滚动缩略图条：列出所有可用皮肤的迷你封面，点击即切换。
 * 当前选中的皮肤用 foil 色描边 + 上浮一点。
 */
@Composable
private fun SkinThumbRail(
    currentSkinId: String,
    stage: com.example.funlife.ui.components.diarybook.BookStageTheme
) {
    val ctx = LocalContext.current
    val repo = remember(ctx) { SkinModule.provide(ctx) }
    val available by repo.availableSkins.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val selectedIndex = available.indexOfFirst { it.id.raw == currentSkinId }.coerceAtLeast(0)

    LaunchedEffect(currentSkinId, available.size) {
        if (available.isNotEmpty()) {
            val density = ctx.resources.displayMetrics.density
            val itemW = (66 * density).toInt()
            val target = (selectedIndex * itemW - (40 * density).toInt()).coerceAtLeast(0)
            scrollState.scrollTo(target)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        available.forEach { s ->
            SkinThumb(
                skin = s,
                isSelected = s.id.raw == currentSkinId,
                stage = stage,
                onClick = {
                    if (s.id.raw != currentSkinId) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { repo.select(s.id) }
                    }
                },
            )
        }
    }
}

@Composable
private fun SkinThumb(
    skin: BookSkin,
    isSelected: Boolean,
    stage: com.example.funlife.ui.components.diarybook.BookStageTheme,
    onClick: () -> Unit
) {
    val thumbW by animateDpAsState(
        targetValue = if (isSelected) 58.dp else 48.dp,
        animationSpec = tween(280),
        label = "thumb_w",
    )
    val lift by animateFloatAsState(
        targetValue = if (isSelected) -4f else 0f,
        animationSpec = tween(280),
        label = "thumb_lift",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.55f else 0f,
        animationSpec = tween(280),
        label = "thumb_glow",
    )
    val borderColor = if (isSelected) stage.halo else skin.palette.foil.base.copy(alpha = 0.35f)
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(thumbW + 4.dp)
            .graphicsLayer { translationY = lift }
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(thumbW)
                .aspectRatio(0.72f),
            contentAlignment = Alignment.Center,
        ) {
            if (glowAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    stage.halo.copy(alpha = glowAlpha),
                                    stage.haloCore.copy(alpha = glowAlpha * 0.35f),
                                    Color.Transparent,
                                ),
                            ),
                            RoundedCornerShape(8.dp),
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .border(borderWidth, borderColor, RoundedCornerShape(5.dp)),
            ) {
                val customization = rememberBookCustomization()
                val defaultTitle = stringResource(R.string.diary_book_default_title)
                val defaultSubtitle = stringResource(R.string.diary_book_default_subtitle)
                val coverTitle = DiaryBookCustomizationStore.resolveTitle(customization, defaultTitle)
                val coverOwner = DiaryBookCustomizationStore.resolveOwnerLine(customization, defaultSubtitle)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawMiniCover(skin, coverTitle, coverOwner)
                }
            }
        }
        Text(
            text = stringResource(skin.meta.displayNameRes),
            fontSize = if (isSelected) 10.sp else 9.sp,
            color = if (isSelected) stage.title else stage.statLabel,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = if (isSelected) 0.5.sp else 0.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
