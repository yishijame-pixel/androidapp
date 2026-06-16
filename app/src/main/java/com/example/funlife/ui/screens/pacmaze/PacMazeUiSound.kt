package com.example.funlife.ui.screens.pacmaze

/**
 * Pac-Maze Hub UI 音效规范。
 *
 * 素材来源：Kenney UI Audio（CC0）→ pac_maze_sfx/curated/ui/ 目录下 ogg 文件
 * 原则：每种交互绑定唯一音效，禁止随机混播；对局内玩法音效见 [PacMazeAudioEvent]。
 */
enum class PacMazeUiSoundId(
    internal val event: PacMazeAudioEvent,
    /** 产品/QA 对照用说明 */
    val specNote: String,
) {
    NavigateBack(
        event = PacMazeAudioEvent.UI_BACK,
        specNote = "顶栏返回、对局 HUD 返回",
    ),
    NavigateForward(
        event = PacMazeAudioEvent.UI_NAV_FORWARD,
        specNote = "章节卡片、打开子页面",
    ),
    PrimaryConfirm(
        event = PacMazeAudioEvent.UI_PRIMARY_CONFIRM,
        specNote = "主 CTA：开始/继续/确认拖尾",
    ),
    SecondaryAction(
        event = PacMazeAudioEvent.UI_SECONDARY,
        specNote = "次要按钮：练习、返回选关、退出",
    ),
    ChipAction(
        event = PacMazeAudioEvent.UI_CHIP,
        specNote = "Hub 文字 Chip：工坊/收藏册/换角色入口",
    ),
    UtilityRecommend(
        event = PacMazeAudioEvent.UI_UTILITY,
        specNote = "推荐搭配、一键应用",
    ),
    TabSwitch(
        event = PacMazeAudioEvent.UI_TAB,
        specNote = "收藏册 Tab 切换",
    ),
    ListSelect(
        event = PacMazeAudioEvent.UI_LIST_SELECT,
        specNote = "关卡列表行、详情页选项",
    ),
    GridSelect(
        event = PacMazeAudioEvent.UI_GRID_SELECT,
        specNote = "皮肤/拖尾网格单元",
    ),
    SeriesCard(
        event = PacMazeAudioEvent.UI_SERIES_CARD,
        specNote = "角色系列大卡片",
    ),
    ModeFeatured(
        event = PacMazeAudioEvent.UI_MODE_FEATURED,
        specNote = "模式选择 · 单人闯关主卡",
    ),
    ModeOption(
        event = PacMazeAudioEvent.UI_MODE_OPTION,
        specNote = "模式选择 · 无尽/迷宫副卡",
    ),
    MapNode(
        event = PacMazeAudioEvent.UI_MAP_NODE,
        specNote = "全景闯关路径节点",
    ),
    MapChip(
        event = PacMazeAudioEvent.UI_MAP_CHIP,
        specNote = "快速选图条",
    ),
    Toggle(
        event = PacMazeAudioEvent.UI_TOGGLE,
        specNote = "折叠/展开、HUD 开关",
    ),
}
