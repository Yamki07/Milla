package code.name.monkey.retromusic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AutomixAnalysisDao {
    @Query("SELECT * FROM track_analysis WHERE source_uri = :sourceUri LIMIT 1")
    suspend fun findAnalysisBySourceUri(sourceUri: String): TrackAnalysisEntity?

    @Query("SELECT * FROM track_analysis WHERE analysis_id = :analysisId LIMIT 1")
    suspend fun findAnalysisById(analysisId: Long): TrackAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalysis(analysis: TrackAnalysisEntity): Long

    @Query("DELETE FROM beat_grid WHERE analysis_id = :analysisId")
    suspend fun deleteBeatGrid(analysisId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeatGrid(beats: List<BeatGridEntity>)

    @Query("DELETE FROM cue_point WHERE analysis_id = :analysisId AND is_user_locked = 0")
    suspend fun deleteGeneratedCuePoints(analysisId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuePoints(cues: List<CuePointEntity>)

    @Query("SELECT * FROM cue_point WHERE analysis_id = :analysisId ORDER BY position_ms")
    suspend fun getCuePoints(analysisId: Long): List<CuePointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransitionPlan(plan: TransitionPlanEntity): Long

    @Query("SELECT * FROM transition_plan WHERE from_analysis_id = :fromId AND to_analysis_id = :toId ORDER BY updated_at DESC LIMIT 1")
    suspend fun getLatestTransitionPlan(fromId: Long, toId: Long): TransitionPlanEntity?

    @Transaction
    suspend fun replaceGeneratedDetails(
        analysisId: Long,
        beats: List<BeatGridEntity>,
        generatedCues: List<CuePointEntity>
    ) {
        deleteBeatGrid(analysisId)
        if (beats.isNotEmpty()) insertBeatGrid(beats)
        deleteGeneratedCuePoints(analysisId)
        if (generatedCues.isNotEmpty()) insertCuePoints(generatedCues)
    }
}
