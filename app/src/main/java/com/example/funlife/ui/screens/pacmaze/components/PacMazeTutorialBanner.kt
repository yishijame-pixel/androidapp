package com.example.funlife.ui.screens.pacmaze.components



import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants

import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog

import com.example.funlife.ui.screens.pacmaze.currentPacMazePlayLayout



/** 闯关分步教学：传送门（L6+）与 L14+ 机关提示。 */

@Composable

fun PacMazeTutorialBanner(

    levelId: Int,

    visitedCheckpointCount: Int,

    hasDynamicTiles: Boolean,

    portalArmedCount: Int = 0,

    portalTotal: Int = 0,

    modifier: Modifier = Modifier,

) {

    val hint = tutorialHint(

        levelId = levelId,

        visited = visitedCheckpointCount,

        hasDynamic = hasDynamicTiles,

        portalArmed = portalArmedCount,

        portalTotal = portalTotal,

    ) ?: return

    val play = currentPacMazePlayLayout()



    Box(

        modifier = modifier

            .fillMaxWidth(0.72f)

            .background(Color(0xCC1A2744), RoundedCornerShape(play.dp(8.dp)))

            .padding(horizontal = play.dp(10.dp), vertical = play.dp(6.dp)),

        contentAlignment = Alignment.Center,

    ) {

        Text(

            text = hint,

            color = Color(0xFFE2E8F0),

            fontSize = 11.sp,

            fontWeight = FontWeight.Medium,

            textAlign = TextAlign.Center,

            lineHeight = 14.sp,

        )

    }

}



private fun tutorialHint(

    levelId: Int,

    visited: Int,

    hasDynamic: Boolean,

    portalArmed: Int,

    portalTotal: Int,

): String? {

    if (portalTotal >= 2 && portalArmed < portalTotal) {

        return when (portalArmed) {

            0 -> "传送门：左右 001 / 002 各踩一次激活（听音效、门变绿）"

            1 -> "还差一扇！去对侧传送门再踩一次，两门都亮后才能传送"

            else -> "在已激活的传送格按 ↑ 或 ↓ 穿过到对侧"

        }

    }

    if (portalTotal >= 2 && portalArmed >= portalTotal) {

        return "两门已激活：站在传送格按 ↑ 或 ↓ 穿过到对侧"

    }



    val meta = PacMazeLevelCatalog.find(levelId) ?: return null

    if (meta.tutorialHint.isBlank()) return null

    return when (levelId) {

        14 -> when {

            visited == 0 -> "① 沿主轴上行，从宽门进入左/右「闸道舱」"

            visited < 2 -> "② 观察顶栏移动墙相位，在开放条纹时穿过 & 栅栏"

            else -> "③ 抵达西闸、东闸与核心，清豆通关拿三星"

        }

        15 -> when {

            !hasDynamic -> meta.tutorialHint

            visited < 2 -> "十字激光扫射时走闸道边带，不要停在中轴交点"

            else -> "双翼 + 核心都到访后再收尾"

        }

        16 -> when {

            visited < 2 -> "能量门与移动墙交替：门开时冲线，门关时走侧道"

            else -> meta.tutorialHint

        }

        17, 18 -> when {

            visited < 2 -> "顶栏激光相位：预警 ${PacMazeConstants.LASER_WARN_TICKS / 60}s 后扫射，别停交点"

            else -> meta.tutorialHint

        }

        else -> meta.tutorialHint

    }

}

