package com.example.funlife.ui.screens.platformer



import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.funlife.game.platformer.catalog.PlatformerResourcePrewarmCoordinator
import com.example.funlife.ui.screens.platformer.GameResourceLoadCopy



private val BANNER_BG = Color(0xE61B2838)

private val ACCENT = Color(0xFF4FC3F7)

private val ACCENT_SOFT = Color(0xFF81C784)

private val TEXT = Color(0xFFFFF8E7)

private val MUTED = Color(0xFFB8A99A)



/**

 * 大厅角落横版资源 decode 进度条。

 * - 快速通道完成前：「横版资源准备中」

 * - 可玩后后台继续：「其余角色后台准备中」（绿色，不阻断游玩）

 */

@Composable

fun PlatformerPrewarmBanner(

    modifier: Modifier = Modifier,

) {

    val prewarm by PlatformerResourcePrewarmCoordinator.state.collectAsStateWithLifecycle()

    val visible = !prewarm.allCharactersReady &&

        (prewarm.running || prewarm.backgroundRunning || prewarm.percent in 1..99)



    val title = when {

        prewarm.minimumPlayableReady && prewarm.backgroundRunning -> "其余角色后台准备中"

        prewarm.minimumPlayableReady -> "坤坤大冒险资源即将就绪"

        else -> "坤坤大冒险资源准备中"

    }

    val accent = if (prewarm.minimumPlayableReady) ACCENT_SOFT else ACCENT



    AnimatedVisibility(

        visible = visible,

        enter = fadeIn(),

        exit = fadeOut(),

        modifier = modifier,

    ) {

        Box(

            modifier = Modifier

                .widthIn(max = 320.dp)

                .clip(RoundedCornerShape(10.dp))

                .background(BANNER_BG)

                .padding(horizontal = 12.dp, vertical = 8.dp),

        ) {

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically,

                ) {

                    Text(

                        title,

                        color = TEXT,

                        fontSize = 12.sp,

                        fontWeight = FontWeight.SemiBold,

                    )

                    Text(

                        "${prewarm.percent.coerceIn(0, 100)}%",

                        color = accent,

                        fontSize = 12.sp,

                        fontWeight = FontWeight.Bold,

                    )

                }

                LinearProgressIndicator(

                    progress = prewarm.percent.coerceIn(0, 100) / 100f,

                    modifier = Modifier.fillMaxWidth(),

                    color = accent,

                    trackColor = Color.White.copy(alpha = 0.12f),

                )

                if (prewarm.phase.isNotBlank()) {

                    Text(

                        GameResourceLoadCopy.forDisplay(prewarm.phase),

                        color = MUTED,

                        fontSize = 10.sp,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis,

                    )

                } else if (prewarm.totalSteps > 0) {

                    Text(

                        "${prewarm.completedSteps}/${prewarm.totalSteps} 项",

                        color = MUTED,

                        fontSize = 10.sp,

                    )

                }

                if (prewarm.minimumPlayableReady && prewarm.backgroundRunning) {

                    Text(

                        "可先进入坤坤大冒险",

                        color = ACCENT_SOFT.copy(alpha = 0.85f),

                        fontSize = 9.sp,

                    )

                }

            }

        }

    }

}


