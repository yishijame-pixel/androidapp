package com.example.funlife.ui.screens.socialgame

import androidx.annotation.DrawableRes
import com.example.funlife.R

object SocialGameIcons {
    @DrawableRes
    fun resId(gameId: String): Int? = when (gameId) {
        "gomoku" -> R.drawable.ic_game_gomoku
        "draw_guess" -> R.drawable.ic_game_draw_guess
        else -> null
    }
}
