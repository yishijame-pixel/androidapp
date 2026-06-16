package com.example.funlife.ui.screens.pacmaze.cosmetic

import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeMotion
import com.example.funlife.ui.screens.pacmaze.components.PacMazeEntityComfortScale
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MAX
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MIN
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimTiming
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRenderProfileCatalog
import kotlin.math.max
import kotlin.math.min

object PacMazeCosmeticCatalog {

    private val definitions: Map<PacMazeSkinId, PacMazeSkinDefinition> = mapOf(
        PacMazeSkinId.LINE_PUPPY to PacMazeSkinDefinition(
            id = PacMazeSkinId.LINE_PUPPY,
            styleFamily = SkinStyleFamily.LINE_ART,
            bodyTier = BodyTier.L,
            recommendedTrailId = PacMazeTrailId.RIBBON_FLOW,
        ),
        PacMazeSkinId.LINE_KITTY to PacMazeSkinDefinition(
            id = PacMazeSkinId.LINE_KITTY,
            styleFamily = SkinStyleFamily.LINE_ART,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_SAKURA,
        ),
        PacMazeSkinId.LINE_BUNNY to PacMazeSkinDefinition(
            id = PacMazeSkinId.LINE_BUNNY,
            styleFamily = SkinStyleFamily.LINE_ART,
            bodyTier = BodyTier.S,
            recommendedTrailId = PacMazeTrailId.RIBBON_SAKURA,
        ),
        PacMazeSkinId.LINE_PANDA to PacMazeSkinDefinition(
            id = PacMazeSkinId.LINE_PANDA,
            styleFamily = SkinStyleFamily.LINE_ART,
            bodyTier = BodyTier.L,
            recommendedTrailId = PacMazeTrailId.RIBBON_JADE,
        ),
        PacMazeSkinId.LINE_FOX to PacMazeSkinDefinition(
            id = PacMazeSkinId.LINE_FOX,
            styleFamily = SkinStyleFamily.LINE_ART,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_PHOENIX,
        ),
        PacMazeSkinId.LINE_BEAR to PacMazeSkinDefinition(
            id = PacMazeSkinId.LINE_BEAR,
            styleFamily = SkinStyleFamily.LINE_ART,
            bodyTier = BodyTier.XL,
            recommendedTrailId = PacMazeTrailId.RIBBON_AURORA,
        ),
        PacMazeSkinId.SEA_SHARK to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_SHARK,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.L,
            recommendedTrailId = PacMazeTrailId.RIBBON_FLOW,
        ),
        PacMazeSkinId.SEA_CLOWNFISH to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_CLOWNFISH,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.S,
            recommendedTrailId = PacMazeTrailId.RIBBON_AURORA,
        ),
        PacMazeSkinId.SEA_JELLYFISH to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_JELLYFISH,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_SOUL,
        ),
        PacMazeSkinId.SEA_OCTOPUS to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_OCTOPUS,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_SOUL,
        ),
        PacMazeSkinId.SEA_TURTLE to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_TURTLE,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.L,
            recommendedTrailId = PacMazeTrailId.RIBBON_JADE,
        ),
        PacMazeSkinId.SEA_MANTA to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_MANTA,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.XL,
            recommendedTrailId = PacMazeTrailId.RIBBON_AURORA,
        ),
        PacMazeSkinId.SEA_SEAHORSE to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_SEAHORSE,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.S,
            recommendedTrailId = PacMazeTrailId.RIBBON_SOUL,
        ),
        PacMazeSkinId.SEA_DOLPHIN to PacMazeSkinDefinition(
            id = PacMazeSkinId.SEA_DOLPHIN,
            styleFamily = SkinStyleFamily.OCEAN,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_FLOW,
        ),
        PacMazeSkinId.CLASSIC_PAC to PacMazeSkinDefinition(
            id = PacMazeSkinId.CLASSIC_PAC,
            styleFamily = SkinStyleFamily.RETRO,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_FLOW,
        ),
        PacMazeSkinId.SCHOLAR to PacMazeSkinDefinition(
            id = PacMazeSkinId.SCHOLAR,
            styleFamily = SkinStyleFamily.INK,
            bodyTier = BodyTier.S,
            recommendedTrailId = PacMazeTrailId.NONE,
        ),
        PacMazeSkinId.LANTERN_FOX to PacMazeSkinDefinition(
            id = PacMazeSkinId.LANTERN_FOX,
            styleFamily = SkinStyleFamily.INK,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.RIBBON_FLOW,
        ),
        PacMazeSkinId.CANDY_SPIRIT to PacMazeSkinDefinition(
            id = PacMazeSkinId.CANDY_SPIRIT,
            styleFamily = SkinStyleFamily.CHIBI,
            bodyTier = BodyTier.S,
            recommendedTrailId = PacMazeTrailId.RIBBON_FLOW,
        ),
        PacMazeSkinId.DATA_CORE to PacMazeSkinDefinition(
            id = PacMazeSkinId.DATA_CORE,
            styleFamily = SkinStyleFamily.CYBER,
            bodyTier = BodyTier.L,
            recommendedTrailId = PacMazeTrailId.ION_WAKE,
        ),
        PacMazeSkinId.BUBBLE_SLIME to PacMazeSkinDefinition(
            id = PacMazeSkinId.BUBBLE_SLIME,
            styleFamily = SkinStyleFamily.CHIBI,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.STAR_COMET,
        ),
        PacMazeSkinId.NOODLE_PHANTOM to PacMazeSkinDefinition(
            id = PacMazeSkinId.NOODLE_PHANTOM,
            styleFamily = SkinStyleFamily.FOOD,
            bodyTier = BodyTier.M,
            recommendedTrailId = PacMazeTrailId.GHOST_ECHO,
        ),
        PacMazeSkinId.GEAR_MOLE to PacMazeSkinDefinition(
            id = PacMazeSkinId.GEAR_MOLE,
            styleFamily = SkinStyleFamily.STEAM,
            bodyTier = BodyTier.L,
            recommendedTrailId = PacMazeTrailId.ION_WAKE,
        ),
        // —— 线条扩展 ——
        PacMazeSkinId.LINE_PENGUIN to skinDef(PacMazeSkinId.LINE_PENGUIN, SkinStyleFamily.LINE_ART, BodyTier.M, PacMazeTrailId.RIBBON_CELADON),
        PacMazeSkinId.LINE_OWL to skinDef(PacMazeSkinId.LINE_OWL, SkinStyleFamily.LINE_ART, BodyTier.M, PacMazeTrailId.RIBBON_NIGHT_INK),
        PacMazeSkinId.LINE_HEDGEHOG to skinDef(PacMazeSkinId.LINE_HEDGEHOG, SkinStyleFamily.LINE_ART, BodyTier.S, PacMazeTrailId.PAW_PRINT),
        PacMazeSkinId.LINE_SHIBA to skinDef(PacMazeSkinId.LINE_SHIBA, SkinStyleFamily.LINE_ART, BodyTier.M, PacMazeTrailId.RIBBON_CINNABAR),
        PacMazeSkinId.LINE_OTTER to skinDef(PacMazeSkinId.LINE_OTTER, SkinStyleFamily.LINE_ART, BodyTier.M, PacMazeTrailId.RIPPLE_STEP),
        PacMazeSkinId.LINE_KOALA to skinDef(PacMazeSkinId.LINE_KOALA, SkinStyleFamily.LINE_ART, BodyTier.L, PacMazeTrailId.RIBBON_GINKGO),
        // —— 深海扩展 ——
        PacMazeSkinId.SEA_SQUID to skinDef(PacMazeSkinId.SEA_SQUID, SkinStyleFamily.OCEAN, BodyTier.M, PacMazeTrailId.RIBBON_SOUL),
        PacMazeSkinId.SEA_ANGLER to skinDef(PacMazeSkinId.SEA_ANGLER, SkinStyleFamily.OCEAN, BodyTier.L, PacMazeTrailId.RIBBON_NIGHT_INK),
        PacMazeSkinId.SEA_HERMIT to skinDef(PacMazeSkinId.SEA_HERMIT, SkinStyleFamily.OCEAN, BodyTier.S, PacMazeTrailId.PAW_PRINT),
        PacMazeSkinId.SEA_STARFISH to skinDef(PacMazeSkinId.SEA_STARFISH, SkinStyleFamily.OCEAN, BodyTier.S, PacMazeTrailId.STAR_COMET),
        PacMazeSkinId.SEA_EEL to skinDef(PacMazeSkinId.SEA_EEL, SkinStyleFamily.OCEAN, BodyTier.M, PacMazeTrailId.RIBBON_VIOLET),
        PacMazeSkinId.SEA_SUNFISH to skinDef(PacMazeSkinId.SEA_SUNFISH, SkinStyleFamily.OCEAN, BodyTier.XL, PacMazeTrailId.RIBBON_AURORA),
        // —— 国风扩展 ——
        PacMazeSkinId.INK_DROP_SPIRIT to skinDef(PacMazeSkinId.INK_DROP_SPIRIT, SkinStyleFamily.INK, BodyTier.M, PacMazeTrailId.RIBBON_NIGHT_INK),
        PacMazeSkinId.INK_PAPER_BIRD to skinDef(PacMazeSkinId.INK_PAPER_BIRD, SkinStyleFamily.INK, BodyTier.S, PacMazeTrailId.PETAL_SHOWER),
        PacMazeSkinId.INK_LION_DANCE to skinDef(PacMazeSkinId.INK_LION_DANCE, SkinStyleFamily.INK, BodyTier.L, PacMazeTrailId.RIBBON_CINNABAR),
        PacMazeSkinId.INK_PORCELAIN to skinDef(PacMazeSkinId.INK_PORCELAIN, SkinStyleFamily.INK, BodyTier.M, PacMazeTrailId.RIBBON_CELADON),
        PacMazeSkinId.INK_KYLIN to skinDef(PacMazeSkinId.INK_KYLIN, SkinStyleFamily.INK, BodyTier.L, PacMazeTrailId.RIBBON_GINKGO),
        PacMazeSkinId.INK_FAN_FAIRY to skinDef(PacMazeSkinId.INK_FAN_FAIRY, SkinStyleFamily.INK, BodyTier.S, PacMazeTrailId.PETAL_SHOWER),
        PacMazeSkinId.INK_LOTUS_BUD to skinDef(PacMazeSkinId.INK_LOTUS_BUD, SkinStyleFamily.INK, BodyTier.M, PacMazeTrailId.RIBBON_JADE),
        PacMazeSkinId.INK_SHADOW_PUPPET to skinDef(PacMazeSkinId.INK_SHADOW_PUPPET, SkinStyleFamily.INK, BodyTier.M, PacMazeTrailId.RIBBON_NIGHT_INK),
        // —— 赛博扩展 ——
        PacMazeSkinId.CYBER_HOLO_CAT to skinDef(PacMazeSkinId.CYBER_HOLO_CAT, SkinStyleFamily.CYBER, BodyTier.M, PacMazeTrailId.DATA_CASCADE),
        PacMazeSkinId.CYBER_GLITCH_CUBE to skinDef(PacMazeSkinId.CYBER_GLITCH_CUBE, SkinStyleFamily.CYBER, BodyTier.M, PacMazeTrailId.CUBE_SHATTER),
        PacMazeSkinId.CYBER_MAGLEV_ORB to skinDef(PacMazeSkinId.CYBER_MAGLEV_ORB, SkinStyleFamily.CYBER, BodyTier.L, PacMazeTrailId.RADAR_SWEEP),
        PacMazeSkinId.CYBER_WIRE_WORM to skinDef(PacMazeSkinId.CYBER_WIRE_WORM, SkinStyleFamily.CYBER, BodyTier.S, PacMazeTrailId.ION_WAKE),
        PacMazeSkinId.CYBER_DRONE_BEE to skinDef(PacMazeSkinId.CYBER_DRONE_BEE, SkinStyleFamily.CYBER, BodyTier.S, PacMazeTrailId.RADAR_SWEEP),
        PacMazeSkinId.CYBER_NEON_SNAKE to skinDef(PacMazeSkinId.CYBER_NEON_SNAKE, SkinStyleFamily.CYBER, BodyTier.M, PacMazeTrailId.ION_WAKE),
        PacMazeSkinId.CYBER_CHIP_MONKEY to skinDef(PacMazeSkinId.CYBER_CHIP_MONKEY, SkinStyleFamily.CYBER, BodyTier.M, PacMazeTrailId.DATA_CASCADE),
        PacMazeSkinId.CYBER_LASER_BEETLE to skinDef(PacMazeSkinId.CYBER_LASER_BEETLE, SkinStyleFamily.CYBER, BodyTier.S, PacMazeTrailId.HEX_HONEY),
        // —— 怪趣零食扩展 ——
        PacMazeSkinId.FOOD_MOCHI to skinDef(PacMazeSkinId.FOOD_MOCHI, SkinStyleFamily.FOOD, BodyTier.S, PacMazeTrailId.RIBBON_MINT_BUBBLE),
        PacMazeSkinId.FOOD_CHILI to skinDef(PacMazeSkinId.FOOD_CHILI, SkinStyleFamily.FOOD, BodyTier.S, PacMazeTrailId.RIBBON_CINNABAR),
        PacMazeSkinId.FOOD_SUSHI to skinDef(PacMazeSkinId.FOOD_SUSHI, SkinStyleFamily.FOOD, BodyTier.M, PacMazeTrailId.RIPPLE_STEP),
        PacMazeSkinId.FOOD_POPCORN to skinDef(PacMazeSkinId.FOOD_POPCORN, SkinStyleFamily.FOOD, BodyTier.M, PacMazeTrailId.CANDY_CRUMB),
        PacMazeSkinId.FOOD_TANGYUAN to skinDef(PacMazeSkinId.FOOD_TANGYUAN, SkinStyleFamily.FOOD, BodyTier.S, PacMazeTrailId.RIBBON_MINT_BUBBLE),
        PacMazeSkinId.FOOD_DUMPLING to skinDef(PacMazeSkinId.FOOD_DUMPLING, SkinStyleFamily.FOOD, BodyTier.M, PacMazeTrailId.RIPPLE_STEP),
        PacMazeSkinId.FOOD_MANGO_PUDDING to skinDef(PacMazeSkinId.FOOD_MANGO_PUDDING, SkinStyleFamily.FOOD, BodyTier.M, PacMazeTrailId.CANDY_CRUMB),
        PacMazeSkinId.FOOD_DONUT to skinDef(PacMazeSkinId.FOOD_DONUT, SkinStyleFamily.FOOD, BodyTier.L, PacMazeTrailId.RIBBON_PHOENIX),
        PacMazeSkinId.FOOD_CHICK_DAZE to skinDef(PacMazeSkinId.FOOD_CHICK_DAZE, SkinStyleFamily.IKUN, BodyTier.M, PacMazeTrailId.CANDY_CRUMB),
        PacMazeSkinId.FOOD_CHICK_BALLER to skinDef(PacMazeSkinId.FOOD_CHICK_BALLER, SkinStyleFamily.IKUN, BodyTier.L, PacMazeTrailId.RIBBON_PHOENIX),
        PacMazeSkinId.FOOD_CHICK_WALKER to skinDef(PacMazeSkinId.FOOD_CHICK_WALKER, SkinStyleFamily.IKUN, BodyTier.M, PacMazeTrailId.PAW_PRINT),
        PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX to skinDef(PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX, SkinStyleFamily.IKUN, BodyTier.L, PacMazeTrailId.RIBBON_PHOENIX),
        PacMazeSkinId.FOOD_XIA_WALK to skinDef(PacMazeSkinId.FOOD_XIA_WALK, SkinStyleFamily.IKUN, BodyTier.M, PacMazeTrailId.PAW_PRINT),
        PacMazeSkinId.FOOD_MOUSE_WALK to skinDef(PacMazeSkinId.FOOD_MOUSE_WALK, SkinStyleFamily.IKUN, BodyTier.S, PacMazeTrailId.RIPPLE_STEP),
        PacMazeSkinId.FOOD_QINGTING_WALK to skinDef(PacMazeSkinId.FOOD_QINGTING_WALK, SkinStyleFamily.IKUN, BodyTier.M, PacMazeTrailId.RIBBON_MINT_BUBBLE),
        PacMazeSkinId.FOOD_MOSQUITO_WALK to skinDef(PacMazeSkinId.FOOD_MOSQUITO_WALK, SkinStyleFamily.IKUN, BodyTier.S, PacMazeTrailId.CANDY_CRUMB),
        PacMazeSkinId.FOOD_TOUSHI_WALK to skinDef(PacMazeSkinId.FOOD_TOUSHI_WALK, SkinStyleFamily.IKUN, BodyTier.M, PacMazeTrailId.RIPPLE_STEP),
    )

    private fun skinDef(id: PacMazeSkinId, family: SkinStyleFamily, tier: BodyTier, trail: PacMazeTrailId) =
        PacMazeSkinDefinition(id = id, styleFamily = family, bodyTier = tier, recommendedTrailId = trail)

    fun definition(skinId: PacMazeSkinId): PacMazeSkinDefinition =
        definitions.getValue(skinId)

    fun recommendedTrail(skinId: PacMazeSkinId): PacMazeTrailId =
        definition(skinId).recommendedTrailId

    fun bodyTier(skinId: PacMazeSkinId): BodyTier = definition(skinId).bodyTier

    /** 逻辑移速倍率：体型越大越慢。位图资源皮肤不受 HUD 缩放影响（仅视觉变大）。 */
    fun speedMultiplier(loadout: PacMazeAvatarLoadout, userDrawScale: Float): Float {
        val tier = bodyTier(loadout.skinId).speedMul
        if (PacMazeSkinRenderProfileCatalog.isBitmapResource(loadout.skinId)) {
            return tier.coerceIn(0.72f, 1.12f)
        }
        val clampedScale = userDrawScale.coerceIn(PAC_MAZE_PLAYER_SCALE_MIN, PAC_MAZE_PLAYER_SCALE_MAX)
        val scalePenalty = if (clampedScale > 1f) {
            1f - (clampedScale - 1f) * 0.12f
        } else {
            1f + (1f - clampedScale) * 0.04f
        }
        return (tier * scalePenalty).coerceIn(0.72f, 1.12f)
    }

    /**
     * 位图皮肤通行碰撞半径（格）：与 HUD 视觉等比，钳在 [BODY_RADIUS, MAX] 内。
     * 下限对齐逻辑体，避免移动/站立半径不一致导致每帧被 clamp 拉回；上限保证仍能在走廊内移动。
     * [canPassCorridor] 用未钳制的原始值判断「视觉是否过大」。
     */
    fun corridorPassRadiusRaw(
        loadout: PacMazeAvatarLoadout,
        userDrawScale: Float,
    ): Float {
        if (!PacMazeSkinRenderProfileCatalog.isBitmapResource(loadout.skinId)) {
            return PacMazeMotion.BODY_RADIUS
        }
        return PacMazeIkunGameplayScale.corridorHalfWidthCells() *
            PacMazeIkunGameplayScale.hudVisualScale(userDrawScale)
    }

    fun gameplayPassRadius(
        loadout: PacMazeAvatarLoadout,
        userDrawScale: Float,
        @Suppress("UNUSED_PARAMETER") entityDrawBoost: Float = 1f,
    ): Float {
        val raw = corridorPassRadiusRaw(loadout, userDrawScale)
        return raw.coerceIn(PacMazeMotion.BODY_RADIUS, PacMazeMotion.MAX_CORRIDOR_BODY_RADIUS)
    }

    fun gameplayBodyRadius(
        loadout: PacMazeAvatarLoadout,
        userDrawScale: Float,
        entityDrawBoost: Float = 1f,
    ): Float = gameplayPassRadius(loadout, userDrawScale, entityDrawBoost)

    fun canPassCorridor(
        loadout: PacMazeAvatarLoadout,
        userDrawScale: Float,
        entityDrawBoost: Float = 1f,
    ): Boolean =
        corridorPassRadiusRaw(loadout, userDrawScale) <= PacMazeMotion.MAX_CORRIDOR_BODY_RADIUS

    /** 屏幕绘制半径：跟用户滑条线性变化。 */
    fun visualRadius(
        cell: Float,
        loadout: PacMazeAvatarLoadout,
        userDrawScale: Float,
        entityDrawBoost: Float = 1f,
        minRadiusPx: Float = 0f,
    ): Float {
        val tier = bodyTier(loadout.skinId).scaleMul
        val clamped = userDrawScale.coerceIn(PAC_MAZE_PLAYER_SCALE_MIN, PAC_MAZE_PLAYER_SCALE_MAX)
        val userTuned = kotlin.math.abs(clamped - 1f) > 0.04f
        val boost = if (userTuned) 1f else entityDrawBoost
        val scaled = PacMazeEntityComfortScale.resolvePlayerRadius(
            entityCell = cell,
            tierScale = tier,
            userScale = clamped,
            boost = boost,
            minRadiusPx = minRadiusPx,
        )
        return if (minRadiusPx > 0f) max(scaled, minRadiusPx) else scaled
    }

    fun shouldRenderTrail(loadout: PacMazeAvatarLoadout, @Suppress("UNUSED_PARAMETER") forceCyber: Boolean): Boolean =
        loadout.trailId != PacMazeTrailId.NONE

    /** Pro Max 等皮肤开局额外攻击次数。 */
    fun startingAttackCharges(skinId: PacMazeSkinId): Int = when (skinId) {
        PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX -> 1
        else -> 0
    }

    /** 皮肤专属攻击动画时长（tick）。 */
    fun attackCooldownTicks(skinId: PacMazeSkinId): Int = when (skinId) {
        PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX -> PacMazeRemoteSkinAnimTiming.ATTACK_ANIM_TICKS
        else -> PacMazeConstants.ATTACK_COOLDOWN_TICKS
    }
}
