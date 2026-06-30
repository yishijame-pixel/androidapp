package com.example.funlife.ui.screens.pacmaze.cosmetic

/** ikun + 一十类行走位图角色，共享缩放、锚点与动作渲染规则。 */
object PacMazeBitmapWalkCatalog {

    val skinIds: List<PacMazeSkinId> =
        PacMazeIkunCatalog.skinIds + PacMazeYishiCatalog.skinIds

    private val skinSet = skinIds.toSet()

    fun contains(skinId: PacMazeSkinId): Boolean = skinId in skinSet
}
