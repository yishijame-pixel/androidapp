package com.example.funlife.vip

import android.content.Context
import com.example.funlife.BuildConfig
import com.example.funlife.security.SecurityManager
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 本地凭证验签器
 *
 * ⚠️ 注意：HMAC 密钥必须与云函数环境变量 HMAC_SECRET 完全一致。
 *
 * 由于 Android 客户端是单密钥对称验签（不能用非对称），密钥必然内嵌在 APK。
 * 这是已知的 trade-off：
 *   - 普通用户：完全无法伪造凭证
 *   - 反编译者：能拿到密钥 → 自签凭证
 *
 * 防御策略：
 *   1. 客户端定期联网 verify → 云端用 DB 状态再次校验
 *   2. 密钥通过 BuildConfig 注入，release 包配合 R8 混淆
 *   3. 关键判断（isVip）走多处不同代码路径
 *   4. 加固包（360/腾讯乐固）进一步抬高门槛
 *
 * 对于小规模售卖，这层防御足够把破解者从"5分钟"提升到"会逆向才能搞"。
 */
object VipCertificateValidator {

    enum class Result {
        VALID,              // 凭证有效
        DEVICE_MISMATCH,    // 凭证不属于本设备
        EXPIRED,            // 凭证 exp 过期，需联网 verify 续期
        VIP_EXPIRED,        // VIP 本身到期（如年费到期）
        BAD_SIGNATURE,      // 签名错误（被篡改）
        EMPTY               // 没有凭证（普通用户）
    }

    fun validate(context: Context, cert: VipCertificate?, signature: String?): Result {
        if (cert == null || signature.isNullOrBlank()) return Result.EMPTY

        // 1. 签名校验
        val expected = hmacSha256(cert.toCanonicalJson(), BuildConfig.VIP_HMAC_SECRET)
        if (!constantTimeEq(expected, signature)) return Result.BAD_SIGNATURE

        // 2. 设备绑定
        val myDevice = SecurityManager.getDeviceFingerprint(context)
        if (cert.deviceId != myDevice) return Result.DEVICE_MISMATCH

        // 🔒 把 cert.issuedAt 作为可信时间锚喂入防回拨时钟
        val clock = com.example.funlife.security.MonotonicClock.get(context)
        clock.feed(cert.issuedAt)

        // 3. 凭证自身过期（强制联网 verify）— 用防回拨时间
        if (clock.effectiveNowSec() >= cert.exp) return Result.EXPIRED

        // 4. VIP 业务到期（年费类型）— 用防回拨日期
        if (!cert.isVipActive(clock.effectiveToday())) return Result.VIP_EXPIRED

        return Result.VALID
    }

    // ─── 工具 ───
    private fun hmacSha256(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val out = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return out.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEq(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
