package com.example.funlife.utils

import android.content.Context
import android.net.Uri
import com.example.funlife.BuildConfig
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.Countdown
import com.example.funlife.data.model.Goal
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.MoodEntry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用户数据本地备份/恢复（人类可读 JSON）。
 *
 * 仅备份与"个人记忆"强相关的数据：纪念日 / 心情 / 目标 / 习惯 / 倒数日。
 * 不备份成就、金币、商城等可重新获取/合成的次要数据。
 *
 * 兼容策略：
 *  - 导出时记录 schemaVersion = BuildConfig.VERSION_CODE 与 backupVersion = 1
 *  - 导入按字段名匹配（Gson 默认行为）；未来新增字段不会破坏旧文件
 *  - 不携带 userId（恢复时强制回写当前登录用户的 id，避免跨账号混淆）
 */
object DataBackupManager {
    private const val BACKUP_VERSION = 1
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class BackupBundle(
        val backupVersion: Int = BACKUP_VERSION,
        val schemaVersion: Int = BuildConfig.VERSION_CODE,
        val appVersion: String = BuildConfig.VERSION_NAME,
        val exportedAt: Long = System.currentTimeMillis(),
        val anniversaries: List<Anniversary> = emptyList(),
        val moods: List<MoodEntry> = emptyList(),
        val goals: List<Goal> = emptyList(),
        val habits: List<Habit> = emptyList(),
        val countdowns: List<Countdown> = emptyList()
    )

    data class ImportResult(
        val anniversaries: Int,
        val moods: Int,
        val goals: Int,
        val habits: Int,
        val countdowns: Int
    ) {
        val total: Int get() = anniversaries + moods + goals + habits + countdowns
    }

    /** 把当前登录用户的数据收集为 JSON 文件（位于 cacheDir/backups/），返回该文件 */
    suspend fun exportToFile(ctx: Context, userId: Long): File {
        val db = AppDatabase.getDatabase(ctx)
        val bundle = BackupBundle(
            anniversaries = db.anniversaryDao().getAllForUserOnce(userId),
            moods = db.moodDao().getAllMoodEntries(userId).first(),
            goals = listOf(
                db.goalDao().getActiveGoals(userId).first(),
                db.goalDao().getCompletedGoals(userId).first()
            ).flatten(),
            habits = db.habitDao().getAllActiveHabits(userId).first(),
            countdowns = db.goalDao().getAllCountdowns(userId).first()
        )

        val dir = File(ctx.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "funlife-backup-$ts.json")
        file.writeText(gson.toJson(bundle))
        return file
    }

    /** 从 Uri 读 JSON 并写入当前用户。所有条目 id=0 让 Room 自动分配，避免与已有冲突。 */
    suspend fun importFromUri(ctx: Context, uri: Uri, currentUserId: Long): ImportResult {
        val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("无法读取所选文件")
        val obj = gson.fromJson(text, JsonObject::class.java)
            ?: throw IllegalStateException("文件格式不是 JSON")
        val db = AppDatabase.getDatabase(ctx)

        var nAnn = 0; var nMood = 0; var nGoal = 0; var nHabit = 0; var nCd = 0

        obj.getAsJsonArray("anniversaries")?.forEach { el ->
            runCatching {
                val a = gson.fromJson(el, Anniversary::class.java)
                    .copy(id = 0, userId = currentUserId)
                db.anniversaryDao().insertAnniversary(a); nAnn++
            }
        }
        obj.getAsJsonArray("moods")?.forEach { el ->
            runCatching {
                val m = gson.fromJson(el, MoodEntry::class.java)
                    .copy(id = 0, userId = currentUserId)
                db.moodDao().insertMood(m); nMood++
            }
        }
        obj.getAsJsonArray("goals")?.forEach { el ->
            runCatching {
                val g = gson.fromJson(el, Goal::class.java)
                    .copy(id = 0, userId = currentUserId)
                db.goalDao().insertGoal(g); nGoal++
            }
        }
        obj.getAsJsonArray("habits")?.forEach { el ->
            runCatching {
                val h = gson.fromJson(el, Habit::class.java)
                    .copy(id = 0, userId = currentUserId)
                db.habitDao().insertHabit(h); nHabit++
            }
        }
        obj.getAsJsonArray("countdowns")?.forEach { el ->
            runCatching {
                val c = gson.fromJson(el, Countdown::class.java)
                    .copy(id = 0, userId = currentUserId)
                db.goalDao().insertCountdown(c); nCd++
            }
        }
        return ImportResult(nAnn, nMood, nGoal, nHabit, nCd)
    }
}
