package com.example.funlife.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.funlife.MainActivity
import com.example.funlife.R
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * 🔔 纪念日闹钟提醒管理器
 * - 检查今日是否有纪念日
 * - 触发：循环震动 + 循环铃声 + Heads-up 顶部通知
 */
object AnniversaryReminderManager {

    private const val CHANNEL_ID = "anniversary_reminder_channel"
    private const val NOTIFICATION_ID = 88520

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val triggeredToday = mutableSetOf<Int>()
    
    // 🎀 App 内悬浮提醒条全局状态
    private val _bannerVisible = MutableStateFlow(false)
    val bannerVisible: StateFlow<Boolean> = _bannerVisible.asStateFlow()
    private val _bannerCount = MutableStateFlow(0)
    val bannerCount: StateFlow<Int> = _bannerCount.asStateFlow()
    
    fun showInAppBanner(count: Int) {
        _bannerCount.value = count
        _bannerVisible.value = true
    }
    
    fun dismissInAppBanner() {
        _bannerVisible.value = false
    }

    fun findTodayAnniversaries(anniversaries: List<Anniversary>): List<Anniversary> {
        val today = LocalDate.now()
        return anniversaries.filter { a ->
            try {
                val d = LocalDate.parse(a.date)
                if (a.isYearly) {
                    d.monthValue == today.monthValue && d.dayOfMonth == today.dayOfMonth
                } else {
                    d == today
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 触发提醒：循环震动 + 循环铃声 + Heads-up 通知
     * 在用户主动 stopAlarm 之前会一直响
     */
    fun triggerAlarm(context: Context, durationMs: Long = 0L) {
        try {
            startVibration(context)
            startAlarmSound(context)
            // 同时发出顶部 heads-up 通知（即使用户切到其他页面也能看到）
            val reminderCount = triggeredToday.size.coerceAtLeast(1)
            showHeadsUpNotification(context, reminderCount)
            // App 内顶部悬浮提醒条（永远不自动消失）
            showInAppBanner(reminderCount)
            // 全局悬浮窗（如有权限），可与 App 内 Banner 同时显示
            try {
                OverlayBannerService.start(context, reminderCount)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            android.util.Log.e("AnniversaryReminder", "触发提醒失败", e)
        }
    }

    private fun startVibration(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        if (v.hasVibrator()) {
            // 循环震动：长短长短长（庆祝感）
            val pattern = longArrayOf(0, 600, 200, 300, 200, 600, 300, 800, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 255, 0, 200, 0, 255, 0, 255, 0)
                v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))  // repeat=0 → 循环
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)  // repeat=0 → 循环
            }
        }
    }

    private fun startAlarmSound(context: Context) {
        try {
            stopMediaPlayer()
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                setOnPreparedListener {
                    try { it.start() } catch (_: Exception) {}
                }
                prepareAsync()  // 异步准备，避免阻塞
            }
        } catch (e: Exception) {
            android.util.Log.e("AnniversaryReminder", "播放铃声失败", e)
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.apply {
                try { if (isPlaying) stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    /**
     * 用户主动关闭：停止震动 + 停止铃声
     * 注意：不主动取消通知，等用户自己手动滑掉
     */
    fun stopAlarm(context: Context? = null) {
        stopMediaPlayer()
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {}
        // 不取消通知！让通知留在状态栏，用户手动滑掉
    }

    // ════════════════════════════════════════════════
    // 🔔 Heads-up 顶部通知
    // ════════════════════════════════════════════════
    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "纪念日提醒",
                    NotificationManager.IMPORTANCE_HIGH  // HIGH → 弹出 Heads-up
                ).apply {
                    description = "今日纪念日到达时的庆祝提醒"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 200, 300, 200, 600, 300, 800)
                    enableLights(true)
                    lightColor = 0xFFEC407A.toInt()
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun showHeadsUpNotification(context: Context, count: Int) {
        try {
            createChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 点击通知 → 打开主 Activity（先停止闹钟）
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val openPi = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
            )
            // 滑掉通知 / 点"停止" → 触发停止闹钟
            val stopIntent = Intent(context, com.example.funlife.utils.StopAlarmReceiver::class.java)
            val stopPi = PendingIntent.getBroadcast(
                context, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🎀 今日纪念日提醒")
                .setContentText("✨ 你有 $count 个值得庆祝的日子！点击查看 💗")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("✨ 你有 $count 个值得庆祝的日子！\n点击查看详情，让美好瞬间不被遗忘 💗")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setContentIntent(openPi)
                .setDeleteIntent(stopPi)
                .addAction(0, "🔕 停止铃声", stopPi)
                .setColor(0xFFEC407A.toInt())
                .setColorized(true)
            // 🔥 全屏意图（仅在用户授权时才挂上，否则会被系统直接丢弃整条通知）
            val canUseFullScreen = if (Build.VERSION.SDK_INT >= 34) {
                try { nm.canUseFullScreenIntent() } catch (_: Exception) { false }
            } else true
            if (canUseFullScreen) {
                builder.setFullScreenIntent(openPi, true)
            }
            val notif = builder.build()

            nm.notify(NOTIFICATION_ID, notif)
            // 写入应用内收件箱
            runCatching {
                com.example.funlife.notifications.InboxStore.add(
                    context,
                    com.example.funlife.notifications.FunChannel.ANNIVERSARY,
                    "🎀 今日纪念日提醒",
                    "你有 $count 个值得庆祝的日子！点击查看 💗",
                    "anniversary"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AnniversaryReminder", "Heads-up 通知失败", e)
        }
    }

    fun markTriggered(anniversaryId: Int) { triggeredToday.add(anniversaryId) }
    fun isTriggered(anniversaryId: Int): Boolean = anniversaryId in triggeredToday
    fun clearTriggered() { triggeredToday.clear() }
}
