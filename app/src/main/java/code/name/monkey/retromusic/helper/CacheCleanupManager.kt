package code.name.monkey.retromusic.helper

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Evicción LRU limitada a [Context.cacheDir]. No recibe ni resuelve rutas de MediaStore,
 * descargas de TIDAL ni almacenamiento externo; por contrato no puede borrar biblioteca.
 */
object CacheCleanupManager {
    private const val TAG = "CacheCleanupManager"
    private const val MAX_CACHE_BYTES = 500L * 1024L * 1024L
    private const val TARGET_CACHE_BYTES = 400L * 1024L * 1024L

    fun cleanupOnStartup(context: Context) {
        Thread({ cleanupIfNeeded(context.applicationContext) }, "milla-cache-lru").start()
    }

    fun cleanupIfNeeded(context: Context): Long {
        val root = runCatching { context.cacheDir.canonicalFile }.getOrNull() ?: return 0L
        if (!root.isDirectory) return 0L
        val files = root.walkTopDown()
            .filter { it.isFile && isInsideCacheRoot(root, it) }
            .toList()
        var size = files.sumOf { it.length() }
        if (size <= MAX_CACHE_BYTES) return 0L

        var deleted = 0L
        for (file in files.sortedBy { it.lastModified() }) {
            if (size <= TARGET_CACHE_BYTES) break
            val fileSize = file.length()
            if (file.delete()) {
                size -= fileSize
                deleted += fileSize
            }
        }
        if (deleted > 0L) Log.i(TAG, "Limpieza LRU: $deleted bytes eliminados solo de ${root.path}")
        return deleted
    }

    private fun isInsideCacheRoot(root: File, candidate: File): Boolean {
        val rootPath = root.path.trimEnd(File.separatorChar) + File.separator
        val path = runCatching { candidate.canonicalFile.path }.getOrNull() ?: return false
        return path.startsWith(rootPath)
    }
}
