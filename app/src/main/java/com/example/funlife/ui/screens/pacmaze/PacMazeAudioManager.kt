package com.example.funlife.ui.screens.pacmaze



import android.content.Context

import android.media.AudioAttributes

import android.media.AudioFocusRequest

import android.media.AudioManager

import android.media.MediaPlayer

import android.media.SoundPool

import android.os.Build

import android.os.Handler

import android.os.Looper

import android.util.Log

import com.example.funlife.resource.ResourceStore

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import java.io.File



/** 豆人迷宫专用音频：仅播放 COS 缓存 `pac_maze_sfx/`，不回退转盘/猜谜音效。 */

internal class PacMazeAudioManager private constructor(

    private val appContext: Context,

) {

    private val soundPool: SoundPool

    private val sfxIds = mutableMapOf<PacMazeAudioEvent, Int>()

    private val loadedSampleIds = mutableSetOf<Int>()

    private val pendingPlays = mutableMapOf<Int, PendingPlay>()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var bgmPlayer: MediaPlayer? = null

    private var currentBgm: PacMazeAudioEvent? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    private var enabled = true

    private var bundleReady = false



    init {

        val attrs = AudioAttributes.Builder()

            .setUsage(AudioAttributes.USAGE_GAME)

            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)

            .build()

        soundPool = SoundPool.Builder()

            .setMaxStreams(8)

            .setAudioAttributes(attrs)

            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->

            if (status == 0) {

                loadedSampleIds.add(sampleId)

                pendingPlays.remove(sampleId)?.let { pending ->

                    soundPool.play(
                        sampleId,
                        pending.event.volume,
                        pending.event.volume,
                        1,
                        0,
                        pending.rate,
                    )

                }

            } else {

                pendingPlays.remove(sampleId)

                Log.w(TAG, "sfx load failed sampleId=$sampleId status=$status")

            }

        }

    }



    suspend fun ensureBundle(): Boolean = withContext(Dispatchers.IO) {

        val ok = runCatching { ResourceStore.ensureBundle(BUNDLE_ID) }.getOrDefault(false)

        bundleReady = ok && ResourceStore.isPacMazeBundleReady(BUNDLE_ID)

        if (bundleReady) {

            mainHandler.post { prepare() }

        }

        Log.i(TAG, "ensureBundle ok=$ok ready=$bundleReady files=${countCachedFiles()} diag=${diagnose().summary}")

        bundleReady

    }

    /** 音效包更新后重建 SoundPool 映射，避免继续播放旧 sampleId。 */
    fun reloadAfterBundleUpdate() {
        mainHandler.post {
            sfxIds.clear()
            loadedSampleIds.clear()
            pendingPlays.clear()
            bundleReady = ResourceStore.isPacMazeBundleReady(BUNDLE_ID)
            if (bundleReady) prepare()
        }
    }



    fun diagnose(): PacMazeAudioDiagnostics {

        val menu = resolveFileForEvent(PacMazeAudioEvent.MENU_BGM)
        val campaign = resolveFileForEvent(PacMazeAudioEvent.CAMPAIGN_BGM)
        val endless = resolveFileForEvent(PacMazeAudioEvent.ENDLESS_BGM)
        val click = resolveFileForEvent(PacMazeAudioEvent.UI_BACK)

        return PacMazeAudioDiagnostics(
            bundleReady = bundleReady,
            menuBgmPath = menu?.absolutePath,
            campaignBgmPath = campaign?.absolutePath,
            endlessBgmPath = endless?.absolutePath,
            uiClickPath = click?.absolutePath,

            cachedFileCount = countCachedFiles(),

            bgmPlaying = bgmPlayer?.isPlaying == true,

            currentBgm = currentBgm?.name,

        )

    }



    fun prepare() {

        PacMazeAudioEvent.entries.filter { !it.isBgm }.forEach { event ->

            if (event in sfxIds) return@forEach

            resolveFileForEvent(event)?.let { file ->

                val id = soundPool.load(file.absolutePath, 1)

                if (id != 0) sfxIds[event] = id

            }

        }

        Log.d(TAG, "prepared sfx ${sfxIds.size}/${PacMazeAudioEvent.entries.count { !it.isBgm }}")

    }



    fun play(event: PacMazeAudioEvent, playbackRate: Float = 1f) {

        if (!enabled) return

        if (event.isBgm) {

            mainHandler.post { startBgm(event) }

            return

        }

        val file = resolveFileForEvent(event)

        if (file == null) {

            Log.w(TAG, "skip sfx (missing): ${event.name}")

            return

        }

        var sampleId = sfxIds[event]

        if (sampleId == null) {

            sampleId = soundPool.load(file.absolutePath, 1)

            if (sampleId != 0) sfxIds[event] = sampleId

        }

        if (sampleId == null || sampleId == 0) return

        val rate = playbackRate.coerceIn(0.85f, 1.15f)

        if (loadedSampleIds.contains(sampleId)) {

            soundPool.play(sampleId, event.volume, event.volume, 1, 0, rate)

        } else {

            pendingPlays[sampleId] = PendingPlay(event, rate)

        }

    }



    fun startMenuBgm() = play(PacMazeAudioEvent.MENU_BGM)

    fun startCampaignBgm() = play(PacMazeAudioEvent.CAMPAIGN_BGM)

    fun startEndlessBgm() = play(PacMazeAudioEvent.ENDLESS_BGM)



    fun pauseBgm() {

        mainHandler.post {

            runCatching { bgmPlayer?.pause() }

        }

    }



    fun stopBgm() {

        mainHandler.post { stopBgmInternal() }

    }



    fun release() {

        mainHandler.post {

            stopBgmInternal()

            soundPool.release()

            sfxIds.clear()

            loadedSampleIds.clear()

            pendingPlays.clear()

            bundleReady = false

            instance = null

        }

    }



    private fun stopBgmInternal() {

        runCatching {

            bgmPlayer?.apply {

                if (isPlaying) stop()

                release()

            }

        }

        bgmPlayer = null

        currentBgm = null

        abandonAudioFocus()

    }



    private fun startBgm(event: PacMazeAudioEvent) {

        if (currentBgm == event && bgmPlayer?.isPlaying == true) return

        val file = resolveFileForEvent(event)

        if (file == null) {

            Log.w(TAG, "skip bgm (missing): ${event.name} candidates=${event.pathCandidates()}")

            logCacheHint()

            return

        }

        stopBgmInternal()

        val attrs = AudioAttributes.Builder()

            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

            .setUsage(AudioAttributes.USAGE_MEDIA)

            .build()

        if (!requestAudioFocus(attrs)) {

            Log.w(TAG, "audio focus not granted for ${event.name}, trying anyway")

        }

        runCatching {

            bgmPlayer = MediaPlayer().apply {

                setAudioAttributes(attrs)

                setDataSource(file.absolutePath)

                isLooping = true

                setVolume(event.volume, event.volume)

                setOnPreparedListener { player ->

                    runCatching {

                        player.start()

                        Log.i(

                            TAG,

                            "bgm playing ${event.name} duration=${player.duration}ms file=${file.name}",

                        )

                    }.onFailure { e ->

                        Log.e(TAG, "bgm start failed after prepare: ${event.name}", e)

                    }

                }

                setOnErrorListener { _, what, extra ->

                    Log.e(TAG, "bgm error ${event.name} what=$what extra=$extra file=${file.absolutePath}")

                    true

                }

                prepareAsync()

            }

            currentBgm = event

            Log.i(TAG, "bgm prepare ${event.name} path=${file.absolutePath} size=${file.length()}")

        }.onFailure { Log.e(TAG, "bgm failed: ${event.name}", it) }

    }



    private fun resolveFileForEvent(event: PacMazeAudioEvent): File? {

        event.pathCandidates().forEach { relative ->

            resolveFile(relative)?.let { return it }

        }

        return null

    }



    private fun resolveFile(relativePath: String): File? {

        val cached = ResourceStore.resolveFile("$BUNDLE_ROOT/$relativePath")

        if (cached != null) return cached

        return null

    }



    private fun hasAnyCachedFile(): Boolean =

        resolveFileForEvent(PacMazeAudioEvent.MENU_BGM) != null ||
            resolveFileForEvent(PacMazeAudioEvent.CAMPAIGN_BGM) != null ||
            resolveFileForEvent(PacMazeAudioEvent.ENDLESS_BGM) != null ||
            resolveFileForEvent(PacMazeAudioEvent.UI_BACK) != null



    private fun countCachedFiles(): Int {
        val root = ResourceStore.bundleCacheDir(BUNDLE_ID) ?: return 0
        return root.walkTopDown().count { it.isFile }
    }

    private fun logCacheHint() {
        val root = ResourceStore.bundleCacheDir(BUNDLE_ID)
        if (root == null) {
            Log.w(TAG, "cache root missing: $BUNDLE_ROOT")
            return
        }

        val samples = root.walkTopDown()

            .filter { it.isFile }

            .map { it.relativeTo(root).path.replace('\\', '/') }

            .take(8)

            .joinToString()

        Log.w(TAG, "cache samples under $BUNDLE_ROOT: $samples")

    }



    private fun requestAudioFocus(attrs: AudioAttributes): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)

                .setAudioAttributes(attrs)

                .setAcceptsDelayedFocusGain(true)

                .setOnAudioFocusChangeListener { focus ->

                    when (focus) {

                        AudioManager.AUDIOFOCUS_LOSS,

                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,

                        -> mainHandler.post { pauseBgmInternal() }

                        AudioManager.AUDIOFOCUS_GAIN -> mainHandler.post { resumeBgmInternal() }

                    }

                }

                .build()

            audioFocusRequest = request

            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        } else {

            @Suppress("DEPRECATION")

            audioManager.requestAudioFocus(

                { focus ->

                    when (focus) {

                        AudioManager.AUDIOFOCUS_LOSS,

                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,

                        -> mainHandler.post { pauseBgmInternal() }

                        AudioManager.AUDIOFOCUS_GAIN -> mainHandler.post { resumeBgmInternal() }

                    }

                },

                AudioManager.STREAM_MUSIC,

                AudioManager.AUDIOFOCUS_GAIN,

            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        }

    }



    private fun abandonAudioFocus() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }

            audioFocusRequest = null

        } else {

            @Suppress("DEPRECATION")

            audioManager.abandonAudioFocus(null)

        }

    }



    private fun pauseBgmInternal() {

        runCatching { bgmPlayer?.pause() }

    }



    private fun resumeBgmInternal() {

        runCatching {

            if (bgmPlayer != null) {

                bgmPlayer?.start()

            } else {

                currentBgm?.let { startBgm(it) }

            }

        }

    }



    companion object {

        private const val TAG = "PacMazeAudio"

        private const val BUNDLE_ID = "pac_maze_sfx"

        private const val BUNDLE_ROOT = "pac_maze_sfx"



        @Volatile

        private var instance: PacMazeAudioManager? = null



        fun getInstance(context: Context): PacMazeAudioManager =

            instance ?: synchronized(this) {

                instance ?: PacMazeAudioManager(context.applicationContext).also { instance = it }

            }

    }

}



