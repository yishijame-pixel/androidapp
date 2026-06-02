// ═══════════════════════════════════════════════════════════════════════════
// BookSkinProvider — 把 SkinRepository.currentSkin 暴露为 CompositionLocal
//
// 用法：
//     BookSkinProvider {
//         // 子树中任何位置可：val skin = LocalBookSkin.current
//     }
//
// 作用域：仅在 DiaryBookHost 内挂载，避免污染全 App。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.data.skin.SkinModule
import com.example.funlife.data.skin.SkinRepository
import com.example.funlife.domain.skin.BookSkin
import com.example.funlife.domain.skin.BuiltInSkins

/** Composition local；默认值 = HengWu，避免未挂载 Provider 时崩。 */
val LocalBookSkin = staticCompositionLocalOf<BookSkin> { BuiltInSkins.default }

/**
 * 把 [repository] 当前皮肤注入到子树。
 *
 * @param repository 默认从 [SkinModule] 取进程单例；测试时可注入 fake。
 */
@Composable
fun BookSkinProvider(
    repository: SkinRepository = SkinModule.provide(LocalContext.current),
    content: @Composable () -> Unit
) {
    val skin by repository.currentSkin.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalBookSkin provides skin) {
        content()
    }
}
