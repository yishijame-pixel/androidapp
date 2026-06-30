package com.example.funlife.game.platformer

/** 闯关同款视觉判定（无尽跑酷复用 1–6 关 Goodly 地砖 + 手绘远景）。 */
object PlatformerCampaignVisuals {

    fun isEndless(level: PlatformerLevelDef): Boolean = level.seriesId == "endless"

    /** 使用闯关 Goodly 图集地砖（非外部 zip 包）。 */
    fun usesGoodlyAtlas(level: PlatformerLevelDef): Boolean =
        level.tilesetPack == PlatformerTilesetPack.GOODLY

    /** 是否绘制 1–6 关同款多层远景（云朵/山峦/树影）。 */
    fun showsScenicBackdrop(level: PlatformerLevelDef): Boolean =
        level.id in 1..6 || (isEndless(level) && usesGoodlyAtlas(level))

    /** 是否绘制地图内 Goodly 树影装饰格。 */
    fun showsGoodlyMapBackdrops(level: PlatformerLevelDef): Boolean = showsScenicBackdrop(level)
}
