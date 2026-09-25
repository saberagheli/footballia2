package com.example.footballia2.data

import com.example.footballia2.data.local.AppDao
import com.example.footballia2.data.local.AppSettings
import com.example.footballia2.data.local.Announcement
import com.example.footballia2.data.local.BonusItem
import com.example.footballia2.data.local.BonusPrediction
import com.example.footballia2.data.local.EliminatedItem
import com.example.footballia2.data.local.Match
import com.example.footballia2.data.local.Prediction
import com.example.footballia2.data.local.User
import com.example.footballia2.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class FootballPredictorRepository(private val appDao: AppDao) {

    // --- USERS MANAGEMENT ---

    fun getLeaderboard(): Flow<List<User>> = appDao.getLeaderboard()

    fun getAllUsersList(): Flow<List<User>> = appDao.getAllUsers()

    fun getUserById(userId: Long): Flow<User?> = appDao.getUserById(userId)

    suspend fun getUserByUsername(username: String): User? {
        return appDao.getUserByUsername(username)
    }

    suspend fun registerUser(username: String, isAdmin: Boolean = false): Boolean {
        val existing = appDao.getUserByUsername(username)
        if (existing != null) return false
        val user = User(username = username, isAdmin = isAdmin, totalPoints = 0)
        appDao.insertUser(user)
        return true
    }

    suspend fun updateUser(user: User) {
        appDao.updateUser(user)
    }

    suspend fun deleteUser(userId: Long) {
        appDao.deleteUserById(userId)
        appDao.deletePredictionsByUserId(userId)
        appDao.deleteBonusPredictionsByUserId(userId)
    }

    suspend fun toggleUserActiveStatus(userId: Long, isActive: Boolean) {
        appDao.updateUserActiveStatus(userId, isActive)
    }

    suspend fun toggleUserAdminStatus(userId: Long, isAdmin: Boolean) {
        appDao.updateUserAdminStatus(userId, isAdmin)
    }

    suspend fun updateUserCustomPoints(userId: Long, newBonusPoints: Int) {
        val user = appDao.getUserByIdDirect(userId) ?: return
        val updatedUser = user.copy(customBonusPoints = newBonusPoints)
        appDao.updateUser(updatedUser)
        recalculateSingleUserTotalPoints(userId)
    }

    // --- MATCHES MANAGEMENT ---

    fun getMatchesByStage(stage: String): Flow<List<Match>> = appDao.getMatchesByStage(stage)

    fun getAllMatchesList(): Flow<List<Match>> = appDao.getAllMatches()

    suspend fun getMatchById(matchId: Long): Match? {
        return appDao.getMatchById(matchId)
    }

    suspend fun createMatch(match: Match) {
        appDao.insertMatch(match)
    }

    suspend fun updateMatch(match: Match) {
        appDao.updateMatch(match)
    }

    suspend fun deleteMatch(matchId: Long) {
        appDao.deleteMatchById(matchId)
        appDao.deletePredictionsByMatchId(matchId)
    }

    // --- ANNOUNCEMENTS ---

    fun getAllAnnouncements(): Flow<List<Announcement>> = appDao.getAllAnnouncements()

    suspend fun postAnnouncement(title: String, message: String) {
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val announcement = Announcement(
            title = title,
            message = message,
            createdAt = dateStr
        )
        appDao.insertAnnouncement(announcement)
    }

    suspend fun clearAnnouncements() {
        appDao.clearAnnouncements()
    }

    // --- SETTINGS & GLOBAL PREDICTIONS ---

    fun getAppSettings(): Flow<AppSettings?> = appDao.getAppSettings()

    suspend fun getAppSettingsDirect(): AppSettings? = appDao.getAppSettingsDirect()

    suspend fun saveAppSettings(settings: AppSettings) {
        appDao.insertAppSettings(settings)
        recalculateAndSaveAllUsersBonusPoints()
    }

    suspend fun submitChampionPrediction(userId: Long, teamName: String) {
        val user = appDao.getUserByIdDirect(userId) ?: return
        appDao.updateUser(user.copy(predictedChampion = teamName))
    }

    suspend fun submitTopScorerPrediction(userId: Long, playerAndTeam: String) {
        val user = appDao.getUserByIdDirect(userId) ?: return
        appDao.updateUser(user.copy(predictedTopScorer = playerAndTeam))
    }

    suspend fun recalculateAndSaveAllUsersBonusPoints() {
        val settings = appDao.getAppSettingsDirect() ?: return
        val users = appDao.getAllUsersDirect()

        for (u in users) {
            var bonus = 0
            if (!settings.actualChampion.isNull_or_Empty() && u.predictedChampion.equals(settings.actualChampion, ignoreCase = true)) {
                bonus += 10
            }
            if (!settings.actualTopScorer.isNull_or_Empty() && u.predictedTopScorer.equals(settings.actualTopScorer, ignoreCase = true)) {
                bonus += 10
            }

            val updatedUser = u.copy(bonusPoints = bonus)
            appDao.updateUser(updatedUser)
            recalculateSingleUserTotalPoints(u.id)
        }
    }

    private fun String?.isNull_or_Empty(): Boolean = this == null || this.trim().isEmpty()

    // --- PREDICTIONS & SCORING ENGINE ---

    fun getPredictionsByUserId(userId: Long): Flow<List<Prediction>> = appDao.getPredictionsByUserId(userId)

    fun getAllPredictionsList(): Flow<List<Prediction>> = appDao.getAllPredictions()

    suspend fun saveMatchResultsAndCustomPoints(
        matchId: Long,
        homeScore: Int,
        awayScore: Int,
        overrideMap: Map<Long, Int>? = null
    ) {
        val match = appDao.getMatchById(matchId) ?: return
        val updatedMatch = match.copy(
            homeScore = homeScore,
            awayScore = awayScore,
            isFinished = true
        )
        appDao.updateMatch(updatedMatch)

        val predictions = appDao.getPredictionsForMatchDirect(matchId)
        for (pred in predictions) {
            val calcScore = calculatePoints(
                actualHome = homeScore,
                actualAway = awayScore,
                predictedHome = pred.predictedHomeScore,
                predictedAway = pred.predictedAwayScore,
                pointsExact = match.pointsExact,
                pointsWinnerAndGd = match.pointsWinnerAndGd,
                pointsWinnerOnly = match.pointsWinnerOnly,
                pointsWrong = match.pointsWrong
            )

            val finalAwardedPoints = overrideMap?.get(pred.userId) ?: calcScore

            val updatedPred = pred.copy(
                pointsEarned = finalAwardedPoints,
                isCalculated = true
            )
            appDao.insertPrediction(updatedPred)
        }

        recalculateAllUsersTotalPoints()
    }

    suspend fun submitPrediction(prediction: Prediction) {
        val match = appDao.getMatchById(prediction.matchId)
        if (match != null && match.isFinished) {
            return
        }
        appDao.insertPrediction(prediction)
    }

    private fun calculatePoints(
        actualHome: Int, actualAway: Int,
        predictedHome: Int, predictedAway: Int,
        pointsExact: Int, pointsWinnerAndGd: Int, pointsWinnerOnly: Int, pointsWrong: Int
    ): Int {
        if (actualHome == predictedHome && actualAway == predictedAway) {
            return pointsExact
        }

        val actualWinner = when {
            actualHome > actualAway -> 1
            actualAway > actualHome -> 2
            else -> 0
        }

        val predictedWinner = when {
            predictedHome > predictedAway -> 1
            predictedAway > predictedHome -> 2
            else -> 0
        }

        if (actualWinner == predictedWinner) {
            val actualGd = actualHome - actualAway
            val predictedGd = predictedHome - predictedAway
            if (actualGd == predictedGd) {
                return pointsWinnerAndGd
            }
            return pointsWinnerOnly
        }

        return pointsWrong
    }

    suspend fun recalculateAllUsersTotalPoints() {
        val users = appDao.getAllUsersDirect()
        for (user in users) {
            recalculateSingleUserTotalPoints(user.id)
        }
    }

    private suspend fun recalculateSingleUserTotalPoints(userId: Long) {
        val user = appDao.getUserByIdDirect(userId) ?: return
        val matchPointsSum = appDao.getUserTotalMatchPointsDirect(userId) ?: 0
        val bonusPredictionsSum = appDao.getUserTotalBonusPredictionsPointsDirect(userId) ?: 0

        val newTotal = matchPointsSum + user.bonusPoints + user.customBonusPoints + bonusPredictionsSum
        appDao.updateUser(user.copy(totalPoints = newTotal))
    }

    // --- BONUS ITEMS & ELIMINATED TEAMS ---

    fun getAllBonusItems(): Flow<List<BonusItem>> = appDao.getAllBonusItems()

    suspend fun createBonusItem(item: BonusItem) {
        appDao.insertBonusItem(item)
    }

    suspend fun updateBonusItem(item: BonusItem) {
        appDao.updateBonusItem(item)
    }

    suspend fun deleteBonusItem(itemId: Long) {
        appDao.deleteBonusItemById(itemId)
        appDao.deleteBonusPredictionsByItemId(itemId)
    }

    fun getBonusPredictionsByUser(userId: Long): Flow<List<BonusPrediction>> = appDao.getBonusPredictionsByUser(userId)

    suspend fun submitBonusPrediction(userId: Long, itemId: Long, predictedAnswer: String) {
        val bp = BonusPrediction(
            userId = userId,
            bonusItemId = itemId,
            predictedAnswer = predictedAnswer
        )
        appDao.insertBonusPrediction(bp)
    }

    suspend fun evaluateBonusItemWinner(itemId: Long, correctAnswer: String) {
        val item = appDao.getBonusItemById(itemId) ?: return
        val updatedItem = item.copy(correctAnswer = correctAnswer, isEvaluated = true)
        appDao.updateBonusItem(updatedItem)

        val predictions = appDao.getBonusPredictionsByItemIdDirect(itemId)
        for (p in predictions) {
            val isCorrect = p.predictedAnswer.equals(correctAnswer, ignoreCase = true)
            val points = if (isCorrect) item.pointsAwarded else 0
            val updatedP = p.copy(pointsEarned = points, isEvaluated = true)
            appDao.insertBonusPrediction(updatedP)
        }

        recalculateAllUsersTotalPoints()
    }

    fun getAllEliminatedItems(): Flow<List<EliminatedItem>> = appDao.getAllEliminatedItems()

    suspend fun addEliminatedItem(name: String, type: String) {
        val item = EliminatedItem(name = name, type = type)
        appDao.insertEliminatedItem(item)
    }

    suspend fun deleteEliminatedItem(itemId: Long) {
        appDao.deleteEliminatedItemById(itemId)
    }

    // --- RESET & MAINTENANCE ---

    suspend fun seedDemoData() {
        val users = appDao.getAllUsersDirect()
        if (users.isEmpty()) {
            val adminUser = User(
                username = "SCHOLES",
                isAdmin = true,
                totalPoints = 0
            )
            appDao.insertUser(adminUser)
        }
    }

    suspend fun resetTournamentSeasonKeepUsers() {
        appDao.clearAllMatches()
        appDao.clearAllPredictions()
        appDao.clearAllBonusItems()
        appDao.clearAllBonusPredictions()
        appDao.clearAllEliminatedItems()
        appDao.clearAnnouncements()

        val users = appDao.getAllUsersDirect()
        for (u in users) {
            val resetUser = u.copy(
                totalPoints = 0,
                bonusPoints = 0,
                customBonusPoints = 0,
                predictedChampion = null,
                predictedTopScorer = null
            )
            appDao.updateUser(resetUser)
        }
    }

    // --- REMOTE SYNCHRONIZATION (FIXED FOR VIEWMODEL) ---

    suspend fun syncMatchesFromRemote(): Boolean {
        return try {
            val response = ApiClient.apiService.getMatches()
            if (response.isSuccessful && response.body() != null) {
                val remoteMatches = response.body()!!
                appDao.insertMatches(remoteMatches)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun submitPredictionsToRemote(userId: Long): Boolean {
        return try {
            val predictions = appDao.getPredictionsByUserIdDirect(userId)
            val response = ApiClient.apiService.postPredictions(predictions)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginRemote(username: String, pass: String): Boolean {
        return try {
            val response = ApiClient.apiService.login(username, pass)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun registerRemote(username: String, pass: String): Boolean {
        return try {
            val response = ApiClient.apiService.register(username, pass)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
