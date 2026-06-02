package com.example.funlife.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 防回拨时间锚（Anti-Rollback Clock）
 *
 * 用途：
 *   防止用户在系统设置里把时间调到过去，让 VIP 永不过期 / 每日奖励反复领。
 *
 * 原理：
 *   - 每次有可信时间样本（启动一次 / 网络响应 Date 头 / cert.issuedAt）都喂入
 *   - 本地持续记录"见过的最大时间戳"
 *   - 业务侧用 [effectiveNowSec] / [effectiveToday] 代替 System.currentTimeMillis / LocalDate.now
 *   - 用户回拨系统时间也不能让"有效时间"倒流
 *
 * 注意：
 *   - 用 EncryptedSharedPreferences 持久化，普通用户改不了
 *   - root 用户仍能改，但门槛已经很高
 */
class MonotonicClock private constructor(context: Context) {

    private val prefs = try {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext, "monotonic_clock", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.applicationContext.getSharedPreferences("monotonic_clock_fb", Context.MODE_PRIVATE)
    }

    /** 喂入一个可信时间戳（秒）。通常来自服务端 cert.issuedAt 或 HTTP Date 头 */
    fun feed(serverEpochSec: Long) {
        if (serverEpochSec <= 0) return
        val current = prefs.getLong(KEY_MAX, 0L)
        // 🔒 防极端污染：单次跳跃不允许超过 1 年（拒绝服务端故障/伪造的离谱未来值）
        val sysNow = System.currentTimeMillis() / 1000
        val baseline = maxOf(current, sysNow)
        val maxAcceptable = baseline + 366L * 86400L
        if (serverEpochSec > maxAcceptable) {
            android.util.Log.w("MonotonicClock", "拒绝异常时间锚 $serverEpochSec（超过基线+1年 $maxAcceptable）")
            return
        }
        if (serverEpochSec > current) {
            prefs.edit().putLong(KEY_MAX, serverEpochSec).apply()
        }
    }

    /** 自启动时调用一次：把系统时间也喂入（仅当大于历史最大值才更新） */
    fun bootstrap() {
        feed(System.currentTimeMillis() / 1000)
    }

    /** 获取"防回拨的当前时间（秒）" = max(系统时间, 历史最大) */
    fun effectiveNowSec(): Long {
        val sys = System.currentTimeMillis() / 1000
        val max = prefs.getLong(KEY_MAX, 0L)
        return maxOf(sys, max)
    }

    /** 获取"防回拨的今天" ISO 字符串（如 "2024-01-15"） */
    fun effectiveToday(): String {
        val sec = effectiveNowSec()
        return try {
            Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        } catch (e: Exception) {
            LocalDate.now().toString()
        }
    }

    /**
     * 检测系统时间是否被回拨。
     * @return true 若系统当前时间比历史最大值小超过 [toleranceSec]
     */
    fun isClockRolledBack(toleranceSec: Long = 86400): Boolean {
        val sys = System.currentTimeMillis() / 1000
        val max = prefs.getLong(KEY_MAX, 0L)
        return max > 0 && sys + toleranceSec < max
    }

    companion object {
        private const val KEY_MAX = "max_seen_ts"

        @Volatile private var INSTANCE: MonotonicClock? = null
        fun get(context: Context): MonotonicClock {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MonotonicClock(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
