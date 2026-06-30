package com.example.funlife.game.platformer

/** SuperTux 扩展章（901+），与主线 1–74 隔离。含 World1/2/Bonus/Redmond 共 107 关。 */
object PlatformerSuperTuxLengthSpec {

    const val SUPERTUX_LEVEL_START = 901
    const val SUPERTUX_LEVEL_END = 1018
    const val SUPERTUX_LEVEL_COUNT = 107

    /** 章节间预留 ID 缝（932–940），无对应关卡。 */
    private val GAP_RANGES = listOf(932..940)

    fun isSuperTuxLevel(levelId: Int): Boolean {
        if (levelId !in SUPERTUX_LEVEL_START..SUPERTUX_LEVEL_END) return false
        return GAP_RANGES.none { levelId in it }
    }

    fun chapterRange(chapterId: String): IntRange? = when (chapterId) {
        "supertux_antarctic" -> 901..931
        "supertux_forest" -> 941..978
        "supertux_bonus" -> 981..1010
        "supertux_redmond" -> 1011..1018
        else -> null
    }
}
