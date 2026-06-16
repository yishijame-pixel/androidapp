package com.example.funlife.ui.screens.pacmaze.cosmetic

data class PacMazeAvatarLoadout(
    val skinId: PacMazeSkinId = PacMazeSkinId.CLASSIC_PAC,
    val trailId: PacMazeTrailId = PacMazeTrailId.NONE,
) {
    fun withRecommendedTrail(): PacMazeAvatarLoadout =
        copy(trailId = PacMazeCosmeticCatalog.recommendedTrail(skinId))
}
