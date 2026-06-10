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
