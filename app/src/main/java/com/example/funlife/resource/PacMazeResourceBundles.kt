package com.example.funlife.resource

/** 豆人迷宫进局前必须就绪的云端资源包（manifest → COS zip）。 */
object PacMazeResourceBundles {
    const val SFX = "pac_maze_sfx"
    const val SKINS = "pac_maze_skins"

    /** 加载页按顺序确保：先皮肤包（体积大、选角依赖），再音效包。 */
    val bootOrder: List<String> = listOf(SKINS, SFX)

    fun displayName(bundleId: String): String = when (bundleId) {
        SFX -> "音效与背景音乐"
        SKINS -> "角色动画资源"
        else -> bundleId
    }
}

data class PacMazeResourceUpdateStatus(
    val manifestVersion: Int = 0,
    val pendingBundleIds: List<String> = emptyList(),
) {
    val hasPending: Boolean get() = pendingBundleIds.isNotEmpty()

    val summary: String
        get() = when {
            pendingBundleIds.isEmpty() -> ""
            pendingBundleIds.size == 1 ->
                "${PacMazeResourceBundles.displayName(pendingBundleIds.first())}有更新"
            else -> "角色与音效资源有更新（${pendingBundleIds.size} 项）"
        }
}
