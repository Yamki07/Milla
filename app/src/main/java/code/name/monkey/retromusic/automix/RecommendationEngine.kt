package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.network.SpotifyClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.automix.TidalHifiApiClient

/**
 * Automix Recommendation Engine
 * Bridges Spotify's powerful recommendation API with ReFreezer's (Deezer) audio streams.
 */
object RecommendationEngine {
    private const val TAG = "RecommendationEngine"

    /**
     * Gets similar tracks (Automix/Infinite Radio) for a given track name/artist.
     * Uses Spotify to find recommendations, then matches them to Tidal tracks.
     */
    suspend fun getSimilarTracksForAutomix(seedTrackName: String, seedArtistName: String, limit: Int = 10): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get Spotify Token
                val token = SpotifyClient.getValidToken()
                if (token.isEmpty()) {
                    Log.e(TAG, "Failed to get Spotify Token")
                    return@withContext emptyList()
                }

                // 2. Search Spotify for the seed track to get its Spotify ID
                val searchResponse = SpotifyClient.apiService.search(
                    authorization = "Bearer $token",
                    query = "track:$seedTrackName artist:$seedArtistName",
                    type = "track",
                    limit = 1
                )

                val spotifySeedId = searchResponse.tracks?.items?.firstOrNull()?.id
                if (spotifySeedId == null) {
                    Log.e(TAG, "Could not find seed track on Spotify: $seedTrackName")
                    return@withContext emptyList()
                }

                // 3. Get Recommendations from Spotify API
                val recommendationsResponse = SpotifyClient.apiService.getRecommendations(
                    authorization = "Bearer $token",
                    seedTracks = spotifySeedId,
                    limit = limit * 2 // Fetch extra in case some don't match on Deezer
                )

                val recommendedSpotifyTracks = recommendationsResponse.tracks
                if (recommendedSpotifyTracks.isEmpty()) {
                    return@withContext emptyList()
                }

                // 4. Resolve Spotify tracks to Tidal tracks for streaming/download
                val matchedTidalTracks = mutableListOf<Song>()
                for (spotifyTrack in recommendedSpotifyTracks) {
                    if (matchedTidalTracks.size >= limit) break

                    val query = "${spotifyTrack.name} ${spotifyTrack.artists.firstOrNull()?.name ?: ""}"
                    try {
                        val tidalResults = TidalHifiApiClient.searchTracks(query)
                        val bestMatch = tidalResults.firstOrNull {
                            it.title.contains(spotifyTrack.name, ignoreCase = true) ||
                            spotifyTrack.name.contains(it.title, ignoreCase = true)
                        } ?: tidalResults.firstOrNull()

                        if (bestMatch != null) {
                            matchedTidalTracks.add(bestMatch)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resolve track on Tidal: $query", e)
                    }
                }

                matchedTidalTracks
            } catch (e: Exception) {
                Log.e(TAG, "Error generating Automix recommendations", e)
                emptyList()
            }
        }
    }
}
