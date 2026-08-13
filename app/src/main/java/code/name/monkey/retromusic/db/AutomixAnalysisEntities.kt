package code.name.monkey.retromusic.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_analysis",
    indices = [
        Index(value = ["source_uri"], unique = true),
        Index(value = ["track_identity"]),
        Index(value = ["analysis_status"])
    ]
)
data class TrackAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "analysis_id")
    val analysisId: Long = 0L,
    @ColumnInfo(name = "legacy_song_id")
    val legacySongId: Long = 0L,
    @ColumnInfo(name = "source_uri")
    val sourceUri: String,
    @ColumnInfo(name = "track_identity")
    val trackIdentity: String,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    val bpm: Float = 0f,
    @ColumnInfo(name = "bpm_confidence")
    val bpmConfidence: Float = 0f,
    @ColumnInfo(name = "musical_key")
    val musicalKey: String = "",
    @ColumnInfo(name = "camelot_key")
    val camelotKey: String = "",
    @ColumnInfo(name = "cue_in_ms")
    val cueInMs: Long = 0L,
    @ColumnInfo(name = "cue_out_ms")
    val cueOutMs: Long = 0L,
    @ColumnInfo(name = "intro_silence_ms")
    val introSilenceMs: Long = 0L,
    @ColumnInfo(name = "outro_silence_ms")
    val outroSilenceMs: Long = 0L,
    @ColumnInfo(name = "integrated_lufs")
    val integratedLufs: Float = 0f,
    @ColumnInfo(name = "true_peak")
    val truePeak: Float = 0f,
    @ColumnInfo(name = "analysis_status")
    val analysisStatus: String = STATUS_PENDING,
    @ColumnInfo(name = "analysis_version")
    val analysisVersion: Int = CURRENT_ANALYSIS_VERSION,
    @ColumnInfo(name = "content_hash")
    val contentHash: String = "",
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ANALYZING = "ANALYZING"
        const val STATUS_READY = "READY"
        const val STATUS_FAILED = "FAILED"
        const val CURRENT_ANALYSIS_VERSION = 1
    }
}

@Entity(
    tableName = "beat_grid",
    primaryKeys = ["analysis_id", "beat_index"],
    indices = [Index(value = ["analysis_id"])]
)
data class BeatGridEntity(
    @ColumnInfo(name = "analysis_id")
    val analysisId: Long,
    @ColumnInfo(name = "beat_index")
    val beatIndex: Int,
    @ColumnInfo(name = "position_ms")
    val positionMs: Long,
    @ColumnInfo(name = "is_downbeat")
    val isDownbeat: Boolean = false,
    val confidence: Float = 0f
)

@Entity(
    tableName = "cue_point",
    indices = [Index(value = ["analysis_id"]), Index(value = ["analysis_id", "cue_type"])]
)
data class CuePointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cue_id")
    val cueId: Long = 0L,
    @ColumnInfo(name = "analysis_id")
    val analysisId: Long,
    @ColumnInfo(name = "cue_type")
    val cueType: String,
    @ColumnInfo(name = "position_ms")
    val positionMs: Long,
    val confidence: Float,
    val source: String,
    @ColumnInfo(name = "is_user_locked")
    val isUserLocked: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val INTRO = "INTRO"
        const val MIX_IN = "MIX_IN"
        const val MIX_OUT = "MIX_OUT"
        const val OUTRO = "OUTRO"
    }
}

@Entity(
    tableName = "transition_plan",
    indices = [Index(value = ["from_analysis_id"]), Index(value = ["to_analysis_id"])]
)
data class TransitionPlanEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "plan_id")
    val planId: Long = 0L,
    @ColumnInfo(name = "from_analysis_id")
    val fromAnalysisId: Long,
    @ColumnInfo(name = "to_analysis_id")
    val toAnalysisId: Long,
    val strategy: String,
    @ColumnInfo(name = "transition_start_ms")
    val transitionStartMs: Long,
    @ColumnInfo(name = "target_start_ms")
    val targetStartMs: Long,
    @ColumnInfo(name = "beat_count")
    val beatCount: Int,
    @ColumnInfo(name = "tempo_ratio")
    val tempoRatio: Float,
    val confidence: Float,
    val explanation: String,
    @ColumnInfo(name = "is_user_locked")
    val isUserLocked: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
