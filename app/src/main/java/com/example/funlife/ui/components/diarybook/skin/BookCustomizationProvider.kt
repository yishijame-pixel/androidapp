// ═══════════════════════════════════════════════════════════════════════════
// BookCustomizationProvider — 魔法书封面定制 CompositionLocal（按皮肤分册）
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.data.BookCustomization
import com.example.funlife.data.DiaryBookCustomizationStore

/** 可变的封面定制状态（含更新回调）。 */
class BookCustomizationState(
    val customization: BookCustomization,
    val update: (bookTitle: String, ownerName: String) -> Unit,
    val reset: () -> Unit,
)

val LocalBookCustomizationState = staticCompositionLocalOf<BookCustomizationState?> { null }

@Composable
fun BookCustomizationProvider(
    userId: Long,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val skin = LocalBookSkin.current
    val skinId = skin.id.raw
    var customization by remember(userId, skinId) {
        mutableStateOf(DiaryBookCustomizationStore.load(ctx, userId, skinId))
    }
    LaunchedEffect(userId, skinId) {
        customization = DiaryBookCustomizationStore.load(ctx, userId, skinId)
    }
    val state = remember(userId, skinId, customization) {
        BookCustomizationState(
            customization = customization,
            update = { title, owner ->
                DiaryBookCustomizationStore.save(ctx, userId, skinId, title, owner)
                customization = DiaryBookCustomizationStore.load(ctx, userId, skinId)
            },
            reset = {
                DiaryBookCustomizationStore.reset(ctx, userId, skinId)
                customization = BookCustomization.Empty
            },
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalBookCustomizationState provides state,
    ) {
        content()
    }
}

/** 读取当前封面定制；未挂载 Provider 时返回空（由调用方回退默认文案）。 */
@Composable
fun rememberBookCustomization(): BookCustomization =
    LocalBookCustomizationState.current?.customization ?: BookCustomization.Empty
