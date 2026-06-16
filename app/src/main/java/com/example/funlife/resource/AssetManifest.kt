package com.example.funlife.resource

data class AssetBundleInfo(
    val id: String,
    val file: String,
    val targetDir: String,
    val url: String?,
)

data class AssetManifestResponse(
    val ok: Boolean = false,
    val version: Int = 0,
    val updatedAt: String? = null,
    val bundles: List<AssetBundleInfo> = emptyList(),
)

/** 云端 bundle 下载/解压进度（供进游戏加载页展示）。 */
data class BundleLoadProgress(
    val phase: String,
    val percent: Int,
)

enum class BundleEnsureFailure {
    MANIFEST_UNAVAILABLE,
    BUNDLE_NOT_IN_MANIFEST,
    DOWNLOAD_URL_UNAVAILABLE,
    DOWNLOAD_FAILED,
    VALIDATION_FAILED,
}

sealed class BundleEnsureResult {
    data object Success : BundleEnsureResult()
    data class Failed(
        val reason: BundleEnsureFailure,
        val detail: String? = null,
    ) : BundleEnsureResult()

    val isSuccess: Boolean get() = this is Success

    fun userMessage(): String = when (this) {
        is Success -> ""
        is Failed -> when (reason) {
            BundleEnsureFailure.MANIFEST_UNAVAILABLE ->
                "无法连接资源服务器，请稍后重试"
            BundleEnsureFailure.BUNDLE_NOT_IN_MANIFEST ->
                "云端尚未发布豆人迷宫资源包"
            BundleEnsureFailure.DOWNLOAD_URL_UNAVAILABLE ->
                "无法获取资源下载链接，请确认云存储已上传"
            BundleEnsureFailure.DOWNLOAD_FAILED ->
                detail?.let { "资源下载失败：$it" } ?: "资源下载失败，请检查网络后重试"
            BundleEnsureFailure.VALIDATION_FAILED ->
                detail ?: "资源包校验未通过，请重试"
        }
    }
}
