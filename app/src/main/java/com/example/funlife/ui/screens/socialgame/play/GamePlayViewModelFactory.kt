package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.UserSession
import com.example.funlife.viewmodel.GamePlayViewModel

@Composable
fun rememberGamePlayViewModel(userSession: UserSession, roomId: String): GamePlayViewModel {
    val app = LocalContext.current.applicationContext as FunLifeApplication
    return viewModel(
        key = "game_play_${userSession.userId}_$roomId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val displayName = userSession.nickname.ifBlank { userSession.username }
                return GamePlayViewModel(app, userSession.userId, roomId, displayName) as T
            }
        },
    )
}
