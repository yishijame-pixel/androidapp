package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.resource.PacMazeResourceBundles
import com.example.funlife.resource.PacMazeResourceUpdateNotifier
import kotlinx.coroutines.launch

/**
 * 大厅顶部资源更新横幅：检测到 manifest / 本地 bundle 过期时提示一键同步。
 */
@Composable
fun PacMazeResourceUpdateBanner(
    modifier: Modifier = Modifier,
) {
    val notice by PacMazeResourceUpdateNotifier.notice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updating by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var progressLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        PacMazeResourceUpdateNotifier.refresh()
    }

    val status = notice ?: return
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF1A2438))
            .border(1.dp, PacMazePalette.accentGold.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "资源更新",
                    color = PacMazePalette.accentGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    status.summary.ifBlank { "云端资源有更新" },
                    color = PacMazePalette.inkSecondary,
                    fontSize = 11.sp,
                )
                if (progressLabel != null) {
                    Text(
                        progressLabel!!,
                        color = PacMazePalette.inkHint,
                        fontSize = 10.sp,
                    )
                }
            }
            if (!updating) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PacMazeSecondaryButton(
                        text = "稍后",
                        onClick = { PacMazeResourceUpdateNotifier.dismissCurrent() },
                        compact = true,
                    )
                    PacMazePrimaryButton(
                        text = "立即更新",
                        onClick = {
                            if (updating) return@PacMazePrimaryButton
                            scope.launch {
                                updating = true
                                progress = 0
                                progressLabel = null
                                try {
                                    val result = PacMazeResourceUpdateNotifier.applyPendingUpdates(context) { bundleId, _, overall ->
                                        progress = overall
                                        progressLabel = "正在同步 ${PacMazeResourceBundles.displayName(bundleId)}…"
                                    }
                                    if (!result.isSuccess) {
                                        progressLabel = result.userMessage().ifBlank { "更新失败，请检查网络" }
                                    } else {
                                        progressLabel = null
                                    }
                                } finally {
                                    updating = false
                                }
                            }
                        },
                        compact = true,
                    )
                }
            }
        }
        if (updating) {
            LinearProgressIndicator(
                progress = progress.coerceIn(0, 100) / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp)),
                color = PacMazePalette.accentOrange,
                trackColor = Color.White.copy(alpha = 0.12f),
            )
        }
    }
}
