package com.example.funlife.ui.screens.platformer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import java.io.File
import java.util.zip.ZipFile

object SuperTuxClassicLauncher {

    const val DEFAULT_LEVEL = "levels/world1/welcome_antarctica.stl"
    private const val NATIVE_LIB = "libsupertux2.so"

    fun isNativeLibraryPresent(context: Context): Boolean {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        if (!nativeDir.isNullOrBlank() && File(nativeDir, NATIVE_LIB).exists()) {
            return true
        }
        return isNativeLibraryBundledInApk(context)
    }

    private fun isNativeLibraryBundledInApk(context: Context): Boolean = runCatching {
        val apkPath = context.applicationInfo.sourceDir ?: return false
        ZipFile(apkPath).use { zip ->
            Build.SUPPORTED_ABIS.any { abi ->
                zip.getEntry("lib/$abi/$NATIVE_LIB") != null
            }
        }
    }.getOrDefault(false)

    fun isDataZipPresent(context: Context): Boolean =
        SuperTuxClassicDataPreparer.isStaged(context) || runCatching {
            context.assets.open("data.zip").close()
            true
        }.getOrDefault(false)

    fun isReady(context: Context): Boolean =
        isNativeLibraryPresent(context) && isDataZipPresent(context)

    fun missingPieces(context: Context): List<String> = buildList {
        if (!isNativeLibraryPresent(context)) add("libsupertux2.so（jniLibs/arm64-v8a）")
        if (!isDataZipPresent(context)) add("assets/data.zip（未解压到外部存储）")
    }

    suspend fun prepareAndStart(
        context: Context,
        levelStl: String,
        onProgress: (SuperTuxClassicDataPreparer.Progress) -> Unit = {},
    ): Boolean {
        if (!isNativeLibraryPresent(context)) return false
        if (!runCatching { context.assets.open("data.zip").close(); true }.getOrDefault(false)) {
            return false
        }
        if (!SuperTuxClassicDataPreparer.ensureReady(context, onProgress)) {
            return false
        }
        start(context, levelStl = levelStl)
        return true
    }

    fun start(
        context: Context,
        levelStl: String = DEFAULT_LEVEL,
        playerSprite: String = "images/creatures/tux/tux.sprite",
        saveSlot: String = "funlife_default",
    ) {
        val intent = Intent(context, SuperTuxClassicActivity::class.java).apply {
            putExtra(SuperTuxClassicActivity.EXTRA_LEVEL_STL, levelStl)
            putExtra(SuperTuxClassicActivity.EXTRA_PLAYER_SPRITE, playerSprite)
            putExtra(SuperTuxClassicActivity.EXTRA_SAVE_SLOT, saveSlot)
            if (context !is ComponentActivity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
        if (context is Activity) {
            context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    /** True when zip is staged and fully extracted — no prep overlay needed. */
    fun isGameDataReady(context: Context): Boolean =
        SuperTuxClassicDataPreparer.isStaged(context) &&
            SuperTuxClassicDataPreparer.isExtracted(context)
}
