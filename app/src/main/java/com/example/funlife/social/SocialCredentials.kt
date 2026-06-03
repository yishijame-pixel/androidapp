package com.example.funlife.social

/** 已通过门禁校验、可直接调用 PocketBase API 的会话凭证。 */
data class SocialCredentials(
    val userId: Long,
    val pbRecordId: String,
    val token: String,
)

/** 社交层统一错误分类 → 用户可读文案。 */
sealed class SocialFailure(val userMessage: String) {
    data object NotConfigured : SocialFailure("社交服务未配置\n请在 local.properties 设置 POCKETBASE_URL")
    data object NotLoggedIn : SocialFailure("请先登录 FunLife 账号")
    data class NotReady(val detail: String) : SocialFailure(detail)
    data class Validation(val detail: String) : SocialFailure(detail)
    data class Timeout(val operation: String) : SocialFailure("$operation 超时，请检查网络或 PocketBase 是否启动")
    data class Api(val message: String) : SocialFailure(message)
    data class Network(val message: String) : SocialFailure(message)
    data class Unknown(val message: String) : SocialFailure(message)

    companion object {
        fun fromThrowable(t: Throwable, fallback: String = "操作失败"): SocialFailure = when (t) {
            is SocialFailureException -> t.failure
            is PocketBaseApiException -> Api(t.message ?: fallback)
            is java.io.IOException -> Network(
                "无法连接社交服务器\n请确认 PocketBase 已启动且手机能访问\n(${PocketBaseConfig.baseUrl()})",
            )
            is IllegalArgumentException -> Validation(t.message ?: fallback)
            is IllegalStateException -> NotReady(t.message ?: fallback)
            else -> Unknown(t.message ?: fallback)
        }
    }
}

class SocialFailureException(val failure: SocialFailure) : Exception(failure.userMessage)

fun Result<*>.socialFailureOrNull(): SocialFailure? =
    exceptionOrNull()?.let { SocialFailure.fromThrowable(it) }
