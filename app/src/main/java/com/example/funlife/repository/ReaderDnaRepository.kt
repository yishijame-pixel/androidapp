// ReaderDnaRepository.kt — v53 阅光书房 · 读者 DNA 人格画像
//
// 流程：
//   1. 检查 VipQuota.readerDnaCooldownDays 是否解锁
//   2. 收集近 N 本读完书的 title + 摘抄前 200 字 + 评分 + 标签 摘要
//   3. 走 ChatAiCloudRepository（mode = "reader_dna"），让云端 LLM 输出严格 JSON
//   4. 解析失败 → 兜底本地启发式（按摘抄关键词频次估算雷达图维度）
//   5. 写入 reader_dna_cards 本地表
//
// 隔离：所有方法 userId > 0；只读取本人 books/quotes。
package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.BookDao
import com.example.funlife.data.dao.QuoteDao
import com.example.funlife.data.dao.ReaderDnaCardDao
import com.example.funlife.data.dao.UserVipDao
import com.example.funlife.data.model.ReaderDnaCard
import com.example.funlife.vip.ChatAiCloudRepository
import com.example.funlife.vip.VipQuota
import com.google.gson.Gson
import org.json.JSONObject

class ReaderDnaRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val quoteDao: QuoteDao,
    private val dnaDao: ReaderDnaCardDao,
    private val userVipDao: UserVipDao,
) {
    private val gson = Gson()

    suspend fun observe(userId: Long) = dnaDao.observeAll(userId)

    /** 距离上次生成是否已过冷却期 */
    suspend fun cooldownRemainingMs(userId: Long): Long {
        require(userId > 0L)
        val vipLevel = userVipDao.getUserVipSync(userId)?.vipLevel ?: 0
        val cooldownDays = VipQuota.readerDnaCooldownDays(vipLevel)
        if (cooldownDays <= 0) return 0L
        val last = dnaDao.latest(userId) ?: return 0L
        val nextAvailable = last.generatedAt + cooldownDays * 24L * 3600L * 1000L
        return (nextAvailable - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * 生成读者 DNA。
     * @return Result.Success(card) / Result.Cooldown(remainingMs) / Result.NoData(needAtLeastBooks)
     */
    suspend fun generate(userId: Long, sampleBooks: Int = 12): GenResult {
        require(userId > 0L)
        val cd = cooldownRemainingMs(userId)
        if (cd > 0L) return GenResult.Cooldown(cd)

        // 取近期已读完的书，按 finishedAt DESC（兜底用 updatedAt）
        val all = bookDao.observeAll(userId).let {
            // 这里直接用一次性查询（observeAll 是 Flow，但 Flow 不能阻塞拿 list；
            // 使用替代：getBooksOfYear 过去两年聚合 OR 我们直接一查所有再 take）
            // 没有现成 suspend list 接口；保守：用 getById 配合 ID 集合不现实。
            // 简化：调用一个已有的"年度"接口覆盖最近两年。
            null
        }
        // 简化收集：调用现有"过去两年"已读完书
        val now = System.currentTimeMillis()
        val twoYearMs = 2L * 365 * 24 * 3600 * 1000
        val finished = bookDao.getBooksOfYear(userId, now - twoYearMs, now)
            .filter { it.finishedAt > 0 }
            .sortedByDescending { it.finishedAt }
            .take(sampleBooks)
        if (finished.size < 1) return GenResult.NoData(needAtLeastBooks = 1)

        val brief = buildString {
            append("用户最近 ${finished.size} 本读完的书与摘抄：\n")
            finished.forEachIndexed { i, b ->
                append("${i + 1}. 《${b.title}》")
                if (b.author.isNotEmpty()) append(" / ${b.author}")
                if (b.rating > 0) append(" · ${b.rating}星")
                if (b.tags.isNotEmpty()) append(" · 标签 ${b.tags}")
                if (b.note.isNotBlank()) append("\n   心得: ${b.note.take(160)}")
                if (b.favoriteQuote.isNotBlank()) append("\n   摘: ${b.favoriteQuote.take(120)}")
                append('\n')
            }
        }

        val system = """
            你是文学心理画像分析师。基于用户的书单与摘抄，判断其阅读人格。
            输出**严格 JSON**（不要 markdown、不要解释），字段如下：
            {
              "rationality": 0.0-1.0,
              "sensibility": 0.0-1.0,
              "inward":      0.0-1.0,
              "outward":     0.0-1.0,
              "gentleness":  0.0-1.0,
              "sharpness":   0.0-1.0,
              "keywords":    ["关键词1","关键词2","关键词3","关键词4","关键词5"],
              "tagline":     "一句不超过 24 字的人格总结，温暖、克制、有诗意"
            }
            6 个维度合理对立（rationality+sensibility 等不必恰好 1）。tagline 必须中文。
        """.trimIndent()

        return try {
            val cloud = ChatAiCloudRepository(context.applicationContext)
            val r = cloud.reply(
                userId = userId,
                body = ChatAiCloudRepository.Body(
                    mode = "reader_dna",
                    personaSystem = system,
                    userText = brief
                )
            )
            when (r) {
                is ChatAiCloudRepository.CallResult.Success -> {
                    val parsed = parseDna(r.reply) ?: localFallback(brief)
                    val card = persist(userId, parsed, finished.size)
                    GenResult.Success(card)
                }
                is ChatAiCloudRepository.CallResult.QuotaExceeded ->
                    GenResult.QuotaExceeded(r.used, r.limit, r.vipLevel)
                else -> {
                    // 云端不可用，本地兜底（避免空白体验）
                    val parsed = localFallback(brief)
                    val card = persist(userId, parsed, finished.size)
                    GenResult.SuccessLocal(card)
                }
            }
        } catch (_: Exception) {
            val parsed = localFallback(brief)
            val card = persist(userId, parsed, finished.size)
            GenResult.SuccessLocal(card)
        }
    }

    private suspend fun persist(userId: Long, parsed: ParsedDna, bookCount: Int): ReaderDnaCard {
        val card = ReaderDnaCard(
            userId = userId,
            generatedAt = System.currentTimeMillis(),
            vectorJson = gson.toJson(parsed),
            tagline = parsed.tagline.ifBlank { "在书里寻找自己的光" },
            basedOnBookCount = bookCount,
        )
        val id = dnaDao.insert(card)
        return card.copy(id = id)
    }

    private fun parseDna(text: String): ParsedDna? {
        return try {
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val json = JSONObject(text.substring(start, end + 1))
            ParsedDna(
                rationality = json.optDouble("rationality", 0.5).toFloat(),
                sensibility = json.optDouble("sensibility", 0.5).toFloat(),
                inward = json.optDouble("inward", 0.5).toFloat(),
                outward = json.optDouble("outward", 0.5).toFloat(),
                gentleness = json.optDouble("gentleness", 0.5).toFloat(),
                sharpness = json.optDouble("sharpness", 0.5).toFloat(),
                keywords = json.optJSONArray("keywords")?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList(),
                tagline = json.optString("tagline", ""),
            )
        } catch (_: Exception) {
            null
        }
    }

    /** 本地启发式：根据 brief 关键词频次估算 6 维向量（兜底） */
    private fun localFallback(brief: String): ParsedDna {
        val lower = brief
        fun count(words: List<String>) = words.sumOf { w ->
            var idx = 0; var c = 0
            while (true) {
                idx = lower.indexOf(w, idx); if (idx < 0) break
                c++; idx += w.length
            }
            c
        }
        val rat = count(listOf("逻辑", "理性", "科学", "分析", "证据", "原理"))
        val sen = count(listOf("温柔", "感动", "眼泪", "回忆", "心动", "孤独"))
        val ind = count(listOf("自我", "内心", "孤独", "沉思", "独处", "梦"))
        val out = count(listOf("社会", "人群", "他人", "城市", "时代", "公共"))
        val gen = count(listOf("温柔", "包容", "慈悲", "缓慢", "等待", "陪伴"))
        val sha = count(listOf("批判", "锋利", "对抗", "讽刺", "残酷", "决断"))
        val total = (rat + sen + ind + out + gen + sha).coerceAtLeast(1)
        fun n(x: Int) = (x.toFloat() / total + 0.2f).coerceIn(0.1f, 0.95f)
        return ParsedDna(
            rationality = n(rat), sensibility = n(sen),
            inward = n(ind), outward = n(out),
            gentleness = n(gen), sharpness = n(sha),
            keywords = listOf("阅读者", "在路上"),
            tagline = "在字里行间，找回自己的形状",
        )
    }

    data class ParsedDna(
        val rationality: Float,
        val sensibility: Float,
        val inward: Float,
        val outward: Float,
        val gentleness: Float,
        val sharpness: Float,
        val keywords: List<String>,
        val tagline: String,
    )

    sealed class GenResult {
        data class Success(val card: ReaderDnaCard) : GenResult()
        data class SuccessLocal(val card: ReaderDnaCard) : GenResult()
        data class Cooldown(val remainingMs: Long) : GenResult()
        data class NoData(val needAtLeastBooks: Int) : GenResult()
        data class QuotaExceeded(val used: Int, val limit: Int, val vipLevel: Int) : GenResult()
    }
}
