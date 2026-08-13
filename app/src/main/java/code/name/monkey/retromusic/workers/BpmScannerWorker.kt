/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import code.name.monkey.retromusic.automix.BpmScanner
import code.name.monkey.retromusic.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker en segundo plano (AndroidX WorkManager) encargado de analizar periódicamente
 * la biblioteca local sin congelar la app ni saturar la CPU.
 *
 * Escanea canciones con bpm == 0f o cueOutMs == 0L por lotes de 10 unidades,
 * apalancándose en Nivel 1 (ID3 local), Nivel 2 (Deezer API Cloud) y Nivel 3 (Análisis RMS).
 */
class BpmScannerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository: RoomRepository = try {
                org.koin.java.KoinJavaComponent.get(RoomRepository::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo inyectar RoomRepository en BpmScannerWorker: ${e.message}")
                return@withContext Result.failure()
            }

            val unscannedSongs = repository.getUnscannedSongs()
            if (unscannedSongs.isEmpty()) {
                Log.i(TAG, "No hay canciones pendientes por escaneo BPM/Key.")
                return@withContext Result.success()
            }

            Log.i(TAG, "Iniciando escaneo de ${unscannedSongs.size} canciones pendientes en lotes de $BATCH_SIZE...")

            // Procesamiento en lotes para no saturar memoria
            val batches = unscannedSongs.chunked(BATCH_SIZE)
            for ((index, batch) in batches.withIndex()) {
                if (isStopped) {
                    Log.w(TAG, "BpmScannerWorker detenido por el sistema antes de concluir.")
                    break
                }

                for (song in batch) {
                    try {
                        val scanned = BpmScanner.scanSongEntity(song, repository)
                        repository.updateSongAutomixData(
                            scanned.id,
                            scanned.bpm,
                            scanned.musicalKey,
                            scanned.replayGain,
                            scanned.cueOutMs
                        )
                        TrackAnalysisWorker.enqueue(
                            context = applicationContext,
                            sourceUri = scanned.data,
                            title = scanned.title,
                            artist = scanned.artistName,
                            sourceType = "local_library",
                            legacySongId = scanned.id
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error escaneando canción ${song.id} (${song.title}): ${e.message}")
                    }
                }

                Log.d(TAG, "Lote ${index + 1}/${batches.size} escaneado correctamente.")
                // Breve pausa para dar respiro al hilo de E/S del dispositivo
                delay(200L)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fallo general en BpmScannerWorker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BpmScannerWorker"
        private const val WORK_NAME = "BPM_SCANNER_WORKER"
        private const val BATCH_SIZE = 10

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BpmScannerWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun scheduleOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<BpmScannerWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_ONE_TIME",
                androidx.work.ExistingWorkPolicy.KEEP,
                workRequest
            )
        }

        fun runOnceImmediately(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<BpmScannerWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
