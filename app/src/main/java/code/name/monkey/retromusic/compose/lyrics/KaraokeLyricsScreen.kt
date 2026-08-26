package code.name.monkey.retromusic.compose.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import code.name.monkey.retromusic.util.LyricLine

@Composable
fun KaraokeLyricsScreen(
    lines: List<LyricLine>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Find the currently active line
    val activeIndex = remember(lines, currentPositionMs) {
        lines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
    }

    // Auto-scroll to active line
    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty() && activeIndex >= 0 && activeIndex < lines.size) {
            coroutineScope.launch {
                // Scroll slightly above center
                listState.animateScrollToItem(activeIndex, scrollOffset = -250)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentPadding = PaddingValues(vertical = 120.dp), // Padding at top and bottom
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex
            
            // Calculate progress for the current line
            val progress = when {
                isPast -> 1f
                isActive -> {
                    // Calculate based on syllables
                    if (line.syllables.isNotEmpty()) {
                        val firstSyl = line.syllables.first().startMs
                        val lastSyl = line.syllables.last()
                        val lineDurationMs = (lastSyl.startMs + lastSyl.durationMs) - firstSyl
                        
                        if (lineDurationMs > 0) {
                            ((currentPositionMs - firstSyl).toFloat() / lineDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                    } else {
                        // If no syllables, just estimate based on next line (passed as durationMs in my old model, but here we can just use 1f or guess)
                        // If we are active but don't have syllables, it just lights up fully or linearly.
                        // For Milla, LrcParser already adds pseudo-syllables so this fallback is rarely hit.
                        1f
                    }
                }
                else -> 0f
            }

            // Karaoke text fill effect using Brush
            // A sharp gradient that acts as a mask
            val brush = Brush.horizontalGradient(
                0f to activeColor,
                progress to activeColor,
                progress + 0.001f to inactiveColor,
                1f to inactiveColor
            )

            Text(
                text = line.text,
                style = TextStyle(
                    fontSize = if (isActive) 34.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    brush = brush,
                    lineHeight = 42.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
