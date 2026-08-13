package code.name.monkey.retromusic.workers

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import code.name.monkey.retromusic.automix.AutomixAnalysisSync
import code.name.monkey.retromusic.automix.BpmScanner
import code.name.monkey.retromusic.automix.PcmAudioAnalyzer
import code.name.monkey.retromusic.db.AutomixAnalysisDao
import code.name.monkey.retromusic.db.BeatGridEntity
import code.name.monkey.retromusic.db.CuePointEntity
import code.name.monkey.retromusic.db.TrackAnalysisEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent

class TrackAnalysisWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sourceUri = inputData.getString(KEY_SOURCE_URI).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val artist = inputData.getString(KEY_ARTIST).orEmpty()
        val sourceType = inputData.getString(KEY_SOURCE_TYPE).orEmpty().ifBlank { SOURCE_LOCAL }
        val legacySongId = inputData.getLong(KEY_LEGACY_SONG_ID, 0L)
        if (sourceUri.isBlank() || !canRead(sourceUri)) return@withContext Result.failure()

        val dao = try {
            KoinJavaComponent.get(AutomixAnalysisDao::class.java)
        } catch (_: Exception) {
            return@withContext Result.retry()
        }
        val existing = dao.findAnalysisBySourceUri(sourceUri)
        val identity = BpmScanner.generateTrackId(artist, title, legacySongId)
        val pending = TrackAnalysisEntity(
            analysisId = existing?.analysisId ?: 0L,
            legacySongId = legacySongId,
            sourceUri = sourceUri,
            trackIdentity = identity,
            sourceType = sourceType,
            bpm = existing?.bpm ?: 0f,
            bpmConfidence = existing?.bpmConfidence ?: 0f,
            musicalKey = existing?.musicalKey ?: "",
            camelotKey = existing?.camelotKey ?: "",
            analysisStatus = TrackAnalysisEntity.STATUS_ANALYZING,
            analysisVersion = TrackAnalysisEntity.CURRENT_ANALYSIS_VERSION,
            contentHash = existing?.contentHash ?: "",
            updatedAt = System.currentTimeMillis()
        )
        val analysisId = dao.upsertAnalysis(pending)

        try {
            val result = PcmAudioAnalyzer.analyze(applicationContext, sourceUri) { isStopped }
            val completed = pending.copy(
                analysisId = analysisId,
                bpm = result.bpm,
                bpmConfidence = result.bpmConfidence,
                cueInMs = result.cueInMs,
                cueOutMs = result.cueOutMs,
                introSilenceMs = result.introSilenceMs,
                outroSilenceMs = result.outroSilenceMs,
                integratedLufs = result.integratedLufsApprox,
                truePeak = result.truePeak,
                analysisStatus = TrackAnalysisEntity.STATUS_READY,
                lastError = null,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertAnalysis(completed)
            val beats = result.beatPositionsMs.mapIndexed { index, position ->
                BeatGridEntity(analysisId, index, position, isDownbeat = index % 4 == 0, confidence = result.bpmConfidence)
            }
            val cues = listOf(
                CuePointEntity(analysisId = analysisId, cueType = CuePointEntity.INTRO, positionMs = result.cueInMs, confidence = result.bpmConfidence, source = SOURCE_MEDIACODEC),
                CuePointEntity(analysisId = analysisId, cueType = CuePointEntity.MIX_IN, positionMs = result.cueInMs, confidence = result.bpmConfidence, source = SOURCE_MEDIACODEC),
                CuePointEntity(analysisId = analysisId, cueType = CuePointEntity.MIX_OUT, positionMs = result.cueOutMs, confidence = result.bpmConfidence, source = SOURCE_MEDIACODEC),
                CuePointEntity(analysisId = analysisId, cueType = CuePointEntity.OUTRO, positionMs = result.cueOutMs, confidence = result.bpmConfidence, source = SOURCE_MEDIACODEC)
            )
            dao.replaceGeneratedDetails(analysisId, beats, cues)
            AutomixAnalysisSync.syncIfEnabled(applicationContext, completed, title, artist)
            Result.success()
        } catch (_: CancellationException) {
            Result.retry()
        } catch (error: Exception) {
            dao.upsertAnalysis(pending.copy(analysisId = analysisId, analysisStatus = TrackAnalysisEntity.STATUS_FAILED, lastError = error.message, updatedAt = System.currentTimeMillis()))
            Result.retry()
        }
    }

    private fun canRead(sourceUri: String): Boolean {
        val uri = if (sourceUri.contains("://")) Uri.parse(sourceUri) else Uri.fromFile(java.io.File(sourceUri))
        if (uri.scheme == "file") return java.io.File(uri.path.orEmpty()).canRead()
        return try {
            applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val WORK_PREFIX = "TRACK_ANALYSIS_"
        private const val KEY_SOURCE_URI = "source_uri"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_LEGACY_SONG_ID = "legacy_song_id"
        private const val SOURCE_LOCAL = "local_library"
        private const val SOURCE_MEDIACODEC = "mediacodec_pcm_v1"

        fun enqueue(
            context: Context,
            sourceUri: String,
            title: String,
            artist: String,
            sourceType: String = SOURCE_LOCAL,
            legacySongId: Long = 0L
        ) {
            if (sourceUri.isBlank() || sourceUri.startsWith("tidal://")) return
            val input = Data.Builder()
                .putString(KEY_SOURCE_URI, sourceUri)
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putString(KEY_SOURCE_TYPE, sourceType)
                .putLong(KEY_LEGACY_SONG_ID, legacySongId)
                .build()
            val request = OneTimeWorkRequestBuilder<TrackAnalysisWorker>()
                .setInputData(input)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag(WORK_PREFIX + sourceUri.hashCode())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_PREFIX + sourceUri.hashCode(),
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
