package code.name.monkey.retromusic.fragments.player.compose

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import code.name.monkey.retromusic.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import code.name.monkey.retromusic.fragments.player.PlayerViewModel
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.RetroUtil
import code.name.monkey.retromusic.views.SyncedLyricsView
import code.name.monkey.retromusic.lyrics.LyricsMockData // Asuming there's a provider or we can fetch real ones

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // We can extract a dark theme background or use Palette from the cover.
    // For now, let's use a sleek dark gradient or solid color.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album Art Section (Top 40%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(24.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(code.name.monkey.retromusic.util.MusicUtil.getMediaStoreAlbumCoverUri(uiState.currentSong.albumId))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.currentSong.title ?: "Unknown Title",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = uiState.currentSong.artistName ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.LightGray,
                    maxLines = 1
                )
            }
            
            // Lyrics View Section (Middle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(vertical = 16.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SyncedLyricsView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setPositionSource(
                                position = { MusicPlayerRemote.currentPlaybackPositionMs },
                                isPlaying = { MusicPlayerRemote.isPlaying }
                            )
                            setOnLineClickListener { timeMs ->
                                viewModel.seekTo(timeMs)
                            }
                            // MOCK/REAL data: We use LyricsMockData to show the enhanced engine if needed
                            // In real scenario, we parse from file. Let's use the mock data for now.
                            submitLines(code.name.monkey.retromusic.lyrics.SyncedLyricsParser.parse(code.name.monkey.retromusic.lyrics.LyricsMockData.ENHANCED_LRC))
                        }
                    },
                    update = { view ->
                        // Re-submit lines if song changes
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Controls Section (Bottom 20%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previous() }) {
                    Icon(painterResource(id = R.drawable.ic_skip_previous), contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                
                FloatingActionButton(
                    onClick = { viewModel.playPause() },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (uiState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = { viewModel.next() }) {
                    Icon(painterResource(id = R.drawable.ic_skip_next), contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}
