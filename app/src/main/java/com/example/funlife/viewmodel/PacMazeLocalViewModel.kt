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
import com.example.funlife.social.game.engine.pacmaze.PacMazeInputController
import com.example.funlife.social.game.engine.pacmaze.PacMazeMovementMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeRawJoystickSample
import com.example.funlife.social.game.engine.pacmaze.PacMazeTickInput
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeLoadParams
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapLoader
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeMotion
import com.example.funlife.social.game.engine.pacmaze.renderInterpolationSnapshot
import com.example.funlife.social.game.engine.pacmaze.playerPacMovedFrom
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunPayload
import com.example.funlife.data.model.PacMazeMazeStats
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeDifficulty
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeExploration
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeMechanics
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunProfile
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeVariant
import com.example.funlife.social.game.engine.pacmaze.PacMazeRules
import com.example.funlife.social.game.engine.pacmaze.PacMazeSimulation
import com.example.funlife.social.game.engine.pacmaze.PacMazeStarEvaluator
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazeIkunDisclosureConfig
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazeSfx
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.PacMazeMenuRoute
import com.example.funlife.ui.screens.pacmaze.PacMazeTestUnlock
import com.example.funlife.ui.screens.pacmaze.PacMazePlayMode
import com.example.funlife.ui.screens.pacmaze.PacMazeSkinSeries
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
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
    val menuRouteStack: List<PacMazeMenuRoute> = listOf(PacMazeMenuRoute.ModeSelect),
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
    val mazeStats: PacMazeMazeStats = PacMazeMazeStats(),
    val mazeDifficulty: PacMazeMazeDifficulty = PacMazeMazeDifficulty.STANDARD,
    val mazeContract: PacMazeMazeContract = PacMazeMazeContract.NONE,
    val mazeUseDailyChallenge: Boolean = true,
    val mazeRunProfile: PacMazeMazeRunProfile = PacMazeMazeRunProfile(),
    val mazeRandomPreviewSeed: Long = 0L,
    val lastMazeStars: Int = 0,
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
    val avatarLoadout: PacMazeAvatarLoadout = PacMazeAvatarLoadout(),
    val playerDrawScale: Float = 1f,
    val movementMode: PacMazeMovementMode = PacMazeMovementMode.Default,
    val mapWidthScale: Float = 1f,
    val mapHeightScale: Float = 1f,
    val playHudPanelsExpanded: Boolean = false,
    val ghostCodexUnlockMask: Int = 0,
    val ikunDisclosureVisible: Boolean = false,
    val ikunDisclosureLoading: Boolean = false,
) {
    val currentMenuRoute: PacMazeMenuRoute
        get() = menuRouteStack.lastOrNull() ?: PacMazeMenuRoute.ModeSelect
}

