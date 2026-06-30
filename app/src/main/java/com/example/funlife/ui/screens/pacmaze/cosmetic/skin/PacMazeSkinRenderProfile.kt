package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeBitmapWalkCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

/**
 * 位图皮肤渲染元数据：四向朝向、默认朝向、walk 同步等。
 * 矢量皮肤（Line/Sea/Family）不走此路径。
 */
internal data class PacMazeSkinRenderProfile(
    val skinId: PacMazeSkinId,
    /** 原图未镜像时是否朝右（云端梗图默认 true；walk_1 本地系 false） */
    val defaultFacesRight: Boolean,
    /** null = 按 [defaultFacesRight] 自动推导（朝左：上 +90 / 下 -90） */
    val upRotationDeg: Float? = null,
    val downRotationDeg: Float? = null,
    /** 61 帧 walk 关闭额外 sin bob，避免双弹跳 */
    val disableExtraWalkBob: Boolean = false,
)

internal object PacMazeSkinRenderProfileCatalog {

    private val ikunProfiles: Map<PacMazeSkinId, PacMazeSkinRenderProfile> =
        PacMazeBitmapWalkCatalog.skinIds.associateWith { skinId ->
            val remoteCfg = PacMazeRemoteSkinAnimCatalog.config(skinId)
            val syncWalk = remoteCfg?.syncWalkCycleToSprite == true
            // 原图默认朝向：walk_1 朝左；老鼠/蚊子等云端包朝右（invertBitmapFacing）
            val facesRight = remoteCfg?.invertBitmapFacing == true
            PacMazeSkinRenderProfile(
                skinId = skinId,
                defaultFacesRight = facesRight,
                disableExtraWalkBob = syncWalk,
            )
        }

    private val assetBitmapProfiles: Map<PacMazeSkinId, PacMazeSkinRenderProfile> = mapOf(
        PacMazeSkinId.FOOD_CHICK_WALKER to PacMazeSkinRenderProfile(
            skinId = PacMazeSkinId.FOOD_CHICK_WALKER,
            defaultFacesRight = false,
            disableExtraWalkBob = true,
        ),
        PacMazeSkinId.FOOD_CHICK_BALLER to PacMazeSkinRenderProfile(
            skinId = PacMazeSkinId.FOOD_CHICK_BALLER,
            defaultFacesRight = false,
        ),
        PacMazeSkinId.FOOD_CHICK_DAZE to PacMazeSkinRenderProfile(
            skinId = PacMazeSkinId.FOOD_CHICK_DAZE,
            defaultFacesRight = false,
        ),
    )

    /** 本地 PNG 图片皮肤（非 ikun 云端行走），与矢量手绘分离渲染。 */
    fun isAssetBitmap(skinId: PacMazeSkinId): Boolean = skinId in assetBitmapProfiles

    fun profile(skinId: PacMazeSkinId): PacMazeSkinRenderProfile? =
        ikunProfiles[skinId] ?: assetBitmapProfiles[skinId]

    fun isBitmapIkun(skinId: PacMazeSkinId): Boolean = skinId in ikunProfiles

    fun defaultFacesRight(skinId: PacMazeSkinId?): Boolean {
        if (skinId == null) return false
        return profile(skinId)?.defaultFacesRight ?: false
    }

    /** 本地 PNG 或 ikun 位图（有 render profile），与矢量手绘分离。 */
    fun isBitmapResource(skinId: PacMazeSkinId): Boolean = profile(skinId) != null

    /** 原图自然朝向（未移动、未镜像时的展示方向）。 */
    fun naturalIdleFacing(skinId: PacMazeSkinId): Direction =
        if (defaultFacesRight(skinId)) Direction.RIGHT else Direction.LEFT

    /**
     * 位图局内四向：移动跟实际方向；停止后保留最后朝向；开局用原图自然朝向。
     */
    fun resolveBitmapDrawFacing(skinId: PacMazeSkinId, entity: PacMazeEntity): Direction =
        PacMazeBitmapFacingState.resolve(skinId, entity)

    /** @deprecated 使用 [resolveBitmapDrawFacing] */
    fun resolveIkunDrawFacing(skinId: PacMazeSkinId, entity: PacMazeEntity): Direction =
        resolveBitmapDrawFacing(skinId, entity)

    fun resolveDrawFacing(skinId: PacMazeSkinId, facing: Direction): Direction = facing

    fun shouldDisableExtraWalkBob(skinId: PacMazeSkinId): Boolean =
        profile(skinId)?.disableExtraWalkBob == true
}
