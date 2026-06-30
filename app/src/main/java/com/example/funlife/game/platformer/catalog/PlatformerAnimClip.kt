package com.example.funlife.game.platformer.catalog

/** 横版角色动画 clip（扩展 PacMaze 标准集）。 */
enum class PlatformerAnimClip(val folder: String, val prefix: String) {
    IDLE("idle", "idle"),
    WALK("walk", "walk"),
    RUN("run", "run"),
    JUMP("jump", "jump"),
    SLIDE("slide", "slide"),
    ATTACK("attack", "attack"),
    SHOOT("shoot", "shoot"),
    HURT("hurt", "hurt"),
    FALL("fall", "fall"),
    CLIMB("climb", "climb"),
    GLIDE("glide", "glide"),
    THROW("throw", "throw"),
    JUMP_ATTACK("jump_attack", "jump_attack"),
    JUMP_THROW("jump_throw", "jump_throw"),
    JUMP_SHOOT("jump_shoot", "jump_shoot"),
    RUN_SHOOT("run_shoot", "run_shoot"),
    FLY("fly", "fly"),
    DIE("die", "die"),
    ;

    companion object {
        fun fromManifestKey(key: String): PlatformerAnimClip? =
            entries.find { it.name.equals(key, ignoreCase = true) || it.folder == key.lowercase() }
    }
}

data class PlatformerAnimConfig(
    val assetRoot: String,
    val clips: Set<PlatformerAnimClip>,
    val syncWalkCycleToSprite: Boolean = true,
    val sampleSize: Int = 1,
    val mirrorDefault: Boolean = true,
    val heightCellFrac: Float = 1.55f,
)
