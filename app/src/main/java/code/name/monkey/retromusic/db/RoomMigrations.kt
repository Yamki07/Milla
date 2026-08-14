package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE LyricsEntity")
        database.execSQL("DROP TABLE BlackListStoreEntity")
    }
}

/**
 * Añade columnas de Time-Stamping de Precisión (ms) para Auto Mix.
 * ALTER TABLE preserva filas existentes; DEFAULT 0 para filas previas.
 */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN track_start_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN track_end_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN intro_silence_duration_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN outro_silence_duration_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN vocal_start_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN vocal_end_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN chorus_start_ms INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN cueOutMs INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * Añade la columna full_profile_json para cachear la metadata de Supabase (Auto Mix Nivel 2+).
 */
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN full_profile_json TEXT"
        )
    }
}

/** Crea tablas normalizadas para el análisis local y los planes AutoMix. */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS track_analysis (
                analysis_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                legacy_song_id INTEGER NOT NULL,
                source_uri TEXT NOT NULL,
                track_identity TEXT NOT NULL,
                source_type TEXT NOT NULL,
                bpm REAL NOT NULL,
                bpm_confidence REAL NOT NULL,
                musical_key TEXT NOT NULL,
                camelot_key TEXT NOT NULL,
                cue_in_ms INTEGER NOT NULL,
                cue_out_ms INTEGER NOT NULL,
                intro_silence_ms INTEGER NOT NULL,
                outro_silence_ms INTEGER NOT NULL,
                integrated_lufs REAL NOT NULL,
                true_peak REAL NOT NULL,
                analysis_status TEXT NOT NULL,
                analysis_version INTEGER NOT NULL,
                content_hash TEXT NOT NULL,
                last_error TEXT,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_analysis_source_uri ON track_analysis(source_uri)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_track_analysis_track_identity ON track_analysis(track_identity)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_track_analysis_analysis_status ON track_analysis(analysis_status)")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS beat_grid (
                analysis_id INTEGER NOT NULL,
                beat_index INTEGER NOT NULL,
                position_ms INTEGER NOT NULL,
                is_downbeat INTEGER NOT NULL,
                confidence REAL NOT NULL,
                PRIMARY KEY(analysis_id, beat_index)
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_beat_grid_analysis_id ON beat_grid(analysis_id)")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS cue_point (
                cue_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                analysis_id INTEGER NOT NULL,
                cue_type TEXT NOT NULL,
                position_ms INTEGER NOT NULL,
                confidence REAL NOT NULL,
                source TEXT NOT NULL,
                is_user_locked INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_cue_point_analysis_id ON cue_point(analysis_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_cue_point_analysis_id_cue_type ON cue_point(analysis_id, cue_type)")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transition_plan (
                plan_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                from_analysis_id INTEGER NOT NULL,
                to_analysis_id INTEGER NOT NULL,
                strategy TEXT NOT NULL,
                transition_start_ms INTEGER NOT NULL,
                target_start_ms INTEGER NOT NULL,
                beat_count INTEGER NOT NULL,
                tempo_ratio REAL NOT NULL,
                confidence REAL NOT NULL,
                explanation TEXT NOT NULL,
                is_user_locked INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transition_plan_from_analysis_id ON transition_plan(from_analysis_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transition_plan_to_analysis_id ON transition_plan(to_analysis_id)")
    }
}

/**
 * Añade la columna synced_lyrics_translated para cachear traducciones offline.
 */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE SongEntity ADD COLUMN synced_lyrics_translated TEXT"
        )
    }
}
