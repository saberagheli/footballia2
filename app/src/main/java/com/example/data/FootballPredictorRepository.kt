package com.myproject.footballpredictor.data.repository

import com.myproject.footballpredictor.data.local.AppDao
import com.myproject.footballpredictor.data.local.entity.MatchEntity
import com.myproject.footballpredictor.data.local.entity.PredictionEntity
import com.myproject.footballpredictor.data.local.entity.UserEntity
import com.myproject.footballpredictor.data.remote.ApiClient
import kotlinx.coroutines.flow.Flow

class FootballPredictorRepository(private val appDao: AppDao) {

    // --- User Operations ---
    suspend fun registerUser(user: UserEntity): Long {
        return appDao.insertUser(user)
    }

    suspend fun loginUser(username: String, passHash: String): UserEntity? {
        return appDao.getUserByUsernameAndHash(username, passHash)
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return appDao.getUserById(userId)
    }

    suspend fun updateUser(user: UserEntity) {
        appDao.updateUser(user)
    }

    fun getAllUsersSortedByPoints(): Flow<List<UserEntity>> {
        return appDao.getAllUsersSortedByPoints()
    }

    // --- Match Operations ---
    suspend fun insertMatches(matches: List<MatchEntity>) {
        appDao.insertMatches(matches)
    }

    fun getAllMatches(): Flow<List<MatchEntity>> {
        return appDao.getAllMatches()
    }

    fun getMatchesByWeek(weekNumber: Int): Flow<List<MatchEntity>> {
        return appDao.getMatchesByWeek(weekNumber)
    }

    suspend fun updateMatch(match: MatchEntity) {
        appDao.updateMatch(match)
    }

    // --- Prediction Operations ---
    suspend fun insertPrediction(prediction: PredictionEntity) {
        appDao.insertPrediction(prediction)
    }

    fun getUserPredictionsForWeek(userId: Long, weekNumber: Int): Flow<List<PredictionEntity>> {
        return appDao.getUserPredictionsForWeek(userId, weekNumber)
    }

    fun getAllPredictionsForMatch(matchId: Long): Flow<List<PredictionEntity>> {
        return appDao.getAllPredictionsForMatch(matchId)
    }

    suspend fun updatePrediction(prediction: PredictionEntity) {
        appDao.updatePrediction(prediction)
    }

    // --- Sync Operations ---
    suspend fun syncMatchesFromRemote(): Boolean {
        return try {
            val response = ApiClient.apiService.getMatches()
            if (response.isSuccessful && response.body() != null) {
                appDao.insertMatches(response.body()!!)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitPredictionsToRemote(userId: Long): Boolean {
        return try {
            val predictions = appDao.getPredictionsByUserIdDirect(userId)
            val response = ApiClient.apiService.postPredictions(predictions)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
