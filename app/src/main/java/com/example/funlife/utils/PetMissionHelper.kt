// PetMissionHelper.kt - 宠物每日任务管理
package com.example.funlife.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 宠物每日任务系统
 * - 每天 3 个固定任务（喂食、玩耍、清洁），完成可领取金币奖励
 * - 进度使用 SharedPreferences 持久化，自动按日期重置
 */
object PetMissionHelper {

    private const val PREF_NAME = "pet_mission_pref"
    private const val KEY_DATE = "mission_date"

    enum class MissionType(
        val key: String,
        val title: String,
        val icon: String,
        val target: Int,
        val reward: Int
    ) {
        FEED("feed_count", "喂食", "🍖", 3, 20),
        PLAY("play_count", "陪伴玩耍", "🎾", 2, 25),
        CLEAN("clean_count", "保持清洁", "💧", 1, 15),
        PET("pet_count", "亲密互动", "💗", 5, 10)
    }

    data class MissionState(
        val type: MissionType,
        val progress: Int,
        val claimed: Boolean
    ) {
        val completed: Boolean get() = progress >= type.target
        val percent: Float get() = (progress.toFloat() / type.target).coerceIn(0f, 1f)
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun sp(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // 🔒 安全修复：任务进度、领取状态按用户 ID 隔离，避免 A 账号完成任务后 B 账号不能领。
    private fun progressKey(userId: Long, type: MissionType) = "u${userId}_${type.key}"
    private fun claimedKey(userId: Long, type: MissionType) = "u${userId}_${type.key}_claimed"
    private fun dateKey(userId: Long) = "u${userId}_$KEY_DATE"

    /** 若日期改变，重置该用户当天进度（不再 clear() 整个文件，避免误删其他账号进度） */
    private fun ensureFreshDay(context: Context, userId: Long) {
        val s = sp(context)
        val saved = s.getString(dateKey(userId), null)
        val today = today()
        if (saved != today) {
            val editor = s.edit()
            // 只清除该用户的进度/领取 key，保留其他用户数据
            MissionType.values().forEach { t ->
                editor.remove(progressKey(userId, t))
                editor.remove(claimedKey(userId, t))
            }
            editor.putString(dateKey(userId), today)
            editor.apply()
        }
    }

    /** 读取当天所有任务状态 */
    fun getMissions(context: Context, userId: Long): List<MissionState> {
        ensureFreshDay(context, userId)
        val s = sp(context)
        return MissionType.values().map { t ->
            MissionState(
                type = t,
                progress = s.getInt(progressKey(userId, t), 0).coerceAtMost(t.target),
                claimed = s.getBoolean(claimedKey(userId, t), false)
            )
        }
    }

    /** 增加某任务进度（达到上限后不再增加） */
    fun increment(context: Context, userId: Long, type: MissionType, amount: Int = 1) {
        ensureFreshDay(context, userId)
        val s = sp(context)
        val current = s.getInt(progressKey(userId, type), 0)
        val next = (current + amount).coerceAtMost(type.target)
        s.edit().putInt(progressKey(userId, type), next).apply()
    }

    /** 标记奖励已领取 */
    fun claim(context: Context, userId: Long, type: MissionType): Boolean {
        ensureFreshDay(context, userId)
        val s = sp(context)
        val progress = s.getInt(progressKey(userId, type), 0)
        if (progress < type.target) return false
        if (s.getBoolean(claimedKey(userId, type), false)) return false
        s.edit().putBoolean(claimedKey(userId, type), true).apply()
        return true
    }
}
