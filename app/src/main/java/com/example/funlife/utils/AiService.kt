package com.example.funlife.utils

import com.example.funlife.BuildConfig
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.ChatPersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ===== AI 调用结果 =====
sealed class AiResult {
    data class Success(val reply: String) : AiResult()
    data class Error(val reason: String) : AiResult()
}

// ===== API 数据结构 =====
data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<Map<String, String>>,
    val max_tokens: Int = 60,
    val temperature: Double = 0.9
)

data class ChatResponse(
    val choices: List<Choice>?,
    val error: ApiError?
)
data class Choice(val message: MessageContent?)
data class MessageContent(val content: String?)
data class ApiError(val message: String?, val type: String?)

// ===== Retrofit API 接口 =====
interface AiApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}

// ===== AI 服务封装 =====
class AiService(private val application: android.app.Application) {

    private val prefs = application.getSharedPreferences("ai_settings", android.content.Context.MODE_PRIVATE)

    // 优先使用运行时配置的key，其次使用BuildConfig的key
    private fun getActiveKey(): String {
        val runtimeKey = prefs.getString("ai_api_key", "") ?: ""
        return runtimeKey.ifBlank { BuildConfig.AI_API_KEY }
    }

    fun getApiKey(): String = prefs.getString("ai_api_key", "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString("ai_api_key", key).apply()
    }

    private val api: AiApiService by lazy {
        val logging = HttpLoggingInterceptor { msg ->
            android.util.Log.d("AiService", msg)
        }.apply { level = HttpLoggingInterceptor.Level.BODY }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(AiApiService::class.java)
    }

    // 最近一次错误信息，供外部读取
    var lastError: String? = null
        private set

    val isAvailable: Boolean get() = getActiveKey().isNotBlank()

    // 对话历史缓存（每个人格维护最近几轮）
    private val conversationHistory = mutableMapOf<String, MutableList<Map<String, String>>>()
    private val maxHistoryRounds = 6 // 保留最近6轮对话

    private fun addToHistory(personaId: String, role: String, content: String) {
        val history = conversationHistory.getOrPut(personaId) { mutableListOf() }
        history.add(mapOf("role" to role, "content" to content))
        // 保留最近N轮（每轮=user+assistant=2条）
        while (history.size > maxHistoryRounds * 2) {
            history.removeAt(0)
        }
    }

    fun clearHistory(personaId: String) {
        conversationHistory.remove(personaId)
    }

