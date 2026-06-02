package com.example.funlife.utils

import android.content.Context
import android.os.Build
import com.example.funlife.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃兜底：把未捕获异常写本地日志文件，最多保留 10 份；用户可在"设置 → 反馈"导出。
 * 不依赖任何外网 SDK，离线可用、零隐私风险。
 */
object CrashHandler {
    private const val DIR = "crash_logs"
    private const val MAX_KEEP = 10

    private var inited = false
    private lateinit var appCtx: Context
    private var prev: Thread.UncaughtExceptionHandler? = null

    fun install(ctx: Context) {
        if (inited) return
        appCtx = ctx.applicationContext
        prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { writeCrash(t, e) }
            // 交回给系统，让 App 正常崩溃（市场 ANR/Crash 统计仍会捕获）
            prev?.uncaughtException(t, e)
        }
        inited = true
    }

    private fun writeCrash(thread: Thread, e: Throwable) {
        val dir = File(appCtx.filesDir, DIR).apply { if (!exists()) mkdirs() }
        // 清理超出数量的旧文件
        dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_KEEP - 1)
            ?.forEach { runCatching { it.delete() } }

        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val f = File(dir, "crash-$ts.log")
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))

        f.writeText(buildString {
            appendLine("=== FunLife Crash ===")
            appendLine("time: ${Date()}")
            appendLine("thread: ${thread.name}")
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android: ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT})")
            appendLine("abi: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            append(sw.toString())
        })
    }

    /** 返回所有崩溃日志（按时间倒序） */
    fun listLogs(ctx: Context): List<File> {
        val dir = File(ctx.filesDir, DIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /** 合并最近 N 条日志为一段文本，供"导出反馈"分享 */
    fun exportRecent(ctx: Context, max: Int = 3): String {
        val logs = listLogs(ctx).take(max)
        if (logs.isEmpty()) return "（暂无崩溃日志）"
        return logs.joinToString("\n\n=== 分割线 ===\n\n") { it.readText() }
    }

    fun clear(ctx: Context) {
        File(ctx.filesDir, DIR).listFiles()?.forEach { runCatching { it.delete() } }
    }
}
