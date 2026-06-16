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
    ENDLESS("endless", "无尽虚空", "波次递进 · 霓虹深渊"),
    MAZE("maze", "石径迷宫", "迷雾通道 · 寻出口"),
    SUBMARINE("submarine", "深潜舱", "声呐 · 水密门"),
    ORBITAL("orbital", "星港环带", "舱段 · 气闸"),
    STEAMPUNK("steampunk", "蒸汽工坊", "铜管 · 齿轮墙"),
    ARCHIVE("archive", "古籍库", "书架 · 封印卷轴"),
    MAGMA("magma", "熔岩矿道", "黑曜石 · 地核"),
    FROST("frost", "极寒冰库", "霜花 · 冷凝雾"),
    METRO("metro", "地铁枢纽", "站牌 · 区间隧道"),
    OPERA("opera", "幻境戏台", "幕布 · 场次牌"),
    VHS("vhs", "故障电视", "扫描线 · RGB 错位"),
    GREENHOUSE("greenhouse", "温室穹顶", "玻璃格架 · 花粉"),
    CHRONO("chrono", "时钟塔", "表盘格 · 指针门"),
    MIRROR("mirror", "镜像维度", "对称倒影 · 双影关"),
    ;

    val isExtremeChapter: Boolean get() = this in EXTREME_CHAPTERS

    companion object {
        val EXTREME_CHAPTERS: Set<PacMazeMapThemeId> = setOf(
            STEAMPUNK, VHS, ORBITAL, MAGMA, SUBMARINE, FROST, ARCHIVE, METRO, OPERA, GREENHOUSE,
        )

        fun fromId(raw: String?): PacMazeMapThemeId? =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) }
    }
}
