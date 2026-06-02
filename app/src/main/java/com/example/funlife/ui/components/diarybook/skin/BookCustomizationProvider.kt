// ═══════════════════════════════════════════════════════════════════════════
// BookCustomizationProvider — 魔法书封面定制 CompositionLocal
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook.skin

import androidx.compose.runtime.Composable
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
    var customization by remember(userId) {
        mutableStateOf(DiaryBookCustomizationStore.load(ctx, userId))
    }
    val state = remember(userId, customization) {
        BookCustomizationState(
            customization = customization,
            update = { title, owner ->
                DiaryBookCustomizationStore.save(ctx, userId, title, owner)
                customization = DiaryBookCustomizationStore.load(ctx, userId)
            },
            reset = {
                DiaryBookCustomizationStore.reset(ctx, userId)
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
