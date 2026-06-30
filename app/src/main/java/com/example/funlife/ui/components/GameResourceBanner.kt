package com.example.funlife.ui.components



import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.expandVertically

import androidx.compose.animation.shrinkVertically

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.CloudDownload

import androidx.compose.material.icons.filled.ExpandLess

import androidx.compose.material.icons.filled.ExpandMore

import androidx.compose.material3.Icon

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

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

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.funlife.BuildConfig

import com.example.funlife.resource.GameResourceBundles

import com.example.funlife.resource.PacMazeResourceUpdateNotifier

import com.example.funlife.resource.ResourceStore

import com.example.funlife.ui.screens.pacmaze.PacMazePalette

import com.example.funlife.ui.screens.pacmaze.PacMazeSecondaryButton



enum class GameResourceBannerStyle {

    Home,

    PacMaze,

}



/**

 * 游戏资源下载/更新横幅：自动检测缺口并后台下载，可展开查看各包状态。

 */

@Composable

fun GameResourceBanner(

    modifier: Modifier = Modifier,

    style: GameResourceBannerStyle = GameResourceBannerStyle.Home,

) {

    if (!ResourceStore.isAssetSourceConfigured()) return



    val notice by PacMazeResourceUpdateNotifier.notice.collectAsStateWithLifecycle()

    val syncUi by PacMazeResourceUpdateNotifier.syncUi.collectAsStateWithLifecycle()

    val activeDownload by ResourceStore.activeDownload.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }

    val localStatus = remember(notice, syncUi.isSyncing, syncUi.overallPercent, activeDownload) {
        ResourceStore.localGameResourceStatus()
    }

    LaunchedEffect(Unit) {
        PacMazeResourceUpdateNotifier.refresh()
    }

    // 下载结束后只做一次 refresh，避免 syncUi/activeDownload 抖动连环触发
    var lastSyncEndRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(syncUi.isSyncing, activeDownload) {
        val syncing = syncUi.isSyncing || activeDownload != null
        if (syncing) {
            lastSyncEndRefresh = false
            return@LaunchedEffect
        }
        if (!lastSyncEndRefresh) {
            lastSyncEndRefresh = true
            PacMazeResourceUpdateNotifier.refresh()
        }
    }



    LaunchedEffect(notice?.pendingBundleIds, localStatus.pendingBundleIds, syncUi.errorMessage) {
        val pending = notice?.pendingBundleIds?.takeIf { it.isNotEmpty() }
            ?: localStatus.pendingBundleIds.takeIf { it.isNotEmpty() }
            ?: return@LaunchedEffect
        if (syncUi.isSyncing) return@LaunchedEffect
        // 本地文件已就绪时不重复走下载横幅（避免每局结束误触「正在下载」）
        if (pending.all { ResourceStore.isPacMazeBundleReady(it) }) {
            PacMazeResourceUpdateNotifier.reconcileLocallyReadyAsync()
            return@LaunchedEffect
        }
        if (!syncUi.errorMessage.isNullOrBlank()) {
            kotlinx.coroutines.delay(PacMazeResourceUpdateNotifier.ERROR_RETRY_DELAY_MS)
            PacMazeResourceUpdateNotifier.clearSyncError()
        }
        PacMazeResourceUpdateNotifier.autoApplyPendingUpdates(context.applicationContext)
    }

    val pendingIds = notice?.pendingBundleIds?.takeIf { it.isNotEmpty() }
        ?: localStatus.pendingBundleIds
    val isDownloading = syncUi.isSyncing || activeDownload != null
    val displayPercent = when {
        syncUi.overallPercent > 0 -> syncUi.overallPercent
        activeDownload != null -> ResourceStore.estimateGameResourceOverallPercent(activeDownload, pendingIds)
        else -> 0
    }
    val visible = when {
        !syncUi.errorMessage.isNullOrBlank() -> true
        isDownloading && displayPercent < 100 -> true
        pendingIds.isNotEmpty() && !isDownloading -> true
        else -> false
    }

    val palette = when (style) {
        GameResourceBannerStyle.Home -> HomeBannerPalette
        GameResourceBannerStyle.PacMaze -> PacMazeBannerPalette
    }

    val shape = RoundedCornerShape(if (style == GameResourceBannerStyle.Home) 10.dp else 12.dp)

    val headline = when {
        !syncUi.errorMessage.isNullOrBlank() -> "游戏资源更新失败"
        isDownloading -> "正在下载游戏资源"
        pendingIds.isNotEmpty() -> notice?.summary?.ifBlank { null }
            ?: "游戏资源待更新（v${ResourceStore.requiredBundleVersion("pac_maze_skins") ?: ResourceStore.PAC_MAZE_SKINS_BUNDLE_VERSION}）"
        else -> ""
    }
    val subtitle = when {
        syncUi.progressLabel != null -> syncUi.progressLabel
        activeDownload != null -> activeDownload!!.label
        expanded -> null
        pendingIds.isNotEmpty() -> "将自动下载：${pendingIds.joinToString(" · ") { GameResourceBundles.shortDisplayName(it) }}"
        else -> "进游戏前预先下载，体验更流畅"
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.background)
                .border(1.dp, palette.border, shape),
        ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .clickable(enabled = !isDownloading) { expanded = !expanded }

                .padding(horizontal = 12.dp, vertical = 10.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(10.dp),

        ) {

            Icon(

                imageVector = Icons.Default.CloudDownload,

                contentDescription = null,

                tint = palette.accent,

                modifier = Modifier.size(20.dp),

            )

            Column(

                modifier = Modifier.weight(1f),

                verticalArrangement = Arrangement.spacedBy(2.dp),

            ) {

                Text(

                    text = headline,

                    color = palette.title,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.SemiBold,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis,

                )

                if (subtitle != null) {

                    Text(

                        text = subtitle,

                        color = palette.subtitle,

                        fontSize = 11.sp,

                        maxLines = if (expanded) 2 else 1,

                        overflow = TextOverflow.Ellipsis,

                    )

                }

            }

            if (!isDownloading) {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(2.dp),

                ) {

                    TextButton(

                        onClick = { expanded = !expanded },

                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),

                    ) {

                        Text(

                            text = if (expanded) "收起" else "详情",

                            color = palette.accent,

                            fontSize = 11.sp,

                        )

                        Icon(

                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,

                            contentDescription = null,

                            tint = palette.accent,

                            modifier = Modifier.size(16.dp),

                        )

                    }

                    if (style == GameResourceBannerStyle.Home) {

                        if (!syncUi.errorMessage.isNullOrBlank()) {
                            TextButton(
                                onClick = { PacMazeResourceUpdateNotifier.retryPendingUpdatesAsync(context) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                            ) {
                                Text("重试", color = palette.accent, fontSize = 11.sp)
                            }
                        }

                        TextButton(

                            onClick = { PacMazeResourceUpdateNotifier.dismissCurrent() },

                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),

                        ) {

                            Text("稍后", color = palette.subtitle, fontSize = 11.sp)

                        }

                    } else {

                        PacMazeSecondaryButton(

                            text = "稍后",

                            onClick = { PacMazeResourceUpdateNotifier.dismissCurrent() },

                            compact = true,

                        )

                    }

                }

            } else {

                Text(

                    text = "${displayPercent.coerceIn(0, 100)}%",

                    color = palette.accent,

                    fontSize = 12.sp,

                    fontWeight = FontWeight.Bold,

                )

            }

        }



        AnimatedVisibility(

            visible = expanded && !isDownloading,

            enter = expandVertically(),

            exit = shrinkVertically(),

        ) {

            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(start = 42.dp, end = 12.dp, bottom = 10.dp),

                verticalArrangement = Arrangement.spacedBy(4.dp),

            ) {

                GameResourceBundles.gameBootOrder.forEach { bundleId ->

                    val ready = bundleId in localStatus.readyBundleIds

                    val pending = bundleId in pendingIds

                    val label = GameResourceBundles.displayName(bundleId)

                    val mark = when {

                        ready -> "✓"

                        pending -> "○"

                        else -> "·"

                    }

                    val color = when {

                        ready -> palette.ready

                        pending -> palette.accent

                        else -> palette.subtitle

                    }

                    Text(

                        text = "$mark $label",

                        color = color,

                        fontSize = 11.sp,

                    )

                }

            }

        }



        if (isDownloading) {
            LinearProgressIndicator(
                progress = displayPercent.coerceIn(0, 100) / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = palette.accent,
                trackColor = palette.progressTrack,
            )
        }
        }
    }
}



