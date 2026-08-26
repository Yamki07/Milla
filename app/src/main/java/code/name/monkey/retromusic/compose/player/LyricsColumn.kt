package code.name.monkey.retromusic.compose.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import code.name.monkey.retromusic.util.LyricLine

@Composable
fun LyricsColumn(
    lines: List<LyricLine>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.4f)
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
                listState.animateScrollToItem(activeIndex, scrollOffset = -80)
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = 0.99f
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val isActive = index == activeIndex
                val isPast = index < activeIndex
                
                // Calculate progress for the current line
                val progress = when {
                    isPast -> 1f
                    isActive -> {
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
                            1f
                        }
                    }
                    else -> 0f
                }

                // Brush for karaoke fill
                val brush = Brush.horizontalGradient(
                    0f to activeColor,
                    progress to activeColor,
                    progress + 0.001f to inactiveColor,
                    1f to inactiveColor
                )

                Text(
                    text = line.text,
                    style = TextStyle(
                        fontSize = if (isActive) 18.sp else 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        brush = brush,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Overlay for top and bottom fade (masking)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black,
                        0.25f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to Color.Black
                    )
                )
                // Removed blendMode to avoid compilation error on older compose versions
        )
    }
}
