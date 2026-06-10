package com.example.funlife.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.PacMazePrefs
import com.example.funlife.data.model.PacMazeProgress
import com.example.funlife.repository.PacMazeProgressRepository
import com.example.funlife.social.game.engine.pacmaze.CampaignLevelSource
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.EndlessLevelSource
import com.example.funlife.social.game.engine.pacmaze.MazeLevelSource
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeInputBuffer
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeLoadParams
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapLoader
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunPayload
import com.example.funlife.social.game.engine.pacmaze.PacMazeSimulation
import com.example.funlife.social.game.engine.pacmaze.PacMazeStarEvaluator
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazeMenuStep
import com.example.funlife.ui.screens.pacmaze.PacMazePlayMode
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PacMazeUiState(
    val screenPhase: PacMazePhase = PacMazePhase.MENU,
    val menuStep: PacMazeMenuStep = PacMazeMenuStep.MODE_SELECT,
    val selectedMode: PacMazePlayMode = PacMazePlayMode.SOLO,
    val runMode: PacMazeRunMode = PacMazeRunMode.CAMPAIGN,
    val levelId: Int = 1,
    val endlessWave: Int = 0,
    val maxLevelReached: Int = 1,
    val highScore: Int = 0,
    val starsBitmask: Int = 0,
    val endlessBestScore: Int = 0,
    val endlessBestWave: Int = 0,
    val mazeBestTimeMs: Long = 0,
    val deathsThisRun: Int = 0,
    val runStartedAtMs: Long = 0L,
    val world: PacMazeWorldState? = null,
    val levelConfig: PacMazeLevelConfig? = null,
    val levelJson: String = "",
    val elapsedSeconds: Int = 0,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val mapThemeId: PacMazeMapThemeId = PacMazeMapThemeId.CYBERPUNK,
    val selectedCharacterId: PacMazeCharacterId = PacMazeCharacterId.CLASSIC_PAC,
    val playerDrawScale: Float = 1f,
)