private data class BannerPalette(

    val background: Brush,

    val border: Color,

    val title: Color,

    val subtitle: Color,

    val accent: Color,

    val ready: Color,

    val progressTrack: Color,

)



private val HomeBannerPalette = BannerPalette(

    background = Brush.horizontalGradient(

        listOf(Color(0xFFF3E5F5), Color(0xFFFCE4EC)),

    ),

    border = Color(0xFFCE93D8).copy(alpha = 0.55f),

    title = Color(0xFF4A148C),

    subtitle = Color(0xFF7B1FA2).copy(alpha = 0.75f),

    accent = Color(0xFF8E24AA),

    ready = Color(0xFF2E7D32),

    progressTrack = Color(0xFFCE93D8).copy(alpha = 0.25f),

)



private val PacMazeBannerPalette = BannerPalette(

    background = Brush.horizontalGradient(

        listOf(Color(0xFF1A2438), Color(0xFF1E2A42)),

    ),

    border = PacMazePalette.accentGold.copy(alpha = 0.45f),

    title = PacMazePalette.accentGold,

    subtitle = PacMazePalette.inkSecondary,

    accent = PacMazePalette.accentOrange,

    ready = Color(0xFF69F0AE),

    progressTrack = Color.White.copy(alpha = 0.12f),

)


