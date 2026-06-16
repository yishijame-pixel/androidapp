package com.example.funlife.ui.screens.pacmaze.character

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRenderProfileCatalog

/** 选角/工坊预览的动画节奏（与局内 tick 驱动的 animPhase 分开调） */
internal object PacMazeCharacterPreviewAnim {

    private val bitmapLeftFacing = setOf(
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

    fun usesSpriteWalk(skinId: PacMazeSkinId): Boolean = skinId == PacMazeSkinId.FOOD_CHICK_WALKER

    fun usesRemoteAnim(skinId: PacMazeSkinId): Boolean =
        PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)

    fun usesLeftPreviewFacing(skinId: PacMazeSkinId): Boolean = skinId in bitmapLeftFacing

    /** 预览区每帧停留毫秒（4 帧 ≈ 1.8s 一圈） */
    const val SPRITE_FRAME_HOLD_MS = 450L

    fun effectiveAnimateWalk(skinId: PacMazeSkinId, animateWalk: Boolean): Boolean =
        animateWalk || usesSpriteWalk(skinId)

    fun previewFacing(skinId: PacMazeSkinId): Direction =
        if (PacMazeSkinRenderProfileCatalog.isBitmapResource(skinId)) {
            PacMazeSkinRenderProfileCatalog.naturalIdleFacing(skinId)
        } else {
            Direction.RIGHT
        }

    fun walkPhaseStep(skinId: PacMazeSkinId, animateWalk: Boolean): Float = when {
        !animateWalk -> if (skinId.isOcean()) 0.035f else 0.038f
        usesRemoteAnim(skinId) -> 0.028f
        usesSpriteWalk(skinId) -> 0.032f
        skinId.isOcean() -> 0.07f
        else -> 0.09f
    }

    /** 拖尾采样轨迹：序列帧角色放慢横向摆动，避免「飞跑」感 */
    fun trailTimeStep(skinId: PacMazeSkinId): Float = when {
        usesRemoteAnim(skinId) -> 0.026f
        usesSpriteWalk(skinId) -> 0.028f
        skinId.isOcean() -> 0.04f
        else -> 0.05f
    }

    fun trailOrbitScale(skinId: PacMazeSkinId): Float = when {
        usesRemoteAnim(skinId) -> 0.52f
        usesSpriteWalk(skinId) -> 0.55f
        else -> 1f
    }

    /** ikun / 位图梗图在预览区略放大（局内尺寸由 [PacMazeIkunGameplayScale] 单独控制） */
    fun previewDrawRadiusMul(skinId: PacMazeSkinId, gridLite: Boolean = false): Float = when {
        gridLite && PacMazeIkunCatalog.contains(skinId) -> 0.92f
        gridLite && usesRemoteAnim(skinId) -> 0.88f
        PacMazeIkunCatalog.contains(skinId) -> 1.38f
        skinId in bitmapLeftFacing -> 1.35f
        usesRemoteAnim(skinId) -> 1.28f
        else -> 1f
    }

    fun previewBaseRadiusMul(skinId: PacMazeSkinId, gridLite: Boolean): Float = when {
        gridLite && PacMazeIkunCatalog.contains(skinId) -> 0.36f
        gridLite && usesRemoteAnim(skinId) -> 0.34f
        PacMazeIkunCatalog.contains(skinId) -> 0.54f
        else -> 0.4f
    }

    fun previewCanvasFill(skinId: PacMazeSkinId, gridLite: Boolean): Float = when {
        gridLite && PacMazeIkunCatalog.contains(skinId) -> 0.78f
        gridLite -> 0.82f
        PacMazeIkunCatalog.contains(skinId) -> 0.98f
        else -> 0.9f
    }

    fun previewCenterYFrac(skinId: PacMazeSkinId, gridLite: Boolean): Float = when {
        gridLite && PacMazeIkunCatalog.contains(skinId) -> 0.62f
        PacMazeIkunCatalog.contains(skinId) -> 0.54f
        else -> 0.5f
    }
}
