package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeDifficulty
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeVariant
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId

/** Hub 内子页面路由（配合 [PacMazeUiState.menuRouteStack] 栈式返回）。 */
sealed class PacMazeMenuRoute {
    data object ModeSelect : PacMazeMenuRoute()
    data object ChapterOverview : PacMazeMenuRoute()
    data class ChapterLevels(val themeId: PacMazeMapThemeId) : PacMazeMenuRoute()
    data class LevelDetail(val levelId: Int) : PacMazeMenuRoute()

    /** @deprecated 使用 [MazeHome] */
    data object MazeHub : PacMazeMenuRoute()
    data object MazeHome : PacMazeMenuRoute()
    data class MazePlayGate(val seedMode: PacMazeMazeSeedMode) : PacMazeMenuRoute()
    data object MazeLaunchConfirm : PacMazeMenuRoute()
    data object MazeTrackPicker : PacMazeMenuRoute()
    data class MazeTrackDetail(val track: PacMazeMazeDifficulty) : PacMazeMenuRoute()
    data object MazeContractLab : PacMazeMenuRoute()
    data class MazeContractDetail(val contract: PacMazeMazeContract) : PacMazeMenuRoute()
    data object MazeArcadeHall : PacMazeMenuRoute()
    data class MazeVariantDetail(val variant: PacMazeMazeVariant) : PacMazeMenuRoute()
    data object MazeCompetitiveHub : PacMazeMenuRoute()
    data object MazeDailyBoard : PacMazeMenuRoute()
    data object MazeWeeklyBoard : PacMazeMenuRoute()
    data object MazeGhostReplay : PacMazeMenuRoute()
    data object MazeCodex : PacMazeMenuRoute()
    data class MazeCodexEntry(val entryId: String) : PacMazeMenuRoute()

    data object SerpentineMap : PacMazeMenuRoute()
    data object CharacterSeries : PacMazeMenuRoute()
    data class CharacterGrid(val series: PacMazeSkinSeries) : PacMazeMenuRoute()
    data class CharacterDetail(val skinId: PacMazeSkinId) : PacMazeMenuRoute()
    data object TrailWorkshop : PacMazeMenuRoute()
    data object CollectionBook : PacMazeMenuRoute()

    /** 本地大厅内：在线开房 / 加入（对决或合作） */
    data class OnlineHub(val subMode: String) : PacMazeMenuRoute()
    data class OnlineLobby(val roomId: String) : PacMazeMenuRoute()
}

fun PacMazeMenuRoute.subtitle(): String = when (this) {
    PacMazeMenuRoute.ModeSelect -> "模式选择"
    PacMazeMenuRoute.ChapterOverview -> "全景闯关径"
    is PacMazeMenuRoute.ChapterLevels -> "章节关卡"
    is PacMazeMenuRoute.LevelDetail -> "关卡详情"
    PacMazeMenuRoute.MazeHub, PacMazeMenuRoute.MazeHome -> "迷雾迷宫"
    is PacMazeMenuRoute.MazePlayGate -> if (seedMode == PacMazeMazeSeedMode.DAILY) "每日挑战" else "自由随机"
    PacMazeMenuRoute.MazeLaunchConfirm -> "确认开局"
    PacMazeMenuRoute.MazeTrackPicker -> "难度赛道"
    is PacMazeMenuRoute.MazeTrackDetail -> "赛道档案"
    PacMazeMenuRoute.MazeContractLab -> "契约工坊"
    is PacMazeMenuRoute.MazeContractDetail -> "契约说明"
    PacMazeMenuRoute.MazeArcadeHall -> "变体馆"
    is PacMazeMenuRoute.MazeVariantDetail -> "变体规则"
    PacMazeMenuRoute.MazeCompetitiveHub -> "竞技中心"
    PacMazeMenuRoute.MazeDailyBoard -> "今日榜"
    PacMazeMenuRoute.MazeWeeklyBoard -> "周赛榜"
    PacMazeMenuRoute.MazeGhostReplay -> "Ghost 轨迹"
    PacMazeMenuRoute.MazeCodex -> "规则图鉴"
    is PacMazeMenuRoute.MazeCodexEntry -> "机制详情"
    PacMazeMenuRoute.SerpentineMap -> "全景闯关径"
    PacMazeMenuRoute.CharacterSeries -> "挑选系列"
    is PacMazeMenuRoute.CharacterGrid -> "皮肤网格"
    is PacMazeMenuRoute.CharacterDetail -> "角色详情"
    PacMazeMenuRoute.TrailWorkshop -> "拖尾工坊"
    PacMazeMenuRoute.CollectionBook -> "收藏册"
    is PacMazeMenuRoute.OnlineHub ->
        if (subMode == "coop_campaign") "并肩闯关" else "豆人对决"
    is PacMazeMenuRoute.OnlineLobby -> "对战大厅"
}

fun PacMazeMenuRoute.showsMazeHubTopStats(): Boolean = when (this) {
    PacMazeMenuRoute.MazeHub,
    PacMazeMenuRoute.MazeHome,
    -> true
    else -> false
}

/** 迷雾迷宫子页：隐藏全局顶栏，改用页内紧凑返回条以腾出内容区。 */
fun PacMazeMenuRoute.usesCompactMazeChrome(): Boolean = isMazeRoute() && !showsMazeHubTopStats()

fun PacMazeMenuRoute.isMazeRoute(): Boolean = when (this) {
    PacMazeMenuRoute.MazeHub,
    PacMazeMenuRoute.MazeHome,
    is PacMazeMenuRoute.MazePlayGate,
    PacMazeMenuRoute.MazeLaunchConfirm,
    PacMazeMenuRoute.MazeTrackPicker,
    is PacMazeMenuRoute.MazeTrackDetail,
    PacMazeMenuRoute.MazeContractLab,
    is PacMazeMenuRoute.MazeContractDetail,
    PacMazeMenuRoute.MazeArcadeHall,
    is PacMazeMenuRoute.MazeVariantDetail,
    PacMazeMenuRoute.MazeCompetitiveHub,
    PacMazeMenuRoute.MazeDailyBoard,
    PacMazeMenuRoute.MazeWeeklyBoard,
    PacMazeMenuRoute.MazeGhostReplay,
    PacMazeMenuRoute.MazeCodex,
    is PacMazeMenuRoute.MazeCodexEntry,
    -> true
    else -> false
}
