package com.example.funlife.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.funlife.BuildConfig
import java.security.MessageDigest

/**
 * 🔒 应用签名自校验
 *
 * 启动时计算自身签名 SHA-256 与 BuildConfig.APP_SIGN_SHA256 比对，
 * 防止攻击者重打包后绕过本地校验逻辑。
 *
 * 行为策略：
 *  - APP_SIGN_SHA256 留空 → 跳过（开发/调试期）
 *  - debug 包 → 仅记录日志，不阻断
 *  - release 包失配 → 记录 + 标记 tampered，调用方可决定是否退出
 */
object AppSignatureGuard {
    private const val TAG = "AppSignatureGuard"

    @Volatile private var checked = false
    @Volatile private var tampered = false

    /** 启动时调用一次。返回 true = 通过校验或已跳过；false = 检测到篡改。 */
    fun verify(context: Context): Boolean {
        if (checked) return !tampered
        checked = true

        val expected = BuildConfig.APP_SIGN_SHA256.trim().uppercase()
        if (expected.isEmpty()) {
            Log.i(TAG, "APP_SIGN_SHA256 未配置，跳过签名自校验")
            return true
        }

        val actual = try { computeSignatureSha256(context) } catch (e: Exception) {
            Log.e(TAG, "读取签名失败", e); null
        }
        if (actual.isNullOrEmpty()) {
            Log.w(TAG, "无法获取应用签名，视为通过（避免误伤）")
            return true
        }

        val ok = constantTimeEq(actual, expected)
        if (!ok) {
            tampered = true
            Log.e(TAG, "❌ 应用签名校验失败！可能被重打包。expected=${expected.take(8)}… actual=${actual.take(8)}…")
            try { AuditTrail.record(context, "APP_TAMPERED", "sig_mismatch") } catch (_: Exception) {}
        } else {
            Log.i(TAG, "✓ 应用签名校验通过")
        }
        return ok
    }

    fun isTampered(): Boolean = tampered

    @Suppress("DEPRECATION")
    private fun computeSignatureSha256(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val md = MessageDigest.getInstance("SHA-256")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return ""
            val sigs = if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
            if (sigs.isNullOrEmpty()) return ""
            return md.digest(sigs[0].toByteArray()).toHex()
        } else {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            val sigs = info.signatures ?: return ""
            if (sigs.isEmpty()) return ""
            return md.digest(sigs[0].toByteArray()).toHex()
        }
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) sb.append(String.format("%02X", b))
        return sb.toString()
    }

    private fun constantTimeEq(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}

/** 极简审计入口，避免循环依赖 AuditLogger。 */
private object AuditTrail {
    fun record(context: Context, tag: String, msg: String) {
        Log.w("AuditTrail", "[$tag] $msg")
    }
}
