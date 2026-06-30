package com.example.funlife.ui.screens.pacmaze.cosmetic

/** 一十类（品牌 / 行走）角色集合。 */
object PacMazeYishiCatalog {

    val skinIds: List<PacMazeSkinId> = listOf(
        PacMazeSkinId.YISHI_FIRE_LONG,
        PacMazeSkinId.YISHI_GREEN_LONG,
        PacMazeSkinId.YISHI_HAIMIAN,
        PacMazeSkinId.YISHI_ICE_LONG,
        PacMazeSkinId.YISHI_LONG,
        PacMazeSkinId.YISHI_MAGIC_DOG,
        PacMazeSkinId.YISHI_PAIDAXIN,
        PacMazeSkinId.YISHI_QISHI_DOG,
        PacMazeSkinId.YISHI_BL_LONG,
    )

    private val skinSet = skinIds.toSet()

    fun contains(skinId: PacMazeSkinId): Boolean = skinId in skinSet
}
