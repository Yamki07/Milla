package code.name.monkey.retromusic.fragments.settings

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.LocalMetadataScanner
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Millay Internet Settings Fragment
 * Controls streaming quality, download quality, embed lyrics/covers, Automix data contribution.
 */
class MillaySettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_millay, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            "millay_scan_local_library" -> {
                val progressDialog = android.app.ProgressDialog(requireContext()).apply {
                    setTitle("Escaneando Biblioteca Local")
                    setMessage("🎵 Analizando BPM y Key...")
                    setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
                    setCancelable(false)
                    max = 100
                    show()
                }
                
                GlobalScope.launch {
                    LocalMetadataScanner.scanEntireDeviceAndUpload(requireContext()) { progress, total ->
                        requireActivity().runOnUiThread {
                            progressDialog.max = total
                            progressDialog.progress = progress
                            progressDialog.setMessage("Analizando canción $progress de $total")
                        }
                    }
                    requireActivity().runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "✅ ¡Escaneo y subida completados!", Toast.LENGTH_LONG).show()
                    }
                }
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    companion object {
        fun getStreamingQuality(context: Context): String {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString("millay_streaming_quality", "flac") ?: "flac"
        }

        fun getDownloadQuality(context: Context): String {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString("millay_download_quality", "flac") ?: "flac"
        }

        fun isEmbedLyrics(context: Context): Boolean {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean("millay_embed_lyrics", true)
        }

        fun isEmbedCover(context: Context): Boolean {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean("millay_embed_cover", true)
        }

        fun isContributeMetadata(context: Context): Boolean {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean("millay_contribute_metadata", true)
        }

        fun getDownloadQualityInt(context: Context): Int {
            return when (getDownloadQuality(context)) {
                "flac" -> 9  // FLAC
                "mp3_320" -> 3  // MP3 320
                "mp3_128" -> 1  // MP3 128
                else -> 9
            }
        }

        fun getStreamingQualityInt(context: Context): Int {
            return when (getStreamingQuality(context)) {
                "flac" -> 9
                "mp3_320" -> 3
                "mp3_128" -> 1
                else -> 9
            }
        }

        fun qualityToBadgeLabel(qualityStr: String): String {
            return when (qualityStr) {
                "flac" -> "FLAC"
                "mp3_320" -> "MP3 320"
                "mp3_128" -> "MP3"
                else -> "HQ"
            }
        }
    }
}
