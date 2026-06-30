package com.example.funlife.game.platformer

/**
 * FunLife 关卡 ID → SuperTux 原生 `.stl` 路径（供经典引擎 Intent 使用）。
 */
object PlatformerSuperTuxClassicPaths {

    /** @return e.g. `levels/world1/welcome_antarctica.stl` */
    fun levelStlPath(levelId: Int): String? {
        val source = PlatformerSuperTuxLevelCatalog.sourceStl(levelId) ?: return null
        return "levels/$source"
    }
}
