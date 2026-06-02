package com.example.funlife.vip

import android.content.Context
import com.example.funlife.BuildConfig
import com.example.funlife.security.SecurityManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 云端 VIP 服务客户端（调 CloudBase HTTP 触发器）
 *
 * 三个接口：
 *   /redeem   兑换卡密 → 拿凭证
 *   /migrate  迁移设备 → 拿新凭证
 *   /verify   定期复验 → 续期凭证
 *
 * 部署后 baseUrl 形如：
 *   https://<envId>-<random>.service.tcloudbase.com
 * 在 CloudBase 控制台 → 云函数 → HTTP 触发器 → 看到的就是。
 */
class VipCloudRepository(
    private val context: Context,
    private val baseUrl: String = BuildConfig.VIP_BACKEND_URL
) {
    private val http: OkHttpClient = SecureHttp.client()

    private val gson = Gson()

    /** 兑换卡密
     *  @param userId 当前登录用户 ID，用于把卡密绑定到账号（防同设备多账号白嫖 reissue）
     */
    suspend fun redeem(code: String, userId: Long): VipCertResponse = post(
        path = "/redeem",
        body = mapOf(
            "code" to code,
            "deviceId" to SecurityManager.getDeviceFingerprint(context),
            "userId" to userId
        )
    )

    /** 迁移设备（新设备调用）
     *  @param username   新设备上当前登录的用户名（用于 device_token 校验）
     *  @param oldDeviceId 原设备指纹（用户从原设备导出的迁移凭证里解析得到），必填
     */
    suspend fun migrate(username: String, code: String, oldDeviceId: String): VipCertResponse {
        val deviceToken = DeviceTokenStore(context).load(username)
        if (deviceToken.isNullOrBlank()) {
            return VipCertResponse(
                ok = false, code = "AUTH_REQUIRED",
                msg = "请重新登录账号后再迁移 VIP"
            )
        }
        return post(
            path = "/migrate",
            body = mapOf(
                "code" to code,
                "username" to username,
                "deviceToken" to deviceToken,
                "oldDeviceId" to oldDeviceId,
                "newDeviceId" to SecurityManager.getDeviceFingerprint(context)
            )
        )
    }

    /** 复验凭证 */
    suspend fun verify(cert: VipCertificate, signature: String): VipCertResponse = post(
        path = "/verify",
        body = mapOf(
            "certificate" to cert,
            "signature" to signature
        )
    )

    private suspend fun post(path: String, body: Any): VipCertResponse = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || !baseUrl.startsWith("http")) {
            return@withContext VipCertResponse(
                ok = false, code = "NO_BACKEND",
                msg = "云端地址未配置"
            )
        }
        try {
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + path)
                .post(gson.toJson(body).toRequestBody(JSON))
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@use VipCertResponse(
                        ok = false, code = "HTTP_${resp.code}",
                        msg = "网络异常 (${resp.code})"
                    )
                }
                gson.fromJson(text, VipCertResponse::class.java)
                    ?: VipCertResponse(ok = false, code = "PARSE_ERROR", msg = "响应解析失败")
            }
        } catch (e: Exception) {
            VipCertResponse(ok = false, code = "NETWORK_ERROR", msg = e.message ?: "网络错误")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
