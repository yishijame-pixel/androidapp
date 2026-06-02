package com.example.funlife.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 🔔 纪念日精确闹钟调度器
 *
 * 在以下时机被调用：
 *   - App 启动（MainActivity.onCreate）
 *   - 添加 / 编辑 / 删除纪念日时
 *   - 设备开机重启后（BootReceiver）
 *
 * 调度规则：
 *   - 每个纪念日下一次到期日的 09:00:00 设一个闹钟
 *   - 每年重复型：取今年/明年的 MM-dd
 *   - 一次性：仅取该日
 *   - 闹钟到点 → 唤起 AnniversaryReminderReceiver → 触发完整提醒
 */
object AnniversaryReminderScheduler {

    private const val TAG = "AnniReminderSched"
    private const val DEFAULT_HOUR = 0        // 00:01 触发（跨日立即提醒，符合用户预期）
    private const val DEFAULT_MIN = 1
    private const val REQUEST_CODE_BASE = 7300000

    /** App 启动时调用：从数据库读取所有纪念日，统一调度
     *  额外：如果今天有纪念日且当前时间已过 09:00 且本进程未触发过 → 立即触发提醒
     */
    fun scheduleAllForUser(context: Context, userId: Long) {
        if (userId <= 0L) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).anniversaryDao()
                val list = dao.getAllForUserOnce(userId)
                android.util.Log.d(TAG, "调度 ${list.size} 个纪念日")
                
                // 1. 调度未来的闹钟
                list.forEach { schedule(context, it) }
                
                // 2. 找出今日纪念日（已到日期但还没触发过的）
                val todays = AnniversaryReminderManager.findTodayAnniversaries(list)
                    .filter { !AnniversaryReminderManager.isTriggered(it.id) }
                if (todays.isNotEmpty()) {
                    android.util.Log.d(TAG, "今日有 ${todays.size} 个未触发纪念日，立即提醒")
                    todays.forEach { AnniversaryReminderManager.markTriggered(it.id) }
                    // 切回主线程触发（涉及 MediaPlayer + UI State）
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        AnniversaryReminderManager.triggerAlarm(context)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "调度全部失败", e)
            }
        }
    }

    /** 调度单个纪念日 */
    fun schedule(context: Context, anniversary: Anniversary) {
        try {
            val triggerMillis = computeNextTriggerMillis(anniversary) ?: run {
                android.util.Log.d(TAG, "纪念日 ${anniversary.name} 无下次触发时间")
                return
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AnniversaryReminderReceiver::class.java).apply {
                putExtra("anniversary_id", anniversary.id)
                putExtra("anniversary_name", anniversary.name)
                putExtra("days_before", 0)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + anniversary.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            // 🔥 用 setAlarmClock() — Android 最高优先级闹钟 API
            // 国产 ROM (MIUI/EMUI/ColorOS) 都强制遵守，应用清后台/Doze 模式都能触发
            // 用户能在状态栏看到小闹钟图标
            try {
                val showIntent = Intent(context, com.example.funlife.MainActivity::class.java)
                val showPi = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_BASE + anniversary.id + 100000,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
                alarmManager.setAlarmClock(info, pi)
                android.util.Log.d(TAG, "✅ setAlarmClock [${anniversary.name}] @ ${java.util.Date(triggerMillis)}")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "setAlarmClock 失败，降级用 setExactAndAllowWhileIdle", e)
                // 降级方案
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerMillis, pi
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerMillis, pi
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMillis, pi
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "调度 ${anniversary.name} 失败", e)
        }
    }

    /** 取消单个纪念日的调度 */
    fun cancel(context: Context, anniversaryId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AnniversaryReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + anniversaryId,
                intent,
                PendingIntent.FLAG_NO_CREATE or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            pi?.let {
                alarmManager.cancel(it)
                it.cancel()
                android.util.Log.d(TAG, "已取消纪念日 #$anniversaryId 的提醒")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "取消失败", e)
        }
    }

    /** 计算下次触发时间（毫秒） */
    private fun computeNextTriggerMillis(anniversary: Anniversary): Long? {
        return try {
            val date = LocalDate.parse(anniversary.date)
            val today = LocalDate.now()
            val time = LocalTime.of(DEFAULT_HOUR, DEFAULT_MIN)
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()

            if (anniversary.isYearly) {
                // 每年重复：取今年/明年的 MM-dd
                var next = date.withYear(today.year)
                var dt = LocalDateTime.of(next, time)
                var millis = dt.atZone(zone).toInstant().toEpochMilli()
                if (millis <= now) {
                    next = next.plusYears(1)
                    dt = LocalDateTime.of(next, time)
                    millis = dt.atZone(zone).toInstant().toEpochMilli()
                }
                millis
            } else {
                val dt = LocalDateTime.of(date, time)
                val millis = dt.atZone(zone).toInstant().toEpochMilli()
                if (millis > now) millis else null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "计算时间失败 [${anniversary.date}]", e)
            null
        }
    }
}
