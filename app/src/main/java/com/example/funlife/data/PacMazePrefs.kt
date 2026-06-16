package com.example.funlife.data

import android.content.Context
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.PacMazeMovementMode

/**
 * 豆人迷宫本地偏好（按 userId 隔离）。
 */
class PacMazePrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun avatarLoadout(userId: Long): PacMazeAvatarLoadout {
        val legacyCharacter = prefs.getString(characterKey(userId), null)
        val skinRaw = prefs.getString(skinKey(userId), null)
        val skinId = when {
            skinRaw != null -> PacMazeSkinId.fromStorage(skinRaw)
            legacyCharacter != null -> PacMazeSkinId.fromStorage(legacyCharacter)
            else -> PacMazeSkinId.CLASSIC_PAC
        }
        val trailId = PacMazeTrailId.fromStorage(prefs.getString(trailKey(userId), null))
        return PacMazeAvatarLoadout(skinId = skinId, trailId = trailId)
    }

    fun setAvatarLoadout(userId: Long, loadout: PacMazeAvatarLoadout) {
        prefs.edit()
            .putString(skinKey(userId), loadout.skinId.storageKey)
            .putString(trailKey(userId), loadout.trailId.storageKey)
            .putString(characterKey(userId), loadout.skinId.storageKey)
            .apply()
    }

    fun selectedCharacterId(userId: Long): PacMazeCharacterId {
        val loadout = avatarLoadout(userId)
        return loadout.skinId.legacyCharacterId() ?: PacMazeCharacterId.CLASSIC_PAC
    }

    fun setSelectedCharacterId(userId: Long, characterId: PacMazeCharacterId) {
        val skinId = PacMazeSkinId.fromLegacy(characterId)
        setAvatarLoadout(
            userId,
            PacMazeAvatarLoadout(
                skinId = skinId,
                trailId = PacMazeCosmeticCatalog.recommendedTrail(skinId),
            ),
        )
    }

    fun setSelectedSkin(userId: Long, skinId: PacMazeSkinId) {
        val current = avatarLoadout(userId)
        setAvatarLoadout(userId, current.copy(skinId = skinId))
    }

    fun setSelectedTrail(userId: Long, trailId: PacMazeTrailId) {
        val current = avatarLoadout(userId)
        setAvatarLoadout(userId, current.copy(trailId = trailId))
    }

    fun playerDrawScale(userId: Long): Float {
        migrateInflatedPlayerScaleIfNeeded(userId)
        migrateDeflatedPlayerScaleIfNeeded(userId)
        return prefs.getFloat(scaleKey(userId), 1f).coerceIn(0.5f, 3.5f)
    }

    /**
     * ikun 位图已独立定标：曾拉到 200%+ 或 50% 救场的，统一复位 100%。
     */
    private fun migrateInflatedPlayerScaleIfNeeded(userId: Long) {
        val flag = scaleNormMigrationKey(userId)
        if (prefs.getBoolean(flag, false)) return
        val raw = prefs.getFloat(scaleKey(userId), 1f)
        if (raw > 1.4f || raw < 0.85f) {
            prefs.edit().putFloat(scaleKey(userId), 1f).apply()
        }
        prefs.edit().putBoolean(flag, true).apply()
    }

    /** v1 未覆盖 50% 等过小滑条；单独复位一次。 */
    private fun migrateDeflatedPlayerScaleIfNeeded(userId: Long) {
        val flag = scaleDeflatedMigrationKey(userId)
        if (prefs.getBoolean(flag, false)) return
        val raw = prefs.getFloat(scaleKey(userId), 1f)
        if (raw < 0.85f) {
            prefs.edit().putFloat(scaleKey(userId), 1f).apply()
        }
        prefs.edit().putBoolean(flag, true).apply()
    }

    fun setPlayerDrawScale(userId: Long, scale: Float) {
        prefs.edit()
            .putFloat(scaleKey(userId), scale.coerceIn(0.5f, 3.5f))
            .apply()
    }

    fun mapWidthScale(userId: Long): Float =
        prefs.getFloat(mapWidthKey(userId), 1f).coerceIn(0.7f, 1.5f)

    fun mapHeightScale(userId: Long): Float =
        prefs.getFloat(mapHeightKey(userId), 1f).coerceIn(0.7f, 1.5f)

    fun setMapWidthScale(userId: Long, scale: Float) {
        prefs.edit()
            .putFloat(mapWidthKey(userId), scale.coerceIn(0.7f, 1.5f))
            .apply()
    }

    fun setMapHeightScale(userId: Long, scale: Float) {
        prefs.edit()
            .putFloat(mapHeightKey(userId), scale.coerceIn(0.7f, 1.5f))
            .apply()
    }

    fun playHudPanelsExpanded(userId: Long): Boolean =
        prefs.getBoolean(hudPanelsKey(userId), false)

    fun pacMazeMovementMode(userId: Long): PacMazeMovementMode {
        val raw = prefs.getString(movementModeKey(userId), PacMazeMovementMode.Default.name)
            ?: PacMazeMovementMode.Default.name
        return runCatching { PacMazeMovementMode.valueOf(raw) }
            .getOrDefault(PacMazeMovementMode.Default)
    }

    fun setPacMazeMovementMode(userId: Long, mode: PacMazeMovementMode) {
        prefs.edit().putString(movementModeKey(userId), mode.name).apply()
    }

    fun setPlayHudPanelsExpanded(userId: Long, expanded: Boolean) {
        prefs.edit().putBoolean(hudPanelsKey(userId), expanded).apply()
    }

    fun ghostCodexUnlockMask(userId: Long): Int =
        prefs.getInt(ghostCodexKey(userId), 0)

    fun unlockAllGhostCodexForTesting(userId: Long) {
        if (!com.example.funlife.ui.screens.pacmaze.PacMazeTestUnlock.enabled) return
        val mask = GhostKind.entries.fold(0) { acc, kind -> acc or kind.codexBit }
        prefs.edit().putInt(ghostCodexKey(userId), mask).apply()
    }

    fun isGhostCodexUnlocked(userId: Long, kind: GhostKind): Boolean {
        if (com.example.funlife.ui.screens.pacmaze.PacMazeTestUnlock.enabled) return true
        return (ghostCodexUnlockMask(userId) and kind.codexBit()) != 0
    }

    fun unlockGhostCodex(userId: Long, kind: GhostKind) {
        val next = ghostCodexUnlockMask(userId) or kind.codexBit()
        prefs.edit().putInt(ghostCodexKey(userId), next).apply()
    }

    fun ghostDeathCount(userId: Long, kind: GhostKind): Int =
        prefs.getInt(ghostDeathKey(userId, kind), 0)

    fun ghostEatCount(userId: Long, kind: GhostKind): Int =
        prefs.getInt(ghostEatKey(userId, kind), 0)

    fun recordGhostEncounter(userId: Long, kind: GhostKind, killedPlayer: Boolean, playerAte: Boolean) {
        unlockGhostCodex(userId, kind)
        val editor = prefs.edit()
        if (killedPlayer) {
            editor.putInt(ghostDeathKey(userId, kind), ghostDeathCount(userId, kind) + 1)
        }
        if (playerAte) {
            editor.putInt(ghostEatKey(userId, kind), ghostEatCount(userId, kind) + 1)
        }
        editor.apply()
    }

    fun mazeStats(userId: Long): com.example.funlife.data.model.PacMazeMazeStats {
        val raw = prefs.getString(mazeStatsKey(userId), null) ?: return com.example.funlife.data.model.PacMazeMazeStats()
        return runCatching {
            val parts = raw.split("|")
            com.example.funlife.data.model.PacMazeMazeStats(
                dailyDate = parts.getOrElse(0) { "" },
                dailyBestTimeMs = parts.getOrElse(1) { "0" }.toLongOrNull() ?: 0L,
                dailyBestStars = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0,
                bestTimeByDifficulty = parseLongMap(parts.getOrElse(3) { "" }),
                bestStarsByDifficulty = parseIntMap(parts.getOrElse(4) { "" }),
                lastDifficultyId = parts.getOrElse(5) { "standard" },
                lastContractId = parts.getOrElse(6) { "none" },
                useDailyChallenge = parts.getOrElse(7) { "1" } != "0",
            )
        }.getOrDefault(com.example.funlife.data.model.PacMazeMazeStats())
    }

    fun setMazeStats(userId: Long, stats: com.example.funlife.data.model.PacMazeMazeStats) {
        val encoded = listOf(
            stats.dailyDate,
            stats.dailyBestTimeMs.toString(),
            stats.dailyBestStars.toString(),
            encodeLongMap(stats.bestTimeByDifficulty),
            encodeIntMap(stats.bestStarsByDifficulty),
            stats.lastDifficultyId,
            stats.lastContractId,
            if (stats.useDailyChallenge) "1" else "0",
        ).joinToString("|")
        prefs.edit().putString(mazeStatsKey(userId), encoded).apply()
    }

    private fun parseLongMap(raw: String): Map<String, Long> =
        if (raw.isBlank()) emptyMap()
        else raw.split(",").mapNotNull { entry ->
            val kv = entry.split(":")
            if (kv.size == 2) kv[0] to (kv[1].toLongOrNull() ?: return@mapNotNull null) else null
        }.toMap()

    private fun parseIntMap(raw: String): Map<String, Int> =
        if (raw.isBlank()) emptyMap()
        else raw.split(",").mapNotNull { entry ->
            val kv = entry.split(":")
            if (kv.size == 2) kv[0] to (kv[1].toIntOrNull() ?: return@mapNotNull null) else null
        }.toMap()

    private fun encodeLongMap(map: Map<String, Long>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun encodeIntMap(map: Map<String, Int>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun mazeStatsKey(userId: Long) = "u${userId}_maze_stats"

    private fun GhostKind.codexBit(): Int = codexBit

    private fun ghostCodexKey(userId: Long) = "u${userId}_ghost_codex"

    private fun ghostDeathKey(userId: Long, kind: GhostKind) = "u${userId}_ghost_death_${kind.id}"

    private fun ghostEatKey(userId: Long, kind: GhostKind) = "u${userId}_ghost_eat_${kind.id}"

    private fun characterKey(userId: Long) = "u${userId}_selected_character"

    private fun skinKey(userId: Long) = "u${userId}_selected_skin"

    private fun trailKey(userId: Long) = "u${userId}_selected_trail"

    private fun scaleKey(userId: Long) = "u${userId}_player_draw_scale"
    private fun scaleNormMigrationKey(userId: Long) = "u${userId}_player_scale_norm_v1"
    private fun scaleDeflatedMigrationKey(userId: Long) = "u${userId}_player_scale_norm_v2"

    private fun mapWidthKey(userId: Long) = "u${userId}_map_width_scale"

    private fun mapHeightKey(userId: Long) = "u${userId}_map_height_scale"

    private fun hudPanelsKey(userId: Long) = "u${userId}_play_hud_panels_expanded"
    private fun movementModeKey(userId: Long) = "u${userId}_pac_maze_movement_mode"

    fun versusRating(userId: Long): Int =
        prefs.getInt(versusRatingKey(userId), 1200)

    fun versusGames(userId: Long): Int =
        prefs.getInt(versusGamesKey(userId), 0)

    fun coopAssists(userId: Long): Int =
        prefs.getInt(coopAssistsKey(userId), 0)

    fun recordVersusResult(userId: Long, won: Boolean, draw: Boolean, eloDelta: Int) {
        val games = versusGames(userId) + 1
        prefs.edit()
            .putInt(versusRatingKey(userId), (versusRating(userId) + eloDelta).coerceAtLeast(100))
            .putInt(versusGamesKey(userId), games)
            .putInt(versusWinsKey(userId), prefs.getInt(versusWinsKey(userId), 0) + if (won && !draw) 1 else 0)
            .apply()
    }

    fun incrementCoopAssists(userId: Long) {
        prefs.edit().putInt(coopAssistsKey(userId), coopAssists(userId) + 1).apply()
    }

    fun ikunDisclosureAgreedVersion(userId: Long): Long =
        prefs.getLong(ikunDisclosureKey(userId), 0L)

    fun setIkunDisclosureAgreedVersion(userId: Long, version: Long) {
        prefs.edit().putLong(ikunDisclosureKey(userId), version.coerceAtLeast(0L)).apply()
    }

    private fun ikunDisclosureKey(userId: Long) = "u${userId}_ikun_disclosure_version"

    private fun versusRatingKey(userId: Long) = "u${userId}_versus_rating"
    private fun versusGamesKey(userId: Long) = "u${userId}_versus_games"
    private fun versusWinsKey(userId: Long) = "u${userId}_versus_wins"
    private fun coopAssistsKey(userId: Long) = "u${userId}_coop_assists"

    companion object {
        private const val PREFS_NAME = "pac_maze_prefs"
    }
}
