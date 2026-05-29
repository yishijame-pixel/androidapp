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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import com.example.funlife.ui.components.diarybook.MagicBook3DWidget
import com.example.funlife.ui.components.diarybook.drawMiniCover
import com.example.funlife.ui.components.diarybook.skin.BookSkinProvider
import com.example.funlife.ui.components.diarybook.skin.LocalBookSkin
import com.example.funlife.ui.components.diarybook.skin.SkinPickerSheet
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

    LaunchedEffect(userId) {
        repo.observeAll(userId).collect { all ->
            totalCount = all.size
            val today = LocalDate.now()
            hasTodayEntry = all.any { it.date == today.toString() }
            // 连续天数（从今天起向前数，相邻日期差 1）
            val days = all.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
                .toSortedSet().toList()
            var s = 0
            var cursor = today
            // 若今天没写、但昨天有，连续天数从昨天起；否则今天起
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

    BookSkinProvider {
        val skin = LocalBookSkin.current
        val palette = skin.palette
        var showSkinPicker by remember { mutableStateOf(false) }

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

        // 整体背景：跟随皮肤 600ms 交叉过渡，避免切皮"硬色块"
        val animSpec = tween<Color>(durationMillis = 600)
        val bgTop by animateColorAsState(
            targetValue = palette.cover.base.copy(alpha = 0.18f),
            animationSpec = animSpec, label = "bgTop"
        )
        val bgMid by animateColorAsState(
            targetValue = palette.paper.copy(alpha = 0.55f),
            animationSpec = animSpec, label = "bgMid"
        )
        val bgBot by animateColorAsState(
            targetValue = palette.foil.base.copy(alpha = 0.16f),
            animationSpec = animSpec, label = "bgBot"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(bgTop, bgMid, bgBot))
                )
        ) {
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
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFF5B4A36))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showSkinPicker = true }) {
                        Icon(Icons.Outlined.ColorLens, "挑选皮肤", tint = Color(0xFF5B4A36))
                    }
                }

                // 标题（古风衬线字体）
                Text(
                    text = stringResource(skin.meta.displayNameRes),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF3D2A1F),
                    letterSpacing = 6.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                Text(
                    text = stringResource(skin.meta.descriptionRes),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF8B6F4E),
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
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
                SkinThumbRail(currentSkinId = skin.id.raw)

                Spacer(Modifier.height(14.dp))

                // 统计条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "总篇数", value = "$totalCount")
                    StatItem(label = "连续天", value = "$streak")
                    StatItem(label = "今日", value = if (hasTodayEntry) "已写" else "未写")
                }

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
                        bg = palette.seal.copy(alpha = 0.92f),
                        fg = Color(0xFFFFF8E7),
                        onClick = onOpenWriteToday,
                        modifier = Modifier.weight(1f)
                    )
                    HubAction(
                        icon = Icons.Outlined.AutoStories,
                        label = "翻阅本卷",
                        bg = palette.cover.base,
                        fg = palette.foil.base,
                        onClick = launchOpen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            if (showSkinPicker) {
                SkinPickerSheet(onDismiss = { showSkinPicker = false })
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2A1F),
            letterSpacing = 1.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF8B6F4E),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun HubAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
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
private fun SkinThumbRail(currentSkinId: String) {
    val ctx = LocalContext.current
    val repo = remember(ctx) { SkinModule.provide(ctx) }
    val available by repo.availableSkins.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        available.forEach { s ->
            SkinThumb(
                skin = s,
                isSelected = s.id.raw == currentSkinId,
                onClick = {
                    if (s.id.raw != currentSkinId) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { repo.select(s.id) }
                    }
                }
            )
        }
    }
}

@Composable
private fun SkinThumb(
    skin: BookSkin,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val foil = skin.palette.foil.base
    val borderColor = if (isSelected) foil else Color.Black.copy(alpha = 0.10f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val thumbW = if (isSelected) 56.dp else 48.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(thumbW)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(thumbW)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(4.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMiniCover(skin)
            }
        }
        Text(
            text = stringResource(skin.meta.displayNameRes),
            fontSize = 9.sp,
            color = if (isSelected) Color(0xFF3D2A1F) else Color(0xFF8B6F4E),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
