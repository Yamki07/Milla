/*
 * Copyright (c) 2026 Milla Automix Engine
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.compose.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.compose.player.PlayerViewModel
import code.name.monkey.retromusic.extensions.albumArtUri
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.service.MusicService
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

// ─────── Design Tokens ───────
private val AccentPurple  = Color(0xFF8B5CF6)
private val BgDeep        = Color(0xFF0A0A0F)
private val BgMid         = Color(0xFF12121E)
private val TextPrimary   = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    onGoToAlbum: () -> Unit = {},
    onGoToArtist: () -> Unit = {},
    onOpenAutomix: () -> Unit = {}
) {
    val song          by viewModel.currentSong.collectAsState()
    val isPlaying     by viewModel.isPlaying.collectAsState()
    val progress      by viewModel.progress.collectAsState()
    val currentPos    by viewModel.currentPositionText.collectAsState()
    val totalDur      by viewModel.totalDurationText.collectAsState()
    val shuffleMode   by viewModel.shuffleMode.collectAsState()
    val repeatMode    by viewModel.repeatMode.collectAsState()
    val automixActive by viewModel.isAutomixActive.collectAsState()
    val lyrics        by viewModel.lyrics.collectAsState()

    // ─── Palette-adaptive colors ───
    var accentColor   by remember { mutableStateOf(AccentPurple) }
    var dominantColor by remember { mutableStateOf(BgMid) }
    val animatedAccent   by animateColorAsState(accentColor,   label = "accent")
    val animatedDominant by animateColorAsState(dominantColor, label = "dominant")

    // ─── Album art painter with Palette extraction ───
    val context = LocalContext.current
    val albumPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .allowHardware(false)
            .build(),
        onSuccess = { state ->
            val bmp: Bitmap? =
                (state.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            bmp?.let {
                Palette.from(it).generate { palette ->
                    palette?.dominantSwatch?.rgb?.let  { c -> dominantColor = Color(c).copy(alpha = 0.65f) }
                    palette?.vibrantSwatch?.rgb?.let   { c -> accentColor   = Color(c) }
                }
            }
        }
    )

    // ─── Spinning album art when playing ───
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "albumSpin"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Blurred background ──
        Image(
            painter = albumPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(80.dp)
        )

        // ── Dark gradient overlay ──
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(
                    BgDeep.copy(alpha = 0.80f),
                    animatedDominant,
                    BgDeep.copy(alpha = 0.95f)
                ))
            )
        )

        // ── Main content column ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 20.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Album art (circular, rotating when playing)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(BgMid)
                    .rotate(if (isPlaying) rotation else 0f)
            ) {
                Image(
                    painter = albumPainter,
                    contentDescription = "Portada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(22.dp))

            // Title
            Text(
                text = song.title.ifBlank { "Sin título" },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoToAlbum
                    )
            )

            Spacer(Modifier.height(5.dp))

            // Artist
            Text(
                text = song.artistName.ifBlank { "Artista desconocido" },
                color = TextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoToArtist
                    )
            )

            Spacer(Modifier.height(18.dp))

            // Lyrics (Phase 2)
            if (lyrics.isNotEmpty()) {
                LyricsColumn(
                    lines = lyrics,
                    currentPositionMs = (progress * (MusicPlayerRemote.songDurationMillis)).toLong(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    activeColor = animatedAccent,
                    inactiveColor = TextSecondary.copy(alpha = 0.6f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♪  Música sin letras",
                        color = TextSecondary.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Progress Slider
            Slider(
                value = progress,
                onValueChange = { viewModel.seekTo(it) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = animatedAccent,
                    activeTrackColor = animatedAccent,
                    inactiveTrackColor = TextSecondary.copy(alpha = 0.22f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(currentPos, color = TextSecondary, fontSize = 12.sp)
                Text(totalDur,   color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat
                IconButton(onClick = { MusicPlayerRemote.cycleRepeatMode() }) {
                    Icon(
                        painter = painterResource(
                            if (repeatMode == MusicService.REPEAT_MODE_THIS) R.drawable.ic_repeat_one
                            else R.drawable.ic_repeat
                        ),
                        contentDescription = "Repetir",
                        tint = if (repeatMode == MusicService.REPEAT_MODE_NONE) TextSecondary else animatedAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = { MusicPlayerRemote.back() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Anterior",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause
                FloatingActionButton(
                    onClick = {
                        if (MusicPlayerRemote.isPlaying) MusicPlayerRemote.pauseSong()
                        else MusicPlayerRemote.resumePlaying()
                    },
                    containerColor = animatedAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next
                IconButton(onClick = { MusicPlayerRemote.playNextSong() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Siguiente",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Shuffle
                IconButton(onClick = { MusicPlayerRemote.toggleShuffleMode() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle),
                        contentDescription = "Aleatorio",
                        tint = if (shuffleMode == MusicService.SHUFFLE_MODE_NONE) TextSecondary else animatedAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Automix chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (automixActive) animatedAccent.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .clickable { onOpenAutomix() }
                    .padding(horizontal = 20.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_equalizer),
                        contentDescription = "Automix",
                        tint = if (automixActive) animatedAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (automixActive) "Automix  ON" else "Automix",
                        color = if (automixActive) animatedAccent else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