    /**
     * 记账场景 AI 回复 - 带上下文
     */
    suspend fun getReply(
        persona: ChatPersona,
        bill: Bill,
        monthlyTotal: Double,
        categoryCount: Int
    ): AiResult {
        if (!isAvailable) return AiResult.Error("未配置 API Key")

        return try {
            withContext(Dispatchers.IO) {
                withTimeout(15000L) {
                    val userMsg = buildString {
                        append("用户刚记了一笔账：${bill.category} ${String.format("%.1f", kotlin.math.abs(bill.amount))}元")
                        if (bill.note.isNotEmpty()) append("，备注「${bill.note}」")
                        append("。本月已消费${String.format("%.0f", kotlin.math.abs(monthlyTotal))}元")
                        if (categoryCount > 1) append("，本月${bill.category}已消费${categoryCount}次")
                        append("。请用一句话回复，限制20字以内。")
                    }

                    val messages = mutableListOf(
                        mapOf("role" to "system", "content" to persona.systemPrompt)
                    )
                    conversationHistory[persona.id]?.let { messages.addAll(it) }
                    messages.add(mapOf("role" to "user", "content" to userMsg))

                    callApi(persona.id, messages, userMsg)
                }
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val errorMsg = parseHttpError(e.code(), errorBody)
            lastError = errorMsg
            android.util.Log.e("AiService", "AI记账HTTP错误 [${e.code()}]: $errorBody")
            AiResult.Error(errorMsg)
        } catch (e: java.net.SocketTimeoutException) {
            lastError = "连接超时，请检查网络"
            android.util.Log.e("AiService", "AI超时: ${e.message}")
            AiResult.Error("连接超时，请检查网络")
        } catch (e: java.net.UnknownHostException) {
            lastError = "无法连接服务器，检查网络"
            android.util.Log.e("AiService", "DNS解析失败: ${e.message}")
            AiResult.Error("无法连接服务器，检查网络")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            lastError = "AI响应超时"
            android.util.Log.e("AiService", "AI超时: ${e.message}")
            AiResult.Error("AI响应超时")
        } catch (e: Exception) {
            lastError = "AI调用异常: ${e.javaClass.simpleName}"
            android.util.Log.e("AiService", "AI记账失败: ${e.javaClass.simpleName}: ${e.message}", e)
            AiResult.Error("AI调用异常: ${e.javaClass.simpleName}")
        }
    }

    /**
     * 普通聊天 AI 回复 - 带对话历史实现多轮对话
     */
    suspend fun getChatReply(persona: ChatPersona, userInput: String): AiResult {
        if (!isAvailable) return AiResult.Error("未配置 API Key")

        return try {
            withContext(Dispatchers.IO) {
                withTimeout(15000L) {
                    val systemPrompt = persona.systemPrompt +
                        "\n回复要求：用一两句话回复，限制30字以内，符合你的人格特点。可以根据之前的对话上下文来回复。"

                    val messages = mutableListOf(
                        mapOf("role" to "system", "content" to systemPrompt)
                    )
                    conversationHistory[persona.id]?.let { messages.addAll(it) }
                    messages.add(mapOf("role" to "user", "content" to userInput))

                    callApi(persona.id, messages, userInput)
                }
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val errorMsg = parseHttpError(e.code(), errorBody)
            lastError = errorMsg
            android.util.Log.e("AiService", "AI聊天HTTP错误 [${e.code()}]: $errorBody")
            AiResult.Error(errorMsg)
        } catch (e: java.net.SocketTimeoutException) {
            lastError = "连接超时，请检查网络"
            android.util.Log.e("AiService", "AI聊天超时: ${e.message}")
            AiResult.Error("连接超时，请检查网络")
        } catch (e: java.net.UnknownHostException) {
            lastError = "无法连接服务器，检查网络"
            android.util.Log.e("AiService", "DNS解析失败: ${e.message}")
            AiResult.Error("无法连接服务器，检查网络")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            lastError = "AI响应超时"
            android.util.Log.e("AiService", "AI聊天超时: ${e.message}")
            AiResult.Error("AI响应超时")
        } catch (e: Exception) {
            lastError = "AI调用异常: ${e.javaClass.simpleName}"
            android.util.Log.e("AiService", "AI聊天失败: ${e.javaClass.simpleName}: ${e.message}", e)
            AiResult.Error("AI调用异常: ${e.javaClass.simpleName}")
        }
    }

    /**
     * 统一API调用
     */
    private suspend fun callApi(
        personaId: String,
        messages: List<Map<String, String>>,
        userMsg: String
    ): AiResult {
        android.util.Log.d("AiService", "发送AI请求: key=${getActiveKey().take(8)}..., messages=${messages.size}条")

        val response = api.chatCompletion(
            "Bearer ${getActiveKey()}",
            ChatRequest(messages = messages, max_tokens = 80, temperature = 0.85)
        )

        // 检查API返回的错误
        if (response.error != null) {
            val errMsg = response.error.message ?: "Unknown API error"
            lastError = errMsg
            android.util.Log.e("AiService", "API返回错误: $errMsg")
            return AiResult.Error(errMsg)
        }

        val reply = cleanResponse(response.choices?.firstOrNull()?.message?.content)
        if (reply != null) {
            lastError = null
            addToHistory(personaId, "user", userMsg)
            addToHistory(personaId, "assistant", reply)
            android.util.Log.d("AiService", "AI回复成功: $reply")
            return AiResult.Success(reply)
        }

        lastError = "AI返回空内容"
        android.util.Log.w("AiService", "AI返回空: choices=${response.choices}")
        return AiResult.Error("AI返回空内容")
    }

    /**
     * 解析HTTP错误码
     */
    private fun parseHttpError(code: Int, body: String): String = when (code) {
        401 -> "API Key 无效或已过期"
        402 -> "账户余额不足，请充值"
        429 -> "请求太频繁，请稍后重试"
        500, 502, 503 -> "DeepSeek 服务器异常，请稍后重试"
        else -> "HTTP错误 $code: ${body.take(100)}"
    }

    private fun cleanResponse(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw
            .lines().firstOrNull { it.isNotBlank() }
            ?.take(60)
            ?.replace(Regex("^[\"「]|[\"」]$"), "")
            ?.trim()
            ?.ifBlank { null }
    }
}
