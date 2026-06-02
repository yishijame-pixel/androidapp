// ═══════════════════════════════════════════════════════════════════════════
// SkinPickerSheet.kt — 皮肤切换底栏
//
// · ModalBottomSheet 列出三套皮肤的迷你封面预览
// · 点击解锁皮肤 → 调 SkinRepository.select() → 自动驱动 LocalBookSkin 重组
// · 未解锁皮肤显示锁图标 + 蒙灰
// · 当前选中皮肤用金色描边 + ✓ 标记
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as DrawSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.funlife.R
import com.example.funlife.data.skin.SkinException
import com.example.funlife.data.skin.SkinModule
import com.example.funlife.data.skin.SkinRepository
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.domain.skin.SkinId
import com.example.funlife.ui.components.diarybook.bookStageThemeFor
import com.example.funlife.ui.components.diarybook.drawMiniCover
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinPickerSheet(
    onDismiss: () -> Unit,
    onAppliedMessage: (String) -> Unit = {},
    repository: SkinRepository = SkinModule.provide(LocalContext.current)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentSkin by repository.currentSkin.collectAsState()
    val available by repository.availableSkins.collectAsState()
    val ctx = LocalContext.current
    val stage = bookStageThemeFor(currentSkin.id.raw)

    // 缓存解锁状态：避免每次重组都跑挂起函数
    val unlockedMap = remember { mutableStateOf(emptyMap<SkinId, Boolean>()) }
    LaunchedEffect(available) {
        unlockedMap.value = available.associate { it.id to repository.isUnlocked(it.id) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = stage.bgMid,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(stage.bgTop, stage.bgMid, stage.bgBot),
                    ),
                )
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.skin_picker_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = stage.title,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = "点击任意一本 · 魔法书即时切换",
                fontSize = 11.sp,
                color = stage.subtitle,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
            ) {
                items(available, key = { it.id.raw }) { skin ->
                    SkinPreviewCard(
                        skin = skin,
                        isSelected = skin.id == currentSkin.id,
                        isUnlocked = unlockedMap.value[skin.id] ?: false,
                        stage = stage,
                        onClick = {
                            scope.launch {
                                val r = repository.select(skin.id)
                                r.onSuccess {
                                    val name = ctx.getString(skin.meta.displayNameRes)
                                    onAppliedMessage(ctx.getString(R.string.skin_applied, name))
                                    onDismiss()
                                }.onFailure { ex ->
                                    if (ex is SkinException.Locked) {
                                        onAppliedMessage(ctx.getString(R.string.skin_locked_hint))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun SkinPreviewCard(
    skin: BookSkin,
    isSelected: Boolean,
    isUnlocked: Boolean,
    stage: com.example.funlife.ui.components.diarybook.BookStageTheme,
    onClick: () -> Unit,
) {
    val palette = skin.palette
    val borderColor = when {
        isSelected -> stage.halo
        isUnlocked -> palette.foil.base.copy(alpha = 0.45f)
        else       -> Color.White.copy(alpha = 0.15f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked) { onClick() }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (isSelected) Modifier.background(
                        Brush.radialGradient(
                            listOf(stage.halo.copy(alpha = 0.35f), Color.Transparent),
                        ),
                        RoundedCornerShape(6.dp),
                    ) else Modifier,
                )
                .border(
                    width = if (isSelected) 2.5.dp else 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp),
                ),
        ) {
            Canvas(
                modifier = Modifier.fillMaxWidth().aspectRatio(0.72f)
            ) {
                drawMiniCover(skin)
            }
            // 选中标记
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.foil.base),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = palette.cover.base,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            // 锁定遮罩
            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(skin.meta.displayNameRes),
            fontSize = 12.sp,
            color = if (isSelected) stage.title else stage.statLabel,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

/** 迷你封面预览：与 drawCoverPage 同主题但简化（只画封面、烫金标题、边框）。 */
@Suppress("unused", "UNUSED_PARAMETER")
private fun DrawScope.drawMiniCover_legacyKept(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val materials = skin.materials
    val foil = palette.foil.base

    // 封面渐变
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(palette.cover.base, palette.cover.accent, palette.coverShadow),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        topLeft = Offset(0f, 0f),
        size = DrawSize(w, h)
    )
    // 噪点（少量）
    for (i in 0 until 80) {
        val sx = ((i * 9301 + 49297) % 233280) / 233280f
        val sy = ((i * 12289 + 33191) % 233280) / 233280f
        drawCircle(
            color = foil.copy(alpha = materials.leatherNoiseAlpha),
            radius = 0.5f,
            center = Offset(w * sx, h * sy)
        )
    }
    // 左侧书脊阴影
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
            startX = 0f,
            endX = w * 0.18f
        ),
        topLeft = Offset(0f, 0f),
        size = DrawSize(w * 0.18f, h)
    )
    // 烫金边框
    val pad = 8f
    drawRect(
        color = foil.copy(alpha = 0.85f),
        topLeft = Offset(pad, pad),
        size = DrawSize(w - pad * 2, h - pad * 2),
        style = Stroke(width = 1f)
    )
    if (materials.foilDoubleStroke) {
        val pad2 = pad + 3f
        drawRect(
            color = foil.copy(alpha = 0.5f),
            topLeft = Offset(pad2, pad2),
            size = DrawSize(w - pad2 * 2, h - pad2 * 2),
            style = Stroke(width = 0.6f)
        )
    }
    // 中央迷你标题
    val nc = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        color = foil.toArgb()
        textSize = w * 0.13f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
        setShadowLayer(3f, 0f, 1f, palette.foil.accent.toArgb())
    }
    nc.drawText("岁时录", w / 2f, h * 0.55f, paint)
}

/** 顶部 SheetState 透出（DiaryBookScreen 可选择持有以做扩展）。 */
@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
val DummySheetStateRef: SheetState? = null
