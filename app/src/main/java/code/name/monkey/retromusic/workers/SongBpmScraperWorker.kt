package code.name.monkey.retromusic.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import code.name.monkey.retromusic.automix.BpmScanner
import code.name.monkey.retromusic.automix.SongBpmScraper
import code.name.monkey.retromusic.network.RemoteTrackMetadata
import code.name.monkey.retromusic.network.SupabaseClientManager
import code.name.monkey.retromusic.repository.Repository
import code.name.monkey.retromusic.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent

class SongBpmScraperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository: Repository = try {
            KoinJavaComponent.get(Repository::class.java)
        } catch (_: Exception) {
            return@withContext Result.retry()
        }

        try {
            val allSongs = repository.allSongs()
            
            // Elegir aleatoriamente 20 canciones para inspeccionar y ver si necesitan update.
            // Al ser periódico, eventualmente cubrirá toda la biblioteca.
            val songsToScrape = allSongs.shuffled().take(20)

            var count = 0
            for (song in songsToScrape) {
                if (isStopped) break

                val trackId = BpmScanner.generateTrackId(song.artistName, song.title, song.id)
                
                // First check if Supabase already has full data (mood != null)
                val existing = SupabaseClientManager.fetchMetadata(trackId)
                if (existing != null && existing.mood != null) {
                    continue // Ya está enriquecido en la base de datos
                }

                Log.d("SongBpmScraperWorker", "Scrapeando: ${song.title} por ${song.artistName}")
                
                // Retraso progresivo para simular humano (entre 5 y 10 segundos)
                delay((5000L..10000L).random())

                val scrapedData = SongBpmScraper.search(song.artistName, song.title)
                if (scrapedData != null && scrapedData.bpm > 0f) {
                    val cueOutMs = if (scrapedData.durationMs > 0) Math.max(0L, scrapedData.durationMs - 5000L) else 0L
                    
                    val newRemoteMeta = RemoteTrackMetadata(
                        trackId = trackId,
                        title = scrapedData.title,
                        artist = scrapedData.artist,
                        bpm = scrapedData.bpm,
                        musicalKey = scrapedData.key,
                        cueOutMs = cueOutMs,
                        replayGain = 0f,
                        mood = scrapedData.mood,
                        halfTimeBpm = scrapedData.halfTimeBpm,
                        mode = scrapedData.mode,
                        energy = scrapedData.energy,
                        danceability = scrapedData.danceability,
                        timeSignature = scrapedData.timeSignature,
                        syncedLyrics = existing?.syncedLyrics,
                        fullProfileJson = existing?.fullProfileJson
                    )
                    
                    SupabaseClientManager.uploadMetadata(newRemoteMeta)
                    count++
                    
                    // También actualizar DB local
                    try {
                        val roomRepo = repository as? RoomRepository
                        roomRepo?.updateSongAutomixData(
                            songId = song.id,
                            bpm = scrapedData.bpm,
                            key = scrapedData.key,
                            replayGain = 0f,
                            cueOut = cueOutMs
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            
            Log.d("SongBpmScraperWorker", "Scraping completado. $count canciones enriquecidas.")
            Result.success()
        } catch (e: Exception) {
            Log.e("SongBpmScraperWorker", "Error en scraper worker: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SongBpmScraperWorker"

        fun schedule(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<SongBpmScraperWorker>(
                2, java.util.concurrent.TimeUnit.HOURS
            ).setConstraints(constraints).build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
