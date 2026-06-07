package com.example.funlife.ui.screens.socialgame

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.SubcomposeAsyncImage
import com.example.funlife.utils.AvatarImageLoader

@Composable
fun SocialGameAvatar(
    displayName: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    size: Dp = 40.dp,
    showOnline: Boolean? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val initial = displayName.firstOrNull()?.toString() ?: "?"
    val boxModifier = modifier
        .size(size)
        .clip(CircleShape)

    LaunchedEffect(avatarUrl, pbAuthToken) {
        AvatarImageLoader.warm(context, avatarUrl, pbAuthToken)
    }

    @Composable
    fun InitialPlaceholder() {
        Box(
            modifier = boxModifier.background(
                Brush.linearGradient(
                    listOf(SocialGamePalette.accentCoral, SocialGamePalette.accentPurple),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
        }
    }

    val localBitmap = AvatarImageLoader.rememberLocalAvatarBitmap(avatarUrl)
    val cachedRemoteBitmap = AvatarImageLoader.rememberCachedRemoteAvatarBitmap(avatarUrl, pbAuthToken)
    val instantBitmap = localBitmap ?: cachedRemoteBitmap

    Box(modifier = modifier.size(size)) {
        when {
            instantBitmap != null -> {
                Image(
                    bitmap = instantBitmap,
                    contentDescription = null,
                    modifier = boxModifier,
                    contentScale = ContentScale.Crop,
                )
            }
            !avatarUrl.isNullOrBlank() -> {
                val model = remember(avatarUrl, pbAuthToken) {
                    AvatarImageLoader.buildRequest(context, avatarUrl, pbAuthToken)
                }
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = boxModifier,
                    contentScale = ContentScale.Crop,
                    loading = { InitialPlaceholder() },
                    error = { InitialPlaceholder() },
                )
            }
            else -> InitialPlaceholder()
        }
        if (showOnline != null) {
            val dotSize = size * 0.28f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (showOnline) SocialGamePalette.online else SocialGamePalette.offline)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}
