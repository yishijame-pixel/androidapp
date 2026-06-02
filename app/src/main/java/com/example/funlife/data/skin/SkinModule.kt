// ═══════════════════════════════════════════════════════════════════════════
// SkinModule — 进程级单例容器
//
// 项目无 Hilt，按现有模式手工提供单例。SkinRepository 是 App 全局状态，
// 跨 Composable 共享、跨进程重建恢复，因此用应用级单例最合适。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data.skin

import android.content.Context

object SkinModule {
    @Volatile
    private var instance: SkinRepository? = null

    fun provide(context: Context): SkinRepository =
        instance ?: synchronized(this) {
            instance ?: DefaultSkinRepository.create(context).also { instance = it }
        }

    /** 仅测试 / 调试用：替换实例。 */
    fun overrideForTest(repo: SkinRepository) {
        instance = repo
    }
}
