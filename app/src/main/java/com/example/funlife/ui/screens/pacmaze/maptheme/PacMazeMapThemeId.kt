package com.example.funlife.ui.screens.pacmaze.maptheme

/** 地图视觉主题（逻辑格与碰撞与主题无关）。 */
enum class PacMazeMapThemeId(
    val id: String,
    val displayName: String,
    val subtitle: String,
) {
    CLASSIC("classic", "经典街机", "原版吃豆风"),
    CYBERPUNK("cyberpunk", "霓虹赛博", "能量管道 · 数据包"),
    GARDEN("garden", "花园迷宫", "树篱 · 萤火虫"),
    FOOD("food", "糖果王国", "饼干墙 · 糖果豆"),
    CHINESE("chinese", "国风长安", "青砖 · 灵气"),
}
