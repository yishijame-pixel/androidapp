package com.example.funlife.vip

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

/**
 * 云端签发的 VIP 凭证（HMAC-SHA256 签名）
 *
 * 字段顺序、JSON key 必须与服务端 functions/redeem/index.js 中
 * `buildCertResponse` 生成的对象**完全一致**，否则验签失败。
 *
 * ⚠️ 千万不要在客户端修改字段名 / 顺序，否则与云端签名不匹配。
 */
data class VipCertificate(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("skuCode") val skuCode: String,
    @SerializedName("vipLevel") val vipLevel: Int,
    @SerializedName("expireDate") val expireDate: String?,  // null = 永久
    @SerializedName("bonusCoins") val bonusCoins: Int,
    @SerializedName("issuedAt") val issuedAt: Long,
    @SerializedName("exp") val exp: Long
) {
    fun toCanonicalJson(): String = GSON.toJson(this)

    fun isValidNow(nowSec: Long = System.currentTimeMillis() / 1000): Boolean {
        return nowSec < exp
    }

    fun isVipActive(today: String = java.time.LocalDate.now().toString()): Boolean {
        if (expireDate == null) return true
        return try { today <= expireDate } catch (e: Exception) { false }
    }

    companion object {
        // ⚠️ 必须 serializeNulls：与云端 JSON.stringify 行为一致，否则 HMAC 验签失败
        // （Gson 默认会省略 null 字段，但 JS 的 JSON.stringify 会保留 "expireDate":null）
        val GSON: Gson = GsonBuilder().serializeNulls().create()
    }
}

/** 云端返回的完整响应 */
data class VipCertResponse(
    val ok: Boolean,
    val code: String? = null,
    val msg: String? = null,
    val isReissue: Boolean? = null,
    val certificate: VipCertificate? = null,
    val signature: String? = null
)
