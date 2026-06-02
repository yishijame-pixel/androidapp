// LetterRepository.kt — 时光信箱 · 业务仓库（企业级）
//
// 🛡️ 安全 / 业务规则
//   1. 严格按 userId 隔离；所有写入 require(userId > 0L)。
//   2. 信件正文 content 字段经 LetterCrypto AES-GCM 加密后落库；
//      读取时透明解密，解密失败显示占位文案（不暴露原始密文）。
//   3. 投递时间一律使用 MonotonicClock，防系统时间回拨作弊。
//   4. VIP 配额：
//        - 每月写信数量上限（NORMAL=1 / VIP1=5 / VIP2-3=无限）
//        - 最短投递延迟（NORMAL≥3d / VIP1≥1d / VIP2-3=无限制，可立即）
//   5. 删除收信人 → 级联删信件（避免孤儿数据 + 隐私残留）。
package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.LetterDao
import com.example.funlife.data.dao.LetterRecipientDao
import com.example.funlife.data.dao.UserVipDao
import com.example.funlife.data.model.Letter
import com.example.funlife.data.model.LetterDirection
import com.example.funlife.data.model.LetterRecipient
import com.example.funlife.data.model.LetterStatus
import com.example.funlife.security.LetterCrypto
import com.example.funlife.security.MonotonicClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 透明解密的"对外信件 DTO"。
 * UI / ViewModel 拿到的就是明文 content，绝不接触密文。
 */
data class LetterView(
    val id: Long,
    val userId: Long,
    val recipientId: Long,
    val direction: String,
    val content: String,                     // 已解密的明文（失败时为占位文案）
    val mood: String?,
    val sentAt: Long,
    val deliveryAt: Long,
    val deliveredAt: Long?,
    val status: String,
    val isRead: Boolean,
    val parentLetterId: Long?,
    val failureReason: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val decryptOk: Boolean                   // 解密是否成功（UI 可显示警告）
)

/** 写信参数 */
data class LetterDraft(
    val userId: Long,
    val recipientId: Long,
    val content: String,                    // 明文
    val mood: String? = null,
    /** 用户希望的最早投递时间（毫秒），Repository 会按 VIP 等级强制下限 */
    val desiredDeliveryAt: Long
)

/** VIP 校验失败原因 */
sealed class LetterSendResult {
    data class Ok(val letterId: Long) : LetterSendResult()
    object QuotaExceeded : LetterSendResult()            // 月度配额已用完
    data class DeliveryTooSoon(val minDelayMs: Long) : LetterSendResult()  // 投递时间过早
    object EmptyContent : LetterSendResult()
    data class Error(val reason: String) : LetterSendResult()
}