class PacMazeLocalViewModel(
    private val currentUserId: Long,
    private val progressRepository: PacMazeProgressRepository,
    private val appContext: Context,
) : ViewModel() {

    private val prefs = PacMazePrefs(appContext)
    private val campaignSource = CampaignLevelSource(appContext)
    private val endlessSource = EndlessLevelSource(appContext)
    private val mazeSource = MazeLevelSource()

    val inputBuffer = PacMazeInputBuffer()

    private val _uiState = MutableStateFlow(PacMazeUiState())
    val uiState: StateFlow<PacMazeUiState> = _uiState.asStateFlow()

    private val _renderFrame = MutableStateFlow<PacMazeRenderFrame?>(null)
    val renderFrame: StateFlow<PacMazeRenderFrame?> = _renderFrame.asStateFlow()

    private var rngSeed: Long = currentUserId xor 0x5A17_4D41L
    private var pendingAttack = false

    init {
        val savedCharacter = prefs.selectedCharacterId(currentUserId)
        val savedScale = prefs.playerDrawScale(currentUserId)
        _uiState.update {
            it.copy(selectedCharacterId = savedCharacter, playerDrawScale = savedScale)
        }
        viewModelScope.launch {
            try {
                val progress = progressRepository.ensureProgress(currentUserId)
                applyProgress(progress)
                _uiState.update {
                    it.copy(
                        levelId = progress.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS),
                        loadError = null,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load pac maze progress for user=$currentUserId", e)
                _uiState.update {
                    it.copy(
                        loadError = "进度加载失败，可直接开始第 1 关",
                        maxLevelReached = 1,
                        levelId = 1,
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun pushDirection(direction: Direction?) {
        try {
            inputBuffer.push(PacMazeConstants.PLAYER_ID, direction)
        } catch (e: Exception) {
            Log.e(TAG, "pushDirection failed dir=$direction", e)
        }
    }

    fun startLevel(levelId: Int) {
        viewModelScope.launch {
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.CAMPAIGN,
                    levelId = levelId,
                    seed = rngSeed,
                ),
            )
        }
    }

    fun startPracticeLevel(levelId: Int) {
        viewModelScope.launch {
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.PRACTICE,
                    levelId = levelId,
                    seed = rngSeed,
                ),
            )
        }
    }

    fun startEndless() {
        viewModelScope.launch {
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.ENDLESS,
                    seed = rngSeed,
                    endlessWave = 1,
                ),
            )
        }
    }

    fun startMaze() {
        viewModelScope.launch {
            rngSeed = System.currentTimeMillis() xor currentUserId
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.MAZE,
                    seed = rngSeed,
                ),
            )
        }
    }

    private suspend fun startRun(params: PacMazeLoadParams) {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        try {
            val payload = when (params.runMode) {
                PacMazeRunMode.CAMPAIGN, PacMazeRunMode.PRACTICE -> campaignSource.load(params)
                PacMazeRunMode.ENDLESS -> endlessSource.load(params)
                PacMazeRunMode.MAZE -> mazeSource.load(params)
            }
            applyPayload(payload, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start run mode=${params.runMode}", e)
            _uiState.update {
                it.copy(
                    screenPhase = PacMazePhase.MENU,
                    world = null,
                    levelConfig = null,
                    levelJson = "",
                    loadError = "关卡加载失败，请稍后重试",
                )
            }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun applyPayload(payload: PacMazeRunPayload, params: PacMazeLoadParams) {
        val world = PacMazeMapLoader.buildInitialWorld(payload.level, payload.json, rngSeed)
        val themeId = PacMazeThemeRegistry.themeForLevel(payload.themeLevelId.coerceAtLeast(1))
        _uiState.update {
            it.copy(
                screenPhase = PacMazePhase.PLAYING,
                runMode = payload.runMode,
                levelId = payload.level.id,
                endlessWave = if (payload.runMode == PacMazeRunMode.ENDLESS) params.endlessWave else 0,
                levelConfig = payload.level,
                levelJson = payload.json,
                world = world,
                elapsedSeconds = 0,
                deathsThisRun = 0,
                runStartedAtMs = System.currentTimeMillis(),
                loadError = null,
                mapThemeId = themeId,
            )
        }
        _renderFrame.value = PacMazeRenderFrame(current = world, previous = null, blend = 1f)
    }

    fun requestAttack() {
        pendingAttack = true
    }

    fun tickFrame(): PacMazeWorldState? {
        try {
            val state = _uiState.value
            val world = state.world ?: return null
            val config = state.levelConfig ?: return null
            if (world.phase != PacMazePhase.PLAYING && world.phase != PacMazePhase.LEVEL_CLEAR) return world

            val previousWorld = world
            val prevLives = world.lives
            val pacInput = inputBuffer.poll(PacMazeConstants.PLAYER_ID)
            val fireAttack = pendingAttack
            pendingAttack = false
            val next = PacMazeSimulation.tick(world, pacInput, config, fireAttack = fireAttack)
            val elapsed = if (world.tick % 60L == 0L) state.elapsedSeconds + 1 else state.elapsedSeconds
            val deaths = if (next.lives < prevLives) state.deathsThisRun + (prevLives - next.lives) else state.deathsThisRun

            when (next.phase) {
                PacMazePhase.LEVEL_CLEAR -> handleLevelClear(next, previousWorld, elapsed, deaths)
                PacMazePhase.GAME_OVER -> handleGameOver(next, previousWorld, elapsed, deaths)
                else -> {
                    _uiState.update {
                        it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.PLAYING)
                    }
                    _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
                }
            }
            return next
        } catch (e: Exception) {
            Log.e(TAG, "tickFrame failed", e)
            inputBuffer.clear(PacMazeConstants.PLAYER_ID)
            return null
        }
    }

    private fun handleLevelClear(
        next: PacMazeWorldState,
        previousWorld: PacMazeWorldState,
        elapsed: Int,
        deaths: Int,
    ) {
        val state = _uiState.value
        when (state.runMode) {
            PacMazeRunMode.ENDLESS -> {
                _uiState.update {
                    it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.PLAYING, isLoading = true)
                }
                viewModelScope.launch {
                    try {
                        progressRepository.saveEndlessResult(currentUserId, next.score, state.endlessWave)
                        val progress = progressRepository.getProgress(currentUserId)
                        if (progress != null) applyProgress(progress)
                        advanceEndlessWave(next, previousWorld, elapsed, deaths)
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            PacMazeRunMode.MAZE -> {
                _uiState.update {
                    it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.LEVEL_CLEAR)
                }
                _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
                onMazeCleared(next.score, elapsed)
            }
            PacMazeRunMode.PRACTICE -> {
                _uiState.update {
                    it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.LEVEL_CLEAR)
                }
                _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
            }
            PacMazeRunMode.CAMPAIGN -> {
                _uiState.update {
                    it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.LEVEL_CLEAR)
                }
                _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
                onCampaignCleared(next.score, elapsed, deaths)
            }
        }
    }

    private suspend fun advanceEndlessWave(
        cleared: PacMazeWorldState,
        previousWorld: PacMazeWorldState,
        elapsed: Int,
        deaths: Int,
    ) {
        val state = _uiState.value
        val nextWave = state.endlessWave + 1
        val payload = endlessSource.load(
            PacMazeLoadParams(
                runMode = PacMazeRunMode.ENDLESS,
                seed = rngSeed,
                endlessWave = nextWave,
            ),
        )
        val fresh = PacMazeMapLoader.buildInitialWorld(payload.level, payload.json, rngSeed)
        val merged = fresh.copy(
            score = cleared.score,
            lives = cleared.lives.coerceAtLeast(1),
            phase = PacMazePhase.PLAYING,
        )
        _uiState.update {
            it.copy(
                world = merged,
                levelConfig = payload.level,
                levelJson = payload.json,
                levelId = payload.level.id,
                endlessWave = nextWave,
                elapsedSeconds = elapsed,
                deathsThisRun = deaths,
                screenPhase = PacMazePhase.PLAYING,
                mapThemeId = PacMazeThemeRegistry.themeForLevel(payload.themeLevelId),
            )
        }
        _renderFrame.value = PacMazeRenderFrame(current = merged, previous = previousWorld, blend = 1f)
    }

    private fun handleGameOver(
        next: PacMazeWorldState,
        previousWorld: PacMazeWorldState,
        elapsed: Int,
        deaths: Int,
    ) {
        val state = _uiState.value
        if (state.runMode == PacMazeRunMode.PRACTICE) {
            val config = state.levelConfig ?: return
            val revived = PacMazeSimulation.restartLevel(
                level = config,
                json = state.levelJson,
                seed = rngSeed,
                lives = PacMazeConstants.INITIAL_LIVES,
                score = next.score,
            )
            _uiState.update {
                it.copy(
                    world = revived,
                    screenPhase = PacMazePhase.PLAYING,
                    deathsThisRun = deaths,
                    elapsedSeconds = elapsed,
                )
            }
            _renderFrame.value = PacMazeRenderFrame(current = revived, previous = previousWorld, blend = 1f)
            return
        }
        _uiState.update {
            it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.GAME_OVER)
        }
        _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
        onGameOver(next.score)
    }

    fun updateRenderBlend(blend: Float) {
        val frame = _renderFrame.value ?: return
        _renderFrame.value = frame.copy(blend = blend.coerceIn(0f, 1f))
    }

    fun pauseGame() {
        _uiState.update { it.copy(screenPhase = PacMazePhase.PAUSED) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(screenPhase = PacMazePhase.PLAYING) }
    }

    fun selectMode(mode: PacMazePlayMode) {
        if (!mode.playable) return
        _uiState.update {
            it.copy(
                selectedMode = mode,
                menuStep = PacMazeMenuStep.CHARACTER_SELECT,
                loadError = null,
            )
        }
    }

    fun selectCharacter(characterId: PacMazeCharacterId) {
        prefs.setSelectedCharacterId(currentUserId, characterId)
        _uiState.update { it.copy(selectedCharacterId = characterId) }
    }

    fun setPlayerDrawScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 1.5f)
        prefs.setPlayerDrawScale(currentUserId, clamped)
        _uiState.update { it.copy(playerDrawScale = clamped) }
    }

    fun confirmCharacterAndGoToLevels() {
        when (_uiState.value.selectedMode) {
            PacMazePlayMode.ENDLESS -> startEndless()
            PacMazePlayMode.MAZE -> startMaze()
            else -> _uiState.update {
                it.copy(menuStep = PacMazeMenuStep.LEVEL_SELECT, loadError = null)
            }
        }
    }

    fun backToModeSelect() {
        _uiState.update {
            it.copy(menuStep = PacMazeMenuStep.MODE_SELECT, loadError = null)
        }
    }

    fun backToCharacterSelect() {
        _uiState.update {
            it.copy(menuStep = PacMazeMenuStep.CHARACTER_SELECT, loadError = null)
        }
    }

    fun backToMenu() {
        inputBuffer.clear(PacMazeConstants.PLAYER_ID)
        _uiState.update {
            it.copy(
                screenPhase = PacMazePhase.MENU,
                menuStep = when (it.selectedMode) {
                    PacMazePlayMode.SOLO -> PacMazeMenuStep.LEVEL_SELECT
                    else -> PacMazeMenuStep.MODE_SELECT
                },
                world = null,
                levelConfig = null,
                levelJson = "",
                runMode = PacMazeRunMode.CAMPAIGN,
                endlessWave = 0,
            )
        }
    }

    fun backToHubRoot() {
        inputBuffer.clear(PacMazeConstants.PLAYER_ID)
        _uiState.update {
            it.copy(
                screenPhase = PacMazePhase.MENU,
                menuStep = PacMazeMenuStep.MODE_SELECT,
                world = null,
                levelConfig = null,
                levelJson = "",
                runMode = PacMazeRunMode.CAMPAIGN,
                endlessWave = 0,
            )
        }
    }

    fun nextLevel() {
        val state = _uiState.value
        if (state.runMode != PacMazeRunMode.CAMPAIGN) {
            backToMenu()
            return
        }
        val nextId = (state.levelId + 1).coerceAtMost(PacMazeLevelCatalog.TOTAL_LEVELS)
        if (nextId > state.levelId) {
            startLevel(nextId)
        } else {
            backToMenu()
        }
    }

    fun retryLevel() {
        val state = _uiState.value
        val config = state.levelConfig ?: return
        val world = PacMazeSimulation.restartLevel(
            level = config,
            json = state.levelJson,
            seed = rngSeed,
            lives = PacMazeConstants.INITIAL_LIVES,
            score = if (state.runMode == PacMazeRunMode.ENDLESS) state.world?.score ?: 0 else 0,
        )
        _uiState.update {
            it.copy(
                world = world,
                screenPhase = PacMazePhase.PLAYING,
                deathsThisRun = 0,
                elapsedSeconds = 0,
                runStartedAtMs = System.currentTimeMillis(),
            )
        }
    }

    fun evaluateStars(score: Int, elapsedSeconds: Int, deaths: Int): Int {
        val criteria = _uiState.value.levelConfig?.starCriteria ?: return 1
        return PacMazeStarEvaluator.evaluate(criteria, score, elapsedSeconds, deaths)
    }

    private fun onCampaignCleared(score: Int, elapsedSeconds: Int, deaths: Int) {
        viewModelScope.launch {
            val levelId = _uiState.value.levelId.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS)
            val stars = evaluateStars(score, elapsedSeconds, deaths)
            progressRepository.saveLevelResult(currentUserId, levelId, score, stars)
            val progress = progressRepository.getProgress(currentUserId)
            if (progress != null) applyProgress(progress)
        }
    }

    private fun onMazeCleared(score: Int, elapsedSeconds: Int) {
        viewModelScope.launch {
            val elapsedMs = (System.currentTimeMillis() - _uiState.value.runStartedAtMs).coerceAtLeast(0L)
            progressRepository.saveMazeResult(currentUserId, elapsedMs, score)
            val progress = progressRepository.getProgress(currentUserId)
            if (progress != null) applyProgress(progress)
        }
    }

    private fun onGameOver(score: Int) {
        viewModelScope.launch {
            when (_uiState.value.runMode) {
                PacMazeRunMode.ENDLESS -> progressRepository.saveEndlessResult(
                    currentUserId,
                    score,
                    _uiState.value.endlessWave,
                )
                else -> progressRepository.saveGameOver(currentUserId, score)
            }
            val progress = progressRepository.getProgress(currentUserId)
            if (progress != null) applyProgress(progress)
        }
    }

    private fun applyProgress(progress: PacMazeProgress) {
        _uiState.update {
            it.copy(
                maxLevelReached = progress.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS),
                highScore = progress.highScore,
                starsBitmask = progress.starsBitmask,
                endlessBestScore = progress.endlessBestScore,
                endlessBestWave = progress.endlessBestWave,
                mazeBestTimeMs = progress.mazeBestTimeMs,
            )
        }
    }

    private companion object {
        private const val TAG = "PacMazeLocalViewModel"
    }
}
