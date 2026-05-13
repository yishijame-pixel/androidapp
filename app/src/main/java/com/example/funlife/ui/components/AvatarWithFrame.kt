// AvatarWithFrame.kt - 带头像框的头像组件
package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * 带头像框的头像组件
 * 支持PNG静态框和GIF动态框
 */
@Composable
fun AvatarWithFrame(
    avatarUri: String?,
    frameAssetPath: String? = null,
    avatarSize: Dp = AvatarSizes.MEDIUM_AVATAR,
    frameSize: Dp = AvatarSizes.MEDIUM_FRAME,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .size(frameSize)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. 底层：用户头像（圆形裁剪）
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUri ?: "")
                .crossfade(true)
                .build(),
            contentDescription = "用户头像",
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentScale = ContentScale.Crop
        )
        
        // 2. 顶层：头像框（如果有）
        frameAssetPath?.let { path ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/$path")
                    .decoderFactory(
                        if (path.endsWith(".gif")) {
                            GifDecoder.Factory()
                        } else {
                            ImageDecoderDecoder.Factory()
                        }
                    )
                    .build(),
                contentDescription = "头像框",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * 小尺寸头像（用于列表、评论）
 */
@Composable
fun SmallAvatarWithFrame(
    avatarUri: String?,
    frameAssetPath: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AvatarWithFrame(
        avatarUri = avatarUri,
        frameAssetPath = frameAssetPath,
        avatarSize = AvatarSizes.SMALL_AVATAR,
        frameSize = AvatarSizes.SMALL_FRAME,
        modifier = modifier,
        onClick = onClick
    )
}

/**
 * 中等尺寸头像（用于个人中心、VIP页面）
 */
@Composable
fun MediumAvatarWithFrame(
    avatarUri: String?,
    frameAssetPath: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AvatarWithFrame(
        avatarUri = avatarUri,
        frameAssetPath = frameAssetPath,
        avatarSize = AvatarSizes.MEDIUM_AVATAR,
        frameSize = AvatarSizes.MEDIUM_FRAME,
        modifier = modifier,
        onClick = onClick
    )
}

/**
 * 大尺寸头像（用于个人主页）
 */
@Composable
fun LargeAvatarWithFrame(
    avatarUri: String?,
    frameAssetPath: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AvatarWithFrame(
        avatarUri = avatarUri,
        frameAssetPath = frameAssetPath,
        avatarSize = AvatarSizes.LARGE_AVATAR,
        frameSize = AvatarSizes.LARGE_FRAME,
        modifier = modifier,
        onClick = onClick
    )
}
