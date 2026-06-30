package com.example.funlife.game.platformer

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.game.platformer.catalog.PlatformerContentCatalog
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.PlatformerSkinRenderer
import com.example.funlife.game.platformer.catalog.catalogId
import com.example.funlife.ui.screens.platformer.GameResourceLoadCopy
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import kotlin.math.abs

enum class PlatformerCharacterId(val title: String, val subtitle: String) {
    CHICK_PRO_MAX("行走小鸡 Pro Max", "官方角色 · 默认"),
    TREASURE_HUNTER("宝藏猎人", "本地像素角色"),
    PIXEL_WALKER("像素行者", "本地资源包"),
    TEMPLE_RUNNER("神庙跑者", "跑酷 · 滑铲"),
    ADVENTURE_GIRL("冒险少女", "近战 · 射击"),
    NINJA_GIRL("忍者少女", "攀爬 · 滑翔"),
    NINJA_BOY("忍者少年", "攀爬 · 滑翔"),
    JACK("杰克", "滑铲 · 跑酷"),
    RED_HAT("小红帽", "受击反馈"),
    ROBOT("战斗机器人", "射击 · 近战"),
    DINO("小恐龙", "重型跳跃"),
    KNIGHT("圣骑士", "攻击 · 跳攻"),
    SANTA("圣诞老人", "节日限定"),
    CAT("猫咪", "宠物系"),
    DOG("狗狗", "宠物系"),
    SUPERTUX_TUX("Tux", "SuperTux 企鹅 · 南极章"),
    ;

    val isCatalogRemote: Boolean
        get() = this !in setOf(CHICK_PRO_MAX, TREASURE_HUNTER, PIXEL_WALKER, SUPERTUX_TUX)

    val displayTitle: String
        get() = PlatformerContentCatalog.characterForEnum(this)?.title ?: title

    val displaySubtitle: String
        get() = GameResourceLoadCopy.forDisplay(
            PlatformerContentCatalog.characterForEnum(this)?.subtitle ?: subtitle,
        )
}

data class PlatformerLocalCharacterAnim(
    val walk: SpriteStrip?,
    val idle: SpriteStrip?,
    val jump: ImageBitmap?,
)

class PlatformerCharacterAssets(
    val local: Map<PlatformerCharacterId, PlatformerLocalCharacterAnim>,
)

object PlatformerCharacterAssetsLoader {

    fun load(context: Context): PlatformerCharacterAssets {
        val thRoot = "platformer/characters/treasure_hunter/player"
        val walkPaths = (1..6).map { "$thRoot/run/run$it.png" }
        val idlePaths = (1..3).map { "$thRoot/idle/idle$it.png" }
        val treasure = PlatformerLocalCharacterAnim(
            walk = PlatformerSpriteSheet.loadSequence(context, walkPaths),
            idle = PlatformerSpriteSheet.loadSequence(context, idlePaths),
            jump = PlatformerSpriteSheet.loadSequence(context, listOf("$thRoot/jump.png"))?.frames?.firstOrNull(),
        )
        val pixelWalkPaths = (1..10).map { "platformer/player/Walk ($it).png" }
        val pixelWalker = PlatformerLocalCharacterAnim(
            walk = PlatformerSpriteSheet.loadSequence(context, pixelWalkPaths),
            idle = PlatformerSpriteSheet.loadSequence(context, listOf(pixelWalkPaths.first())),
            jump = PlatformerSpriteSheet.loadSequence(context, listOf(pixelWalkPaths[4]))?.frames?.firstOrNull(),
        )
        val tuxRoot = "platformer_supertux/characters/tux"
        val tuxWalk = (0..5).mapNotNull { i ->
            loadBundleBitmap(context, "$tuxRoot/walk/walk-$i.png")
        }
        val tuxIdle = (0..5).mapNotNull { i ->
            loadBundleBitmap(context, "$tuxRoot/stand/stand-$i.png")
        }
        val tux = if (tuxWalk.isNotEmpty()) {
            PlatformerLocalCharacterAnim(
                walk = PlatformerSpriteSheet.fromBitmaps(tuxWalk),
                idle = PlatformerSpriteSheet.fromBitmaps(tuxIdle.ifEmpty { tuxWalk }),
                jump = loadBundleBitmap(context, "$tuxRoot/jump/jump-0.png"),
            )
        } else {
            null
        }

        return PlatformerCharacterAssets(
            mapOf(
                PlatformerCharacterId.TREASURE_HUNTER to treasure,
                PlatformerCharacterId.PIXEL_WALKER to pixelWalker,
            ) + (tux?.let { PlatformerCharacterId.SUPERTUX_TUX to it }.let { if (it != null) mapOf(it) else emptyMap() }),
        )
    }

