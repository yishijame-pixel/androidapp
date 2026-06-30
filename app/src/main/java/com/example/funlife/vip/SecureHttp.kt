package com.example.funlife.vip

import android.net.Uri
import com.example.funlife.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 共享 HTTP 客户端工厂。
 *
 * 安全特性：
 *   1. **Cert Pinning**：当 BuildConfig.VIP_BACKEND_PIN 非空时，绑定云函数证书指纹，
 *      即使设备装了恶意根证书也无法中间人。
 *   2. 统一超时设置（10s 连接 / 15s 读 / 写）。
 *
 * 注意：Pin 配置错误（如证书轮换后未发新版）会导致全部用户连接失败！
 *      必须 pin 主证书 + 备用证书；建议监控接入失败率。
 */
object SecureHttp {

    /** 统一带 cert pinning 的 OkHttpClient.Builder */
    fun newBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        applyPinning(builder)
        return builder
    }

    /** 直接拿到带 pinning 的实例（无定制需求时用这个） */
    fun client(): OkHttpClient = newBuilder().build()

    private fun applyPinning(builder: OkHttpClient.Builder) {
        val pinnerBuilder = CertificatePinner.Builder()
        var pinned = false
        pinned = addHostPins(pinnerBuilder, BuildConfig.VIP_BACKEND_URL, BuildConfig.VIP_BACKEND_PIN) || pinned
        pinned = addHostPins(pinnerBuilder, BuildConfig.ASSET_MANIFEST_URL, BuildConfig.ASSET_MANIFEST_PIN) || pinned
        if (pinned) builder.certificatePinner(pinnerBuilder.build())
    }

    private fun addHostPins(pinnerBuilder: CertificatePinner.Builder, baseUrl: String, pinSpec: String): Boolean {
        if (pinSpec.isBlank() || baseUrl.isBlank()) return false
        val host = try {
            Uri.parse(baseUrl).host ?: return false
        } catch (e: Exception) {
            return false
        }
        pinSpec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { p ->
            pinnerBuilder.add(host, p)
        }
        return true
    }
}
