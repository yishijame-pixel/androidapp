package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.ui.screens.platformer.GameResourceLoadCopy
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreviewAnim
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.pacMazeSkinAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberRemoteSkinLoadStatus(
    skinId: PacMazeSkinId,
    loadMode: RemoteSkinLoadMode = RemoteSkinLoadMode.FullAnimation,
): RemoteSkinLoadStatus {
    if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) {
        return RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100)
    }

    when (loadMode) {
        RemoteSkinLoadMode.CoverOnly -> {
            var coverReady by remember(skinId) {
                mutableStateOf(PacMazeRemoteSkinAnimCache.isCoverReady(skinId))
            }
            var coverCachedOnDisk by remember(skinId) { mutableStateOf(false) }
            LaunchedEffect(skinId) {
                coverCachedOnDisk = withContext(Dispatchers.IO) {
                    PacMazeRemoteSkinAnimCache.hasCoverCacheOnDisk(skinId)
                }
                if (!coverReady) {
                    withContext(Dispatchers.IO) {
                        if (!PacMazeRemoteSkinAnimCache.hydrateCoverFromDisk(skinId)) {
                            PacMazeRemoteSkinAnimCache.preloadCover(skinId)
                        }
                    }
                    coverReady = PacMazeRemoteSkinAnimCache.isCoverReady(skinId)
                }
            }
            return if (coverReady) {
                RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "封面已就绪")
            } else if (coverCachedOnDisk || PacMazeRemoteSkinAnimCache.hasCoverCache(skinId)) {
                RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 90, "恢复封面…")
            } else {
                RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 0, "加载封面…")
            }
        }
        RemoteSkinLoadMode.FullAnimation -> {
            val allStatus by PacMazeRemoteSkinAnimCache.status.collectAsStateWithLifecycle()
            val config = PacMazeRemoteSkinAnimCatalog.config(skinId)
            LaunchedEffect(skinId, loadMode) {
                if (PacMazeRemoteSkinAnimCache.isPlaybackReady(skinId)) return@LaunchedEffect
                if (PacMazeRemoteSkinAnimCache.hasSheetBundle(skinId) && config != null) {
                    PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(skinId, config.primaryClip())
                } else if (!PacMazeRemoteSkinAnimCache.isAnimInMemory(skinId)) {
                    PacMazeRemoteSkinAnimCache.preloadForSkin(skinId)
                }
            }
            val cached = allStatus[skinId]
            return when {
                PacMazeRemoteSkinAnimCache.isPlaybackReady(skinId) ->
                    RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪")
                cached != null -> cached
                PacMazeRemoteSkinAnimCache.isCoverReady(skinId) ->
                    RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 72, "解析角色资源…")
                PacMazeRemoteSkinAnimCache.isReady(skinId) ->
                    RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 88, "从本地缓存恢复…")
                else -> RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 0, "准备加载…")
            }
        }
    }
}

@Composable
fun PacMazeRemoteSkinLoadOverlay(
    skinId: PacMazeSkinId,
    modifier: Modifier = Modifier,
    loadMode: RemoteSkinLoadMode = RemoteSkinLoadMode.FullAnimation,
    content: @Composable () -> Unit,
) {
    val loadStatus = rememberRemoteSkinLoadStatus(skinId, loadMode)
    val accent = pacMazeSkinAccent(skinId)
    val scope = rememberCoroutineScope()
    val isRemote = PacMazeCharacterPreviewAnim.usesRemoteAnim(skinId)
    var coverCachedOnDisk by remember(skinId) { mutableStateOf(false) }
    LaunchedEffect(skinId, loadMode) {
        if (loadMode == RemoteSkinLoadMode.CoverOnly) {
            coverCachedOnDisk = withContext(Dispatchers.IO) {
                PacMazeRemoteSkinAnimCache.hasCoverCacheOnDisk(skinId)
            }
        }
    }
    val coverReady = PacMazeRemoteSkinAnimCache.isCoverReady(skinId)
    val playbackReady = PacMazeRemoteSkinAnimCache.isPlaybackReady(skinId)
    val coverCached = coverCachedOnDisk || PacMazeRemoteSkinAnimCache.hasCoverCache(skinId) || coverReady
    val ready = when (loadMode) {
        RemoteSkinLoadMode.CoverOnly -> coverReady
        RemoteSkinLoadMode.FullAnimation -> playbackReady
    }
    val hasVisualFallback = coverReady || coverCached
    val showBlockingOverlay = isRemote && !ready && !hasVisualFallback &&
        loadStatus.phase != RemoteSkinLoadPhase.Ready
    val showDecodeBadge = isRemote && !ready && hasVisualFallback &&
        loadStatus.phase != RemoteSkinLoadPhase.Ready &&
        loadStatus.phase != RemoteSkinLoadPhase.Failed

    Box(modifier = modifier) {
        content()
        if (showBlockingOverlay || loadStatus.phase == RemoteSkinLoadPhase.Failed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF121828).copy(alpha = if (loadStatus.phase == RemoteSkinLoadPhase.Failed) 0.92f else 0.88f))
                    .then(
                        if (loadStatus.phase == RemoteSkinLoadPhase.Failed) {
                            Modifier.clickable {
                                scope.launch {
                                    when (loadMode) {
                                        RemoteSkinLoadMode.CoverOnly ->
                                            PacMazeRemoteSkinAnimCache.preloadCover(skinId)
                                        RemoteSkinLoadMode.FullAnimation ->
                                            PacMazeRemoteSkinAnimCache.preloadForSkin(skinId)
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (loadStatus.phase == RemoteSkinLoadPhase.Failed) {
                        Text("⚠", fontSize = 28.sp)
                        Text(
                            GameResourceLoadCopy.forDisplay(
                                loadStatus.message.ifBlank { "下载失败" },
                            ),
                            color = PacMazePalette.accentOrange,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text("点击重试", color = accent, fontSize = 10.sp)
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = accent,
                            strokeWidth = 3.dp,
                        )
                        Text(
                            GameResourceLoadCopy.forDisplay(
                                loadStatus.message.ifBlank {
                                    when (loadMode) {
                                        RemoteSkinLoadMode.CoverOnly -> "加载封面…"
                                        RemoteSkinLoadMode.FullAnimation -> "解析角色资源…"
                                    }
                                },
                            ),
                            color = PacMazePalette.inkSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else if (showDecodeBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF121828).copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = loadStatus.percent.coerceIn(0, 100) / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp)),
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                    Text(
                        GameResourceLoadCopy.forDisplay(
                            loadStatus.message.ifBlank { "解析角色资源…" },
                        ),
                        color = PacMazePalette.inkSecondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
