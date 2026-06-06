package com.example.funlife.vip

import android.content.Context
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserVip
import com.example.funlife.security.VipSecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 云端 VIP 系统对外的"门面"。
 *
 * 与现有 [com.example.funlife.repository.VipRepository] 共存（不破坏）：
 *   - 旧的本地 redeemCode 入口保留为"赠送码 / 演示"模式
 *   - 新的云端流程通过本类调用
 *
 * 主要职责：
 *   1. 调云端 → 拿凭证 → 验签 → 落本地 UserVip 表
 *   2. 启动时检查凭证，无效则清除 VIP 状态
 *   3. 定期复验（7 天间隔）
 */
class VipManager(private val context: Context) {

    private val cloud = VipCloudRepository(context)
    private val store = VipCertificateStore(context)
    private val securityValidator = VipSecurityValidator(context)
    private val db = AppDatabase.getDatabase(context)

    sealed class Outcome {
        data class Success(
            val cert: VipCertificate,
            val isReissue: Boolean,
            val coinsGranted: Int
        ) : Outcome()
        data class Failure(val code: String, val msg: String) : Outcome()
    }

    /** 兑换卡密 → 拉云端 → 验签 → 写本地 UserVip 或 AI 卡凭证 */
    suspend fun redeem(userId: Long, code: String): Outcome = withContext(Dispatchers.IO) {
        val res = cloud.redeem(code, userId)
        handleCertResponse(userId, res, isMigrate = false, redeemCode = code)
    }

    /** 聊天页专用：激活 AI 额度卡（非 VIP 卡会返回失败） */
    suspend fun redeemChatAi(userId: Long, code: String): Outcome = withContext(Dispatchers.IO) {
        val res = cloud.redeem(code, userId)
        if (!res.ok || res.certificate == null) {
            return@withContext Outcome.Failure(res.code ?: "UNKNOWN", res.msg ?: "未知错误")
        }
        if (!ChatAiSku.isChatAiCert(res.certificate)) {
            return@withContext Outcome.Failure(
                "WRONG_TYPE",
                "此为 VIP 专用卡，请前往会员页兑换"
            )
        }
        handleCertResponse(userId, res, isMigrate = false, redeemCode = code)
    }

    /** 当前用户的 AI 卡凭证（不含 VIP 凭证） */
    fun getChatAiCertOrNull(userId: Long): VipCertificate? =
        store.loadChatAi(userId)?.first

    /** 设备迁移：用户在新设备上恢复 VIP
     *  @param oldDeviceId 原设备指纹（由用户从原设备「导出迁移凭证」拿到），必填
     */
    suspend fun migrate(userId: Long, code: String, oldDeviceId: String): Outcome = withContext(Dispatchers.IO) {
        // 取当前用户的用户名 → 拼凑 device_token 鉴权
        val username = try { db.userDao().getUserById(userId)?.username.orEmpty() } catch (e: Exception) { "" }
        if (username.isBlank()) return@withContext Outcome.Failure("NO_USER", "未登录账号，无法迁移")
        val res = cloud.migrate(username, code, oldDeviceId)
        handleCertResponse(userId, res, isMigrate = true, redeemCode = code)
    }

    /** 取当前用户已激活的凭证（用于"导出迁移凭证"） */
    fun getCurrentCertOrNull(userId: Long): VipCertificate? = store.load(userId)?.first

    /** 取当前用户兑换时用的 code（用于"导出迁移凭证"） */
    fun getCurrentRedeemCodeOrNull(userId: Long): String? = store.loadRedeemCode(userId)

    /** 启动时调用：本地验签 → 决定是否保持 VIP */
    suspend fun validateOnStartup(userId: Long): VipCertificateValidator.Result = withContext(Dispatchers.IO) {
        val pair = store.load(userId) ?: return@withContext VipCertificateValidator.Result.EMPTY
        val (cert, sig) = pair
        val result = VipCertificateValidator.validate(context, cert, sig)
        if (result != VipCertificateValidator.Result.VALID) {
            // 凭证失效 → 清掉本地 VIP（保留凭证文件以便用户看到错误原因）
            downgradeLocalVip(userId)
        }
        result
    }

