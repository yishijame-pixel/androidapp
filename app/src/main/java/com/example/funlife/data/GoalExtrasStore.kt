// ═════════════════════════════════════════════════════════════════════════
// GoalExtrasStore.kt
// 目标&倒数日扩展数据：里程碑 / 打卡 / 成就 / 倒数日提醒
// 使用 SharedPreferences + JSON，避免侵入 Room 数据库
// 严格按 userId 做隔离
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ═════════════════════════════════════════════════════════════════════════
// 模型
// ═════════════════════════════════════════════════════════════════════════
data class Milestone(
    val id: String,           // 客户端生成 uuid
    val text: String,
    val done: Boolean,
    val doneAt: String? = null
)

data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val emoji: String,
    val unlockedAt: String? = null
) {
    val unlocked: Boolean get() = !unlockedAt.isNullOrBlank()
}

// ═════════════════════════════════════════════════════════════════════════
// 里程碑：goalId -> List<Milestone>
// ═════════════════════════════════════════════════════════════════════════
object MilestoneStore {
    private const val PREF = "goal_milestones"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun key(userId: Long, goalId: Int) = "u${userId}_g${goalId}"

    fun load(c: Context, userId: Long, goalId: Int): List<Milestone> = runCatching {
        val raw = prefs(c).getString(key(userId, goalId), null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Milestone(
                id = o.optString("id"),
                text = o.optString("text"),
                done = o.optBoolean("done"),
                doneAt = o.optString("doneAt").takeIf { it.isNotBlank() }
            )
        }
    }.getOrElse { emptyList() }

