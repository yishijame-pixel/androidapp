package com.example.funlife.ui.screens.platformer

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
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 横版冒险专用音频：播放 `platformer_sfx/` bundle。 */
internal class PlatformerAudioManager private constructor(
    private val appContext: Context,
) {
    private val gson = Gson()
    private val soundPool: SoundPool
    private val sfxIds = mutableMapOf<PlatformerSfxEvent, Int>()
    private val loadedSampleIds = mutableSetOf<Int>()
    private val pendingPlays = mutableMapOf<Int, PendingPlay>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var bgmPlayer: MediaPlayer? = null
    private var currentBgm: PlatformerSfxEvent? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var bundleReady = false
    private var manifest: SfxManifest? = null

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSampleIds.add(sampleId)
                pendingPlays.remove(sampleId)?.let { pending ->
                    soundPool.play(
                        sampleId,
                        pending.volume,
                        pending.volume,
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
        bundleReady = ok && ResourceStore.isPlatformerSfxBundleReady()
        if (bundleReady) {
            manifest = loadManifest()
            mainHandler.post { prepare() }
        }
        Log.i(TAG, "ensureBundle ok=$ok ready=$bundleReady events=${manifest?.events?.size ?: 0}")
        bundleReady
    }

    fun reloadAfterBundleUpdate() {
        mainHandler.post {
            sfxIds.clear()
            loadedSampleIds.clear()
            pendingPlays.clear()
            manifest = loadManifest()
            bundleReady = ResourceStore.isPlatformerSfxBundleReady()
            if (bundleReady) prepare()
        }
    }

    fun prepare() {
        if (manifest == null) manifest = loadManifest()
        PlatformerSfxEvent.entries.filter { !it.isBgm }.forEach { event ->
            if (event in sfxIds) return@forEach
            resolveFileForEvent(event)?.let { file ->
                val id = soundPool.load(file.absolutePath, 1)
                if (id != 0) sfxIds[event] = id
            }
        }
    }

    fun play(event: PlatformerSfxEvent, playbackRate: Float = 1f) {
        if (event.isBgm) {
            mainHandler.post { startBgm(event) }
            return
        }
        val file = resolveFileForEvent(event)
        if (file == null) {
            Log.w(TAG, "skip sfx (missing): ${event.manifestId}")
            return
        }
        var sampleId = sfxIds[event]
        if (sampleId == null) {
            sampleId = soundPool.load(file.absolutePath, 1)
            if (sampleId != 0) sfxIds[event] = sampleId
        }
        if (sampleId == null || sampleId == 0) return
        val volume = volumeFor(event)
        val rate = playbackRate.coerceIn(0.85f, 1.15f)
        if (loadedSampleIds.contains(sampleId)) {
            soundPool.play(sampleId, volume, volume, 1, 0, rate)
        } else {
            pendingPlays[sampleId] = PendingPlay(volume, rate)
        }
    }

    fun startGameplayBgm(bgmEvent: PlatformerSfxEvent = PlatformerSfxEvent.BGM_PLATFORMER) {
        play(bgmEvent)
    }

    fun pauseBgm() {
        mainHandler.post { runCatching { bgmPlayer?.pause() } }
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
            manifest = null
            instance = null
        }
    }

    private fun loadManifest(): SfxManifest? {
        val file = resolveFile("sfx_manifest.json") ?: return null
        return runCatching {
            gson.fromJson(file.readText(), SfxManifest::class.java)
        }.getOrNull()
    }

    private fun volumeFor(event: PlatformerSfxEvent): Float {
        val entry = manifest?.events?.get(event.manifestId)
        return entry?.volume?.toFloat()?.coerceIn(0f, 1f) ?: 0.65f
    }

    private fun resolveFileForEvent(event: PlatformerSfxEvent): File? {
        val rel = manifest?.events?.get(event.manifestId)?.file
            ?: defaultPath(event)
            ?: return null
        return resolveFile(rel)
    }

    private fun defaultPath(event: PlatformerSfxEvent): String? = when (event) {
        PlatformerSfxEvent.PLAYER_JUMP -> "curated/platformer/jump.wav"
        PlatformerSfxEvent.PLAYER_BIG_JUMP -> "curated/platformer/bigjump.wav"
        PlatformerSfxEvent.PLAYER_LAND -> "curated/platformer/thud.ogg"
        PlatformerSfxEvent.PLAYER_HURT -> "curated/platformer/hurt.wav"
        PlatformerSfxEvent.PLAYER_DIE -> "curated/platformer/kill.wav"
        PlatformerSfxEvent.PICKUP_GEM -> "curated/platformer/coin.wav"
        PlatformerSfxEvent.ENEMY_STOMP -> "curated/platformer/stomp.wav"
        PlatformerSfxEvent.SPRING_BOUNCE -> "curated/platformer/trampoline.wav"
        PlatformerSfxEvent.LEVEL_CLEAR -> "curated/platformer/welldone.ogg"
        PlatformerSfxEvent.CHECKPOINT -> "curated/platformer/savebell2.wav"
        PlatformerSfxEvent.SHOOT -> "curated/platformer/shoot.wav"
        PlatformerSfxEvent.BGM_PLATFORMER,
        PlatformerSfxEvent.BGM_SUPERTUX_ANTARCTIC,
        -> "curated/platformer/bgm/snowm_theme.ogg"
        else -> null
    }

    private fun resolveFile(relativePath: String): File? =
        ResourceStore.resolveFile("$BUNDLE_ROOT/$relativePath")

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

    private fun startBgm(event: PlatformerSfxEvent) {
        if (currentBgm == event && bgmPlayer?.isPlaying == true) return
        val file = resolveFileForEvent(event) ?: return
        stopBgmInternal()
        val volume = volumeFor(event)
        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        requestAudioFocus(attrs)
        runCatching {
            bgmPlayer = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(file.absolutePath)
                isLooping = true
                setVolume(volume, volume)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
            currentBgm = event
        }.onFailure { Log.e(TAG, "bgm failed: ${event.manifestId}", it) }
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
                        -> mainHandler.post { runCatching { bgmPlayer?.pause() } }
                        AudioManager.AUDIOFOCUS_GAIN -> mainHandler.post { runCatching { bgmPlayer?.start() } }
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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

    companion object {
        private const val TAG = "PlatformerAudio"
        private const val BUNDLE_ID = "platformer_sfx"
        private const val BUNDLE_ROOT = "platformer_sfx"

        @Volatile
        private var instance: PlatformerAudioManager? = null

        fun getInstance(context: Context): PlatformerAudioManager =
            instance ?: synchronized(this) {
                instance ?: PlatformerAudioManager(context.applicationContext).also { instance = it }
            }
    }
}

private data class PendingPlay(val volume: Float, val rate: Float)

private data class SfxManifest(
    val version: Int = 1,
    val events: Map<String, SfxManifestEntry> = emptyMap(),
)

private data class SfxManifestEntry(
    val file: String = "",
    val volume: Double = 0.65,
    @SerializedName("loop") val loop: Boolean = false,
)
