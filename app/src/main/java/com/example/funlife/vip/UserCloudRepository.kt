package com.example.funlife.vip

import android.content.Context
import com.example.funlife.BuildConfig
import com.example.funlife.account.CloudWalletSnapshot
import com.example.funlife.security.SecurityManager
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 用户相关云端调用：
 *  1. registerLog  注册成功后异步上报，让后台能看到所有用户
 *  2. checkBanStatus  登录/启动时检查是否被封禁
 *
 * 网络异常一律按"未封禁"处理，避免后端故障导致用户大面积无法登录。
 */
class UserCloudRepository(private val context: Context) {

    private val gson = Gson()
    private val client = SecureHttp.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    sealed class BanStatus {
        object NotBanned : BanStatus()
        data class Banned(val reason: String, val scope: String) : BanStatus()
        object NetworkError : BanStatus()  // 视为未封禁，但调用方可记录
    }

    /** 注册上报结果：明确区分成功 / 业务拒绝 / 网络异常，便于上层做不同处理 */
    sealed class RegisterResult {
        data class Ok(val deviceToken: String?) : RegisterResult()
        /** 服务端业务拒绝（用户名已注册、密码不对、设备冲突等），msg 可直接给用户看 */
        data class Rejected(val code: String, val msg: String) : RegisterResult()
        object NetworkError : RegisterResult()
    }

    /** 清数据后云端验密恢复本地账号 */
    sealed class RecoverResult {
        data class Ok(
            val nickname: String,
            val deviceToken: String?,
            val wallet: CloudWalletSnapshot,
        ) : RecoverResult()

        /** 用户名不存在或密码错误（统一码，防枚举） */
        object CredentialsInvalid : RecoverResult()

        data class Rejected(val code: String, val msg: String) : RecoverResult()
        object NetworkError : RecoverResult()
    }

    /**
     * 云端验密恢复：清本机数据后，凭 passwordProof 重建登录身份并拉取钱包快照。
     */
    fun recoverAccount(username: String, password: String): RecoverResult {
        val baseUrl = BuildConfig.VIP_BACKEND_URL
        if (baseUrl.isNullOrBlank()) return RecoverResult.NetworkError
        return try {
            val deviceId = SecurityManager.getDeviceFingerprint(context)
            val passwordProof = computePasswordProof(username, password)
            val body = mapOf(
                "username" to username,
                "passwordProof" to passwordProof,
                "deviceId" to deviceId,
            )
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/account_recover")
                .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return RecoverResult.NetworkError
                val r = gson.fromJson(text, Map::class.java) as? Map<*, *>
                    ?: return RecoverResult.NetworkError
                val ok = r["ok"] as? Boolean ?: false
                if (!ok) {
                    val code = (r["code"] as? String) ?: "UNKNOWN"
                    if (code == "CREDENTIALS_INVALID" || code == "WRONG_PASSWORD") {
                        return RecoverResult.CredentialsInvalid
                    }
                    val msg = (r["msg"] as? String) ?: defaultRecoverRejectMsg(code)
                    return RecoverResult.Rejected(code, msg)
                }
                val token = r["deviceToken"] as? String
                if (!token.isNullOrBlank()) {
                    DeviceTokenStore(context).save(username, token)
                }
                val nickname = (r["nickname"] as? String)?.takeIf { it.isNotBlank() } ?: username
                @Suppress("UNCHECKED_CAST")
                val walletMap = r["wallet"] as? Map<String, Any>
                RecoverResult.Ok(
                    nickname = nickname,
                    deviceToken = token,
                    wallet = parseWallet(walletMap),
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("UserCloudRepository", "recoverAccount failed: ${e.message}")
            RecoverResult.NetworkError
        }
    }

    private fun parseWallet(map: Map<String, Any>?): CloudWalletSnapshot {
        if (map == null) return CloudWalletSnapshot()
        fun num(key: String): Int = (map[key] as? Number)?.toInt() ?: 0
        return CloudWalletSnapshot(
            balance = num("balance"),
            totalEarned = num("totalEarned"),
            totalSpent = num("totalSpent"),
            pointsBalance = num("pointsBalance"),
            hasSnapshot = map["hasSnapshot"] as? Boolean ?: (num("balance") > 0 || num("pointsBalance") > 0),
        )
    }

    private fun defaultRecoverRejectMsg(code: String): String = when (code) {
        "BANNED" -> "该账号已被封禁"
        "DEVICE_CONFLICT" -> "该账号已在其他设备注册，请使用 VIP 迁移或在原设备恢复"
        "PROOF_REQUIRED" -> "请升级 App 后重新登录"
        "RATE_LIMITED" -> "请求过于频繁，请稍后再试"
        "INVALID" -> "请求参数无效，请检查输入"
        "BAD_REQUEST" -> "请求格式错误，请重试"
        "DB_ERROR" -> "服务繁忙，请稍后再试"
        else -> {
            android.util.Log.w("UserCloudRepository", "Unmapped recover reject code: $code")
            "账号恢复失败，请稍后重试"
        }
    }

    /**
     * 上报用户注册并领取 device_token（同步）
     *
     * @param password 用户明文密码（仅用于本地计算 passwordProof，不会上传）
     */
    fun registerLog(
        username: String,
        nickname: String,
        betaCode: String,
        password: String,
        mode: String = "register",   // "register" = 注册流程严格拒重复；"refresh" = 已登录补领 token
        dryRun: Boolean = false,     // true = 只做用户名冲突预检，不写库不签发 token
    ): RegisterResult {
        val baseUrl = BuildConfig.VIP_BACKEND_URL
        if (baseUrl.isNullOrBlank()) return RegisterResult.NetworkError
        return try {
            val deviceId = SecurityManager.getDeviceFingerprint(context)
            val passwordProof = computePasswordProof(username, password)
            val body = mapOf(
                "username" to username,
                "nickname" to nickname,
                "deviceId" to deviceId,
                "betaCode" to betaCode,
                "passwordProof" to passwordProof,
                "mode" to mode,
                "dryRun" to dryRun,
            )
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/register_log")
                .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return RegisterResult.NetworkError
                val r = gson.fromJson(text, Map::class.java) as? Map<*, *>
                    ?: return RegisterResult.NetworkError
                val ok = r["ok"] as? Boolean ?: false
                if (!ok) {
                    val code = (r["code"] as? String) ?: "UNKNOWN"
                    val msg = (r["msg"] as? String) ?: defaultRejectMsg(code)
                    return RegisterResult.Rejected(code, msg)
                }
                val token = r["deviceToken"] as? String
                if (!token.isNullOrBlank()) {
                    DeviceTokenStore(context).save(username, token)
                }
                RegisterResult.Ok(token)
            }
        } catch (e: Exception) {
            RegisterResult.NetworkError
        }
    }

