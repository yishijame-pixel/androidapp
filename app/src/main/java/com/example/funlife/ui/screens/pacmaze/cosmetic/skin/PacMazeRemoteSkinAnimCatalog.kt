package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.resource.ResourceStore
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

enum class PacMazeSkinAnimClip(val folder: String, val prefix: String) {
    IDLE("idle", "idle"),
    WALK("walk", "walk"),
    RUN("run", "run"),
    JUMP("jump", "jump"),
    ATTACK("attack", "attack"),
    DIE("die", "die"),
}

internal data class PacMazeRemoteSkinAnimConfig(
    val skinId: PacMazeSkinId,
    val assetRoot: String,
    val clips: Set<PacMazeSkinAnimClip>,
    /** 1 = 原图分辨率；曾用 8 缩小以省内存 */
    val sampleSize: Int = 1,
    /** 原图默认朝右（与 walk_1 朝左的梗图相反），绘制时对 facing 取反 */
    val invertBitmapFacing: Boolean = false,
    /** 61 帧 walk：整圈时长对齐 4 帧梗图（~0.67s），避免局内爬行过慢 */
    val syncWalkCycleToSprite: Boolean = false,
    /** 吃豆人局内不播放的 clip（横版等平台仍可用 manifest 全量 clips） */
    val pacMazeExcludedClips: Set<PacMazeSkinAnimClip> = emptySet(),
) {
    fun primaryClip(): PacMazeSkinAnimClip = when {
        PacMazeSkinAnimClip.WALK in clips -> PacMazeSkinAnimClip.WALK
        PacMazeSkinAnimClip.IDLE in clips -> PacMazeSkinAnimClip.IDLE
        else -> clips.first()
    }
}

internal object PacMazeRemoteSkinAnimCatalog {

    fun previewAssetPath(skinId: PacMazeSkinId): String? = resolvePreviewAssetPath(skinId)

    /** preview.png 优先；旧包无封面时回退 walk_1 / idle_1 */
    fun resolvePreviewAssetPath(skinId: PacMazeSkinId): String? {
        val config = config(skinId) ?: return null
        val candidates = listOf(
            "${config.assetRoot}/preview.png",
            "${config.assetRoot}/walk/walk_1.png",
            "${config.assetRoot}/idle/idle_1.png",
        )
        return candidates.firstOrNull { ResourceStore.resourceExists(it) }
            ?: "${config.assetRoot}/walk/walk_1.png"
    }

