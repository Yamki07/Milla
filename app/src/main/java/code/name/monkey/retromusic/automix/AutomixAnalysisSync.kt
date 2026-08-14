package code.name.monkey.retromusic.automix

import android.content.Context
import code.name.monkey.retromusic.db.TrackAnalysisEntity
import code.name.monkey.retromusic.fragments.settings.MillaySettingsFragment
import code.name.monkey.retromusic.network.RemoteTrackMetadata
import code.name.monkey.retromusic.network.SupabaseClientManager
import org.json.JSONObject

/** Prepara un upsert de metadatos analizados; nunca transfiere audio. */
object AutomixAnalysisSync {
    suspend fun syncIfEnabled(
        context: Context,
        analysis: TrackAnalysisEntity,
        title: String,
        artist: String
    ) {
        if (!MillaySettingsFragment.isContributeMetadata(context)) return
        if (analysis.analysisStatus != TrackAnalysisEntity.STATUS_READY) return

        val trackId = BpmScanner.generateTrackId(artist, title, analysis.legacySongId)
        val profile = JSONObject().apply {
            put("analysis_version", analysis.analysisVersion)
            put("bpm_confidence", analysis.bpmConfidence.toDouble())
            put("camelot_key", analysis.camelotKey)
            put("cue_in_ms", analysis.cueInMs)
            put("cue_out_ms", analysis.cueOutMs)
            put("intro_silence_ms", analysis.introSilenceMs)
            put("outro_silence_ms", analysis.outroSilenceMs)
            put("integrated_lufs_approx", analysis.integratedLufs.toDouble())
            put("true_peak", analysis.truePeak.toDouble())
        }
        SupabaseClientManager.uploadMetadata(
            RemoteTrackMetadata(
                trackId = trackId,
                title = title,
                artist = artist,
                bpm = analysis.bpm.takeIf { it > 0f },
                musicalKey = analysis.camelotKey.ifBlank { analysis.musicalKey }.ifBlank { null },
                cueOutMs = analysis.cueOutMs,
                replayGain = analysis.integratedLufs,
                fullProfileJson = profile.toString()
            )
        )
    }
}
