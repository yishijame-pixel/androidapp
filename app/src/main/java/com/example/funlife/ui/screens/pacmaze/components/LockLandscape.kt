package com.example.funlife.ui.screens.pacmaze.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LockLandscape() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            // 配置变更（横竖屏切换）销毁 Activity 时勿恢复竖屏，否则会与上面的横屏锁定
            // 形成「强制横屏 → 重建 → 恢复竖屏 → 再强制横屏」的死循环，表现为启动页/加载页来回跳。
            if (activity?.isChangingConfigurations != true) {
                activity?.requestedOrientation =
                    original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}

/** 对局全屏：隐藏电量/时间/WiFi 等系统状态栏与导航栏，仅用户边缘滑动可临时唤出。 */
@Composable
fun PacMazeHideSystemBars() {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        if (activity == null) {
            return@DisposableEffect onDispose {}
        }
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            if (activity.isChangingConfigurations) return@onDispose
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    SideEffect {
        val activity = context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view)
            .hide(WindowInsetsCompat.Type.systemBars())
    }
}