    fun save(c: Context, userId: Long, goalId: Int, items: List<Milestone>) = runCatching {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("text", it.text)
                put("done", it.done)
                if (it.doneAt != null) put("doneAt", it.doneAt)
            })
        }
        prefs(c).edit().putString(key(userId, goalId), arr.toString()).apply()
    }

    /** 完成度 0..1，无里程碑则返回 null */
    fun completionFraction(c: Context, userId: Long, goalId: Int): Float? {
        val list = load(c, userId, goalId)
        if (list.isEmpty()) return null
        val done = list.count { it.done }
        return done.toFloat() / list.size
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 目标自定义图标：goalId -> 图标字符串（emoji 或 file:/abs/path）
// ═════════════════════════════════════════════════════════════════════════
object GoalIconStore {
    private const val PREF = "goal_icons"
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun key(userId: Long, goalId: Int) = "u${userId}_g${goalId}"

    fun get(c: Context, userId: Long, goalId: Int): String? =
        runCatching { prefs(c).getString(key(userId, goalId), null)?.takeIf { it.isNotBlank() } }.getOrNull()

    fun set(c: Context, userId: Long, goalId: Int, icon: String?) {
        val k = key(userId, goalId)
        if (icon.isNullOrBlank()) prefs(c).edit().remove(k).apply()
        else prefs(c).edit().putString(k, icon).apply()
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 每日打卡：goalId -> Set<日期>
// ═════════════════════════════════════════════════════════════════════════
object GoalCheckInStore {
    private const val PREF = "goal_checkins"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun key(userId: Long, goalId: Int) = "u${userId}_g${goalId}"
    private fun keyAll(userId: Long) = "u${userId}_all"

    fun load(c: Context, userId: Long, goalId: Int): Set<String> = runCatching {
        prefs(c).getStringSet(key(userId, goalId), emptySet())?.toSet() ?: emptySet()
    }.getOrElse { emptySet() }

    fun loadAll(c: Context, userId: Long): Set<String> = runCatching {
        prefs(c).getStringSet(keyAll(userId), emptySet())?.toSet() ?: emptySet()
    }.getOrElse { emptySet() }

    fun checkInToday(c: Context, userId: Long, goalId: Int): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cur = load(c, userId, goalId).toMutableSet()
        if (today in cur) return false
        cur.add(today)
        prefs(c).edit().putStringSet(key(userId, goalId), cur).apply()
        // 同时记录"任意目标在今天打过卡"用于全局连签
        val all = loadAll(c, userId).toMutableSet()
        all.add(today)
        prefs(c).edit().putStringSet(keyAll(userId), all).apply()
        return true
    }

    /** 计算从今天往前的连续打卡天数（任意目标） */
    fun globalStreak(c: Context, userId: Long): Int {
        val all = loadAll(c, userId)
        var streak = 0
        var d = LocalDate.now()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        while (d.format(fmt) in all) {
            streak++
            d = d.minusDays(1)
        }
        return streak
    }

    fun isCheckedToday(c: Context, userId: Long, goalId: Int): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return today in load(c, userId, goalId)
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 成就墙：unlockedIds + 解锁时间
// ═════════════════════════════════════════════════════════════════════════
object GoalAchievementStore {
    private const val PREF = "goal_achievements"

    val Catalog = listOf(
        Achievement("first_goal", "破冰之始", "创建第一个目标", "🎯"),
        Achievement("first_done", "首战告捷", "完成第一个目标", "🏆"),
        Achievement("five_done", "五连封神", "累计完成 5 个目标", "🌟"),
        Achievement("ten_done", "十全十美", "累计完成 10 个目标", "💎"),
        Achievement("streak_3", "三日不辍", "连续打卡 3 天", "🔥"),
        Achievement("streak_7", "七日连击", "连续打卡 7 天", "⚡"),
        Achievement("streak_30", "月之恒星", "连续打卡 30 天", "🌙"),
        Achievement("first_countdown", "时间使者", "添加第一个倒数日", "⏳")
    )

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun key(userId: Long) = "u${userId}"

    fun load(c: Context, userId: Long): List<Achievement> = runCatching {
        val raw = prefs(c).getString(key(userId), null) ?: return Catalog
        val obj = JSONObject(raw)
        Catalog.map { def ->
            def.copy(unlockedAt = obj.optString(def.id).takeIf { it.isNotBlank() })
        }
    }.getOrElse { Catalog }

    fun unlock(c: Context, userId: Long, id: String): Boolean {
        if (Catalog.none { it.id == id }) return false
        val list = load(c, userId)
        if (list.firstOrNull { it.id == id }?.unlocked == true) return false
        val obj = JSONObject()
        list.forEach { a ->
            val time = if (a.id == id) LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            else a.unlockedAt
            if (!time.isNullOrBlank()) obj.put(a.id, time)
        }
        prefs(c).edit().putString(key(userId), obj.toString()).apply()
        return true
    }

    /**
     * 评估并解锁所有满足条件的成就
     * 返回新解锁的列表
     */
    fun evaluate(
        c: Context,
        userId: Long,
        totalGoals: Int,
        completedGoals: Int,
        countdownTotal: Int
    ): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        val streak = GoalCheckInStore.globalStreak(c, userId)
        val rules: List<Pair<String, Boolean>> = listOf(
            "first_goal" to (totalGoals >= 1),
            "first_done" to (completedGoals >= 1),
            "five_done" to (completedGoals >= 5),
            "ten_done" to (completedGoals >= 10),
            "streak_3" to (streak >= 3),
            "streak_7" to (streak >= 7),
            "streak_30" to (streak >= 30),
            "first_countdown" to (countdownTotal >= 1)
        )
        rules.forEach { (id, ok) ->
            if (ok && unlock(c, userId, id)) {
                Catalog.firstOrNull { it.id == id }?.let { newly.add(it) }
            }
        }
        return newly
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 倒数日提醒：countdownId -> 触发时间戳(ms)
// ═════════════════════════════════════════════════════════════════════════
object CountdownReminderStore {
    private const val PREF = "countdown_reminders"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun key(userId: Long, cid: Int) = "u${userId}_c${cid}"

    fun get(c: Context, userId: Long, cid: Int): Long? {
        val v = prefs(c).getLong(key(userId, cid), -1L)
        return if (v <= 0L) null else v
    }

    fun set(c: Context, userId: Long, cid: Int, triggerAtMs: Long) {
        prefs(c).edit().putLong(key(userId, cid), triggerAtMs).apply()
    }

    fun clear(c: Context, userId: Long, cid: Int) {
        prefs(c).edit().remove(key(userId, cid)).apply()
    }
}
