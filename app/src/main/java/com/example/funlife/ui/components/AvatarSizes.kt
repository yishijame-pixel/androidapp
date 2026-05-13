// AvatarSizes.kt - 头像尺寸常量
package com.example.funlife.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 头像和头像框的标准尺寸定义
 */
object AvatarSizes {
    // 小尺寸（列表、评论）
    val SMALL_AVATAR = 48.dp
    val SMALL_FRAME = 56.dp
    
    // 中等尺寸（个人中心、VIP页面）- 主要使用
    val MEDIUM_AVATAR = 100.dp
    val MEDIUM_FRAME = 120.dp
    
    // 大尺寸（个人主页）
    val LARGE_AVATAR = 120.dp
    val LARGE_FRAME = 140.dp
    
    // 商城预览尺寸
    val SHOP_PREVIEW_AVATAR = 80.dp
    val SHOP_PREVIEW_FRAME = 96.dp
    
    /**
     * 计算边框宽度
     */
    fun getFrameBorderWidth(avatarSize: Dp, frameSize: Dp): Dp {
        return (frameSize - avatarSize) / 2
    }
}
