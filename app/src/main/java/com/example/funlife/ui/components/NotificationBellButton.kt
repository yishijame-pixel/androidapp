package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.funlife.notifications.InboxStore
import com.example.funlife.social.SocialInboxSync

/** 首页欢迎区 / 个人页 PROFILE 栏 — 两处铃铛统一样式源 */
enum class NotificationBellVariant {
    /** 首页顶部：圆形 48dp */
    Home,
    /** 我的页 PROFILE 右侧：圆角方钮 40dp */
    Profile,
}

/**
 * 应用内通知铃铛（首页 + 我的页共用）：InboxStore 未读红点 + 好友待办后台同步。
 */
@Composable
fun NotificationBellButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NotificationBellVariant = NotificationBellVariant.Home,
    iconTint: Color = Color.White,
    bubbleBackground: Color = Color.White.copy(alpha = 0.28f),
) {
    val outerSize = when (variant) {
        NotificationBellVariant.Home -> 48.dp
        NotificationBellVariant.Profile -> 40.dp
    }
    val innerSize = when (variant) {
        NotificationBellVariant.Home -> 40.dp
        NotificationBellVariant.Profile -> 34.dp
    }
    val innerShape: Shape = when (variant) {
        NotificationBellVariant.Home -> CircleShape
        NotificationBellVariant.Profile -> RoundedCornerShape(11.dp)
    }
    val iconSize = when (variant) {
        NotificationBellVariant.Home -> 20.dp
        NotificationBellVariant.Profile -> 15.dp
    }
    val badgeFontSize = when (variant) {
        NotificationBellVariant.Home -> 10.sp
        NotificationBellVariant.Profile -> 8.sp
    }
    val ctx = LocalContext.current
    val unread by InboxStore.unreadFlow.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        InboxStore.refreshUnread(ctx)
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) {
                InboxStore.refreshUnread(ctx)
                SocialInboxSync.syncNowAsync(ctx, force = true)
                com.example.funlife.social.SocialSessionManager.warmStartAsync(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    DisposableEffect(Unit) {
        val processObs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_START) {
                InboxStore.refreshUnread(ctx)
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObs)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(processObs) }
    }

    Box(
        modifier = modifier
            .size(outerSize)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(innerShape)
                .background(bubbleBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "通知",
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(horizontal = if (unread > 9) 3.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        unread > 99 -> "99+"
                        unread > 9 -> "9+"
                        else -> unread.toString()
                    },
                    color = Color.White,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = badgeFontSize,
                )
            }
        }
    }
}