    /** 每 7 天后台联网复验一次（建议 WorkManager 调用） */
    suspend fun reverify(userId: Long): Boolean = withContext(Dispatchers.IO) {
        val pair = store.load(userId) ?: return@withContext false
        val (cert, sig) = pair
        val res = cloud.verify(cert, sig)
        if (res.ok && res.certificate != null && res.signature != null) {
            // 🔒 必须先本地验签 + 校验设备绑定，防中间人篡改新 cert 喂污染时钟/换设备
            val v = VipCertificateValidator.validate(context, res.certificate, res.signature)
            if (v != VipCertificateValidator.Result.VALID) {
                android.util.Log.w("VipManager", "reverify 收到的新 cert 验签失败: $v")
                // 不 feed、不存储，但保留旧凭证；下次再试
                return@withContext true
            }
            // validate 内部已经 feed 过 issuedAt，这里直接存
            store.save(userId, res.certificate, res.signature)
            true
        } else if (res.code == "NETWORK_ERROR" || res.code?.startsWith("HTTP_") == true) {
            // 🔒 网络失败的离线宽限：cert.issuedAt 距今超过 OFFLINE_GRACE_DAYS 仍未续期 → 降级
            //    防止用户切飞行模式 + 改时间永久白嫖 VIP
            val clock = com.example.funlife.security.MonotonicClock.get(context)
            val daysSinceIssued = (clock.effectiveNowSec() - cert.issuedAt) / 86400
            if (daysSinceIssued > OFFLINE_GRACE_DAYS) {
                android.util.Log.w("VipManager", "离线超过 $OFFLINE_GRACE_DAYS 天未续期，降级 VIP")
                downgradeLocalVip(userId)
                false
            } else {
                true // 短暂网络问题给宽限
            }
        } else {
            // 云端明确拒绝 → 降级
            downgradeLocalVip(userId)
            store.clear(userId)
            false
        }
    }

    /** 退出登录 / 切换用户时调用，仅清当前用户的凭证 */
    fun clearCertificate(userId: Long) {
        store.clear(userId)
    }

    // ─── 内部 ───

    private suspend fun handleCertResponse(
        userId: Long,
        res: VipCertResponse,
        isMigrate: Boolean,
        redeemCode: String? = null,
    ): Outcome {
        if (!res.ok || res.certificate == null || res.signature == null) {
            return Outcome.Failure(res.code ?: "UNKNOWN", res.msg ?: "未知错误")
        }
        val cert = res.certificate
        val sig = res.signature

        // 本地验签（防止云端响应被中间人篡改）
        val validate = VipCertificateValidator.validate(context, cert, sig)
        if (validate != VipCertificateValidator.Result.VALID) {
            return Outcome.Failure("VALIDATE_FAILED", "凭证验签失败: $validate")
        }

        // chat_ai 卡：只存 AI 凭证，不写 UserVip、不赠金币
        if (ChatAiSku.isChatAiCert(cert)) {
            store.saveChatAi(userId, cert, sig, redeemCode)
            return Outcome.Success(cert, res.isReissue == true, 0)
        }

        // 落本地 UserVip
        applyCertToLocalVip(userId, cert)

        // 持久化凭证（含 code，用于以后导出迁移凭证）
        store.save(userId, cert, sig, redeemCode)

        // 赠送金币（首次兑换）
        var coinsGranted = 0
        if (!isMigrate && cert.bonusCoins > 0) {
            try {
                db.coinDao().initializeCoins(userId)
                db.coinDao().addCoins(userId, cert.bonusCoins)
                coinsGranted = cert.bonusCoins
            } catch (e: Exception) {
                android.util.Log.e("VipManager", "赠送金币失败", e)
            }
        }

        return Outcome.Success(cert, res.isReissue == true, coinsGranted)
    }

    private suspend fun applyCertToLocalVip(userId: Long, cert: VipCertificate) {
        val current = db.userVipDao().getUserVipSync(userId)
        val newVip = (current?.copy(
            vipLevel = cert.vipLevel,
            expireDate = cert.expireDate
        ) ?: UserVip(
            userId = userId,
            vipLevel = cert.vipLevel,
            expireDate = cert.expireDate
        )).let { it.copy(signature = securityValidator.signVipStatus(it)) }
        db.userVipDao().insertOrUpdate(newVip)
    }

    private suspend fun downgradeLocalVip(userId: Long) {
        try {
            val current = db.userVipDao().getUserVipSync(userId) ?: return
            // 仅降级为非 VIP，不删账号
            val downgraded = current.copy(vipLevel = 0, expireDate = null)
                .let { it.copy(signature = securityValidator.signVipStatus(it)) }
            db.userVipDao().insertOrUpdate(downgraded)
        } catch (e: Exception) {
            android.util.Log.e("VipManager", "降级本地 VIP 失败", e)
        }
    }

    companion object {
        /**
         * 离线宽限期：cert.issuedAt 距今超过 X 天仍未续验成功 → 降级 VIP
         * 设计目的：用户飞行模式 + 改时间不能永久白嫖 VIP；
         * 21 天足够覆盖普通用户的偶发断网/换设备过渡期。
         */
        private const val OFFLINE_GRACE_DAYS = 21L
    }
}
