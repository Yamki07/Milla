/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.db

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(indices = [Index(value = ["playlist_creator_id", "id"], unique = true)])
class SongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "song_key")
    val songPrimaryKey: Long = 0L,
    @ColumnInfo(name = "playlist_creator_id")
    val playlistCreatorId: Long,
    val id: Long,
    val title: String,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int,
    val year: Int,
    val duration: Long,
    val data: String,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long,
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    @ColumnInfo(name = "album_name")
    val albumName: String,
    @ColumnInfo(name = "artist_id")
    val artistId: Long,
    @ColumnInfo(name = "artist_name")
    val artistName: String,
    val composer: String?,
    @ColumnInfo(name = "album_artist")
    val albumArtist: String?,
    val bpm: Float = 0f,
    val replayGain: Float = 0f,
    val musicalKey: String = "",
    /** Inicio efectivo de la pista (ms), excluyendo silencio de intro si aplica. */
    @ColumnInfo(name = "track_start_ms", defaultValue = "0")
    val trackStartMs: Long = 0L,
    /** Fin efectivo de la pista (ms). */
    @ColumnInfo(name = "track_end_ms", defaultValue = "0")
    val trackEndMs: Long = 0L,
    /** Duración del silencio de intro detectado (ms). */
    @ColumnInfo(name = "intro_silence_duration_ms", defaultValue = "0")
    val introSilenceDurationMs: Long = 0L,
    /** Duración del silencio de outro detectado (ms). */
    @ColumnInfo(name = "outro_silence_duration_ms", defaultValue = "0")
    val outroSilenceDurationMs: Long = 0L,
    /** Inicio de la sección vocal (ms). */
    @ColumnInfo(name = "vocal_start_ms", defaultValue = "0")
    val vocalStartMs: Long = 0L,
    /** Fin de la sección vocal (ms). */
    @ColumnInfo(name = "vocal_end_ms", defaultValue = "0")
    val vocalEndMs: Long = 0L,
    /** Inicio del estribillo / chorus (ms). */
    @ColumnInfo(name = "chorus_start_ms", defaultValue = "0")
    val chorusStartMs: Long = 0L,
    /** Ventana de salida Automix (ms antes del fin efectivo). */
    val cueOutMs: Long = 0L,
    /** JSON caché con el perfil completo desde Supabase (Beats, curvas de energía, etc). */
    @ColumnInfo(name = "full_profile_json")
    val fullProfileJson: String? = null
) : Parcelable
