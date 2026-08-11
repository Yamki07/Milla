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
