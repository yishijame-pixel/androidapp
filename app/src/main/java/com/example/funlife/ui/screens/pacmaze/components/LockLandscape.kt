package com.example.funlife.ui.screens.pacmaze.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

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
