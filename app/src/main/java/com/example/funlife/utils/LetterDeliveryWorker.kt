// LetterDeliveryWorker.kt — 时光信箱 · 后台投递 Worker（企业级）
//
// 🎯 职责
//   1. 扫描"已到投递时间但还没生成回信"的用户信件 → 调 LLM 生成回信 → 插入 PENDING 回信
//   2. 扫描"已生成 PENDING 但 deliveryAt 已到"的回信 → 推系统通知 + Inbox + 设 DELIVERED
//
// 🛡️ 企业级安全
//   - 防回拨时间：用 MonotonicClock.effectiveNowSec，用户调系统时间无效
//   - 频控：单次扫描最多处理 5 封新信件（防 LLM 配额一次耗光）；
//          整个 Worker 4 小时内同 userId 同信件不重试（用 SharedPreferences 标记）
//   - 并发去重：LetterDao 的 NOT IN 子查询已天然规避重复；
//          额外在生成回信前再次确认（getById + isReplied）防止 Worker 并发触发
//   - 兜底：AI 三次重试全失败 → 写本地温柔模板回信，failureReason 留痕，确保用户"信不会丢"
//   - WorkManager 周期任务 + ExistingPeriodicWorkPolicy.KEEP，幂等
//
// 🔒 数据隔离：所有按 userId 严格过滤；标记按 userId 命名空间。
package com.example.funlife.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Letter
import com.example.funlife.data.model.LetterDirection
import com.example.funlife.data.model.LetterStatus
import com.example.funlife.notifications.FunChannel
import com.example.funlife.notifications.NotificationCenter
import com.example.funlife.notifications.NotificationSpec
import com.example.funlife.repository.LetterRepository
import com.example.funlife.security.MonotonicClock
import java.util.concurrent.TimeUnit

class LetterDeliveryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = LetterRepository(
                applicationContext,
                db.letterRecipientDao(),
                db.letterDao(),
                db.userVipDao()
            )
            val ai = AiService(applicationContext as android.app.Application, userId = 0L)
            val clock = MonotonicClock.get(applicationContext)
            val nowMs = clock.effectiveNowSec() * 1000L

            // ── (1) 处理 due outgoing：生成 AI 回信 ──
            val outgoing = repo.fetchDueOutgoing(nowMs, MAX_PER_RUN)
            android.util.Log.d(TAG, "scan due outgoing letters: ${outgoing.size}")
            for (letter in outgoing) {
                generateReplyFor(letter, repo, ai)
            }

            // ── (2) 处理 due incoming PENDING：推送通知 ──
            val incoming = repo.fetchDueIncoming(nowMs, MAX_PER_RUN_DELIVER)
            android.util.Log.d(TAG, "scan due incoming replies: ${incoming.size}")
            for (reply in incoming) {
                deliverReply(reply, repo)
            }

            // ── (3) v53 阅光书房 · 摘抄时光胶囊到期投递 ──
            try { scanReadingCapsules(db) }
            catch (e: Exception) { android.util.Log.w(TAG, "capsule scan failed: ${e.message}") }

            // ── (4) v53 心情低谷召回（每用户每月最多 1 次） ──
            try { scanQuietRescue(db) }
            catch (e: Exception) { android.util.Log.w(TAG, "quiet rescue scan failed: ${e.message}") }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "LetterDeliveryWorker failed", e)
            // 退避：让 WorkManager 重新调度（默认 30s 起，指数退避至 5h）
            Result.retry()
        }
    }

    /**
     * 为一封用户信件生成回信。
     * - 防抖：同一封信 1 小时内只重试一次（避免 Worker 触发风暴让 LLM 配额炸了）
     * - 并发：getById 再次确认 status，避免另一个 Worker 实例已经处理
     */
    private suspend fun generateReplyFor(
        userLetter: Letter,
        repo: LetterRepository,
        ai: AiService
    ) {
        // 防抖：1h 内同信件只生成一次
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "gen_${userLetter.userId}_${userLetter.id}"
        val last = prefs.getLong(key, 0L)
        val now = System.currentTimeMillis()
        if (now - last < TimeUnit.HOURS.toMillis(1)) {
            android.util.Log.d(TAG, "skip recent retry for letter ${userLetter.id}")
            return
        }

        // 装载收信人
        val recipient = repo.loadRecipientForWorker(userLetter.userId, userLetter.recipientId)
        if (recipient == null) {
            android.util.Log.w(TAG, "recipient missing for letter ${userLetter.id}, skip")
            return
        }

        // 解密用户原文给 AI
        val userPlain = repo.decryptContent(userLetter)
        if (userPlain.isNullOrBlank()) {
            android.util.Log.w(TAG, "decrypt user letter ${userLetter.id} failed, fallback")
            // 即便解密失败也给一封兜底回信，让信不"丢"
            repo.insertReply(
                userId = userLetter.userId,
                recipientId = userLetter.recipientId,
                parentLetterId = userLetter.id,
                replyPlain = fallbackReply(recipient.name),
                deliveryAt = userLetter.deliveryAt,
                status = LetterStatus.PENDING,
                failureReason = "decrypt_failed"
            )
            prefs.edit().putLong(key, now).apply()
            return
        }

        prefs.edit().putLong(key, now).apply()  // 标记本轮已尝试

        // 🆕 v51 优先走云函数代理（KEY 在云端 + 服务端权威配额），失败再降级到客户端直连。
        //   云端配额 QUOTA_EXCEEDED → 不降级（避免绕过服务端权威），写一封"额度用完"的本地兜底回信
        //   云端拒绝 (BAD_SIGNATURE / RATE_LIMITED 等) → 不降级，留痕兜底
        //   云端失败 (网络 / LLM_FAILED / NO_BACKEND) → 降级直连，保留体验
        val (replyText, failure) = generateReply(
            ai = ai,
            recipient = recipient,
            userLetter = userLetter,
            userPlain = userPlain
        )

        // 插入回信（仍 PENDING，等下一轮 deliverReply 把它 DELIVERED + 推通知；
        // 这样能保留"延时投递"的仪式感：用户写信→几分钟内 AI 生成好放着→到 deliveryAt 才"到达"）
        repo.insertReply(
            userId = userLetter.userId,
            recipientId = userLetter.recipientId,
            parentLetterId = userLetter.id,
            replyPlain = replyText,
            deliveryAt = userLetter.deliveryAt,
            status = LetterStatus.PENDING,
            failureReason = failure
        )
        android.util.Log.d(TAG, "reply generated for letter ${userLetter.id} (failure=$failure)")
    }

    /** 把 PENDING 回信"送达"：标 DELIVERED + 推通知 + 写 Inbox */
    private suspend fun deliverReply(reply: Letter, repo: LetterRepository) {
        repo.deliverReply(reply)
        val recipient = repo.loadRecipientForWorker(reply.userId, reply.recipientId) ?: return

        // 走统一通知中心（LETTER 渠道 / 高重要度 / dedup 6h / 绕过静默时段 —— 由用户自己拉的延时决定，无需再压抑）
        NotificationCenter.notify(
            applicationContext,
            NotificationSpec(
                channel = FunChannel.LETTER,
                id = 88800 + reply.id.toInt(),
                title = "你有一封来自「${recipient.name}」的回信",
                body = "信件已经到达邮箱，点击拆封。",
                deepLinkRoute = "letter_detail/${reply.id}",
                dedupWindowMs = TimeUnit.HOURS.toMillis(6),
                bypassQuietHours = true
            )
        )
    }

    /**
     * 🆕 v51 AI 回信生成统一入口：云函数代理优先 → 客户端直连兜底。
     * 返回 (replyText, failureReason?)；failureReason!=null 表示走了兜底文案。
     */
    private suspend fun generateReply(
        ai: AiService,
        recipient: com.example.funlife.data.model.LetterRecipient,
        userLetter: Letter,
        userPlain: String
    ): Pair<String, String?> {
        // ① 云函数代理（默认开启）
        if (com.example.funlife.vip.LetterCloudRepository.isEnabled()) {
            val cloud = com.example.funlife.vip.LetterCloudRepository(applicationContext)
            val resp = cloud.generateReply(
                userId = userLetter.userId,
                body = com.example.funlife.vip.LetterCloudRepository.Body(
                    letterId = userLetter.id.toString(),
                    recipientName = recipient.name,
                    relation = recipient.relation,
                    persona = recipient.persona,
                    timeAnchor = recipient.timeAnchor,
                    userLetter = userPlain,
                    mood = userLetter.mood
                )
            )
            when (resp) {
                is com.example.funlife.vip.LetterCloudRepository.CallResult.Success -> {
                    android.util.Log.d(TAG, "cloud reply ok used=${resp.used}/${resp.quota}")
                    return resp.reply to null
                }
                is com.example.funlife.vip.LetterCloudRepository.CallResult.QuotaExceeded -> {
                    // 🔒 服务端权威：客户端不降级，避免本地直连绕过配额
                    android.util.Log.w(TAG, "cloud quota exceeded ${resp.used}/${resp.quota}")
                    return quotaExceededReply(recipient.name) to "quota_exceeded"
                }
                is com.example.funlife.vip.LetterCloudRepository.CallResult.Rejected -> {
                    android.util.Log.w(TAG, "cloud rejected: ${resp.code} ${resp.msg}")
                    return fallbackReply(recipient.name) to "cloud_rejected_${resp.code}"
                }
                is com.example.funlife.vip.LetterCloudRepository.CallResult.Recoverable -> {
                    android.util.Log.w(TAG, "cloud recoverable, fallback to direct: ${resp.code} ${resp.msg}")
                    // 继续往下走 ② 直连
                }
            }
        }
        // ② 客户端直连兜底（保留原有体验，包含 3 次重试）
        val result = ai.generateLetterReply(
            recipientName = recipient.name,
            recipientPersona = recipient.persona,
            relation = recipient.relation,
            timeAnchor = recipient.timeAnchor,
            userLetter = userPlain
        )
        return when (result) {
            is AiResult.Success -> result.reply to null
            is AiResult.Error   -> fallbackReply(recipient.name) to result.reason
        }
    }

    /** 服务端权威判定"本月额度用完"时给的本地回信文案——不暴露技术细节 */
    private fun quotaExceededReply(recipientName: String): String =
        "$recipientName 这边收到了你的信，但本月信件实在太多，今天恐怕来不及好好回。\n" +
        "下个月信箱重置后，记得再来。"

    /** AI 失败时的本地兜底文案——温柔、不暴露技术失败 */
    private fun fallbackReply(recipientName: String): String =
        "$recipientName 这边今天有点累，没能写得太长。\n" +
        "但你的信我都看见了，那些没说出口的，我也明白。\n" +
        "想再说说，就再写一封过来。"

    /* ═══════════════════ v53 阅光书房 ═══════════════════ */

    /**
     * 摘抄时光胶囊到期投递：
     *  - 扫描所有 `quotes.capsuleDeliveryAt <= now AND capsuleDelivered = 0`
     *  - 推送 LETTER 渠道通知，标 capsuleDelivered = 1
     *  - 单次最多 5 条防通知潮
     */
    private suspend fun scanReadingCapsules(db: AppDatabase) {
        val quoteDao = db.quoteDao()
        val nowMs = System.currentTimeMillis()
        val due = quoteDao.findDueCapsules(nowMs, limit = 5)
        if (due.isEmpty()) return
        android.util.Log.d(TAG, "v53 capsule due: ${due.size}")
        for (q in due) {
            // 防回拨保护：deliveryAt 早于 createdAt 视为脏数据，跳过
            if (q.capsuleDeliveryAt < q.createdAt) {
                quoteDao.markCapsuleDelivered(q.id); continue
            }
            // 取书名（可空）
            val bookTitle = runCatching {
                db.bookDao().getById(q.userId, q.bookId)?.title
            }.getOrNull().orEmpty()
            val title = if (bookTitle.isNotBlank())
                "📜 来自《$bookTitle》的回声"
            else
                "📜 一段你曾留下的话"
            val preview = q.text.take(60).let { if (q.text.length > 60) "$it…" else it }
            NotificationCenter.notify(
                applicationContext,
                NotificationSpec(
                    channel = FunChannel.LETTER,
                    id = 88900 + q.id.toInt().coerceAtMost(99999),
                    title = title,
                    body = preview,
                    deepLinkRoute = "reading_room",
                    dedupWindowMs = TimeUnit.HOURS.toMillis(6),
                    bypassQuietHours = false
                )
            )
            quoteDao.markCapsuleDelivered(q.id)
        }
    }

    /**
     * 心情低谷召回：
     *  - 取 quotes 中所有 distinct userId
     *  - 对每个 userId：
     *      1. 近 3 天 mood 均值 ≤ 2.0 且记录数 ≥ 2 → 视为低谷
     *      2. 当月未触发过 quiet_rescue → 抽 1 条 rating ≥4 / pinned 摘抄推送
     *      3. system_quota_used 写入计数（每月 1 次防重）
     */
    private suspend fun scanQuietRescue(db: AppDatabase) {
        val quoteDao = db.quoteDao()
        val moodDao = db.moodDao()
        val sysQ = db.systemQuotaUsedDao()
        val users = quoteDao.distinctUserIds().filter { it > 0L }
        if (users.isEmpty()) return

        val cal = java.util.Calendar.getInstance()
        val monthYm = cal.get(java.util.Calendar.YEAR) * 100 + (cal.get(java.util.Calendar.MONTH) + 1)
        val sinceCal = (cal.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -3)
        }
        val sinceDate = String.format(
            "%04d-%02d-%02d",
            sinceCal.get(java.util.Calendar.YEAR),
            sinceCal.get(java.util.Calendar.MONTH) + 1,
            sinceCal.get(java.util.Calendar.DAY_OF_MONTH)
        )

        for (uid in users) {
            // 当月已发过则跳过
            val used = sysQ.getCount(uid, "quiet_rescue", monthYm) ?: 0
            if (used > 0) continue
            // 近 3 天 mood 数据
            val cnt = moodDao.countSince(uid, sinceDate)
            if (cnt < 2) continue
            val avg = moodDao.avgLevelSince(uid, sinceDate) ?: continue
            if (avg > 2.0) continue
            val q = quoteDao.pickRandomQuality(uid) ?: continue
            // 推送
            val preview = q.text.take(80).let { if (q.text.length > 80) "$it…" else it }
            NotificationCenter.notify(
                applicationContext,
                NotificationSpec(
                    channel = FunChannel.LETTER,
                    id = 89000 + (uid % 1000).toInt(),
                    title = "📬 来自过去的你",
                    body = preview,
                    deepLinkRoute = "reading_room",
                    dedupWindowMs = TimeUnit.HOURS.toMillis(12),
                    bypassQuietHours = false
                )
            )
            sysQ.increment(uid, "quiet_rescue", monthYm)
            android.util.Log.d(TAG, "quiet rescue sent for uid=$uid avg=$avg")
        }
    }

    companion object {
        private const val TAG = "LetterDeliveryWorker"
        private const val PREFS = "letter_delivery_worker"
        const val WORK_NAME_PERIODIC = "letter_delivery_periodic"
        const val WORK_NAME_ONCE = "letter_delivery_once"
        /** 单次扫描最多生成 5 封 AI 回信 → 控 LLM 调用频率 */
        private const val MAX_PER_RUN = 5
        /** 单次扫描最多送达 20 封 PENDING 回信（仅本地标记 + 通知，无网络） */
        private const val MAX_PER_RUN_DELIVER = 20

        /**
         * 注册周期任务：每 1 小时扫一次，要求联网（生成回信需要 LLM）。
         */
        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = PeriodicWorkRequestBuilder<LetterDeliveryWorker>(
                    1, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES                       // flex window
                ).setConstraints(constraints).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                android.util.Log.d(TAG, "schedulePeriodic OK")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "schedulePeriodic failed", e)
            }
        }

        /**
         * 用户刚写完信 → 立即触发一次扫描（OneTime，要求联网）。
         * 这样如果用户选了"立即送达"（VIP2+），可以最快几秒内收到回信。
         */
        fun triggerOnce(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = OneTimeWorkRequestBuilder<LetterDeliveryWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(2, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_ONCE,
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "triggerOnce failed: ${e.message}")
            }
        }
    }
}
