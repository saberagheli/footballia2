package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// -------------------------------------------------------------
// Data Transfer Objects (DTOs) for https://footballfun.ir
// -------------------------------------------------------------

data class RemoteMatchDto(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val matchTime: String,
    val stageName: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val isFinished: Boolean = false,
    val pointsExactScore: Int = 5,
    val pointsWinnerAndGd: Int = 3,
    val pointsWinnerOnly: Int = 2,
    val pointsWrong: Int = 0
)

data class RemotePredictionDto(
    val matchId: Int,
    val userId: Int,
    val homeScore: Int,
    val awayScore: Int
)

data class PredictionSubmissionItem(
    val matchId: Int,
    val homeScore: Int,
    val awayScore: Int
)

data class SubmitPredictionsRequest(
    val userId: Int,
    val predictions: List<PredictionSubmissionItem>
)

data class GenericApiResponse(
    val success: Boolean,
    val message: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val userId: Int? = null,
    val username: String? = null,
    val displayName: String? = null,
    val isAdmin: Boolean = false,
    val message: String? = null
)

data class RegisterRequest(
    val username: String,
    val displayName: String,
    val password: String
)

data class LeaderboardEntryDto(
    val rank: Int,
    val userId: Int,
    val username: String,
    val displayName: String,
    val totalPoints: Int,
    val exactMatchesCount: Int
)

data class BonusSubmissionRequest(
    val userId: Int,
    val bonusItemId: Int,
    val predictionText: String
)

// -------------------------------------------------------------
// Retrofit Interface for https://footballfun.ir
// -------------------------------------------------------------

interface FootballApiService {

    @GET("api/matches")
    suspend fun getMatches(@Query("stage") stage: String? = null): Response<List<RemoteMatchDto>>

    @POST("api/predictions/submit")
    suspend fun submitPredictions(@Body request: SubmitPredictionsRequest): Response<GenericApiResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("api/leaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntryDto>>

    @POST("api/bonus/submit")
    suspend fun submitBonus(@Body request: BonusSubmissionRequest): Response<GenericApiResponse>
}

// -------------------------------------------------------------
// ApiClient Singleton
// -------------------------------------------------------------

object ApiClient {
    const val BASE_URL = "https://footballfun.ir/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: FootballApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FootballApiService::class.java)
    }
}
