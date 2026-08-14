package code.name.monkey.retromusic.automix

import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import kotlin.math.abs

/** Ordena una cola con la metadata disponible; conserva el orden original cuando faltan análisis. */
object AutomixQueuePlanner {
    suspend fun smartDj(seed: Song, songs: List<Song>): List<Song> = withContext(Dispatchers.IO) {
        val repository = try { KoinJavaComponent.get(RoomRepository::class.java) as RoomRepository } catch (e: Exception) { null }
        val analyses = songs.associateWith { song -> repository?.getAutomixDataBySongId(song.id) }
        val remaining = songs.filter { it.id != seed.id }.toMutableList()
        val result = mutableListOf(seed)
        var current = seed
        while (remaining.isNotEmpty()) {
            val currentAnalysis = analyses[current]
            val next = remaining.maxByOrNull { candidate ->
                val candidateAnalysis = analyses[candidate]
                compatibility(currentAnalysis?.bpm ?: 0f, currentAnalysis?.musicalKey.orEmpty(), candidateAnalysis?.bpm ?: 0f, candidateAnalysis?.musicalKey.orEmpty())
            } ?: remaining.first()
            result += next
            remaining -= next
            current = next
        }
        result
    }

    private fun compatibility(fromBpm: Float, fromKey: String, toBpm: Float, toKey: String): Float {
        val bpmScore = if (fromBpm <= 0f || toBpm <= 0f) 0.4f else (1f - (abs(fromBpm - toBpm) / 12f)).coerceIn(0f, 1f)
        val keyScore = when {
            fromKey.isBlank() || toKey.isBlank() -> 0.5f
            fromKey == toKey -> 1f
            fromKey.dropLast(1) == toKey.dropLast(1) -> 0.85f
            else -> 0.25f
        }
        return bpmScore * 0.65f + keyScore * 0.35f
    }
}
