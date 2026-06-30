package com.example.funlife.resource

import com.example.funlife.ui.screens.platformer.GameResourceLoadCopy

/**
 * 核心玩法云端资源包：豆人迷宫 + 横版冒险共用同一套 boot 顺序，
 * 进主游戏加载页时一次性拉齐，避免进入横版冒险再单独等下载。
 */
object GameResourceBundles {
    const val SKINS = PacMazeResourceBundles.SKINS
    const val SFX = PacMazeResourceBundles.SFX
    const val PLATFORMER = "platformer_characters"
    const val PLATFORMER_SFX = "platformer_sfx"
    const val PLATFORMER_SUPERTUX = "platformer_supertux"

    /** 加载页顺序：皮肤 → 豆人音效 → 横版音效 → 横版角色包 */
    val gameBootOrder: List<String> = listOf(SKINS, SFX, PLATFORMER_SFX, PLATFORMER)

    /** SuperTux 扩展包按需下载（进入南极章前 ensure）。 */
    val platformerOptionalBundles: List<String> = listOf(PLATFORMER_SUPERTUX)

    fun displayName(bundleId: String): String = GameResourceLoadCopy.forDisplay(
        when (bundleId) {
            SFX -> "音效与背景音乐"
            SKINS -> "角色游戏资源"
            PLATFORMER -> "坤坤大冒险资源"
            PLATFORMER_SFX -> "横版冒险音效"
            PLATFORMER_SUPERTUX -> "南极探险扩展包"
            else -> bundleId
        },
    )

    fun shortDisplayName(bundleId: String): String = GameResourceLoadCopy.forDisplay(
        when (bundleId) {
            SFX -> "音效"
            SKINS -> "角色资源"
            PLATFORMER -> "坤坤大冒险"
            PLATFORMER_SFX -> "横版音效"
            PLATFORMER_SUPERTUX -> "南极扩展"
            else -> displayName(bundleId)
        },
    )

    /** 进局进度条权重（合计 100） */
    fun bootWeight(bundleId: String): Int = when (bundleId) {
        SKINS -> 46
        SFX -> 18
        PLATFORMER_SFX -> 14
        PLATFORMER -> 22
        else -> 30
    }

    fun isCoreGameBundle(bundleId: String): Boolean =
        bundleId in gameBootOrder || bundleId in platformerOptionalBundles
}
