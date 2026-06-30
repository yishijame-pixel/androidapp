package com.example.funlife.ui.screens.platformer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import android.widget.Toast
import com.example.funlife.game.platformer.*
import com.example.funlife.game.platformer.PlatformerSuperTuxClassicPaths
import com.example.funlife.game.platformer.PlatformerAssets
import com.example.funlife.game.platformer.tmx.PlatformerTmxWorldBuilder
import com.example.funlife.game.platformer.catalog.PlatformerAssetService
import com.example.funlife.game.platformer.catalog.PlatformerCharacterPrefetch
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.ui.screens.platformer.PlatformerBootCache
import com.example.funlife.ui.screens.platformer.PlatformerBootLoader
import com.example.funlife.ui.screens.platformer.PlatformerPrewarmBanner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.funlife.ui.screens.platformer.minigame.PlatformerHillClimbScreen
import com.example.funlife.ui.screens.platformer.minigame.PlatformerPlaneShooterScreen
import com.example.funlife.ui.screens.platformer.minigame.PlatformerTempleRunScreen
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeHideSystemBars
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.funlife.ui.screens.platformer.PlatformerSfx
import com.example.funlife.resource.ResourceStore
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformerScreen(onNavigateBack: () -> Unit) {
    LockLandscape()
    PacMazeHideSystemBars()

    val context = LocalContext.current
    // 须在读取 PlatformerLevelProgress / PlatformerUnlockProgress 之前完成 init（否则 lateinit appContext 崩溃）
    PlatformerAssetService.ensureInitialized(context)

    var selectedCharacter by remember {
        mutableStateOf(PlatformerCharacterPrefs.get(context))
    }
    var assets by remember { mutableStateOf<PlatformerAssets?>(null) }
    var bootMessage by remember { mutableStateOf("初始化坤坤大冒险…") }
    var bootProgress by remember { mutableIntStateOf(0) }
    var bootPhase by remember { mutableStateOf(PlatformerBootLoader.BootPhase.INIT) }
    var bootReady by remember { mutableStateOf(false) }
    val classicEngineReady = remember(context) { SuperTuxClassicLauncher.isReady(context) }
    val scope = rememberCoroutineScope()
    var classicPrepProgress by remember { mutableIntStateOf(-1) }
    var classicPrepStage by remember { mutableStateOf("") }
    var classicPrepLevel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        PlatformerAssetService.ensureInitialized(context)
        try {
            val loaded = PlatformerBootLoader.load(context, selectedCharacter) { state ->
                bootMessage = state.message
                bootProgress = state.progress
                bootPhase = state.phase
            }
            assets = loaded
            android.util.Log.i(
                "PlatformerScreen",
                "boot complete playable=${PlatformerBootCache.isPlayable(selectedCharacter)} assets=${loaded != null}",
            )
        } catch (t: Throwable) {
            android.util.Log.e("PlatformerScreen", "boot failed, fallback assets", t)
            assets = runCatching { PlatformerBootCache.obtainAssets(context) }
                .getOrElse { PlatformerAssets.loadFallback(context) }
            bootMessage = "资源加载异常，已降级进入"
            bootProgress = 92
            bootPhase = PlatformerBootLoader.BootPhase.PLAYABLE
            PlatformerBootCache.forceMarkPlayable(selectedCharacter)
        } finally {
            bootReady = true
            PlatformerSfx.prefetch(context)
        }
    }

    LaunchedEffect(selectedCharacter) {
        if (!bootReady) return@LaunchedEffect
        PlatformerCharacterPrefetch.prefetchOnSelect(selectedCharacter)
        if (PlatformerBootCache.isPlayable(selectedCharacter) ||
            PlatformerCharacterRenderer.isBootstrapPlayable(selectedCharacter)
        ) {
            PlatformerBootCache.markPlayable(selectedCharacter)
            PlatformerBootCache.scheduleFullWarmup(selectedCharacter)
            return@LaunchedEffect
        }
        val fast = runCatching {
            PlatformerCharacterPrefetch.hydrateLight(selectedCharacter)
        }.getOrDefault(false)
        if (fast || PlatformerBootCache.isPlayable(selectedCharacter)) {
            PlatformerBootCache.scheduleFullWarmup(selectedCharacter)
            return@LaunchedEffect
        }
        bootReady = false
        bootProgress = 8
        PlatformerBootLoader.reloadCharacter(selectedCharacter) { state ->
            bootMessage = state.message
            bootProgress = state.progress
            bootPhase = state.phase
        }
        bootReady = true
    }

    var screen by remember { mutableStateOf(PlatformerUiScreen.LevelSelect) }
    var levelIndex by remember { mutableIntStateOf(0) }
    var playSession by remember { mutableIntStateOf(0) }
    var endlessSession by remember { mutableIntStateOf(0) }
    var testUnlockAll by remember { mutableStateOf(PlatformerLevelProgress.testUnlockAll) }
    var maxUnlockedLevel by remember { mutableIntStateOf(PlatformerLevelProgress.maxUnlockedLevelId) }

    if (!bootReady || assets == null) {
        val (displayMessage, displayProgress) = rememberPlatformerLoadProgress(
            fallbackMessage = bootMessage,
            fallbackProgress = bootProgress,
            canEnter = false,
        )
        PlatformerLoadingScreen(
            message = displayMessage,
            progress = displayProgress,
            phaseLabel = bootPhase.label,
            onBack = onNavigateBack,
        )
        return
    }

    val loadedAssets = assets!!

    LaunchedEffect(bootReady, selectedCharacter, screen) {
        if (!bootReady || screen != PlatformerUiScreen.LevelSelect) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching { PlatformerCharacterPrefetch.hydrateLight(selectedCharacter) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        PlatformerPrewarmBanner(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        )
        if (classicPrepProgress >= 0) {
            SuperTuxClassicPrepOverlay(
                progress = classicPrepProgress,
                stage = classicPrepStage,
                modifier = Modifier.fillMaxSize(),
            )
        }
        when (screen) {
            PlatformerUiScreen.LevelSelect -> {
                PlatformerLevelSelectPanel(
                    onBack = onNavigateBack,
                    testUnlockAll = testUnlockAll,
                    maxUnlockedLevel = maxUnlockedLevel,
                    selectedCharacter = selectedCharacter,
                    onCharacterSelect = { id ->
                        PlatformerCharacterPrefetch.prefetchOnSelect(id)
                        selectedCharacter = id
                        PlatformerCharacterPrefs.set(context, id)
                    },
                    onUnlockAll = {
                        PlatformerLevelProgress.unlockAllForTest()
                        testUnlockAll = true
                        maxUnlockedLevel = PLATFORMER_TOTAL_LEVEL_COUNT
                    },
                    onSelect = { idx ->
                        levelIndex = idx
                        playSession++
                        screen = PlatformerUiScreen.Playing
                    },
                    onStartClassic = { levelId ->
                        val stl = PlatformerSuperTuxClassicPaths.levelStlPath(levelId)
                        if (stl == null) {
                            Toast.makeText(context, "该关暂无经典 STL 映射", Toast.LENGTH_SHORT).show()
                            return@PlatformerLevelSelectPanel
                        }
                        if (!SuperTuxClassicLauncher.isNativeLibraryPresent(context)) {
                            Toast.makeText(
                                context,
                                "经典引擎 native 库未就绪，请运行 prepare_supertux_classic_android.ps1",
                                Toast.LENGTH_LONG,
                            ).show()
                            return@PlatformerLevelSelectPanel
                        }
                        scope.launch {
                            val needsPrep = !SuperTuxClassicLauncher.isGameDataReady(context)
                            if (needsPrep) {
                                classicPrepLevel = stl
                                classicPrepProgress = 0
                                classicPrepStage = "准备 SuperTux 经典引擎…"
                            }
                            val ok = SuperTuxClassicLauncher.prepareAndStart(context, stl) { p ->
                                if (needsPrep) {
                                    classicPrepProgress = p.percent
                                    classicPrepStage = p.stage
                                }
                            }
                            classicPrepProgress = -1
                            classicPrepStage = ""
                            classicPrepLevel = null
                            if (!ok) {
                                Toast.makeText(
                                    context,
                                    "SuperTux 资源解压失败，请检查存储空间后重试",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    classicEngineReady = classicEngineReady,
                    onStartEndless = {
                        endlessSession++
                        screen = PlatformerUiScreen.EndlessPlaying
                    },
                    onStartTempleRun = { screen = PlatformerUiScreen.TempleRun },
                    onStartPlaneShooter = { screen = PlatformerUiScreen.PlaneShooter },
                    onStartHillClimb = { screen = PlatformerUiScreen.HillClimb },
                )
            }
            PlatformerUiScreen.TempleRun -> PlatformerTempleRunScreen(onBack = { screen = PlatformerUiScreen.LevelSelect })
            PlatformerUiScreen.PlaneShooter -> PlatformerPlaneShooterScreen(onBack = { screen = PlatformerUiScreen.LevelSelect })
            PlatformerUiScreen.HillClimb -> PlatformerHillClimbScreen(onBack = { screen = PlatformerUiScreen.LevelSelect })
            PlatformerUiScreen.Playing -> {
                key(playSession, levelIndex, selectedCharacter) {
                    val levelDef = PlatformerLevels.all.getOrNull(levelIndex)
                        ?: PlatformerLevels.all.first()
                    var startWorld by remember(playSession, levelIndex, selectedCharacter) {
                        mutableStateOf<PlatformerWorld?>(null)
                    }
                    LaunchedEffect(playSession, levelIndex, selectedCharacter) {
                        startWorld = null
                        if (PlatformerSuperTuxLevelCatalog.isSuperTuxLevel(levelDef.id)) {
                            withContext(Dispatchers.IO) {
                                runCatching { ResourceStore.ensureBundle("platformer_supertux") }
                            }
                        }
                        startWorld = withContext(Dispatchers.Default) {
                            val playCharacter = if (PlatformerSuperTuxLevelCatalog.isSuperTuxLevel(levelDef.id)) {
                                PlatformerCharacterId.SUPERTUX_TUX
                            } else {
                                selectedCharacter
                            }
                            PlatformerLevels.buildWorldOrFallback(context, levelDef, playCharacter)
                        }
                    }
                    if (startWorld == null) {
                        PlatformerLoadingScreen(
                            message = "加载关卡…",
                            progress = 96,
                            phaseLabel = "关卡",
                            onBack = {
                                screen = PlatformerUiScreen.LevelSelect
                            },
                        )
                    } else {
                        GamePlayArea(
                            world = startWorld!!,
                            assets = loadedAssets,
                            context = context,
                            onExit = {
                                maxUnlockedLevel = PlatformerLevelProgress.maxUnlockedLevelId
                                screen = PlatformerUiScreen.LevelSelect
                            },
                            onRetry = {
                                playSession++
                            },
                            onNextLevel = {
                                maxUnlockedLevel = PlatformerLevelProgress.maxUnlockedLevelId
                                if (levelIndex < PlatformerLevels.all.lastIndex) {
                                    levelIndex++
                                    playSession++
                                } else {
                                    screen = PlatformerUiScreen.LevelSelect
                                }
                            },
                        )
                    }
                }
            }
            PlatformerUiScreen.EndlessPlaying -> {
                key(endlessSession, selectedCharacter) {
                    var startWorld by remember(endlessSession, selectedCharacter) {
                        mutableStateOf<PlatformerWorld?>(null)
                    }
                    LaunchedEffect(endlessSession, selectedCharacter) {
                        startWorld = null
                        startWorld = withContext(Dispatchers.Default) {
                            PlatformerEndlessRunner.buildInitial(context, selectedCharacter)
                        }
                    }
                    if (startWorld == null) {
                        PlatformerLoadingScreen(
                            message = "加载无尽模式…",
                            progress = 96,
                            phaseLabel = "关卡",
                            onBack = { screen = PlatformerUiScreen.LevelSelect },
                        )
                    } else {
                        GamePlayArea(
                            world = startWorld!!,
                            assets = loadedAssets,
                            context = context,
                            onExit = { screen = PlatformerUiScreen.LevelSelect },
                            onRetry = { endlessSession++ },
                            onNextLevel = { endlessSession++ },
                        )
                    }
                }
            }
        }
    }
}

private enum class PlatformerUiScreen {
    LevelSelect, Playing, EndlessPlaying, TempleRun, PlaneShooter, HillClimb,
}

internal fun themeAccent(theme: PlatformerTheme): Color = platformerThemeAccent(theme)

@Composable
internal fun GamePlayArea(
    world: PlatformerWorld,
    assets: PlatformerAssets,
    onExit: () -> Unit,
    onRetry: () -> Unit,
    onNextLevel: () -> Unit,
    context: android.content.Context? = null,
) {
    var currentWorld by remember { mutableStateOf(world) }
    val gameInputState = remember { mutableStateOf(PlatformerInput()) }
    var animTime by remember { mutableFloatStateOf(0f) }
    var renderTick by remember { mutableIntStateOf(0) }
    var viewWorldW by remember { mutableFloatStateOf(PlatformerViewport.VISIBLE_TILES_W * PLATFORMER_TILE_PX) }
    var respawnFlash by remember { mutableFloatStateOf(0f) }
    var deathHint by remember { mutableStateOf<String?>(null) }
    var deathOverlayText by remember { mutableStateOf<String?>(null) }
    var tmxExtraBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    var characterAnimReady by remember(world.characterId) {
        mutableStateOf(
            world.characterId != PlatformerCharacterId.CHICK_PRO_MAX ||
                PlatformerPlayerSprites.isBootstrapPlayable(),
        )
    }
    var audioPrevGrounded by remember { mutableStateOf(world.player.grounded) }
    var audioPrevGems by remember { mutableIntStateOf(world.gemsCollected) }
    var audioPrevPhase by remember { mutableStateOf(world.phase) }
    var audioPrevDying by remember { mutableStateOf(world.player.dying) }
    var audioPrevEnemiesAlive by remember { mutableIntStateOf(world.enemies.count { it.alive }) }
    val playCtx = context ?: LocalContext.current

    LaunchedEffect(world.level.id) {
        PlatformerSfx.startLevelBgm(playCtx, world.level.id)
    }

    DisposableEffect(Unit) {
        onDispose { PlatformerSfx.stopBgm(playCtx) }
    }

    val skinPlaybackRevision by PacMazeRemoteSkinAnimCache.playbackRevision.collectAsStateWithLifecycle()

    LaunchedEffect(currentWorld.characterId) {
        PlatformerChickLoadLog.resetSession()
        PlatformerChickWalkDbg.resetSession()
        PlatformerPlayerSprites.resetIdlePhaseTracking()
        PlatformerCharacterPrefetch.prefetchOnSelect(currentWorld.characterId)
        when (currentWorld.characterId) {
            PlatformerCharacterId.CHICK_PRO_MAX -> {
                if (!PlatformerPlayerSprites.isBootstrapPlayable()) {
                    characterAnimReady = false
                    withContext(Dispatchers.IO) {
                        runCatching {
                            PacMazeRemoteSkinAnimCache.preparePlatformerSheetsForBoot(PlatformerPlayerSprites.skinId)
                        }
                    }
                }
                characterAnimReady = PlatformerPlayerSprites.isBootstrapPlayable()
                PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(
                    PlatformerPlayerSprites.skinId,
                    PacMazeSkinAnimClip.DIE,
                )
            }
            else -> {
                characterAnimReady = true
                if (currentWorld.characterId.isCatalogRemote) {
                    if (!PlatformerCharacterRenderer.isBootstrapPlayable(currentWorld.characterId)) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                PlatformerRemoteAnimCache.prepareSheetsForBoot(currentWorld.characterId)
                            }
                        }
                    }
                    PlatformerRemoteAnimCache.requestSheetPlaybackAsync(
                        currentWorld.characterId,
                        PlatformerAnimClip.DIE,
                    )
                }
            }
        }
    }

    if (!characterAnimReady && currentWorld.characterId == PlatformerCharacterId.CHICK_PRO_MAX) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1a1a2e)),
            contentAlignment = Alignment.Center,
        ) {
            Text("加载行走小鸡…", color = Color.White, fontSize = 16.sp)
        }
        return
    }

    LaunchedEffect(currentWorld.level.id, currentWorld.enemies.mapNotNull { it.catalogId }.distinct()) {
        currentWorld.enemies.mapNotNull { it.catalogId }.distinct().forEach { catalogId ->
            PlatformerRemoteAnimCache.requestEnemyWarmup(catalogId)
        }
    }

    LaunchedEffect(world.tmx?.tilesetPath, world.tmx?.backgroundPath, context) {
        val tmx = world.tmx ?: return@LaunchedEffect
        val ctx = context ?: return@LaunchedEffect
        val extra = buildMap {
            if (assets.tmxBitmaps[tmx.tilesetPath] == null) {
                PlatformerTmxWorldBuilder.loadTilesetBitmap(ctx, tmx.tilesetPath)?.let { put(tmx.tilesetPath, it) }
            }
            tmx.backgroundPath?.let { bg ->
                if (assets.tmxBitmaps[bg] == null) {
                    PlatformerTmxWorldBuilder.loadTilesetBitmap(ctx, bg)?.let { put(bg, it) }
                }
            }
        }
        tmxExtraBitmaps = extra
    }

    val renderAssets = remember(assets, tmxExtraBitmaps) { assets.withTmxBitmaps(tmxExtraBitmaps) }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .graphicsLayer { clip = false },
        ) {
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()
        val vp = PlatformerViewport.compute(currentWorld, viewportW, viewportH)
        viewWorldW = vp.viewWorldW
        val isPaused = currentWorld.phase != PlatformerPhase.PLAYING

        LaunchedEffect(currentWorld.phase, currentWorld.level.id) {
            if (currentWorld.phase == PlatformerPhase.LEVEL_CLEAR && !currentWorld.endlessMode) {
                PlatformerLevelProgress.onLevelCleared(currentWorld.level.id)
                PlatformerCampaignTelemetry.onLevelClear(currentWorld.level.id)
                if (audioPrevPhase != PlatformerPhase.LEVEL_CLEAR) {
                    PlatformerSfx.playLevelClear(playCtx)
                }
            }
        }

        LaunchedEffect(world) {
            if (!world.endlessMode) {
                PlatformerCampaignTelemetry.onLevelStart(world.level.id)
            }
        }

        LaunchedEffect(Unit) {
            var lastNs = 0L
            while (true) {
                withFrameNanos { frameNs ->
                    if (lastNs == 0L) {
                        lastNs = frameNs
                        return@withFrameNanos
                    }
                    val dt = ((frameNs - lastNs) / 1_000_000_000f).coerceIn(1f / 240f, 1f / 20f)
                    lastNs = frameNs
                    animTime += dt
                    if (respawnFlash > 0f) {
                        respawnFlash = (respawnFlash - dt * 2.8f).coerceAtLeast(0f)
                    }

                    if (currentWorld.player.dying) {
                        val deathTime = currentWorld.player.deathAnimTime + dt
                        val totalDeath = PlatformerDeathAnim.totalPhaseSec(currentWorld.characterId)
                        deathOverlayText = PlatformerDeathAnim.overlayMessage(
                            currentWorld.characterId,
                            deathTime,
                            deathHint,
                        )
                        val pw = PlatformerPhysics.playerW(currentWorld.tilePx)
                        val ph = PlatformerPhysics.playerH(currentWorld.tilePx)
                        val p = currentWorld.player
                        val skyTick = PlatformerSkyChickSystem.tick(
                            world = currentWorld,
                            playerX = p.x,
                            playerY = p.y,
                            playerW = pw,
                            playerH = ph,
                            dt = dt,
                            time = animTime,
                            combatActive = false,
                        )
                        if (deathTime >= totalDeath) {
                            deathOverlayText = null
                            val deadWorld = currentWorld.copy(
                                player = currentWorld.player.copy(
                                    dying = false,
                                    deathAnimTime = deathTime,
                                ),
                            )
                            PlatformerCampaignTelemetry.onDeath(deadWorld.level.id)
                            currentWorld = if (deadWorld.endlessMode) {
                                PlatformerEndlessRunner.onPlayerDeath(deadWorld)
                            } else if (deadWorld.campaignScrollMode) {
                                PlatformerCampaignScrollRunner.respawnAtCheckpoint(deadWorld)
                                    ?: if (context != null) {
                                        PlatformerLevels.buildWorldOrFallback(
                                            context, deadWorld.level, deadWorld.characterId,
                                        )
                                    } else {
                                        PlatformerLevels.buildWorld(deadWorld.level, deadWorld.characterId)
                                    }
                            } else {
                                PlatformerCampaignScrollRunner.respawnAtLevelStart(deadWorld)
                            }
                            respawnFlash = 1f
                        } else {
                            currentWorld = currentWorld.copy(
                                player = currentWorld.player.copy(deathAnimTime = deathTime),
                                skyChick = skyTick.skyChick,
                                skyEggs = skyTick.skyEggs,
                            )
                        }
                        renderTick++
                        return@withFrameNanos
                    }

                    if (currentWorld.phase != PlatformerPhase.PLAYING) return@withFrameNanos

                    val frameInput = gameInputState.value
                    var next = PlatformerPhysics.tick(currentWorld, frameInput, dt, viewWorldW, animTime)
                    if (frameInput.jumpPressed) {
                        gameInputState.value = gameInputState.value.copy(jumpPressed = false)
                    }
                    if (frameInput.attackPressed) {
                        gameInputState.value = gameInputState.value.copy(attackPressed = false)
                    }
                    if (frameInput.rangedPressed) {
                        gameInputState.value = gameInputState.value.copy(rangedPressed = false)
                    }
                    if (PlatformerPhysics.isDead(next, animTime)) {
                        deathHint = when {
                            PlatformerHazards.hitsSpike(
                                next, next.player.x, next.player.y,
                                PlatformerPhysics.playerW(next.tilePx),
                                PlatformerPhysics.playerH(next.tilePx),
                            ) -> "地刺！"
                            next.projectiles.any { proj ->
                                PlatformerTrapSystem.projectileHitsPlayer(
                                    proj, next.player.x, next.player.y,
                                    PlatformerPhysics.playerW(next.tilePx),
                                    PlatformerPhysics.playerH(next.tilePx),
                                    next.tilePx,
                                )
                            } -> "炮台弹丸！"
                            next.traps.any { t ->
                                PlatformerTrapSystem.hitsPlayer(
                                    t, next.player.x, next.player.y,
                                    PlatformerPhysics.playerW(next.tilePx),
                                    PlatformerPhysics.playerH(next.tilePx),
                                    next.tilePx, animTime,
                                )
                            } -> "机关陷阱！"
                            next.lethalSkyEggHit -> "天降鸡蛋！"
                            next.enemies.any { e ->
                                e.alive && PlatformerEnemySystem.hitsPlayer(
                                    e, next.player.x, next.player.y,
                                    PlatformerPhysics.playerW(next.tilePx),
                                    PlatformerPhysics.playerH(next.tilePx),
                                    next.tilePx,
                                )
                            } -> "遭遇敌人！"
                            else -> "坠入深渊"
                        }
                        currentWorld = next.copy(
                            player = next.player.copy(dying = true, deathAnimTime = 0f),
                        )
                        deathOverlayText = deathHint ?: "阵亡"
                        when (next.characterId) {
                            PlatformerCharacterId.CHICK_PRO_MAX ->
                                PacMazeRemoteSkinAnimCache.requestSheetPlaybackAsync(
                                    PlatformerPlayerSprites.skinId,
                                    PacMazeSkinAnimClip.DIE,
                                )
                            else -> if (next.characterId.isCatalogRemote) {
                                PlatformerRemoteAnimCache.requestSheetPlaybackAsync(
                                    next.characterId,
                                    PlatformerAnimClip.DIE,
                                )
                            }
                        }
                        renderTick++
                        return@withFrameNanos
                    }
                    currentWorld = next
                    val p = next.player
                    if (!audioPrevGrounded && p.grounded && p.vy >= -8f) {
                        PlatformerSfx.playLand(playCtx)
                    }
                    if (p.vy < -400f && audioPrevGrounded && !p.grounded) {
                        PlatformerSfx.playJump(playCtx, bigJump = p.airJumpsLeft == 0 && !audioPrevGrounded)
                    }
                    if (next.gemsCollected > audioPrevGems) {
                        PlatformerSfx.playGem(playCtx)
                    }
                    val alive = next.enemies.count { it.alive }
                    if (alive < audioPrevEnemiesAlive) {
                        PlatformerSfx.playStomp(playCtx)
                    }
                    if (p.dying && !audioPrevDying) {
                        PlatformerSfx.playDie(playCtx)
                    }
                    if (next.projectiles.size > currentWorld.projectiles.size &&
                        next.player.rangedProjectileSpawned
                    ) {
                        PlatformerSfx.playShoot(playCtx)
                    }
                    audioPrevGrounded = p.grounded
                    audioPrevGems = next.gemsCollected
                    audioPrevPhase = next.phase
                    audioPrevDying = p.dying
                    audioPrevEnemiesAlive = alive
                    renderTick++
                }
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { clip = false },
        ) {
            @Suppress("UNUSED_EXPRESSION")
            renderTick
            @Suppress("UNUSED_EXPRESSION")
            skinPlaybackRevision
            PlatformerRenderer.draw(
                scope = this,
                world = currentWorld,
                assets = renderAssets,
                viewportW = size.width,
                viewportH = size.height,
                animTime = animTime,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "退出", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currentWorld.level.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (currentWorld.endlessMode) {
                    val runTiles = currentWorld.endlessTilesRun +
                        (currentWorld.player.x / currentWorld.tileF).toInt()
                    Text(
                        "距离 $runTiles 格 · ${currentWorld.level.subtitle}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                    )
                    Text(
                        "最佳 ${PlatformerEndlessRunner.bestTilesRun} 格 · 下一段落 ${PlatformerEndlessBiomes.SEGMENTS_PER_BIOME * PlatformerSegmentLibrary.SEGMENT_W} 格换景",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                    )
                } else {
                    Text(
                        "宝石 ${currentWorld.gemsCollected}/${currentWorld.gems.size}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (!isPaused) {
            if (!currentWorld.endlessMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp),
                ) {
                    PlatformerVirtualJoystick(
                        onDirection = { left, right ->
                            gameInputState.value = gameInputState.value.copy(left = left, right = right)
                        },
                    )
                }
            } else {
                Text(
                    text = "自动奔跑中 · 点击右侧跳跃",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 20.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (PlatformerCombat.canAttack(currentWorld.characterId)) {
                        PlatformerAttackButton(
                            modifier = Modifier.size(64.dp),
                            enabled = currentWorld.phase == PlatformerPhase.PLAYING,
                            cooldownFraction = PlatformerCombat.attackCooldownFraction(currentWorld.player),
                            onPress = {
                                gameInputState.value = gameInputState.value.copy(attackPressed = true)
                            },
                        )
                    }
                    if (PlatformerRangedCombat.canRangedAttack(currentWorld.characterId)) {
                        PlatformerRangedButton(
                            modifier = Modifier.size(64.dp),
                            label = PlatformerRangedCombat.rangedButtonLabel(currentWorld.characterId),
                            enabled = currentWorld.phase == PlatformerPhase.PLAYING,
                            cooldownFraction = PlatformerRangedCombat.rangedCooldownFraction(
                                currentWorld.player,
                                currentWorld.characterId,
                            ),
                            onPress = {
                                gameInputState.value = gameInputState.value.copy(rangedPressed = true)
                            },
                        )
                    }
                    PlatformerJumpButton(
                        modifier = Modifier.size(80.dp),
                        airJumpsLeft = if (currentWorld.player.grounded) 0 else currentWorld.player.airJumpsLeft,
                        onPress = {
                            gameInputState.value = gameInputState.value.copy(jumpPressed = true, jumpHeld = true)
                        },
                        onRelease = {
                            gameInputState.value = gameInputState.value.copy(jumpHeld = false)
                        },
                    )
                }
            }
        }

        if (currentWorld.player.dying && deathOverlayText != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD32F2F).copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = deathOverlayText ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    )
                    val showCountdown = deathOverlayText?.startsWith("复活") == true
                    if (showCountdown) {
                        Text(
                            text = "请稍候…",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        if (respawnFlash > 0f && currentWorld.phase == PlatformerPhase.PLAYING && !currentWorld.endlessMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD32F2F).copy(alpha = respawnFlash * 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                if (respawnFlash > 0.55f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = deathHint ?: "复活",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        )
                        Text(
                            text = "已回到起点",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        if (isPaused) {
            when (currentWorld.phase) {
                PlatformerPhase.GAME_OVER -> EndlessGameOverOverlay(
                    runTiles = currentWorld.endlessTilesRun +
                        (currentWorld.player.x / currentWorld.tileF).toInt(),
                    bestTiles = PlatformerEndlessRunner.bestTilesRun,
                    gems = currentWorld.gemsCollected,
                    onRetry = onRetry,
                    onExit = onExit,
                )
                PlatformerPhase.LEVEL_CLEAR -> LevelClearOverlay(
                    level = currentWorld.level,
                    gems = currentWorld.gemsCollected,
                    totalGems = currentWorld.gems.size,
                    hasNext = hasNextPlatformerLevel(currentWorld.level.id),
                    onRetry = onRetry,
                    onNext = onNextLevel,
                    onExit = onExit,
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun EndlessGameOverOverlay(
    runTiles: Int,
    bestTiles: Int,
    gems: Int,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("跑酷结束", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("本次距离 $runTiles 格", fontSize = 18.sp, color = Color.Gray)
                Text("最佳纪录 $bestTiles 格", fontSize = 15.sp)
                Text("收集宝石 $gems", fontSize = 14.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onExit) { Text("选关") }
                    Button(onClick = onRetry) { Text("再来一局") }
                }
            }
        }
    }
}

private fun hasNextPlatformerLevel(levelId: Int): Boolean = when {
    PlatformerSuperTuxLevelCatalog.isSuperTuxLevel(levelId) ->
        PlatformerSuperTuxLevelCatalog.hasNextLevel(levelId)
    else -> levelId < PLATFORMER_TOTAL_LEVEL_COUNT
}

@Composable
private fun LevelClearOverlay(
    level: PlatformerLevelDef,
    gems: Int,
    totalGems: Int,
    hasNext: Boolean,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("关卡完成！", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(level.title, fontSize = 18.sp, color = Color.Gray)
                Text("收集宝石 $gems / $totalGems", fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onExit) { Text("选关") }
                    OutlinedButton(onClick = onRetry) { Text("再来一次") }
                    if (hasNext) {
                        Button(onClick = onNext) { Text("下一关") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperTuxClassicPrepOverlay(
    progress: Int,
    stage: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color(0xFF0D1B2A).copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "SuperTux 经典引擎",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    stage.ifBlank {
                        if (progress >= 100) "即将启动…" else "正在准备资源…"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                )
                LinearProgressIndicator(
                    progress = progress.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4FC3F7),
                    trackColor = Color(0x33FFFFFF),
                )
                Text(
                    "${progress.coerceIn(0, 100)}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    if (progress < 35) {
                        "首次需复制约 274MB 资源包"
                    } else if (progress < 100) {
                        "首次需解压图块与音效（仅需一次）"
                    } else {
                        "准备完成，正在打开引擎…"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF78909C),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
