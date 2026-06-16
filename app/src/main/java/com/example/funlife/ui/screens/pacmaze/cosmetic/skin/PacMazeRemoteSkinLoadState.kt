package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

enum class RemoteSkinLoadPhase {
    None,
    Downloading,
    Decoding,
    Ready,
    Failed,
}

data class RemoteSkinLoadStatus(
    val phase: RemoteSkinLoadPhase = RemoteSkinLoadPhase.None,
    val percent: Int = 0,
    val message: String = "",
) {
    val isBlockingPreview: Boolean
        get() = phase == RemoteSkinLoadPhase.Downloading ||
            phase == RemoteSkinLoadPhase.Decoding ||
            phase == RemoteSkinLoadPhase.Failed
}

internal fun PacMazeRemoteSkinAnimConfig.primaryClip(): PacMazeSkinAnimClip = when {
    PacMazeSkinAnimClip.WALK in clips -> PacMazeSkinAnimClip.WALK
    PacMazeSkinAnimClip.IDLE in clips -> PacMazeSkinAnimClip.IDLE
    else -> clips.first()
}