private data class PendingPlay(
    val event: PacMazeAudioEvent,
    val rate: Float,
)



data class PacMazeAudioDiagnostics(
    val bundleReady: Boolean,
    val menuBgmPath: String?,
    val campaignBgmPath: String?,
    val endlessBgmPath: String?,
    val uiClickPath: String?,
    val cachedFileCount: Int,
    val bgmPlaying: Boolean,
    val currentBgm: String?,
) {
    val menuBgmFound: Boolean get() = !menuBgmPath.isNullOrBlank()
    val campaignBgmFound: Boolean get() = !campaignBgmPath.isNullOrBlank()
    val endlessBgmFound: Boolean get() = !endlessBgmPath.isNullOrBlank()
    val summary: String
        get() = buildString {
            append("bundle=$bundleReady files=$cachedFileCount")
            append(" menu=$menuBgmFound campaign=$campaignBgmFound endless=$endlessBgmFound")
            append(" playing=$bgmPlaying track=$currentBgm")
        }
}



internal enum class PacMazeAudioEvent(

    val relativePath: String,

    val volume: Float,

    val isBgm: Boolean = false,

) {

    CHECKPOINT("curated/checkpoint.wav", 0.85f),

    /** Kenney UI Audio → curated/ui/，与 [PacMazeUiSoundId] 一一对应。 */
    UI_BACK("curated/ui/back.ogg", 0.55f),
    UI_NAV_FORWARD("curated/ui/nav_forward.ogg", 0.52f),
    UI_PRIMARY_CONFIRM("curated/ui/primary_confirm.ogg", 0.62f),
    UI_SECONDARY("curated/ui/secondary.ogg", 0.50f),
    UI_CHIP("curated/ui/chip.ogg", 0.48f),
    UI_UTILITY("curated/ui/utility.ogg", 0.54f),
    UI_TAB("curated/ui/tab.ogg", 0.50f),
    UI_LIST_SELECT("curated/ui/list_select.ogg", 0.52f),
    UI_GRID_SELECT("curated/ui/grid_select.ogg", 0.50f),
    UI_SERIES_CARD("curated/ui/series_card.ogg", 0.56f),
    UI_MODE_FEATURED("curated/ui/mode_featured.ogg", 0.58f),
    UI_MODE_OPTION("curated/ui/mode_option.ogg", 0.54f),
    UI_MAP_NODE("curated/ui/map_node.ogg", 0.56f),
    UI_MAP_CHIP("curated/ui/map_chip.ogg", 0.50f),
    UI_TOGGLE("curated/ui/toggle.ogg", 0.52f),

    MENU_BGM("variants/gameplay/gameplay_01.mp3", 0.6f, isBgm = true),
    /** 单人闯关 / 练习 / 迷宫 */
    CAMPAIGN_BGM("curated/bgm_campaign.mp3", 0.6f, isBgm = true),
    /** 无尽模式 */
    ENDLESS_BGM("curated/bgm_endless.mp3", 0.65f, isBgm = true),
    POWER_PELLET("curated/power_pellet.wav", 0.75f),
    ATTACK("curated/attack.mp3", 0.85f),
    GHOST_HIT("curated/ghost_hit.wav", 0.75f),
    PLAYER_HURT("curated/hurt.wav", 0.75f),

    VICTORY("curated/level_clear.mp3", 0.9f),

    DEFEAT("curated/game_over.mp3", 0.8f),

    ;



    fun pathCandidates(): List<String> = when (this) {

        MENU_BGM -> listOf(

            relativePath,

            "curated/bgm_gameplay.mp3",

            "variants/gameplay/gameplay_02.mp3",

        )

        CAMPAIGN_BGM -> listOf(
            relativePath,
            "variants/gameplay/gameplay_04.mp3",
            "curated/bgm_gameplay.mp3",
        )
        ENDLESS_BGM -> listOf(
            relativePath,
            "variants/gameplay/gameplay_03.mp3",
            "curated/bgm_gameplay.mp3",
        )
        GHOST_HIT, PLAYER_HURT -> listOf(
            relativePath,
            "curated/explosion.wav",
        )
        else -> listOf(relativePath)

    }

}


