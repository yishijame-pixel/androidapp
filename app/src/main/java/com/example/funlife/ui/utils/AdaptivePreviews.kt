package com.example.funlife.ui.utils

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * 🔥 多机型预览注解集合
 *
 * 在任意 @Composable 上加上 @AdaptivePreviews，Android Studio Preview 面板就会
 * 同时渲染 4 种机型，一眼看出哪些机型上布局有问题。
 *
 * 用法：
 * ```kotlin
 * @AdaptivePreviews
 * @Composable
 * fun MyScreenPreview() {
 *     FunLifeTheme {
 *         MyScreen()
 *     }
 * }
 * ```
 */
@Preview(name = "1.小屏 5.0\" (360x640)", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "2.主流 6.1\" (393x852)", widthDp = 393, heightDp = 852, showBackground = true)
@Preview(name = "3.大屏 6.7\" (412x915)", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "4.平板 (800x1280)", widthDp = 800, heightDp = 1280, showBackground = true)
annotation class AdaptivePreviews

/**
 * 仅手机预览（紧凑型 + 大屏型）
 */
@Preview(name = "小屏手机 (360dp)", widthDp = 360, heightDp = 640)
@Preview(name = "主流手机 (393dp)", widthDp = 393, heightDp = 852)
@Preview(name = "大屏手机 (412dp)", widthDp = 412, heightDp = 915)
annotation class PhonePreviews

/**
 * 横屏 + 竖屏对比预览
 */
@Preview(name = "竖屏 (393x852)", widthDp = 393, heightDp = 852)
@Preview(name = "横屏 (852x393)", widthDp = 852, heightDp = 393)
annotation class OrientationPreviews

/**
 * 字体大小预览（系统设置大字号场景）
 */
@Preview(name = "默认字号 (1.0x)", fontScale = 1.0f, widthDp = 393, heightDp = 852)
@Preview(name = "大字号 (1.3x)", fontScale = 1.3f, widthDp = 393, heightDp = 852)
@Preview(name = "超大字号 (1.5x)", fontScale = 1.5f, widthDp = 393, heightDp = 852)
annotation class FontScalePreviews

/**
 * 暗色主题预览（如果项目支持）
 */
@Preview(name = "亮色", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO,
    widthDp = 393, heightDp = 852)
@Preview(name = "暗色", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    widthDp = 393, heightDp = 852)
annotation class ThemePreviews
