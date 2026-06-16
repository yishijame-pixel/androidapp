package com.example.funlife.ui.screens.pacmaze

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PacMazeAudioEventTest {

    @Test
    fun campaignBgm_usesGameplay4Asset() {
        assertThat(PacMazeAudioEvent.CAMPAIGN_BGM.relativePath)
            .isEqualTo("curated/bgm_campaign.mp3")
        assertThat(PacMazeAudioEvent.CAMPAIGN_BGM.pathCandidates().first())
            .isEqualTo("curated/bgm_campaign.mp3")
    }

    @Test
    fun endlessBgm_usesGameplay3Asset() {
        assertThat(PacMazeAudioEvent.ENDLESS_BGM.relativePath)
            .isEqualTo("curated/bgm_endless.mp3")
        assertThat(PacMazeAudioEvent.ENDLESS_BGM.pathCandidates().first())
            .isEqualTo("curated/bgm_endless.mp3")
    }

    @Test
    fun uiSoundIds_mapToKenneyUiCuratedPaths() {
        assertThat(PacMazeUiSoundId.entries).hasSize(15)
        PacMazeUiSoundId.entries.forEach { sound ->
            assertThat(sound.event.relativePath).startsWith("curated/ui/")
        }
        assertThat(PacMazeUiSoundId.PrimaryConfirm.event.relativePath)
            .isEqualTo("curated/ui/primary_confirm.ogg")
        assertThat(PacMazeUiSoundId.NavigateBack.event.relativePath)
            .isEqualTo("curated/ui/back.ogg")
    }
}
