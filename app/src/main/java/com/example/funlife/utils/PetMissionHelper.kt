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

    /** 若日期改变，重置所有进度 */
    private fun ensureFreshDay(context: Context) {
        val s = sp(context)
        val saved = s.getString(KEY_DATE, null)
        val today = today()
        if (saved != today) {
            val editor = s.edit().clear().putString(KEY_DATE, today)
            editor.apply()
        }
    }

    /** 读取当天所有任务状态 */
    fun getMissions(context: Context): List<MissionState> {
        ensureFreshDay(context)
        val s = sp(context)
        return MissionType.values().map { t ->
            MissionState(
                type = t,
                progress = s.getInt(t.key, 0).coerceAtMost(t.target),
                claimed = s.getBoolean("${t.key}_claimed", false)
            )
        }
    }

    /** 增加某任务进度（达到上限后不再增加） */
    fun increment(context: Context, type: MissionType, amount: Int = 1) {
        ensureFreshDay(context)
        val s = sp(context)
        val current = s.getInt(type.key, 0)
        val next = (current + amount).coerceAtMost(type.target)
        s.edit().putInt(type.key, next).apply()
    }

    /** 标记奖励已领取 */
    fun claim(context: Context, type: MissionType): Boolean {
        ensureFreshDay(context)
        val s = sp(context)
        val progress = s.getInt(type.key, 0)
        if (progress < type.target) return false
        if (s.getBoolean("${type.key}_claimed", false)) return false
        s.edit().putBoolean("${type.key}_claimed", true).apply()
        return true
    }
}
