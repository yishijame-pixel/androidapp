package com.example.funlife.resource

/** 豆人迷宫进局前必须就绪的云端资源包（manifest → COS zip）。 */
object PacMazeResourceBundles {
    const val SFX = "pac_maze_sfx"
    const val SKINS = "pac_maze_skins"

    /** @see GameResourceBundles.gameBootOrder */
    val bootOrder: List<String> get() = GameResourceBundles.gameBootOrder

    fun displayName(bundleId: String): String = GameResourceBundles.displayName(bundleId)
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
                "${GameResourceBundles.displayName(pendingBundleIds.first())}待下载"
            else -> "游戏资源待下载（${pendingBundleIds.size} 项）"
        }

    val detailLine: String
        get() = pendingBundleIds.joinToString(" · ") { GameResourceBundles.shortDisplayName(it) }
}

data class GameResourceLocalStatus(
    val readyBundleIds: List<String> = emptyList(),
    val pendingBundleIds: List<String> = emptyList(),
) {
    val totalCount: Int get() = GameResourceBundles.gameBootOrder.size
    val readyCount: Int get() = readyBundleIds.size
    val allReady: Boolean get() = pendingBundleIds.isEmpty()
}

data class GameResourceSyncUiState(
    val isSyncing: Boolean = false,
    val overallPercent: Int = 0,
    val activeBundleId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val progressLabel: String?
        get() = errorMessage?.takeIf { it.isNotBlank() }
            ?: statusMessage
            ?: activeBundleId?.let { id ->
                val name = GameResourceBundles.displayName(id)
                if (overallPercent > 0) "正在下载 $name… $overallPercent%" else "正在下载 $name…"
            }
}

data class ActiveBundleDownloadProgress(
    val bundleId: String,
    val phase: String,
    val percent: Int,
) {
    val label: String
        get() = when (phase) {
            "manifest" -> "检查 ${GameResourceBundles.shortDisplayName(bundleId)}…"
            "download" -> "下载 ${GameResourceBundles.shortDisplayName(bundleId)}… $percent%"
            "unzip" -> "解压 ${GameResourceBundles.shortDisplayName(bundleId)}…"
            "ready" -> "${GameResourceBundles.shortDisplayName(bundleId)} 已就绪"
            else -> GameResourceBundles.shortDisplayName(bundleId)
        }
}
