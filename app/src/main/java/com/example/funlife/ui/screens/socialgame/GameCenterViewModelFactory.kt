package com.example.funlife.ui.screens.socialgame

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.UserSession
import com.example.funlife.viewmodel.GameCenterViewModel

/** MainActivity 与 NavGraph 共用同一 Activity-scoped [GameCenterViewModel]，避免邀请状态分裂。 */
@Composable
fun rememberGameCenterViewModel(userSession: UserSession): GameCenterViewModel {
    val activity = LocalContext.current as ComponentActivity
    return viewModel(
        viewModelStoreOwner = activity,
        key = "game_center_${userSession.userId}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameCenterViewModel(
                    application = activity.application as FunLifeApplication,
                    currentUserId = userSession.userId,
                    myFunlifeUsername = userSession.username,
                    displayName = userSession.nickname.ifBlank { userSession.username },
                ) as T
            }
        },
    )
}
