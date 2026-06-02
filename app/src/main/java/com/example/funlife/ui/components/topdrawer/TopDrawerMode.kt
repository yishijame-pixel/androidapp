// ═══════════════════════════════════════════════════════════════════════════
// TopDrawerMode.kt
// 下拉抽屉模式注册表：定义所有可用的"主题/形态"。
// 新增模式 3 步走：
//   ① 在 [TopDrawerMode] 中加一项；
//   ② 在 modes/ 目录下新建 ModeXxx.kt，提供一个 @Composable 内容；
//   ③ 在 TopDrawer.kt 的 RenderMode() 里加一个 when 分支。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer

/**
 * 下拉抽屉所有可选模式。id 用作持久化键，**永远不要修改**已发布过的 id。
 */
enum class TopDrawerMode(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String
) {
    WINDOW(
        id = "window",
        title = "窗",
        emoji = "🪟",
        description = "一扇会呼吸的窗，看看外面的天"
    ),
    DIARY(
        id = "diary",
        title = "日记",
        emoji = "📖",
        description = "今天的我，自动汇总的一页日记"
    ),
    STAR_SEA(
        id = "starsea",
        title = "星海",
        emoji = "⭐",
        description = "你所有的过去，化成一片星空"
    );

    companion object {
        /** 按 id 解析；找不到回退到 WINDOW。 */
        fun fromId(id: String?): TopDrawerMode =
            values().firstOrNull { it.id == id } ?: WINDOW
    }
}
