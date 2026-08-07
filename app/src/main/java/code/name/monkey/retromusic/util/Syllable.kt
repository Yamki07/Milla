/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.util

/**
 * Representa una sílaba (o palabra) sincronizada en el tiempo.
 */
data class Syllable(
    val text: String,
    val startMs: Long,
    val durationMs: Long
)

/**
 * Representa una línea de letra que puede contener sílabas sincronizadas.
 * Si [syllables] está vacío, la línea funciona como un LRC clásico.
 */
data class EnhancedLyricLine(
    val timeMs: Long,
    val text: String,
    val syllables: List<Syllable> = emptyList()
)
