package com.example.funlife.ui.screens.pacmaze.cosmetic

/** ikun 类（梗图 / 云端行走）角色集合。 */
object PacMazeIkunCatalog {

    val skinIds: List<PacMazeSkinId> = listOf(
        PacMazeSkinId.FOOD_CHICK_DAZE,
        PacMazeSkinId.FOOD_CHICK_BALLER,
        PacMazeSkinId.FOOD_CHICK_WALKER,
        PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX,
        PacMazeSkinId.FOOD_XIA_WALK,
        PacMazeSkinId.FOOD_MOUSE_WALK,
        PacMazeSkinId.FOOD_QINGTING_WALK,
        PacMazeSkinId.FOOD_MOSQUITO_WALK,
        PacMazeSkinId.FOOD_TOUSHI_WALK,
    )

    private val skinSet = skinIds.toSet()

    fun contains(skinId: PacMazeSkinId): Boolean = skinId in skinSet
}
