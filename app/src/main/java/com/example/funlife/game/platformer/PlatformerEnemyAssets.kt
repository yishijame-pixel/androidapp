package com.example.funlife.game.platformer

import android.content.Context

data class PlatformerEnemyAnimSet(
    val idle: SpriteStrip? = null,
    val move: SpriteStrip? = null,
    val fly: SpriteStrip? = null,
)

class PlatformerEnemyAssets(
    val byType: Map<PlatformerEnemyType, PlatformerEnemyAnimSet>,
)

object PlatformerEnemyAssetsLoader {

    private const val ROOT = "platformer/enemies/pixel_adventure"

    fun load(context: Context): PlatformerEnemyAssets {
        fun strip(folder: String, file: String): SpriteStrip? =
            PlatformerSpriteSheet.loadStrip(context, "$ROOT/$folder/$file")

        val map = mapOf(
            PlatformerEnemyType.SLIME to PlatformerEnemyAnimSet(
                move = strip("Slime", "Idle-Run (44x30).png"),
            ),
            PlatformerEnemyType.MUSHROOM to PlatformerEnemyAnimSet(
                move = strip("Mushroom", "Run (32x32).png"),
                idle = strip("Mushroom", "Idle (32x32).png"),
            ),
            PlatformerEnemyType.BAT to PlatformerEnemyAnimSet(
                fly = strip("Bat", "Flying (46x30).png"),
            ),
            PlatformerEnemyType.GHOST to PlatformerEnemyAnimSet(
                fly = strip("Ghost", "Idle (44x30).png"),
            ),
            PlatformerEnemyType.CHICKEN to PlatformerEnemyAnimSet(
                move = strip("Chicken", "Run (32x34).png"),
            ),
            PlatformerEnemyType.SNAIL to PlatformerEnemyAnimSet(
                move = strip("Snail", "Walk (38x24).png"),
            ),
            PlatformerEnemyType.BLUE_BIRD to PlatformerEnemyAnimSet(
                fly = strip("BlueBird", "Flying (32x32).png"),
            ),
            PlatformerEnemyType.SKULL to PlatformerEnemyAnimSet(
                move = strip("Skull", "Idle 1 (52x54).png"),
            ),
        )
        return PlatformerEnemyAssets(map)
    }
}
