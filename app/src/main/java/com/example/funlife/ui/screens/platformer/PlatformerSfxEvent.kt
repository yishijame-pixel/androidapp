package com.example.funlife.ui.screens.platformer

/** 横版冒险音效事件 ID，与 `platformer_sfx/sfx_manifest.json` 键一致。 */
enum class PlatformerSfxEvent(val manifestId: String) {
    PLAYER_JUMP("player_jump"),
    PLAYER_BIG_JUMP("player_big_jump"),
    PLAYER_LAND("player_land"),
    PLAYER_HURT("player_hurt"),
    PLAYER_DIE("player_die"),
    PICKUP_GEM("pickup_gem"),
    ENEMY_STOMP("enemy_stomp"),
    SPRING_BOUNCE("spring_bounce"),
    LEVEL_CLEAR("level_clear"),
    CHECKPOINT("checkpoint"),
    SHOOT("shoot"),
    SPLASH("splash"),
    SWITCH_TOGGLE("switch_toggle"),
    POWER_UP("power_up"),
    BGM_PLATFORMER("bgm_platformer"),
    BGM_SUPERTUX_ANTARCTIC("bgm_supertux_antarctic"),
    ;

    val isBgm: Boolean get() = name.startsWith("BGM_")
}
