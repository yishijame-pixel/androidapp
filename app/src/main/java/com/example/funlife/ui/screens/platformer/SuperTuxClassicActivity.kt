package com.example.funlife.ui.screens.platformer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * SuperTux 经典模式壳（Phase 4 占位）。
 * 待 `engine/supertux-fork` 产出 libsupertux2.so 后继承 SDLActivity。
 */
class SuperTuxClassicActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val level = intent.getStringExtra(EXTRA_LEVEL_STL) ?: DEFAULT_LEVEL
        Toast.makeText(
            this,
            "经典模式引擎构建中\n关卡: $level\n见 docs/supertux-fork-build-guide.md",
            Toast.LENGTH_LONG,
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_LEVEL_STL = "level_stl"
        const val EXTRA_PLAYER_SPRITE = "player_sprite"
        const val EXTRA_SAVE_SLOT = "save_slot"
        private const val DEFAULT_LEVEL = "levels/world1/welcome_antarctica.stl"

        fun start(
            context: Context,
            levelStl: String = DEFAULT_LEVEL,
            playerSprite: String = "images/creatures/tux/tux.sprite",
            saveSlot: String = "funlife_default",
        ) {
            context.startActivity(
                Intent(context, SuperTuxClassicActivity::class.java).apply {
                    putExtra(EXTRA_LEVEL_STL, levelStl)
                    putExtra(EXTRA_PLAYER_SPRITE, playerSprite)
                    putExtra(EXTRA_SAVE_SLOT, saveSlot)
                    if (context !is ComponentActivity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
