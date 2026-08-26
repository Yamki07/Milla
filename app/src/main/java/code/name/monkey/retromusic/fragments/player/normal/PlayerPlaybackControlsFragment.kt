/*
 * Copyright (c) 2020 Hemanth Savarla.
 * Fase 1 — Migrated to Jetpack Compose PlayerScreen.
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.player.normal

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.AutomixBottomSheet
import code.name.monkey.retromusic.compose.player.PlayerScreen
import code.name.monkey.retromusic.compose.player.PlayerViewModel
import code.name.monkey.retromusic.fragments.base.AbsPlayerControlsFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.google.android.material.slider.Slider

/**
 * Fase 1: PlayerPlaybackControlsFragment ahora monta un ComposeView con PlayerScreen.
 * La lógica visual vive en PlayerScreen.kt y PlayerViewModel.kt.
 * Se mantiene la estructura de AbsPlayerControlsFragment para compatibilidad con el
 * sistema de navegación y callbacks de MusicService.
 */
class PlayerPlaybackControlsFragment :
    AbsPlayerControlsFragment(R.layout.fragment_player_playback_controls) {

    private val playerViewModel: PlayerViewModel by viewModels()

    // ── Stubs for AbsPlayerControlsFragment abstract/open properties ──
    // Progress is managed by Compose + PlayerViewModel, not by XML views.
    override val progressSlider: Slider? get() = null
    override val shuffleButton: ImageButton get() = _stubButton
    override val repeatButton: ImageButton  get() = _stubButton
    override val nextButton: ImageButton?     get() = null
    override val previousButton: ImageButton? get() = null
    override val automixButton: ImageButton?  get() = null
    override val songTotalTime: TextView?     get() = null
    override val songCurrentProgress: TextView? get() = null

    /** Invisible stub button — satisfies abstract constraint, never shown */
    private val _stubButton: ImageButton by lazy {
        ImageButton(requireContext()).apply { visibility = View.GONE }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sync initial state into ViewModel
        playerViewModel.updateSong(MusicPlayerRemote.currentSong)
        playerViewModel.updatePlayState(MusicPlayerRemote.isPlaying)
        playerViewModel.updateShuffleMode(MusicPlayerRemote.shuffleMode)
        playerViewModel.updateRepeatMode(MusicPlayerRemote.repeatMode)
        playerViewModel.updateAutomixState(MusicPlayerRemote.isAutomixActive)

        // Mount Compose UI
        view.findViewById<ComposeView>(R.id.composePlayerControls)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PlayerScreen(
                    viewModel = playerViewModel,
                    onGoToAlbum  = { code.name.monkey.retromusic.fragments.base.goToAlbum(requireActivity()) },
                    onGoToArtist = { code.name.monkey.retromusic.fragments.base.goToArtist(requireActivity()) },
                    onOpenAutomix = {
                        val active = MusicPlayerRemote.toggleClubMode()
                        playerViewModel.updateAutomixState(active)
                        AutomixBottomSheet.newInstance()
                            .show(parentFragmentManager, "AUTOMIX_BOTTOM_SHEET")
                    }
                )
            }
        }
    }

    // ── MusicService Callbacks ── keep ViewModel in sync ──

    override fun onServiceConnected() {
        playerViewModel.updateSong(MusicPlayerRemote.currentSong)
        playerViewModel.updatePlayState(MusicPlayerRemote.isPlaying)
        playerViewModel.updateShuffleMode(MusicPlayerRemote.shuffleMode)
        playerViewModel.updateRepeatMode(MusicPlayerRemote.repeatMode)
        playerViewModel.updateAutomixState(MusicPlayerRemote.isAutomixActive)
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        playerViewModel.updateSong(MusicPlayerRemote.currentSong)
    }

    override fun onPlayStateChanged() {
        playerViewModel.updatePlayState(MusicPlayerRemote.isPlaying)
    }

    override fun onRepeatModeChanged() {
        playerViewModel.updateRepeatMode(MusicPlayerRemote.repeatMode)
    }

    override fun onShuffleModeChanged() {
        playerViewModel.updateShuffleMode(MusicPlayerRemote.shuffleMode)
    }

    // ── setColor: no-op — Compose handles colors via Palette ──
    override fun setColor(color: MediaNotificationProcessor) {
        // Intentionally empty: palette-based coloring done inside PlayerScreen
    }

    override fun show() { /* Compose handles visibility */ }
    override fun hide() { /* Compose handles visibility */ }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
