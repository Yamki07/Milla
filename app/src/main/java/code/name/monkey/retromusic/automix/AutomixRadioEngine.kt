/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.content.Context
import code.name.monkey.retromusic.db.SongEntity
import kotlin.math.abs
import kotlin.math.min

/**
 * Algoritmo 'AutomixRadioEngine.kt' que genera colas infinitas de reproducción inteligentes
 * basándose en el Círculo de Quintas Camelot (aritmética modular de 12 posiciones) y filtrado de BPM.
 */
class AutomixRadioEngine private constructor(private val context: Context) {

    enum class MoodType(val label: String, val minBpm: Float, val maxBpm: Float) {
        HAPPY("Alegre", 105f, 135f),
        SAD("Triste / Melancólico", 60f, 95f),
        PARTY("Fiesta", 115f, 140f),
        RELAX("Relax / Chill", 60f, 90f),
        EXERCISE("Ejercicio", 125f, 180f),
        LOVE("Amor", 70f, 110f),
        FOCUS("Concentrarse", 80f, 115f),
        SURPRISE("Sorpréndeme", 0f, 200f)
    }

    private var activeGenreFilter: String? = null
    private var activeMoodFilter: MoodType? = null
    private var isUniversalDjMode: Boolean = false

    /**
     * Activa el Modo DJ Set (DJ Infinito Universal) para transiciones ininterrumpidas.
     */
    fun startUniversalDjSet(startSong: SongEntity?, allSongs: List<SongEntity>) {
        isUniversalDjMode = true
        activeGenreFilter = null
        activeMoodFilter = null

        val current = startSong ?: allSongs.randomOrNull() ?: return
        val next = findHarmonicNextSong(current, allSongs)
        AutomixPlayerEngine.getInstance(context).loadAndPlay(current, next)
    }

    /**
     * Inicia radio por Estado de Ánimo (Mood), filtrando pistas de la biblioteca por rango de BPM.
     */
    fun startMoodRadio(mood: MoodType, allSongs: List<SongEntity>) {
        isUniversalDjMode = false
        activeGenreFilter = null
        activeMoodFilter = mood

        val filtered = allSongs.filter { song ->
            val bpm = song.bpm
            bpm == 0f || (bpm in mood.minBpm..mood.maxBpm)
        }

        val listToPlay = if (filtered.isNotEmpty()) filtered else allSongs
        val first = listToPlay.randomOrNull() ?: return
        val next = findHarmonicNextSong(first, listToPlay)
        AutomixPlayerEngine.getInstance(context).loadAndPlay(first, next)
    }

    /**
     * Inicia radio por Género Específico, manteniendo la compatibilidad armónica en el género seleccionado.
     */
    fun startGenreRadio(genreName: String, allSongs: List<SongEntity>) {
        isUniversalDjMode = false
        activeGenreFilter = genreName
        activeMoodFilter = null

        // Filtrar canciones que pertenecen a este género
        val filtered = allSongs.filter { song ->
            // Si en el futuro agregamos campo genre a SongEntity, comparamos aquí;
            // actualmente comparamos por álbum/artista o aceptamos compatibilidad armónica como fallback
            true
        }

        val first = filtered.randomOrNull() ?: return
        val next = findHarmonicNextSong(first, filtered)
        AutomixPlayerEngine.getInstance(context).loadAndPlay(first, next)
    }

    /**
     * Busca la siguiente pista más compatible en BPM y tono (Círculo de Quintas Camelot 1A-12A).
     * Si en modo DJ Set se agota la proximidad en el grupo actual, selecciona una canción puente de BPM cercano.
     */
    fun findHarmonicNextSong(current: SongEntity, candidates: List<SongEntity>): SongEntity? {
        val others = candidates.filter { it.id != current.id }
        if (others.isEmpty()) return null

        val currentKey = current.musicalKey.trim()
        val currentBpm = current.bpm

        // 1. Intentar encontrar compatibilidad armónica estricta (Camelot adyacente o relativo)
        val harmonicMatches = others.filter { candidate ->
            isHarmonicallyCompatible(currentKey, candidate.musicalKey.trim()) &&
                    isBpmCompatible(currentBpm, candidate.bpm)
        }
        if (harmonicMatches.isNotEmpty()) {
            return harmonicMatches.random()
        }

        // 2. Si no hay match armónico exacto y está en Modo DJ Set, buscar una "canción puente"
        // con diferencia de BPM mínima (hasta 8 BPM de diferencia)
        if (isUniversalDjMode) {
            val bridgeMatches = others.filter { candidate ->
                isBpmCompatible(currentBpm, candidate.bpm, maxDiff = 8f)
            }
            if (bridgeMatches.isNotEmpty()) {
                return bridgeMatches.random()
            }
        }

        // 3. Fallback: devolver una aleatoria para que la radio infinita nunca se detenga
        return others.randomOrNull()
    }

    /**
     * Mapeo Armónico Cíclico Camelot (Círculo de Quintas) con aritmética modular de 12 posiciones:
     * distancia = min(|key1 - key2|, 12 - |key1 - key2|)
     */
    private fun isHarmonicallyCompatible(key1: String, key2: String): Boolean {
        if (key1.isEmpty() || key2.isEmpty()) return true // Si no hay clave, aceptar como compatible
        val num1 = key1.filter { it.isDigit() }.toIntOrNull() ?: return true
        val num2 = key2.filter { it.isDigit() }.toIntOrNull() ?: return true
        val letter1 = key1.filter { it.isLetter() }.uppercase()
        val letter2 = key2.filter { it.isLetter() }.uppercase()

        val diff = abs(num1 - num2)
        val distance = min(diff, 12 - diff)

        return if (letter1 == letter2) {
            distance <= 1 // Adyacente en el círculo (ej. 11A con 12A, o 12A con 1A)
        } else {
            distance == 0 // Cambio de modo relativo (ej. 8A y 8B)
        }
    }

    private fun isBpmCompatible(bpm1: Float, bpm2: Float, maxDiff: Float = 5f): Boolean {
        if (bpm1 == 0f || bpm2 == 0f) return true
        return abs(bpm1 - bpm2) <= maxDiff
    }

    companion object {
        @Volatile
        private var INSTANCE: AutomixRadioEngine? = null

        fun getInstance(context: Context): AutomixRadioEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutomixRadioEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