    private val configs = mapOf(
        PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX,
            assetRoot = "pac_maze_skins/food_chick_walker_pro_max",
            clips = PacMazeSkinAnimClip.entries.toSet(),
            syncWalkCycleToSprite = true,
            // idle 未归一化（993×928 格）→ 局内定标极小；静止用 walk 首帧
            pacMazeExcludedClips = setOf(PacMazeSkinAnimClip.IDLE),
        ),
        PacMazeSkinId.FOOD_XIA_WALK to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_XIA_WALK,
            assetRoot = "pac_maze_skins/xia_walk",
            clips = setOf(PacMazeSkinAnimClip.WALK),
            syncWalkCycleToSprite = true,
        ),
        PacMazeSkinId.FOOD_MOUSE_WALK to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_MOUSE_WALK,
            assetRoot = "pac_maze_skins/laoshu_walk",
            clips = setOf(PacMazeSkinAnimClip.WALK),
            syncWalkCycleToSprite = true,
        ),
        PacMazeSkinId.FOOD_QINGTING_WALK to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_QINGTING_WALK,
            assetRoot = "pac_maze_skins/qinting_walk",
            clips = setOf(PacMazeSkinAnimClip.WALK),
        ),
        PacMazeSkinId.FOOD_MOSQUITO_WALK to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_MOSQUITO_WALK,
            assetRoot = "pac_maze_skins/wenzi_walk",
            clips = setOf(PacMazeSkinAnimClip.WALK),
            syncWalkCycleToSprite = true,
        ),
        PacMazeSkinId.FOOD_TOUSHI_WALK to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_TOUSHI_WALK,
            assetRoot = "pac_maze_skins/toushi_walk",
            clips = setOf(PacMazeSkinAnimClip.WALK),
            syncWalkCycleToSprite = true,
        ),
        PacMazeSkinId.FOOD_ZOMBIE_WALK to PacMazeRemoteSkinAnimConfig(
            skinId = PacMazeSkinId.FOOD_ZOMBIE_WALK,
            assetRoot = "pac_maze_skins/zombie_walk",
            clips = setOf(PacMazeSkinAnimClip.WALK),
            syncWalkCycleToSprite = true,
        ),
        PacMazeSkinId.YISHI_FIRE_LONG to walkOnly(PacMazeSkinId.YISHI_FIRE_LONG, "fire_long_walk"),
        PacMazeSkinId.YISHI_GREEN_LONG to walkOnly(PacMazeSkinId.YISHI_GREEN_LONG, "green_long_walk"),
        PacMazeSkinId.YISHI_HAIMIAN to walkOnly(PacMazeSkinId.YISHI_HAIMIAN, "haimian_walk"),
        PacMazeSkinId.YISHI_ICE_LONG to walkOnly(PacMazeSkinId.YISHI_ICE_LONG, "ice_long_walk"),
        PacMazeSkinId.YISHI_LONG to walkOnly(PacMazeSkinId.YISHI_LONG, "long_walk"),
        PacMazeSkinId.YISHI_MAGIC_DOG to walkOnly(PacMazeSkinId.YISHI_MAGIC_DOG, "magic_dog_walk"),
        PacMazeSkinId.YISHI_PAIDAXIN to walkOnly(PacMazeSkinId.YISHI_PAIDAXIN, "paidaxin_walk"),
        PacMazeSkinId.YISHI_QISHI_DOG to walkOnly(PacMazeSkinId.YISHI_QISHI_DOG, "qishi_dog_walk"),
        PacMazeSkinId.YISHI_BL_LONG to walkOnly(PacMazeSkinId.YISHI_BL_LONG, "bl_long_walk"),
    )

    private fun walkOnly(
        skinId: PacMazeSkinId,
        folder: String,
        invertFacing: Boolean = false,
    ) = PacMazeRemoteSkinAnimConfig(
        skinId = skinId,
        assetRoot = "pac_maze_skins/$folder",
        clips = setOf(PacMazeSkinAnimClip.WALK),
        invertBitmapFacing = invertFacing,
        syncWalkCycleToSprite = true,
    )

    fun config(skinId: PacMazeSkinId): PacMazeRemoteSkinAnimConfig? {
        val base = configs[skinId] ?: return null
        val manifest = runCatching { PacMazeSkinAnimManifest.load(base.assetRoot) }.getOrNull() ?: return base
        val clipSet = manifest.clipSet().ifEmpty { base.clips }
        val render = manifest.render
        return base.copy(
            clips = clipSet,
            sampleSize = render?.sampleSize ?: base.sampleSize,
            invertBitmapFacing = render?.invertBitmapFacing ?: base.invertBitmapFacing,
            syncWalkCycleToSprite = render?.syncWalkCycleToSprite ?: base.syncWalkCycleToSprite,
        )
    }

    /** 吃豆人局内实际可用的 clip 集合（排除 [PacMazeRemoteSkinAnimConfig.pacMazeExcludedClips]）。 */
    fun pacMazeClips(skinId: PacMazeSkinId): Set<PacMazeSkinAnimClip> {
        val cfg = config(skinId) ?: return emptySet()
        return cfg.clips - cfg.pacMazeExcludedClips
    }

    internal fun baseConfig(skinId: PacMazeSkinId): PacMazeRemoteSkinAnimConfig? = configs[skinId]

    fun assetRoot(skinId: PacMazeSkinId): String? = configs[skinId]?.assetRoot

    fun drawFacing(skinId: PacMazeSkinId, facing: Direction): Direction =
        PacMazeSkinRenderProfileCatalog.resolveDrawFacing(skinId, facing)

    fun usesRemoteAnim(skinId: PacMazeSkinId): Boolean = skinId in configs

    val remoteSkinIds: Set<PacMazeSkinId> get() = configs.keys

    fun pickClip(skinId: PacMazeSkinId, pose: PacMazeCharacterPose): PacMazeSkinAnimClip {
        val clips = pacMazeClips(skinId).ifEmpty { return PacMazeSkinAnimClip.WALK }
        return when {
            PacMazeSkinAnimClip.ATTACK in clips && pose.attackCooldownTicksLeft > 0 ->
                PacMazeSkinAnimClip.ATTACK
            PacMazeSkinAnimClip.DIE in clips && pose.isDead ->
                PacMazeSkinAnimClip.DIE
            pose.airborne && PacMazeSkinAnimClip.JUMP in clips ->
                PacMazeSkinAnimClip.JUMP
            pose.speedBoostActive && pose.isMoving && PacMazeSkinAnimClip.RUN in clips ->
                PacMazeSkinAnimClip.RUN
            pose.isMoving && PacMazeSkinAnimClip.WALK in clips ->
                PacMazeSkinAnimClip.WALK
            PacMazeSkinAnimClip.IDLE in clips ->
                PacMazeSkinAnimClip.IDLE
            PacMazeSkinAnimClip.WALK in clips ->
                PacMazeSkinAnimClip.WALK
            else -> clips.first()
        }
    }

    fun frameIndex(
        skinId: PacMazeSkinId,
        pose: PacMazeCharacterPose,
        clip: PacMazeSkinAnimClip,
        frameCount: Int,
    ): Int {
        if (frameCount <= 0) return 0
        pose.spriteFrameOverride?.let { return it.mod(frameCount) }
        return when (clip) {
            PacMazeSkinAnimClip.ATTACK -> {
                val total = pose.attackCooldownTotal.coerceAtLeast(1)
                val elapsed = (total - pose.attackCooldownTicksLeft).coerceAtLeast(0)
                ((elapsed * frameCount) / total).coerceIn(0, frameCount - 1)
            }
            PacMazeSkinAnimClip.DIE -> {
                val perFrame = if (pose.walkPreview) 0.95f else 1.05f
                ((pose.animPhase / perFrame).toInt()).mod(frameCount)
            }
            PacMazeSkinAnimClip.IDLE -> {
                val perFrame = if (pose.walkPreview) 0.95f else 1.05f
                ((pose.animPhase / perFrame).toInt()).mod(frameCount)
            }
            PacMazeSkinAnimClip.WALK, PacMazeSkinAnimClip.RUN -> {
                val perFrame = PacMazeRemoteSkinAnimTiming.walkPhasePerFrame(
                    skinId = skinId,
                    walkPreview = pose.walkPreview,
                    frameCount = frameCount,
                )
                if (!pose.isMoving) return 0
                ((pose.animPhase / perFrame).toInt()).mod(frameCount)
            }
            PacMazeSkinAnimClip.JUMP -> {
                pose.spriteFrameOverride?.let { return it.mod(frameCount) }
                val perFrame = if (pose.walkPreview) 0.85f else 0.75f
                ((pose.animPhase / perFrame).toInt()).mod(frameCount)
            }
        }
    }
}

internal object PacMazeRemoteSkinAnimTiming {
    const val ATTACK_ANIM_TICKS = 61
    const val DIE_ANIM_TICKS = 61
    /** 与 [PacMazeSpriteWalkAnim] 局内 walk 一致 */
    private const val GAMEPLAY_WALK_PHASE_PER_FRAME = 0.80f
    /** 4 帧 walk 整圈 animPhase 量 → 61 帧同步用 */
    private const val SPRITE_WALK_CYCLE_PHASE = 4f * GAMEPLAY_WALK_PHASE_PER_FRAME

    fun walkPhasePerFrame(skinId: PacMazeSkinId, walkPreview: Boolean, frameCount: Int): Float {
        if (walkPreview) return 0.95f
        if (config(skinId)?.syncWalkCycleToSprite == true) {
            return SPRITE_WALK_CYCLE_PHASE / frameCount.coerceAtLeast(1)
        }
        return GAMEPLAY_WALK_PHASE_PER_FRAME
    }

    private fun config(skinId: PacMazeSkinId) = PacMazeRemoteSkinAnimCatalog.config(skinId)
}
