package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.PacMazeStarCriteria

/** 关卡详情页：三星条件与机制说明（UI 层摘要，与 JSON starCriteria 大致对齐）。 */
object PacMazeLevelDetailInfo {

    data class StarGoalLine(val stars: Int, val text: String)

    fun starGoals(levelId: Int): List<StarGoalLine> {
        val criteria = criteriaFor(levelId)
        return buildList {
            add(StarGoalLine(1, "通关即可"))
            add(StarGoalLine(2, "得分 ≥ ${criteria.twoStarMinScore}"))
            val threeParts = mutableListOf<String>()
            threeParts += "得分 ≥ ${criteria.threeStarMinScore}"
            if (criteria.threeStarMaxSeconds > 0) {
                threeParts += "用时 ≤ ${criteria.threeStarMaxSeconds} 秒"
            }
            if (criteria.threeStarNoDeath) {
                threeParts += "零死亡"
            }
            if (criteria.threeStarRequiredTags.isNotEmpty()) {
                threeParts += "抵达指定检查点"
            }
            add(StarGoalLine(3, threeParts.joinToString(" · ")))
        }
    }

    fun criteriaFor(levelId: Int): PacMazeStarCriteria = when (levelId) {
        in 1..5 -> PacMazeStarCriteria(twoStarMinScore = 1200, threeStarMinScore = 2400)
        in 6..9 -> PacMazeStarCriteria(twoStarMinScore = 1500, threeStarMinScore = 3000)
        in 10..13 -> PacMazeStarCriteria(
            twoStarMinScore = 1800,
            threeStarMinScore = 3600,
            threeStarMaxSeconds = 180,
        )
        in 14..17 -> PacMazeStarCriteria(
            twoStarMinScore = 2000,
            threeStarMinScore = 4000,
            threeStarMaxSeconds = 150,
            threeStarNoDeath = levelId >= 16,
        )
        in 18..20 -> PacMazeStarCriteria(
            twoStarMinScore = 2200,
            threeStarMinScore = 4500,
            threeStarMaxSeconds = 120,
            threeStarNoDeath = true,
        )
        else -> PacMazeStarCriteria(
            twoStarMinScore = 2500,
            threeStarMinScore = 5000,
            threeStarMaxSeconds = 90,
            threeStarNoDeath = true,
        )
    }

    fun unlockHint(levelId: Int, maxLevelReached: Int): String? =
        if (PacMazeTestUnlock.isLevelUnlocked(levelId, maxLevelReached)) {
            if (PacMazeTestUnlock.enabled) "测试包 · 全部关卡已解锁" else null
        } else {
            "通关第 $maxLevelReached 关后解锁"
        }
}