    private fun loadBundleBitmap(context: Context, path: String): ImageBitmap? {
        com.example.funlife.resource.ResourceStore.openInputStream(path)?.use { stream ->
            return runCatching {
                android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }.getOrNull()
        }
        return runCatching {
            context.assets.open(path).use {
                android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

object PlatformerCharacterPrefs {
    private const val PREFS = "platformer_prefs"
    private const val KEY_CHAR = "character_id"

    fun get(context: Context): PlatformerCharacterId {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CHAR, PlatformerCharacterId.CHICK_PRO_MAX.name)
        return runCatching { PlatformerCharacterId.valueOf(name!!) }
            .getOrDefault(PlatformerCharacterId.CHICK_PRO_MAX)
    }

    fun set(context: Context, id: PlatformerCharacterId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHAR, id.name)
            .apply()
    }
}

object PlatformerCharacterRenderer {

    fun resolveFrame(
        characterId: PlatformerCharacterId,
        player: PlatformerPlayer,
        animTime: Float,
        characterAssets: PlatformerCharacterAssets?,
    ): ImageBitmap? = when {
        characterId == PlatformerCharacterId.CHICK_PRO_MAX ->
            PlatformerPlayerSprites.resolveChickFrame(player, animTime)
        characterId.isCatalogRemote ->
            PlatformerRemoteAnimCache.resolveFrame(characterId, player, animTime)
        else -> resolveLocalFrame(characterId, player, animTime, characterAssets)
    }

    private fun resolveLocalFrame(
        id: PlatformerCharacterId,
        player: PlatformerPlayer,
        animTime: Float,
        assets: PlatformerCharacterAssets?,
    ): ImageBitmap? {
        val anim = assets?.local?.get(id) ?: return null
        return when {
            !player.grounded && anim.jump != null -> anim.jump
            player.locomoting -> {
                val strip = anim.walk ?: return anim.idle?.frames?.firstOrNull()
                val idx = (animTime * PlatformerPlayerSprites.WALK_FRAME_RATE).toInt() % strip.frames.size
                strip.frames[idx]
            }
            else -> {
                val strip = anim.idle ?: anim.walk ?: return null
                val idx = (animTime / 0.35f).toInt() % strip.frames.size
                strip.frames[idx]
            }
        }
    }

    fun mirrorHorizontally(characterId: PlatformerCharacterId, facingRight: Boolean): Boolean =
        PlatformerSkinRenderer.mirrorHorizontally(characterId, facingRight)

    suspend fun warmup(characterId: PlatformerCharacterId) {
        when {
            characterId == PlatformerCharacterId.CHICK_PRO_MAX -> {
                if (PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(PlatformerPlayerSprites.skinId)) {
                    PlatformerPlayerSprites.warmupDieSheetOnly()
                } else {
                    PlatformerPlayerSprites.warmup()
                }
            }
            characterId.isCatalogRemote -> PlatformerRemoteAnimCache.warmup(characterId)
        }
    }

    fun isReady(characterId: PlatformerCharacterId): Boolean = when {
        characterId == PlatformerCharacterId.CHICK_PRO_MAX -> PlatformerPlayerSprites.isReady()
        characterId.isCatalogRemote -> PlatformerRemoteAnimCache.isReady(characterId)
        else -> true
    }

    fun isPlayableReady(characterId: PlatformerCharacterId): Boolean = when {
        characterId == PlatformerCharacterId.CHICK_PRO_MAX -> PlatformerPlayerSprites.isPlayableReady()
        characterId.isCatalogRemote -> PlatformerRemoteAnimCache.isPlayableReady(characterId)
        else -> true
    }

    /** 进局/启动门槛：bootstrap 帧即可，不等全量解码。 */
    fun isBootstrapPlayable(characterId: PlatformerCharacterId): Boolean = when {
        characterId == PlatformerCharacterId.CHICK_PRO_MAX -> PlatformerPlayerSprites.isBootstrapPlayable()
        characterId.isCatalogRemote -> PlatformerRemoteAnimCache.isBootstrapPlayable(characterId)
        else -> true
    }

    fun chickSkinId(): PacMazeSkinId = PlatformerPlayerSprites.skinId

    fun usesRemoteCatalog(characterId: PlatformerCharacterId): Boolean =
        characterId.isCatalogRemote
}
