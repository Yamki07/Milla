package code.name.monkey.retromusic.util

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import code.name.monkey.retromusic.network.SupabaseClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/**
 * Scans the local device for music files and extracts custom DJ metadata (BPM/Key)
 * using Jaudiotagger. Pushes these real tags to Supabase.
 */
object LocalMetadataScanner {
    private const val TAG = "LocalMetadataScanner"

    suspend fun scanAndUploadLocalTags(context: Context) {
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
            )

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                null
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                val supabaseData = mutableListOf<Map<String, Any>>()

                while (c.moveToNext()) {
                    val filePath = c.getString(dataCol)
                    val title = c.getString(titleCol)
                    val artist = c.getString(artistCol)
                    
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            val audioFile = AudioFileIO.read(file)
                            val tag = audioFile.tag
                            
                            if (tag != null) {
                                val bpmStr = tag.getFirst(FieldKey.BPM)
                                val keyStr = tag.getFirst(FieldKey.KEY)
                                
                                val bpm = bpmStr.toDoubleOrNull() ?: 0.0
                                val key = keyStr.ifEmpty { "C" }
                                
                                // Only upload if we actually found valid DJ metadata
                                if (bpm > 0.0) {
                                    // Construct the unique milla_id exactly as we do everywhere else
                                    val cleanArtist = artist.filter { it.isLetterOrDigit() || it.isWhitespace() }.replace(" ", "_").lowercase()
                                    val cleanTitle = title.filter { it.isLetterOrDigit() || it.isWhitespace() }.replace(" ", "_").lowercase()
                                    val millaId = "${cleanArtist}_${cleanTitle}"

                                    supabaseData.add(
                                        mapOf(
                                            "track_id" to millaId,
                                            "title" to title,
                                            "artist" to artist,
                                            "bpm" to bpm,
                                            "musical_key" to key,
                                            "cue_out_ms" to 5000.0,
                                            "replay_gain" to -5.0
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading tags from $filePath", e)
                    }

                    // Batch upload every 50 files
                    if (supabaseData.size >= 50) {
                        SupabaseClientManager.insertTrackMetadata(supabaseData)
                        supabaseData.clear()
                    }
                }
                
                // Upload remaining
                if (supabaseData.isNotEmpty()) {
                    SupabaseClientManager.insertTrackMetadata(supabaseData)
                }
            }
        }
    }
}
