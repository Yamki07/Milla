/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import org.json.JSONObject
import org.json.JSONArray
import android.util.Log

/**
 * Modelo en memoria para la lógica avanzada del DJ Automix (Niveles 2, 3 y 4).
 * Se deserializa a partir del `full_profile_json` almacenado en Room / Supabase.
 */
data class AdvancedAutomixProfile(
    val trackId: String = "",
    val bpm: Float = 0f,
    val musicalKey: String = "",
    val beats: List<Float> = emptyList(),
    val downbeats: List<Float> = emptyList(),
    val sections: List<Section> = emptyList(),
    val energyCurve: List<EnergyPoint> = emptyList(),
    val vocalSegments: List<Segment> = emptyList(),
    val mixInPoints: List<Float> = emptyList(),
    val mixOutPoints: List<Float> = emptyList(),
    val endingType: String = "unknown",
    val introStyle: String = "unknown"
) {
    data class Section(val start: Float, val end: Float, val label: String)
    data class EnergyPoint(val time: Float, val energy: Float)
    data class Segment(val start: Float, val end: Float)

    companion object {
        fun fromJson(jsonString: String?): AdvancedAutomixProfile {
            if (jsonString.isNullOrBlank()) return AdvancedAutomixProfile()
            return try {
                val obj = JSONObject(jsonString)
                AdvancedAutomixProfile(
                    trackId = obj.optString("track_id", ""),
                    bpm = obj.optDouble("bpm", 0.0).toFloat(),
                    musicalKey = obj.optString("musical_key", ""),
                    beats = parseFloatArray(obj.optJSONArray("beats")),
                    downbeats = parseFloatArray(obj.optJSONArray("downbeats")),
                    sections = parseSections(obj.optJSONArray("sections")),
                    energyCurve = parseEnergyCurve(obj.optJSONArray("energy_curve")),
                    vocalSegments = parseSegments(obj.optJSONArray("vocal_segments")),
                    mixInPoints = parseFloatArray(obj.optJSONArray("mix_in_points")),
                    mixOutPoints = parseFloatArray(obj.optJSONArray("mix_out_points")),
                    endingType = obj.optString("ending_type", "unknown"),
                    introStyle = obj.optString("intro_style", "unknown")
                )
            } catch (e: Exception) {
                Log.w("AutomixProfile", "Error parseando json: ${e.message}")
                AdvancedAutomixProfile()
            }
        }

        private fun parseFloatArray(arr: JSONArray?): List<Float> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull {
                try { arr.getDouble(it).toFloat() } catch (e: Exception) { null }
            }
        }

        private fun parseSections(arr: JSONArray?): List<Section> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull {
                try {
                    val o = arr.getJSONObject(it)
                    Section(
                        start = o.optDouble("start", 0.0).toFloat(),
                        end = o.optDouble("end", 0.0).toFloat(),
                        label = o.optString("label", "unknown")
                    )
                } catch (e: Exception) { null }
            }
        }

        private fun parseEnergyCurve(arr: JSONArray?): List<EnergyPoint> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull {
                try {
                    val o = arr.getJSONObject(it)
                    EnergyPoint(
                        time = o.optDouble("time", 0.0).toFloat(),
                        energy = o.optDouble("energy", 0.0).toFloat()
                    )
                } catch (e: Exception) { null }
            }
        }

        private fun parseSegments(arr: JSONArray?): List<Segment> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull {
                try {
                    val o = arr.getJSONObject(it)
                    Segment(
                        start = o.optDouble("start", 0.0).toFloat(),
                        end = o.optDouble("end", 0.0).toFloat()
                    )
                } catch (e: Exception) { null }
            }
        }
    }
}