    private fun defaultRejectMsg(code: String): String = when (code) {
        "WRONG_PASSWORD"     -> "用户名或密码错误"
        "DEVICE_CONFLICT"    -> "该用户名已在其他设备注册，请直接登录或换用户名"
        "ALREADY_REGISTERED" -> "该用户名已被注册，请直接登录或换一个"
        "PROOF_REQUIRED"     -> "请升级 App 后重新登录"
        "RATE_LIMITED"       -> "请求过于频繁，请稍后再试"
        "INVALID"            -> "请求参数无效，请检查输入"
        "BAD_REQUEST"        -> "请求格式错误，请重试"
        "DB_ERROR"           -> "服务繁忙，请稍后再试"
        else                 -> {
            // 🔒 不把未映射的英文 code 直接显示给用户，仅日志可见
            android.util.Log.w("UserCloudRepository", "Unmapped register reject code: $code")
            "注册失败，请稍后重试"
        }
    }

    /**
     * 检查封号状态。返回 Banned / NotBanned / NetworkError。
     */
    fun checkBanStatus(username: String): BanStatus {
        val baseUrl = BuildConfig.VIP_BACKEND_URL
        if (baseUrl.isNullOrBlank()) return BanStatus.NetworkError
        return try {
            val deviceId = SecurityManager.getDeviceFingerprint(context)
            val body = mapOf("username" to username, "deviceId" to deviceId)
            val req = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/user_status")
                .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return BanStatus.NetworkError
                val r = gson.fromJson(text, Map::class.java) as Map<*, *>
                val ok = r["ok"] as? Boolean ?: false
                if (!ok) return BanStatus.NetworkError
                val banned = r["banned"] as? Boolean ?: false
                if (!banned) BanStatus.NotBanned
                else BanStatus.Banned(
                    (r["reason"] as? String) ?: "您的账号已被封禁",
                    (r["scope"] as? String) ?: "user",
                )
            }
        } catch (e: Exception) {
            BanStatus.NetworkError
        }
    }

    companion object {
        /**
         * 计算 passwordProof = SHA-256("FunLifeAuth|" + username + "|" + password)
         * 仅用于服务端验证"调用方持有正确密码"，不会反推出原密码（单向 hash + 域名前缀）。
         */
        fun computePasswordProof(username: String, password: String): String {
            val raw = "FunLifeAuth|${username.trim()}|$password"
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