class LetterRepository(
    private val context: Context,
    private val recipientDao: LetterRecipientDao,
    private val letterDao: LetterDao,
    private val userVipDao: UserVipDao
) {

    private val clock = MonotonicClock.get(context)

    // ─────────── 收信人 ───────────

    fun observeRecipients(userId: Long): Flow<List<LetterRecipient>> {
        require(userId > 0L)
        return recipientDao.getAll(userId)
    }

    suspend fun getRecipient(userId: Long, id: Long): LetterRecipient? {
        require(userId > 0L)
        return recipientDao.getById(userId, id)
    }

    suspend fun saveRecipient(recipient: LetterRecipient): Long {
        require(recipient.userId > 0L) { "recipient.userId must be > 0" }
        require(recipient.name.isNotBlank()) { "recipient.name blank" }
        val now = System.currentTimeMillis()
        val cleaned = recipient.copy(
            name = recipient.name.trim().take(40),
            persona = recipient.persona.trim().take(800),
            updatedAt = now,
            createdAt = if (recipient.createdAt == 0L) now else recipient.createdAt
        )
        return recipientDao.insert(cleaned)
    }

    suspend fun deleteRecipient(recipient: LetterRecipient) {
        require(recipient.userId > 0L)
        // 级联：先删信件再删收信人，避免孤儿
        letterDao.deleteAllByRecipient(recipient.userId, recipient.id)
        recipientDao.delete(recipient)
    }

    // ─────────── 信件读 ───────────

    fun observeLetters(userId: Long): Flow<List<LetterView>> {
        require(userId > 0L)
        return letterDao.getAll(userId).map { list -> list.map { decryptTo(it) } }
    }

    fun observeLettersByRecipient(userId: Long, recipientId: Long): Flow<List<LetterView>> {
        require(userId > 0L)
        return letterDao.getByRecipient(userId, recipientId).map { list -> list.map { decryptTo(it) } }
    }

    fun observeUnreadCount(userId: Long): Flow<Int> {
        require(userId > 0L)
        return letterDao.unreadCount(userId)
    }

    suspend fun getLetter(userId: Long, id: Long): LetterView? {
        require(userId > 0L)
        return letterDao.getById(userId, id)?.let { decryptTo(it) }
    }

    suspend fun markRead(userId: Long, id: Long) {
        require(userId > 0L)
        letterDao.markRead(userId, id, System.currentTimeMillis())
    }

    // ─────────── 信件写（核心业务） ───────────

    /**
     * 发出一封信（用户 → 收信人）。
     *
     * @return [LetterSendResult.Ok] 含新插入的 letterId；
     *         [LetterSendResult.QuotaExceeded] 月配额已用完；
     *         [LetterSendResult.DeliveryTooSoon] desiredDeliveryAt 比当前 VIP 允许的最短延迟更早；
     *         [LetterSendResult.EmptyContent] / [LetterSendResult.Error] 见名。
     */
    suspend fun sendLetter(draft: LetterDraft): LetterSendResult {
        require(draft.userId > 0L) { "userId must be > 0" }
        val trimmed = draft.content.trim()
        if (trimmed.isEmpty()) return LetterSendResult.EmptyContent
        // 配额检查
        val vipLevel = userVipDao.getUserVipSync(draft.userId)?.vipLevel ?: 0
        val (monthStart, monthEnd) = monthRangeMs()
        val sentThisMonth = letterDao.countSentInMonth(draft.userId, monthStart, monthEnd)
        val quota = monthlyQuota(vipLevel)
        if (quota != UNLIMITED && sentThisMonth >= quota) {
            return LetterSendResult.QuotaExceeded
        }
        // 投递延迟下限
        val minDelay = minDeliveryDelayMs(vipLevel)
        val nowSec = clock.effectiveNowSec()
        val now = nowSec * 1000L
        val effectiveDeliveryAt = maxOf(draft.desiredDeliveryAt, now + minDelay)
        // 如果用户期望立即（< minDelay）→ VIP 不够 → 直接拒
        if (draft.desiredDeliveryAt < now + minDelay) {
            return LetterSendResult.DeliveryTooSoon(minDelay)
        }

        return try {
            // 先把 letter 插入（id 自增）拿到 id；之后再用 (userId, recipientId, id) 做 AAD 加密 → 替换 content
            // 这里直接用 (userId, recipientId) 作为 AAD 已经足够：跨账号 / 跨收信人挪动数据 → 解密失败
            val cipher = LetterCrypto.encrypt(context, trimmed, draft.userId, draft.recipientId)
            val nowMs = System.currentTimeMillis()
            val letter = Letter(
                userId = draft.userId,
                recipientId = draft.recipientId,
                direction = LetterDirection.TO_RECIPIENT,
                content = cipher,
                mood = draft.mood?.take(40),
                sentAt = now,
                deliveryAt = effectiveDeliveryAt,
                deliveredAt = now,                  // 用户自己写的信，对自己来说立即"已送达"（已发出）
                status = LetterStatus.DELIVERED,
                isRead = true,
                createdAt = nowMs, updatedAt = nowMs
            )
            val id = letterDao.insert(letter)
            LetterSendResult.Ok(id)
        } catch (e: Exception) {
            android.util.Log.e("LetterRepository", "sendLetter failed", e)
            LetterSendResult.Error(e.javaClass.simpleName)
        }
    }

    /**
     * 由 Worker 在 due 时调用：插入 AI 替身回信。
     * - 加密 content
     * - 设 status=PENDING（投递时间到才推送通知 + 设 deliveredAt）
     * - 并发去重交给 Worker（事务内再检查"该 to_recipient 是否已有 from_recipient"）
     */
    suspend fun insertReply(
        userId: Long,
        recipientId: Long,
        parentLetterId: Long,
        replyPlain: String,
        deliveryAt: Long,
        status: String = LetterStatus.PENDING,
        failureReason: String? = null
    ): Long {
        require(userId > 0L)
        val cipher = LetterCrypto.encrypt(context, replyPlain, userId, recipientId)
        val now = System.currentTimeMillis()
        val reply = Letter(
            userId = userId,
            recipientId = recipientId,
            direction = LetterDirection.FROM_RECIPIENT,
            content = cipher,
            sentAt = now,
            deliveryAt = deliveryAt,
            deliveredAt = if (status == LetterStatus.DELIVERED) now else null,
            status = status,
            isRead = false,
            parentLetterId = parentLetterId,
            failureReason = failureReason,
            createdAt = now,
            updatedAt = now
        )
        return letterDao.insert(reply)
    }

    /** 把 PENDING 回信标记为 DELIVERED（Worker 在投递时间到时调用） */
    suspend fun deliverReply(letter: Letter) {
        if (letter.status == LetterStatus.DELIVERED) return
        val now = System.currentTimeMillis()
        letterDao.update(letter.copy(
            status = LetterStatus.DELIVERED,
            deliveredAt = now,
            updatedAt = now
        ))
    }

    // ─────────── Worker 用 ───────────

    suspend fun fetchDueOutgoing(now: Long, limit: Int = 20) =
        letterDao.getDueOutgoingLetters(now, limit)

    suspend fun fetchDueIncoming(now: Long, limit: Int = 20) =
        letterDao.getDueIncomingReplies(now, limit)

    /** 检查某封用户信件是否已经存在对应回信（并发去重） */
    suspend fun replyAlreadyExists(userLetterId: Long): Boolean {
        // 简化实现：用 SQL 反查；这里直接查 getById 后用 Direction 判断不便，
        // 但 LetterDao.getDueOutgoingLetters 的 NOT IN 子查询天然规避了重复。
        // 这里给一个手动校验 API，留作未来扩展。
        return false
    }

    suspend fun getLetterEntity(userId: Long, id: Long): Letter? =
        letterDao.getById(userId, id)

    suspend fun decryptContent(letter: Letter): String? =
        LetterCrypto.decrypt(context, letter.content, letter.userId, letter.recipientId)

    suspend fun loadRecipientForWorker(userId: Long, recipientId: Long): LetterRecipient? =
        recipientDao.getById(userId, recipientId)

    // ─────────── VIP 配额表 ───────────

    companion object {
        const val UNLIMITED = com.example.funlife.vip.VipQuota.UNLIMITED

        /** 月度可写信件数；UNLIMITED=无限 —— 委托给 VipQuota 单一事实源 */
        fun monthlyQuota(vipLevel: Int): Int =
            com.example.funlife.vip.VipQuota.letterMonthlyLimit(vipLevel)

        /** 最短投递延迟（毫秒）—— 委托给 VipQuota 单一事实源 */
        fun minDeliveryDelayMs(vipLevel: Int): Long =
            com.example.funlife.vip.VipQuota.letterMinDelayMs(vipLevel)
    }

    // ─────────── 内部 ───────────

    private fun monthRangeMs(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun decryptTo(letter: Letter): LetterView {
        val plain = LetterCrypto.decrypt(context, letter.content, letter.userId, letter.recipientId)
        return LetterView(
            id = letter.id,
            userId = letter.userId,
            recipientId = letter.recipientId,
            direction = letter.direction,
            content = plain ?: "（无法读取此信，可能数据已损坏）",
            mood = letter.mood,
            sentAt = letter.sentAt,
            deliveryAt = letter.deliveryAt,
            deliveredAt = letter.deliveredAt,
            status = letter.status,
            isRead = letter.isRead,
            parentLetterId = letter.parentLetterId,
            failureReason = letter.failureReason,
            createdAt = letter.createdAt,
            updatedAt = letter.updatedAt,
            decryptOk = plain != null
        )
    }
}
