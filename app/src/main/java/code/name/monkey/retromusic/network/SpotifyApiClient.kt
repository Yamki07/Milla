package code.name.monkey.retromusic.network

import android.util.Base64
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface SpotifyAuthService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): SpotifyTokenResponse
}

interface SpotifyApiService {
    @GET("v1/search")
    suspend fun search(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("limit") limit: Int = 20
    ): SpotifySearchResponse

    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Header("Authorization") authorization: String,
        @Query("seed_artists") seedArtists: String? = null,
        @Query("seed_tracks") seedTracks: String? = null,
        @Query("limit") limit: Int = 20
    ): SpotifyRecommendationsResponse

    @GET("v1/artists/{id}")
    suspend fun getArtist(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): SpotifyArtist
}

data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class SpotifySearchResponse(
    @SerializedName("tracks") val tracks: SpotifyTracksPaging?
)

data class SpotifyTracksPaging(
    @SerializedName("items") val items: List<SpotifyTrack>
)

data class SpotifyRecommendationsResponse(
    @SerializedName("tracks") val tracks: List<SpotifyTrack>
)

data class SpotifyTrack(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("artists") val artists: List<SpotifyArtist>,
    @SerializedName("album") val album: SpotifyAlbum
)

data class SpotifyArtist(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("images") val images: List<SpotifyImage>? = null
)

data class SpotifyAlbum(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("images") val images: List<SpotifyImage>
)

data class SpotifyImage(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)

object SpotifyClient {
    private const val CLIENT_ID = "57706a980f2043dc9e7e5c4c60e15924"
    private const val CLIENT_SECRET = "b1b46434500f4b93888df96784663679"

    private var accessToken: String? = null
    private var tokenExpirationTime: Long = 0

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val authRetrofit = Retrofit.Builder()
        .baseUrl("https://accounts.spotify.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiRetrofit = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: SpotifyAuthService = authRetrofit.create(SpotifyAuthService::class.java)
    val apiService: SpotifyApiService = apiRetrofit.create(SpotifyApiService::class.java)

    suspend fun getValidToken(): String {
        val currentTime = System.currentTimeMillis()
        if (accessToken == null || currentTime >= tokenExpirationTime) {
            val authString = "$CLIENT_ID:$CLIENT_SECRET"
            val base64Auth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
            
            try {
                val response = authService.getAccessToken("Basic $base64Auth")
                accessToken = response.accessToken
                // Subtract 60 seconds as buffer
                tokenExpirationTime = currentTime + (response.expiresIn * 1000L) - 60000L
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }
        return accessToken ?: ""
    }
}
