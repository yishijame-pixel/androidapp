package com.example.funlife.repository

import android.content.Context
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.PocketBaseApiException
import com.example.funlife.social.game.engine.GomokuEloCalculator
import com.example.funlife.social.game.engine.GomokuPlayerStats
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 五子棋玩家战绩 Repository
 *
 * 管理 PocketBase `gomoku_player_stats` collection
 */
class GomokuStatsRepository(context: Context) {

    private val api = PocketBaseApiClient(context.applicationContext)
    private val gson = Gson()

    /**
     * 获取玩家战绩（不存在则创建默认记录）
     */
    suspend fun getOrCreateStats(token: String, pbId: String): GomokuPlayerStats =
        withContext(Dispatchers.IO) {
            getStats(token, pbId) ?: createStats(token, pbId)
        }

    /**
     * 获取玩家战绩
     */
    suspend fun getStats(token: String, pbId: String): GomokuPlayerStats? =
        withContext(Dispatchers.IO) {
            try {
                val filter = URLEncoder.encode(
                    "user = '$pbId'",
                    StandardCharsets.UTF_8.name(),
                )
                val url = "${apiBase()}/collections/gomoku_player_stats/records?filter=$filter&perPage=1"
                val json = getJson(url, token)
                val items = json.getAsJsonArray("items")
                if (items == null || items.size() == 0) return@withContext null
                parseStats(items[0].asJsonObject, pbId)
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 创建初始战绩记录
     */
    private suspend fun createStats(token: String, pbId: String): GomokuPlayerStats =
        withContext(Dispatchers.IO) {
            val stats = GomokuPlayerStats(pbId = pbId)
            val body = stats.toMap()
            val json = postJson("${apiBase()}/collections/gomoku_player_stats/records", body, token)
            parseStats(json, pbId)
        }

    /**
     * 更新战绩（对局结束后调用）
     */
    suspend fun updateStats(token: String, stats: GomokuPlayerStats): GomokuPlayerStats =
        withContext(Dispatchers.IO) {
            val recordId = findRecordId(token, stats.pbId) ?: run {
                // 记录不存在，创建新的
                return@withContext createStats(token, stats.pbId).let { created ->
                    // 合并新数据
                    updateStatsRecord(token, findRecordId(token, stats.pbId)!!, stats)
                }
            }
            updateStatsRecord(token, recordId, stats)
        }

    private suspend fun updateStatsRecord(
        token: String,
        recordId: String,
        stats: GomokuPlayerStats,
    ): GomokuPlayerStats {
        val body = mapOf(
            "elo_rating" to stats.eloRating,
            "games_played" to stats.gamesPlayed,
            "games_won" to stats.gamesWon,
            "games_lost" to stats.gamesLost,
            "games_drawn" to stats.gamesDrawn,
            "win_streak" to stats.winStreak,
            "best_streak" to stats.bestStreak,
        )
        val json = patchJson("${apiBase()}/collections/gomoku_player_stats/records/$recordId", body, token)
        return parseStats(json, stats.pbId)
    }

    /**
     * 处理对局结果，更新双方 ELO
     */
    suspend fun processMatchResult(
        token: String,
        winnerPbId: String,
        loserPbId: String,
        isDraw: Boolean = false,
    ): MatchResultUpdate = withContext(Dispatchers.IO) {
        val winnerStats = getOrCreateStats(token, winnerPbId)
        val loserStats = getOrCreateStats(token, loserPbId)

        val eloResult = GomokuEloCalculator.calculate(
            winnerElo = winnerStats.eloRating,
            loserElo = loserStats.eloRating,
            winnerGames = winnerStats.gamesPlayed,
            loserGames = loserStats.gamesPlayed,
            isDraw = isDraw,
        )

        val newWinnerStats = if (isDraw) {
            winnerStats.onDraw(eloResult.winnerNewElo)
        } else {
            winnerStats.onWin(eloResult.winnerNewElo)
        }

        val newLoserStats = if (isDraw) {
            loserStats.onDraw(eloResult.loserNewElo)
        } else {
            loserStats.onLoss(eloResult.loserNewElo)
        }

        // 更新数据库
        updateStats(token, newWinnerStats)
        updateStats(token, newLoserStats)

        MatchResultUpdate(
            winnerOldElo = winnerStats.eloRating,
            winnerNewElo = eloResult.winnerNewElo,
            winnerDelta = eloResult.winnerDelta,
            loserOldElo = loserStats.eloRating,
            loserNewElo = eloResult.loserNewElo,
            loserDelta = eloResult.loserDelta,
        )
    }

    private suspend fun findRecordId(token: String, pbId: String): String? {
        val filter = URLEncoder.encode(
            "user = '$pbId'",
            StandardCharsets.UTF_8.name(),
        )
        val url = "${apiBase()}/collections/gomoku_player_stats/records?filter=$filter&perPage=1"
        return try {
            val json = getJson(url, token)
            val items = json.getAsJsonArray("items")
            if (items != null && items.size() > 0) {
                items[0].asJsonObject.get("id")?.asString
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseStats(obj: com.google.gson.JsonObject, pbId: String): GomokuPlayerStats {
        return GomokuPlayerStats(
            pbId = pbId,
            eloRating = obj.get("elo_rating")?.asInt ?: GomokuEloCalculator.INITIAL_ELO,
            gamesPlayed = obj.get("games_played")?.asInt ?: 0,
            gamesWon = obj.get("games_won")?.asInt ?: 0,
            gamesLost = obj.get("games_lost")?.asInt ?: 0,
            gamesDrawn = obj.get("games_drawn")?.asInt ?: 0,
            winStreak = obj.get("win_streak")?.asInt ?: 0,
            bestStreak = obj.get("best_streak")?.asInt ?: 0,
            updatedAtMs = System.currentTimeMillis(),
        )
    }

    private fun apiBase(): String = com.example.funlife.social.PocketBaseConfig.apiBase()

    private fun getJson(url: String, token: String): com.google.gson.JsonObject {
        val http = com.example.funlife.social.PocketBaseHttp.client()
        val req = okhttp3.Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw PocketBaseApiException(resp.code, text)
            }
            if (text.isBlank()) return com.google.gson.JsonObject()
            return JsonParser.parseString(text).asJsonObject
        }
    }

    private fun postJson(
        url: String,
        body: Map<String, Any?>,
        token: String,
    ): com.google.gson.JsonObject {
        val http = com.example.funlife.social.PocketBaseHttp.client()
        val jsonType = "application/json".toMediaType()
        val req = okhttp3.Request.Builder()
            .url(url)
            .post(gson.toJson(body).toRequestBody(jsonType))
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw PocketBaseApiException(resp.code, text)
            }
            return JsonParser.parseString(text).asJsonObject
        }
    }

    private fun patchJson(
        url: String,
        body: Map<String, Any?>,
        token: String,
    ): com.google.gson.JsonObject {
        val http = com.example.funlife.social.PocketBaseHttp.client()
        val jsonType = "application/json".toMediaType()
        val req = okhttp3.Request.Builder()
            .url(url)
            .patch(gson.toJson(body).toRequestBody(jsonType))
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw PocketBaseApiException(resp.code, text)
            }
            return JsonParser.parseString(text).asJsonObject
        }
    }
}

/**
 * 对局结果更新信息
 */
data class MatchResultUpdate(
    val winnerOldElo: Int,
    val winnerNewElo: Int,
    val winnerDelta: Int,
    val loserOldElo: Int,
    val loserNewElo: Int,
    val loserDelta: Int,
)