class PacMazeLocalViewModel(
    val currentUserId: Long,
    private val progressRepository: PacMazeProgressRepository,
    private val appContext: Context,
) : ViewModel() {

    private val prefs = PacMazePrefs(appContext)
    private val campaignSource = CampaignLevelSource(appContext)
    private val endlessSource = EndlessLevelSource(appContext)
    private val mazeSource = MazeLevelSource()

    private val inputController = PacMazeInputController()

    /** 仿真用世界状态；uiState.world 仅在 HUD 需要时同步，减轻每 tick 重组。 */
    private var simulationWorld: PacMazeWorldState? = null

    private val _uiState = MutableStateFlow(PacMazeUiState())
    val uiState: StateFlow<PacMazeUiState> = _uiState.asStateFlow()

    private val _renderFrame = MutableStateFlow<PacMazeRenderFrame?>(null)
    val renderFrame: StateFlow<PacMazeRenderFrame?> = _renderFrame.asStateFlow()

    /** 最近一次 tick 前的 sim 态，供多 tick 帧内插值（= 末 tick 的前一 sim 步）。 */
    private var penultimateSimSnapshot: PacMazeWorldState? = null
    /** tick 用关卡配置缓存，避免 uiState 同步间隙导致 tick 失败、accum 堆债。 */
    private var cachedLevelConfig: PacMazeLevelConfig? = null

    fun peekPenultimateSimSnapshot(): PacMazeWorldState? = penultimateSimSnapshot

    private var rngSeed: Long = currentUserId xor 0x5A17_4D41L
    private var pendingAttack = false
    private var pendingIkunSeries: PacMazeSkinSeries? = null
    private var pendingIkunSkinId: PacMazeSkinId? = null

    init {
        val savedLoadout = prefs.avatarLoadout(currentUserId)
        val savedScale = prefs.playerDrawScale(currentUserId)
        val savedMapW = prefs.mapWidthScale(currentUserId)
        val savedMapH = prefs.mapHeightScale(currentUserId)
        val savedHudExpanded = prefs.playHudPanelsExpanded(currentUserId)
        val savedMovementMode = prefs.pacMazeMovementMode(currentUserId)
        val codexMask = prefs.ghostCodexUnlockMask(currentUserId)
        _uiState.update {
            it.copy(
                selectedCharacterId = savedLoadout.skinId.legacyCharacterId() ?: PacMazeCharacterId.CLASSIC_PAC,
                avatarLoadout = savedLoadout,
                playerDrawScale = savedScale,
                mapWidthScale = savedMapW,
                mapHeightScale = savedMapH,
                playHudPanelsExpanded = savedHudExpanded,
                movementMode = savedMovementMode,
                ghostCodexUnlockMask = codexMask,
            )
        }
        viewModelScope.launch {
            PacMazeSfx.prefetch(appContext)
            PacMazeIkunDisclosureConfig.loadFromCache(appContext)
            PacMazeIkunDisclosureConfig.refreshAsync(appContext)
            try {
                if (PacMazeTestUnlock.enabled) {
                    progressRepository.unlockAllLevelsForTesting(currentUserId)
                    prefs.unlockAllGhostCodexForTesting(currentUserId)
                }
                val progress = progressRepository.ensureProgress(currentUserId)
                applyProgress(progress)
                val effectiveMax = PacMazeTestUnlock.effectiveMaxLevelReached(progress.maxLevelReached)
                _uiState.update {
                    it.copy(
                        levelId = effectiveMax,
                        ghostCodexUnlockMask = PacMazeTestUnlock.ghostCodexMask(prefs.ghostCodexUnlockMask(currentUserId)),
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

    /** UI 层上报原始摇杆样本（drag 与 tick 采样均可调用）。 */
    fun updateJoystickRaw(sample: PacMazeRawJoystickSample) {
        inputController.submitRaw(sample)
    }

    fun syncJoystickSample(offsetX: Float, offsetY: Float, maxRadius: Float, fingerDown: Boolean) {
        inputController.submitRaw(
            PacMazeRawJoystickSample(
                offsetX = offsetX,
                offsetY = offsetY,
                maxRadius = maxRadius.coerceAtLeast(1f),
                fingerDown = fingerDown,
            ),
        )
    }

    fun clearLoadError() {
        _uiState.update { it.copy(loadError = null) }
    }

    /** 手指离开摇杆。 */
    fun releaseJoystick() {
        inputController.submitRaw(PacMazeRawJoystickSample.Released)
    }

    /** 开局/重开：清空全部输入，防止上一局残留导致自动移动。 */
    fun resetJoystickInput() {
        inputController.reset()
    }

    fun startLevel(levelId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMode = PacMazePlayMode.SOLO) }
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.CAMPAIGN,
                    levelId = levelId,
                    seed = rngSeed,
                ),
            )
        }
    }

    /** 大厅「继续闯关」：用当前解锁进度关直接开局。 */
    fun continueCampaign() {
        val levelId = _uiState.value.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS)
        startLevel(levelId)
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
            val maxLevel = _uiState.value.maxLevelReached
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.ENDLESS,
                    seed = rngSeed,
                    endlessWave = 1,
                    maxLevelReached = maxLevel,
                ),
            )
        }
    }

    fun startMaze() {
        viewModelScope.launch {
            val state = _uiState.value
            val profile = state.mazeRunProfile
            if (profile.seedMode == PacMazeMazeSeedMode.RANDOM) {
                rngSeed = System.currentTimeMillis() xor currentUserId
            } else {
                rngSeed = PacMazeMazeRunOptions.dailySeed()
            }
            prefs.setMazeStats(
                currentUserId,
                state.mazeStats.copy(
                    lastDifficultyId = profile.difficulty.id,
                    lastContractId = profile.contract.id,
                    useDailyChallenge = profile.seedMode == PacMazeMazeSeedMode.DAILY,
                ),
            )
            startRun(
                PacMazeLoadParams(
                    runMode = PacMazeRunMode.MAZE,
                    seed = rngSeed,
                    userId = currentUserId,
                    mazeProfile = profile,
                    mazeDifficultyId = profile.difficulty.id,
                    mazeContractId = profile.contract.id,
                    mazeDailyChallenge = profile.seedMode == PacMazeMazeSeedMode.DAILY,
                    mazeVariantId = profile.variant.id,
                    mazeKeyModeId = profile.resolvedKeyMode(rngSeed).id,
                    mazeMutatorId = profile.resolvedMutator(rngSeed).id,
                ),
            )
        }
    }

    fun updateMazeProfile(mutator: (PacMazeMazeRunProfile) -> PacMazeMazeRunProfile) {
        _uiState.update { state ->
            val next = mutator(state.mazeRunProfile)
            state.copy(
                mazeRunProfile = next,
                mazeDifficulty = next.difficulty,
                mazeContract = next.contract,
                mazeUseDailyChallenge = next.seedMode == PacMazeMazeSeedMode.DAILY,
            )
        }
    }

    fun refreshMazeRandomPreviewSeed() {
        _uiState.update {
            it.copy(mazeRandomPreviewSeed = System.nanoTime() xor currentUserId)
        }
    }

    fun openMazePlayGate(seedMode: PacMazeMazeSeedMode) {
        updateMazeProfile { it.copy(seedMode = seedMode) }
        pushMenuRoute(PacMazeMenuRoute.MazePlayGate(seedMode))
    }

    fun openMazeLaunchConfirm() = pushMenuRoute(PacMazeMenuRoute.MazeLaunchConfirm)
    fun openMazeTrackPicker() = pushMenuRoute(PacMazeMenuRoute.MazeTrackPicker)
    fun openMazeTrackDetail(track: PacMazeMazeDifficulty) = pushMenuRoute(PacMazeMenuRoute.MazeTrackDetail(track))
    fun openMazeContractLab() = pushMenuRoute(PacMazeMenuRoute.MazeContractLab)
    fun openMazeContractDetail(contract: PacMazeMazeContract) = pushMenuRoute(PacMazeMenuRoute.MazeContractDetail(contract))
    fun openMazeArcadeHall() = pushMenuRoute(PacMazeMenuRoute.MazeArcadeHall)
    fun openMazeVariantDetail(variant: PacMazeMazeVariant) = pushMenuRoute(PacMazeMenuRoute.MazeVariantDetail(variant))
    fun openMazeCompetitiveHub() = pushMenuRoute(PacMazeMenuRoute.MazeCompetitiveHub)
    fun openMazeDailyBoard() = pushMenuRoute(PacMazeMenuRoute.MazeDailyBoard)
    fun openMazeWeeklyBoard() = pushMenuRoute(PacMazeMenuRoute.MazeWeeklyBoard)
    fun openMazeGhostReplay() = pushMenuRoute(PacMazeMenuRoute.MazeGhostReplay)
    fun openMazeCodex() = pushMenuRoute(PacMazeMenuRoute.MazeCodex)
    fun openMazeCodexEntry(entryId: String) = pushMenuRoute(PacMazeMenuRoute.MazeCodexEntry(entryId))

    fun spendMazeIntelRevealQuadrant(quadrant: Int) {
        val state = _uiState.value
        val config = state.levelConfig ?: return
        val world = simulationWorld ?: state.world ?: return
        val next = PacMazeMazeMechanics.spendIntelRevealQuadrant(world, config, quadrant)
        if (next == world) return
        simulationWorld = next
        _uiState.update { it.copy(world = next) }
    }

    fun spendMazeIntelKeyHint() {
        val state = _uiState.value
        val config = state.levelConfig ?: return
        val world = simulationWorld ?: state.world ?: return
        val next = PacMazeMazeMechanics.spendIntelKeyDistance(world, config)
        if (next == world) return
        simulationWorld = next
        _uiState.update { it.copy(world = next) }
    }

    fun selectMazeDifficulty(difficulty: PacMazeMazeDifficulty) {
        updateMazeProfile { it.copy(difficulty = difficulty) }
    }

    fun selectMazeContract(contract: PacMazeMazeContract) {
        updateMazeProfile { it.copy(contract = contract) }
    }

    fun setMazeDailyChallenge(enabled: Boolean) {
        updateMazeProfile {
            it.copy(seedMode = if (enabled) PacMazeMazeSeedMode.DAILY else PacMazeMazeSeedMode.RANDOM)
        }
    }

    fun requestRadar() {
        val state = _uiState.value
        val config = state.levelConfig ?: return
        val world = simulationWorld ?: state.world ?: return
        val next = PacMazeMazeExploration.tryPulseRadar(world, config)
        if (next == world) return
        simulationWorld = next
        _uiState.update { it.copy(world = next) }
        _renderFrame.update { frame ->
            frame?.copy(current = next) ?: PacMazeRenderFrame(current = next, previous = world, blend = 1f)
        }
    }

    private suspend fun startRun(params: PacMazeLoadParams) {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        val skinId = _uiState.value.avatarLoadout.skinId
        // 皮肤预热后台进行，不阻塞关卡 JSON / 世界构建
        PacMazeRemoteSkinAnimCache.requestGameplayWarmupAsync(skinId)
        try {
            val (payload, baseWorld) = withContext(Dispatchers.Default) {
                val loaded = when (params.runMode) {
                    PacMazeRunMode.CAMPAIGN, PacMazeRunMode.PRACTICE -> campaignSource.load(params)
                    PacMazeRunMode.ENDLESS -> endlessSource.load(params)
                    PacMazeRunMode.MAZE -> mazeSource.load(params)
                }
                loaded to PacMazeMapLoader.buildInitialWorld(loaded.level, loaded.json, rngSeed)
            }
            applyPayload(payload, params, baseWorld)
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

    private fun applyPayload(
        payload: PacMazeRunPayload,
        params: PacMazeLoadParams,
        prebuiltBaseWorld: PacMazeWorldState,
    ) {
        val loadout = _uiState.value.avatarLoadout
        val bonusAttack = PacMazeCosmeticCatalog.startingAttackCharges(loadout.skinId)
        val mode = _uiState.value.movementMode
        val world = (if (bonusAttack > 0) {
            prebuiltBaseWorld.copy(attackCharges = prebuiltBaseWorld.attackCharges + bonusAttack)
        } else {
            prebuiltBaseWorld
        }).copy(movementMode = mode)
        val themeId = PacMazeThemeRegistry.themeForRun(payload.runMode, payload.themeLevelId.coerceAtLeast(1))
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
        resetJoystickInput()
        pendingAttack = false
        com.example.funlife.ui.screens.pacmaze.debug.PacMazeMotionDiag.resetSession()
        cachedLevelConfig = payload.level
        simulationWorld = world
        _renderFrame.value = PacMazeRenderFrame(current = world, previous = null, blend = 1f)
        penultimateSimSnapshot = null
    }

    fun requestAttack() {
        pendingAttack = true
    }

    fun tickFrameSilent(): PacMazeWorldState? {
        val result = tickFrame(updateRenderFrame = false)
        if (result == null && com.example.funlife.BuildConfig.DEBUG) {
            val state = _uiState.value
            android.util.Log.w(
                TAG,
                "tickFrameSilent=null sim=${simulationWorld != null} uiWorld=${state.world != null} " +
                    "cfg=${state.levelConfig != null} screen=${state.screenPhase} " +
                    "worldPhase=${simulationWorld?.phase ?: state.world?.phase}",
            )
        }
        return result
    }

    fun tickFrame(updateRenderFrame: Boolean = true): PacMazeWorldState? {
        try {
            val state = _uiState.value
            val world = simulationWorld ?: state.world ?: return null
            val config = state.levelConfig ?: cachedLevelConfig ?: return null
            if (world.phase != PacMazePhase.PLAYING && world.phase != PacMazePhase.LEVEL_CLEAR) return world

            penultimateSimSnapshot = world.renderInterpolationSnapshot()
            val previousWorld = penultimateSimSnapshot!!
            val prevLives = world.lives
            val pacInput = inputController.advanceTick(world.tick)
            val fireAttack = pendingAttack
            pendingAttack = false
            val speedMul = PacMazeCosmeticCatalog.speedMultiplier(
                loadout = state.avatarLoadout,
                userDrawScale = state.playerDrawScale,
            )
            val passRadius = PacMazeCosmeticCatalog.gameplayPassRadius(
                loadout = state.avatarLoadout,
                userDrawScale = state.playerDrawScale,
            )
            val attackCooldown = PacMazeCosmeticCatalog.attackCooldownTicks(state.avatarLoadout.skinId)
            val next = PacMazeSimulation.tick(
                world,
                pacInput,
                config,
                fireAttack = fireAttack,
                cosmeticSpeedMultiplier = speedMul,
                attackCooldownTicks = attackCooldown,
                playerPassRadius = passRadius,
            )
            val elapsed = if (world.tick % 60L == 0L) state.elapsedSeconds + 1 else state.elapsedSeconds
            val timed = PacMazeRules.checkTimeLimit(next, config, elapsed)
            simulationWorld = timed
            playGameplaySfx(world, timed)
            val deaths = if (timed.lives < prevLives) state.deathsThisRun + (prevLives - timed.lives) else state.deathsThisRun

            when (timed.phase) {
                PacMazePhase.LEVEL_CLEAR -> handleLevelClear(timed, previousWorld, elapsed, deaths)
                PacMazePhase.GAME_OVER -> handleGameOver(timed, previousWorld, elapsed, deaths)
                else -> {
                    if (updateRenderFrame) {
                        _renderFrame.value = PacMazeRenderFrame(current = timed, previous = previousWorld, blend = 0f)
                    }
                    if (shouldSyncPlayUi(previousWorld, timed, elapsed, state.elapsedSeconds)) {
                        _uiState.update {
                            it.copy(
                                world = timed,
                                elapsedSeconds = elapsed,
                                deathsThisRun = deaths,
                                screenPhase = PacMazePhase.PLAYING,
                            )
                        }
                    }
                }
            }
            return timed
        } catch (e: Exception) {
            Log.e(TAG, "tickFrame failed", e)
            inputController.reset()
            return null
        }
    }

    private fun shouldSyncPlayUi(
        prev: PacMazeWorldState,
        next: PacMazeWorldState,
        elapsed: Int,
        prevElapsed: Int,
    ): Boolean =
        prev.score != next.score ||
            prev.lives != next.lives ||
            prev.attackCharges != next.attackCharges ||
            prev.powerTicksLeft != next.powerTicksLeft ||
            prev.shieldCharges != next.shieldCharges ||
            prev.magnetTicksLeft != next.magnetTicksLeft ||
            prev.frostTicksLeft != next.frostTicksLeft ||
            prev.speedBoostTicksLeft != next.speedBoostTicksLeft ||
            prev.scoreBoostTicksLeft != next.scoreBoostTicksLeft ||
            prev.visitedCheckpointTags != next.visitedCheckpointTags ||
            prev.radarCooldownTicksLeft != next.radarCooldownTicksLeft ||
            prev.radarRevealTicksLeft != next.radarRevealTicksLeft ||
            prev.exploredTiles != next.exploredTiles ||
            (prev.attackCooldownTicksLeft <= 0) != (next.attackCooldownTicksLeft <= 0) ||
            elapsed != prevElapsed ||
            next.tick % 12L == 0L

    private fun playGameplaySfx(previous: PacMazeWorldState, next: PacMazeWorldState) {
        if (next.visitedCheckpointTags.size > previous.visitedCheckpointTags.size) {
            PacMazeSfx.playCheckpoint(appContext)
        }
        if (next.lives < previous.lives) {
            PacMazeSfx.playPlayerHurt(appContext)
            recordGhostCodexFromCollision(previous, next, killedPlayer = true)
        }
        if (next.attackCharges > previous.attackCharges) {
            PacMazeSfx.playPowerPellet(appContext)
        }
        if (next.projectiles.size > previous.projectiles.size) {
            PacMazeSfx.playAttack(appContext)
        }
        previous.entities.filter { it.role == "ghost" }.forEach { prevGhost ->
            val nextGhost = next.entities.firstOrNull { it.id == prevGhost.id } ?: return@forEach
            if (prevGhost.hitStunTicksLeft == 0 && nextGhost.hitStunTicksLeft > 0) {
                PacMazeSfx.playGhostHit(appContext)
            }
            if (prevGhost.ghostMode != GhostMode.EATEN && nextGhost.ghostMode == GhostMode.EATEN) {
                prefs.recordGhostEncounter(currentUserId, prevGhost.ghostKind, killedPlayer = false, playerAte = true)
                refreshGhostCodexMask()
            }
        }
    }

    private fun recordGhostCodexFromCollision(
        previous: PacMazeWorldState,
        next: PacMazeWorldState,
        killedPlayer: Boolean,
    ) {
        val pac = next.entities.firstOrNull { it.role == "pac" } ?: return
        val px = PacMazeMotion.tileX(pac.x)
        val py = PacMazeMotion.tileY(pac.y)
        val killer = previous.entities.firstOrNull { entity ->
            entity.role == "ghost" &&
                entity.ghostMode != GhostMode.EATEN &&
                PacMazeMotion.tileX(entity.x) == px &&
                PacMazeMotion.tileY(entity.y) == py
        } ?: return
        prefs.recordGhostEncounter(
            currentUserId,
            killer.ghostKind,
            killedPlayer = killedPlayer,
            playerAte = false,
        )
        refreshGhostCodexMask()
    }

    private fun refreshGhostCodexMask() {
        val mask = prefs.ghostCodexUnlockMask(currentUserId)
        _uiState.update { it.copy(ghostCodexUnlockMask = mask) }
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
                PacMazeSfx.playVictory(appContext)
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
                PacMazeSfx.playVictory(appContext)
                _uiState.update {
                    it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.LEVEL_CLEAR)
                }
                _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
                onMazeCleared(next.score, elapsed, deaths)
            }
            PacMazeRunMode.PRACTICE -> {
                _uiState.update {
                    it.copy(world = next, elapsedSeconds = elapsed, deathsThisRun = deaths, screenPhase = PacMazePhase.LEVEL_CLEAR)
                }
                _renderFrame.value = PacMazeRenderFrame(current = next, previous = previousWorld, blend = 0f)
            }
            PacMazeRunMode.CAMPAIGN -> {
                PacMazeSfx.playVictory(appContext)
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
                maxLevelReached = state.maxLevelReached,
            ),
        )
        val fresh = PacMazeMapLoader.buildInitialWorld(payload.level, payload.json, rngSeed)
        val merged = fresh.copy(
            score = cleared.score,
            lives = cleared.lives.coerceAtLeast(1),
            phase = PacMazePhase.PLAYING,
            movementMode = state.movementMode,
        )
        simulationWorld = merged
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
                mapThemeId = PacMazeThemeRegistry.themeForRun(PacMazeRunMode.ENDLESS, payload.themeLevelId),
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
            simulationWorld = revived
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
        PacMazeSfx.playDefeat(appContext)
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

    /** 同帧内多 tick 追帧：用帧初世界态快照作插值起点。 */
    fun restoreRenderPrevious(world: PacMazeWorldState) {
        val frame = _renderFrame.value ?: return
        _renderFrame.value = frame.copy(previous = world, blend = 0f)
    }

    fun captureRenderSnapshot(): PacMazeWorldState? =
        _renderFrame.value?.current?.renderInterpolationSnapshot()

    /**
     * 显示帧末一次性发布 render 态，避免 tick 循环内多次更新 renderFrame 导致
     * Canvas 在 renderBlend 写入前重组（log 里 blend 恒为 1 的根因）。
     */
    /** 每 vsync 发布：penultimate→current；[spanDurationNs]>0 时 Canvas 用 displayClock 连续插值。 */
    fun publishDisplayFrame(
        spanStartNs: Long,
        spanDurationNs: Long,
        displayClockNs: Long = 0L,
    ) {
        val current = simulationWorld ?: return
        val previous = penultimateSimSnapshot
        val fallbackBlend = if (previous != null && spanDurationNs <= 0L) 1f else 0f
        val blend = if (previous != null && spanDurationNs > 0L && displayClockNs > 0L) {
            com.example.funlife.ui.screens.pacmaze.components.PacMazeRenderTickLoop.displaySpanBlend(
                spanStartNs = spanStartNs,
                spanDurationNs = spanDurationNs,
                displayClockNs = displayClockNs,
                fallbackBlend = fallbackBlend,
            )
        } else {
            fallbackBlend
        }
        _renderFrame.value = PacMazeRenderFrame(
            current = current,
            previous = previous,
            blend = blend,
            spanStartNs = spanStartNs,
            spanDurationNs = spanDurationNs,
        )
    }

    fun pauseGame() {
        releaseJoystick()
        _uiState.update { it.copy(screenPhase = PacMazePhase.PAUSED) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(screenPhase = PacMazePhase.PLAYING) }
    }

    fun selectMode(mode: PacMazePlayMode) {
        if (!mode.playable) return
        when (mode) {
            PacMazePlayMode.SOLO -> setMenuRoutes(
                PacMazeMenuRoute.ChapterOverview,
            ) {
                it.copy(selectedMode = mode, loadError = null)
            }
            PacMazePlayMode.ENDLESS -> {
                _uiState.update { it.copy(selectedMode = mode, loadError = null) }
                startEndless()
            }
            PacMazePlayMode.MAZE -> setMenuRoutes(PacMazeMenuRoute.MazeHome) {
                it.copy(selectedMode = mode, loadError = null)
            }
            PacMazePlayMode.ONLINE_VERSUS -> openOnlineHub("versus_duel")
            PacMazePlayMode.ONLINE_COOP -> openOnlineHub("coop_campaign")
            PacMazePlayMode.ONLINE -> openOnlineHub("versus_duel")
            else -> Unit
        }
    }

    fun openOnlineHub(subMode: String) {
        val mode = when (subMode) {
            "coop_campaign" -> PacMazePlayMode.ONLINE_COOP
            else -> PacMazePlayMode.ONLINE_VERSUS
        }
        _uiState.update { it.copy(selectedMode = mode, loadError = null) }
        pushMenuRoute(PacMazeMenuRoute.OnlineHub(subMode))
    }

    fun openOnlineLobby(roomId: String) {
        pushMenuRoute(PacMazeMenuRoute.OnlineLobby(roomId))
    }

    /** 外部深链 / 接受邀请：直达横屏豆人大厅内的对战房间。 */
    fun openOnlineLobbyFromDeepLink(roomId: String, matchMode: String? = null) {
        val mode = when (matchMode) {
            "coop_campaign" -> PacMazePlayMode.ONLINE_COOP
            else -> PacMazePlayMode.ONLINE_VERSUS
        }
        setMenuRoutes(PacMazeMenuRoute.ModeSelect, PacMazeMenuRoute.OnlineLobby(roomId)) {
            it.copy(selectedMode = mode)
        }
    }

    /** 从选关等入口进入换角色。 */
    fun openCharacterSelect() {
        pushMenuRoute(PacMazeMenuRoute.CharacterSeries)
    }

    fun backFromCharacterSelect() {
        popMenuRoute(defaultIfEmpty = when (_uiState.value.selectedMode) {
            PacMazePlayMode.SOLO -> PacMazeMenuRoute.ChapterOverview
            PacMazePlayMode.MAZE -> PacMazeMenuRoute.MazeHome
            else -> PacMazeMenuRoute.ModeSelect
        })
    }

    fun hubBack() {
        popMenuRoute()
    }

    fun openChapter(themeId: PacMazeMapThemeId) {
        pushMenuRoute(PacMazeMenuRoute.ChapterLevels(themeId))
    }

    fun openLevelDetail(levelId: Int) {
        pushMenuRoute(PacMazeMenuRoute.LevelDetail(levelId))
    }

    fun openSerpentineMap() {
        // 全景闯关径已合并为 ChapterOverview 主视图，保持 API 兼容
        setMenuRoutes(PacMazeMenuRoute.ChapterOverview)
    }

    fun openCharacterSeries() {
        pushMenuRoute(PacMazeMenuRoute.CharacterSeries)
    }

    fun openCharacterGrid(series: PacMazeSkinSeries) {
        pushMenuRoute(PacMazeMenuRoute.CharacterGrid(series))
    }

    fun requestOpenCharacterGrid(series: PacMazeSkinSeries) {
        if (series != PacMazeSkinSeries.IKUN || !ikunDisclosureRequired()) {
            openCharacterGrid(series)
            return
        }
        beginIkunDisclosure(pendingSeries = series)
    }

    fun requestSelectSkin(skinId: PacMazeSkinId) {
        if (!PacMazeIkunCatalog.contains(skinId) || !ikunDisclosureRequired()) {
            selectSkin(skinId)
            return
        }
        beginIkunDisclosure(pendingSkin = skinId)
    }

    private fun ikunDisclosureRequired(): Boolean {
        PacMazeIkunDisclosureConfig.loadFromCache(appContext)
        val config = PacMazeIkunDisclosureConfig.current
        if (!config.enabled) return false
        return prefs.ikunDisclosureAgreedVersion(currentUserId) < config.version
    }

    private fun beginIkunDisclosure(
        pendingSeries: PacMazeSkinSeries? = null,
        pendingSkin: PacMazeSkinId? = null,
    ) {
        pendingIkunSeries = pendingSeries
        pendingIkunSkinId = pendingSkin
        _uiState.update { it.copy(ikunDisclosureVisible = true, ikunDisclosureLoading = true) }
        viewModelScope.launch {
            PacMazeIkunDisclosureConfig.refresh(appContext, force = true)
            PacMazeRemoteSkinAnimCache.requestPreloadAllCoversAsync()
            _uiState.update { it.copy(ikunDisclosureLoading = false) }
        }
    }

    fun confirmIkunDisclosure() {
        val config = PacMazeIkunDisclosureConfig.current
        prefs.setIkunDisclosureAgreedVersion(currentUserId, config.version)
        val skin = pendingIkunSkinId
        val series = pendingIkunSeries
        pendingIkunSkinId = null
        pendingIkunSeries = null
        _uiState.update { it.copy(ikunDisclosureVisible = false, ikunDisclosureLoading = false) }
        when {
            skin != null -> selectSkin(skin)
            series != null -> openCharacterGrid(series)
            else -> openCharacterGrid(PacMazeSkinSeries.IKUN)
        }
    }

    fun openCharacterDetail(skinId: PacMazeSkinId) {
        pushMenuRoute(PacMazeMenuRoute.CharacterDetail(skinId))
    }

    fun openTrailWorkshop() {
        pushMenuRoute(PacMazeMenuRoute.TrailWorkshop)
    }

    fun openCollectionBook() {
        pushMenuRoute(PacMazeMenuRoute.CollectionBook)
    }

    private fun pushMenuRoute(route: PacMazeMenuRoute) {
        _uiState.update {
            it.copy(menuRouteStack = it.menuRouteStack + route, loadError = null)
        }
    }

    private fun popMenuRoute(defaultIfEmpty: PacMazeMenuRoute = PacMazeMenuRoute.ModeSelect) {
        _uiState.update {
            when {
                it.menuRouteStack.size > 1 -> it.copy(
                    menuRouteStack = it.menuRouteStack.dropLast(1),
                    loadError = null,
                )
                it.menuRouteStack.singleOrNull() != defaultIfEmpty ->
                    it.copy(menuRouteStack = listOf(defaultIfEmpty), loadError = null)
                else -> it
            }
        }
    }

    private inline fun setMenuRoutes(
        vararg routes: PacMazeMenuRoute,
        crossinline mutate: (PacMazeUiState) -> PacMazeUiState = { it },
    ) {
        _uiState.update {
            mutate(it).copy(menuRouteStack = routes.toList(), loadError = null)
        }
    }

    /** 外部入口进入时回到模式大厅（避免落在上次的子步骤）。 */
    fun ensureHubRoot() {
        _uiState.update {
            if (it.screenPhase != PacMazePhase.MENU) {
                it
            } else {
                it.copy(menuRouteStack = listOf(PacMazeMenuRoute.ModeSelect), loadError = null)
            }
        }
    }

    fun selectCharacter(characterId: PacMazeCharacterId) {
        val skinId = PacMazeSkinId.fromLegacy(characterId)
        requestSelectSkin(skinId)
    }

    fun selectSkin(skinId: PacMazeSkinId) {
        prefs.setSelectedSkin(currentUserId, skinId)
        _uiState.update {
            it.copy(
                avatarLoadout = it.avatarLoadout.copy(skinId = skinId),
                selectedCharacterId = skinId.legacyCharacterId() ?: PacMazeCharacterId.CLASSIC_PAC,
            )
        }
        PacMazeRemoteSkinAnimCache.requestGameplayWarmupAsync(skinId)
    }

    fun selectTrail(trailId: PacMazeTrailId) {
        prefs.setSelectedTrail(currentUserId, trailId)
        _uiState.update { it.copy(avatarLoadout = it.avatarLoadout.copy(trailId = trailId)) }
    }

    fun applyRecommendedTrail() {
        val skinId = _uiState.value.avatarLoadout.skinId
        selectTrail(PacMazeCosmeticCatalog.recommendedTrail(skinId))
    }

    fun setPlayerDrawScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 3.5f)
        prefs.setPlayerDrawScale(currentUserId, clamped)
        _uiState.update { it.copy(playerDrawScale = clamped) }
    }

    fun setMovementMode(mode: PacMazeMovementMode) {
        prefs.setPacMazeMovementMode(currentUserId, mode)
        simulationWorld = simulationWorld?.copy(movementMode = mode)
        val frame = _renderFrame.value
        if (frame != null) {
            _renderFrame.value = frame.copy(current = frame.current.copy(movementMode = mode))
        }
        _uiState.update { state ->
            state.copy(
                movementMode = mode,
                world = state.world?.copy(movementMode = mode),
            )
        }
    }

    fun toggleMovementMode() {
        val next = if (_uiState.value.movementMode == PacMazeMovementMode.AUTO) PacMazeMovementMode.MANUAL else PacMazeMovementMode.AUTO
        setMovementMode(next)
    }

    fun setMapWidthScale(scale: Float) {
        val clamped = scale.coerceIn(0.7f, 1.5f)
        prefs.setMapWidthScale(currentUserId, clamped)
        _uiState.update { it.copy(mapWidthScale = clamped) }
    }

    fun setMapHeightScale(scale: Float) {
        val clamped = scale.coerceIn(0.7f, 1.5f)
        prefs.setMapHeightScale(currentUserId, clamped)
        _uiState.update { it.copy(mapHeightScale = clamped) }
    }

    fun setPlayHudPanelsExpanded(expanded: Boolean) {
        prefs.setPlayHudPanelsExpanded(currentUserId, expanded)
        _uiState.update { it.copy(playHudPanelsExpanded = expanded) }
    }

    fun togglePlayHudPanels() {
        setPlayHudPanelsExpanded(!_uiState.value.playHudPanelsExpanded)
    }

    fun confirmCharacterAndGoToLevels() {
        when (_uiState.value.selectedMode) {
            PacMazePlayMode.ENDLESS -> startEndless()
            PacMazePlayMode.MAZE -> setMenuRoutes(PacMazeMenuRoute.MazeHome)
            else -> setMenuRoutes(PacMazeMenuRoute.ChapterOverview)
        }
    }

    fun confirmTrailAndBack() {
        popMenuRoute()
    }

    fun backToModeSelect() {
        setMenuRoutes(PacMazeMenuRoute.ModeSelect)
    }

    fun backToChapterOverview() {
        setMenuRoutes(PacMazeMenuRoute.ChapterOverview)
    }

    fun backToCharacterSelect() {
        setMenuRoutes(PacMazeMenuRoute.CharacterSeries)
    }

    fun backToMenu() {
        inputController.reset()
        simulationWorld = null
        cachedLevelConfig = null
        _uiState.update {
            it.copy(
                screenPhase = PacMazePhase.MENU,
                menuRouteStack = when (it.selectedMode) {
                    PacMazePlayMode.SOLO -> listOf(PacMazeMenuRoute.ChapterOverview)
                    PacMazePlayMode.MAZE -> listOf(PacMazeMenuRoute.MazeHome)
                    else -> listOf(PacMazeMenuRoute.ModeSelect)
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
        inputController.reset()
        simulationWorld = null
        cachedLevelConfig = null
        _uiState.update {
            it.copy(
                screenPhase = PacMazePhase.MENU,
                menuRouteStack = listOf(PacMazeMenuRoute.ModeSelect),
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
        ).copy(movementMode = state.movementMode)
        simulationWorld = world
        _uiState.update {
            it.copy(
                world = world,
                screenPhase = PacMazePhase.PLAYING,
                deathsThisRun = 0,
                elapsedSeconds = 0,
                runStartedAtMs = System.currentTimeMillis(),
            )
        }
        _renderFrame.value = PacMazeRenderFrame(current = world, previous = null, blend = 1f)
        penultimateSimSnapshot = null
    }

    fun evaluateStars(score: Int, elapsedSeconds: Int, deaths: Int): Int {
        val criteria = _uiState.value.levelConfig?.starCriteria ?: return 1
        val visited = _uiState.value.world?.visitedCheckpointTags ?: emptySet()
        return PacMazeStarEvaluator.evaluate(criteria, score, elapsedSeconds, deaths, visited)
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

    private fun onMazeCleared(score: Int, elapsedSeconds: Int, deaths: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val elapsedMs = (System.currentTimeMillis() - state.runStartedAtMs).coerceAtLeast(0L)
            val stars = evaluateStars(score, elapsedSeconds, deaths)
            progressRepository.saveMazeResult(currentUserId, elapsedMs, score)

            val difficultyId = state.mazeRunProfile.difficulty.id
            val today = PacMazeMazeRunOptions.todayDateString()
            var stats = prefs.mazeStats(currentUserId)
            val prevBestTime = stats.bestTimeByDifficulty[difficultyId] ?: Long.MAX_VALUE
            val prevBestStars = stats.bestStarsByDifficulty[difficultyId] ?: 0
            stats = stats.copy(
                bestTimeByDifficulty = if (elapsedMs < prevBestTime) {
                    stats.bestTimeByDifficulty + (difficultyId to elapsedMs)
                } else {
                    stats.bestTimeByDifficulty
                },
                bestStarsByDifficulty = if (stars > prevBestStars) {
                    stats.bestStarsByDifficulty + (difficultyId to stars)
                } else {
                    stats.bestStarsByDifficulty
                },
            )
            if (state.mazeRunProfile.seedMode == PacMazeMazeSeedMode.DAILY) {
                val dailyBest = if (stats.dailyDate == today) stats.dailyBestTimeMs else Long.MAX_VALUE
                if (elapsedMs < dailyBest || stats.dailyDate != today) {
                    stats = stats.copy(
                        dailyDate = today,
                        dailyBestTimeMs = elapsedMs.coerceAtMost(dailyBest),
                        dailyBestStars = stars.coerceAtLeast(if (stats.dailyDate == today) stats.dailyBestStars else 0),
                    )
                } else if (stars > stats.dailyBestStars) {
                    stats = stats.copy(dailyBestStars = stars)
                }
            }
            prefs.setMazeStats(currentUserId, stats)

            val progress = progressRepository.getProgress(currentUserId)
            if (progress != null) applyProgress(progress)
            _uiState.update { it.copy(mazeStats = stats, lastMazeStars = stars) }
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
        val effectiveMax = PacMazeTestUnlock.effectiveMaxLevelReached(progress.maxLevelReached)
        val mazeStats = prefs.mazeStats(currentUserId)
        _uiState.update {
            it.copy(
                maxLevelReached = effectiveMax,
                highScore = progress.highScore,
                starsBitmask = progress.starsBitmask,
                endlessBestScore = progress.endlessBestScore,
                endlessBestWave = progress.endlessBestWave,
                mazeBestTimeMs = progress.mazeBestTimeMs,
                mazeStats = mazeStats,
                mazeDifficulty = PacMazeMazeDifficulty.fromId(mazeStats.lastDifficultyId),
                mazeContract = PacMazeMazeContract.fromId(mazeStats.lastContractId),
                mazeRunProfile = PacMazeMazeRunProfile.fromLegacy(
                    difficulty = PacMazeMazeDifficulty.fromId(mazeStats.lastDifficultyId),
                    contract = PacMazeMazeContract.fromId(mazeStats.lastContractId),
                    dailyChallenge = mazeStats.useDailyChallenge,
                ),
                mazeUseDailyChallenge = mazeStats.useDailyChallenge,
                mazeRandomPreviewSeed = System.nanoTime() xor currentUserId,
                ghostCodexUnlockMask = PacMazeTestUnlock.ghostCodexMask(prefs.ghostCodexUnlockMask(currentUserId)),
            )
        }
    }

    private companion object {
        private const val TAG = "PacMazeLocalViewModel"
    }
}
