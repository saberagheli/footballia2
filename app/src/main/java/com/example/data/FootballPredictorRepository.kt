package com.example.data

import com.example.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FootballPredictorRepository(private val appDao: AppDao) {

    // Users
    val leaderboard: Flow<List<User>> = appDao.getLeaderboard()

    fun getUserByIdFlow(userId: Int): Flow<User?> {
        return appDao.getUserByIdFlow(userId)
    }

    suspend fun getUserByUsername(username: String): User? {
        return appDao.getUserByUsername(username)
    }

    suspend fun getUserById(userId: Int): User? {
        return appDao.getUserById(userId)
    }

    suspend fun registerUser(username: String, displayName: String, password: String = "123456", isAdmin: Boolean = false): User? {
        val cleanedUsername = username.trim()
        val cleanedPassword = password.trim()
        if (cleanedUsername.isEmpty() || displayName.trim().isEmpty() || cleanedPassword.isEmpty()) return null
        
        // Check if exists
        val existing = appDao.getUserByUsername(cleanedUsername)
        if (existing != null) return null

        val newUser = User(
            username = cleanedUsername,
            displayName = displayName.trim(),
            password = cleanedPassword,
            isAdmin = isAdmin,
            isActive = true
        )
        val id = appDao.insertUser(newUser)
        return newUser.copy(id = id.toInt())
    }

    suspend fun toggleUserActiveStatus(userId: Int, isActive: Boolean) {
        val user = appDao.getUserById(userId) ?: return
        val updated = user.copy(isActive = isActive)
        appDao.updateUser(updated)
    }

    suspend fun toggleUserAdminStatus(userId: Int, isAdmin: Boolean) {
        val user = appDao.getUserById(userId) ?: return
        val updated = user.copy(isAdmin = isAdmin)
        appDao.updateUser(updated)
    }

    suspend fun updateUser(user: User) {
        appDao.updateUser(user)
    }

    suspend fun deleteUser(userId: Int) {
        val user = appDao.getUserById(userId) ?: return
        appDao.deletePredictionsForUser(userId)
        appDao.deleteUserBonusPredictionsForUser(userId)
        appDao.deleteStageSubmissionsForUser(userId)
        appDao.deleteUser(user)
    }

    // Matches
    val allMatches: Flow<List<MatchEntity>> = appDao.getAllMatches()

    fun getMatchesByStage(stageName: String): Flow<List<MatchEntity>> {
        return appDao.getMatchesByStage(stageName)
    }

    suspend fun getMatchById(matchId: Int): MatchEntity? {
        return appDao.getMatchById(matchId)
    }

    suspend fun createMatch(
        homeTeam: String, 
        awayTeam: String, 
        matchTime: String, 
        stageName: String,
        pointsExactScore: Int = 5,
        pointsWinnerAndGd: Int = 3,
        pointsWinnerOnly: Int = 2,
        pointsWrong: Int = 0,
        isPublished: Boolean = false
    ): MatchEntity {
        val match = MatchEntity(
            homeTeam = homeTeam.trim(),
            awayTeam = awayTeam.trim(),
            matchTime = matchTime.trim(),
            stageName = stageName,
            pointsExactScore = pointsExactScore,
            pointsWinnerAndGd = pointsWinnerAndGd,
            pointsWinnerOnly = pointsWinnerOnly,
            pointsWrong = pointsWrong,
            isPublished = isPublished
        )
        val id = appDao.insertMatch(match)
        return match.copy(id = id.toInt())
    }

    suspend fun updateMatch(match: MatchEntity) {
        appDao.updateMatch(match)
    }

    suspend fun deleteMatch(match: MatchEntity) {
        appDao.deleteMatch(match)
    }

    // Announcements
    val allAnnouncements: Flow<List<Announcement>> = appDao.getAllAnnouncements()

    suspend fun postAnnouncement(title: String, message: String, targetUserIds: String? = null) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val announcement = Announcement(
            title = title,
            message = message,
            timestamp = sdf.format(Date()),
            targetUserIds = targetUserIds
        )
        appDao.insertAnnouncement(announcement)
    }

    suspend fun clearAnnouncements() {
        appDao.clearAnnouncements()
    }

    // Predictions
    val allPredictions: Flow<List<Prediction>> = appDao.getAllPredictionsFlow()
    val appSettings: Flow<AppSettings?> = appDao.getSettingsFlow()
    val allStageSubmissions: Flow<List<StageSubmission>> = appDao.getAllStageSubmissionsFlow()

    suspend fun getAppSettingsDirect(): AppSettings {
        var settings = appDao.getSettingsDirect()
        if (settings == null) {
            settings = AppSettings()
            appDao.insertSettings(settings)
        }
        return settings
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        appDao.insertSettings(settings)
        recalculateAndSaveAllUsersBonusPoints(settings)
    }

    suspend fun getStageSubmission(userId: Int, stageName: String): StageSubmission? {
        return appDao.getStageSubmission(userId, stageName)
    }

    suspend fun submitStagePredictions(userId: Int, stageName: String) {
        val submission = StageSubmission(userId = userId, stageName = stageName, isSubmitted = true)
        appDao.insertStageSubmission(submission)
    }

    suspend fun submitChampionPrediction(userId: Int, firstChoice: String, secondChoice: String) {
        val user = appDao.getUserById(userId) ?: return
        val updatedUser = user.copy(
            championFirstChoice = firstChoice,
            championSecondChoice = secondChoice,
            championSubmitted = true
        )
        appDao.updateUser(updatedUser)
        
        val settings = getAppSettingsDirect()
        recalculateAndSaveAllUsersBonusPoints(settings)
    }

    suspend fun submitTopScorerPrediction(userId: Int, playerChoice: String) {
        val user = appDao.getUserById(userId) ?: return
        val updatedUser = user.copy(
            topScorerChoice = playerChoice,
            topScorerSubmitted = true
        )
        appDao.updateUser(updatedUser)
        
        val settings = getAppSettingsDirect()
        recalculateAndSaveAllUsersBonusPoints(settings)
    }

    suspend fun adminUpdatePenaltyPoints(userId: Int, penaltyPoints: Int) {
        val user = appDao.getUserById(userId) ?: return
        val oldPenalty = user.penaltyPoints
        val diff = penaltyPoints - oldPenalty
        val newTotal = (user.totalPoints - diff).coerceAtLeast(0)
        val updatedUser = user.copy(
            penaltyPoints = penaltyPoints,
            totalPoints = newTotal
        )
        appDao.updateUser(updatedUser)
    }

    suspend fun recalculateAndSaveAllUsersBonusPoints(settings: AppSettings) {
        val users = appDao.getLeaderboard().first()
        for (user in users) {
            var champPts = 0
            if (user.championSubmitted) {
                if (settings.actualChampion != null) {
                    champPts = when {
                        user.championFirstChoice?.trim()?.equals(settings.actualChampion.trim(), ignoreCase = true) == true -> {
                            settings.championFirstPoints
                        }
                        user.championSecondChoice?.trim()?.equals(settings.actualChampion.trim(), ignoreCase = true) == true -> {
                            settings.championSecondPoints
                        }
                        else -> {
                            settings.championWrongPoints
                        }
                    }
                }
            }

            var topScorerPts = 0
            if (user.topScorerSubmitted) {
                if (settings.actualTopScorer != null && user.topScorerChoice != null) {
                    if (user.topScorerChoice.trim().equals(settings.actualTopScorer.trim(), ignoreCase = true)) {
                        topScorerPts = settings.topScorerPoints
                    }
                }
            }

            val oldBonus = user.championPointsEarned + user.topScorerPointsEarned
            val newBonus = champPts + topScorerPts
            val bonusDiff = newBonus - oldBonus

            if (bonusDiff != 0 || user.championPointsEarned != champPts || user.topScorerPointsEarned != topScorerPts) {
                val updatedUser = user.copy(
                    championPointsEarned = champPts,
                    topScorerPointsEarned = topScorerPts,
                    totalPoints = (user.totalPoints + bonusDiff).coerceAtLeast(0)
                )
                appDao.updateUser(updatedUser)
            }
        }
    }

    fun getPredictionsForUser(userId: Int): Flow<List<UserPredictionWithMatch>> {
        return appDao.getPredictionsForUser(userId)
    }

    suspend fun getPredictionsForMatch(matchId: Int): List<MatchPredictionWithUser> {
        return appDao.getPredictionsForMatch(matchId)
    }

    suspend fun getPredictionByUserAndMatch(userId: Int, matchId: Int): Prediction? {
        return appDao.getPredictionByUserAndMatch(userId, matchId)
    }

    suspend fun submitPrediction(userId: Int, matchId: Int, homeScore: Int, awayScore: Int): Prediction {
        val existing = appDao.getPredictionByUserAndMatch(userId, matchId)
        val prediction = if (existing != null) {
            existing.copy(predictedHomeScore = homeScore, predictedAwayScore = awayScore)
        } else {
            Prediction(userId = userId, matchId = matchId, predictedHomeScore = homeScore, predictedAwayScore = awayScore)
        }
        
        val id = if (prediction.id == 0) {
            appDao.insertPrediction(prediction).toInt()
        } else {
            appDao.updatePrediction(prediction)
            prediction.id
        }
        return prediction.copy(id = id)
    }

    // Scoring & Point Award Logic based on the 4 Custom Scores of the Match
    suspend fun saveMatchResultsAndCustomPoints(
        matchId: Int,
        actualHomeScore: Int,
        actualAwayScore: Int,
        customPointsMap: Map<Int, Int> // Prediction ID -> Manual Override Points
    ) {
        val match = appDao.getMatchById(matchId) ?: return
        val updatedMatch = match.copy(
            homeScore = actualHomeScore,
            awayScore = actualAwayScore,
            isFinished = true
        )
        appDao.updateMatch(updatedMatch)

        val predictionsWithUser = appDao.getPredictionsForMatch(matchId)
        for (predUser in predictionsWithUser) {
            val pred = predUser.prediction
            val user = predUser.user
            
            // Get custom points entered by admin, or calculate automatically based on the 4 rules
            val finalPoints = customPointsMap[pred.id] ?: calculatePoints(
                actualHome = actualHomeScore,
                actualAway = actualAwayScore,
                predHome = pred.predictedHomeScore,
                predAway = pred.predictedAwayScore,
                pointsExact = match.pointsExactScore,
                pointsWinnerAndGd = match.pointsWinnerAndGd,
                pointsWinnerOnly = match.pointsWinnerOnly,
                pointsWrong = match.pointsWrong
            )

            val oldPoints = if (pred.isScored) pred.pointsEarned ?: 0 else 0
            val pointDiff = finalPoints - oldPoints

            // Update user's aggregate points
            val updatedUser = user.copy(totalPoints = (user.totalPoints + pointDiff).coerceAtLeast(0))
            appDao.updateUser(updatedUser)

            // Update prediction
            val updatedPred = pred.copy(
                pointsEarned = finalPoints,
                isScored = true
            )
            appDao.updatePrediction(updatedPred)
        }
    }

    fun calculatePoints(
        actualHome: Int, actualAway: Int,
        predHome: Int, predAway: Int,
        pointsExact: Int, pointsWinnerAndGd: Int,
        pointsWinnerOnly: Int, pointsWrong: Int
    ): Int {
        // 1. Exact score match
        if (actualHome == predHome && actualAway == predAway) {
            return pointsExact
        }

        val actualOutcome = actualHome.compareTo(actualAway)
        val predOutcome = predHome.compareTo(predAway)

        // 2. Correct outcome winner (or draw)
        if (actualOutcome == predOutcome) {
            val actualGd = Math.abs(actualHome - actualAway)
            val predGd = Math.abs(predHome - predAway)
            return if (actualGd == predGd) {
                pointsWinnerAndGd // Winner and same goal difference
            } else {
                pointsWinnerOnly // Winner only
            }
        }

        // 3. Wrong prediction
        return pointsWrong
    }

    // Seed Demo Data
    suspend fun seedDemoData() {
        appDao.clearUsers()
        appDao.clearMatches()
        appDao.clearPredictions()
        appDao.clearAnnouncements()

        // Create Super Admin SCHOLES account
        val scholes = User(username = "SCHOLES", displayName = "SCHOLES", password = "11971197", isAdmin = true, totalPoints = 0, isActive = true)
        appDao.insertUser(scholes)
    }

    // Bonus Prediction Items & Eliminated Items
    val allBonusItems: Flow<List<BonusPredictionItem>> = appDao.getAllBonusItemsFlow()
    val allUserBonusPredictions: Flow<List<UserBonusPrediction>> = appDao.getAllUserBonusPredictionsFlow()
    val allEliminatedItems: Flow<List<EliminatedItem>> = appDao.getAllEliminatedItemsFlow()

    suspend fun addBonusItem(title: String, points: Int): BonusPredictionItem? {
        if (title.isBlank() || points <= 0) return null
        val item = BonusPredictionItem(title = title.trim(), points = points)
        val id = appDao.insertBonusItem(item)
        return item.copy(id = id.toInt())
    }

    suspend fun updateBonusItem(item: BonusPredictionItem) {
        appDao.updateBonusItem(item)
    }

    suspend fun deleteBonusItem(item: BonusPredictionItem) {
        appDao.deleteUserBonusPredictionsForItem(item.id)
        appDao.deleteBonusItem(item)
    }

    suspend fun submitUserBonusPredictions(userId: Int, predictions: Map<Int, String>) {
        predictions.forEach { (bonusItemId, text) ->
            if (text.isNotBlank()) {
                val existing = appDao.getUserBonusPrediction(userId, bonusItemId)
                val entry = UserBonusPrediction(
                    userId = userId,
                    bonusItemId = bonusItemId,
                    predictionText = text.trim(),
                    isSubmitted = true,
                    pointsEarned = existing?.pointsEarned ?: 0
                )
                appDao.insertUserBonusPrediction(entry)
            }
        }
    }

    suspend fun evaluateBonusItemWinner(bonusItemId: Int, actualWinner: String): String {
        val bonusItem = appDao.getBonusItemById(bonusItemId) ?: return "امتیاز تشویقی پیدا نشد."
        val cleanedWinner = actualWinner.trim()
        if (cleanedWinner.isBlank()) return "لطفاً نام برنده واقعی را وارد کنید."

        appDao.updateBonusItem(bonusItem.copy(actualWinner = cleanedWinner, isEvaluated = true))

        val allUserPreds = appDao.getAllUserBonusPredictions().filter { it.bonusItemId == bonusItemId }
        var winnersCount = 0

        for (userPred in allUserPreds) {
            val isCorrect = userPred.predictionText.trim().equals(cleanedWinner, ignoreCase = true)
            val newPointsEarned = if (isCorrect) bonusItem.points else 0
            val pointDiff = newPointsEarned - userPred.pointsEarned

            if (pointDiff != 0) {
                val user = appDao.getUserById(userPred.userId)
                if (user != null) {
                    val updatedTotalPoints = (user.totalPoints + pointDiff).coerceAtLeast(0)
                    appDao.updateUser(user.copy(totalPoints = updatedTotalPoints))
                }
            }

            appDao.insertUserBonusPrediction(
                userPred.copy(pointsEarned = newPointsEarned)
            )
            if (isCorrect) winnersCount++
        }

        return "برنده واقعی ($cleanedWinner) ثبت شد. به $winnersCount کاربر امتیاز اعطا گردید."
    }

    suspend fun addEliminatedItem(name: String): EliminatedItem? {
        val cleaned = name.trim()
        if (cleaned.isBlank()) return null
        val item = EliminatedItem(name = cleaned)
        val id = appDao.insertEliminatedItem(item)
        return item.copy(id = id.toInt())
    }

    suspend fun deleteEliminatedItem(item: EliminatedItem) {
        appDao.deleteEliminatedItem(item)
    }

    suspend fun updateUserBonusPredictionText(userId: Int, bonusItemId: Int, newText: String) {
        val cleaned = newText.trim()
        val existing = appDao.getUserBonusPrediction(userId, bonusItemId)
        if (existing != null) {
            val updated = existing.copy(predictionText = cleaned)
            appDao.insertUserBonusPrediction(updated)
        } else if (cleaned.isNotEmpty()) {
            val created = UserBonusPrediction(
                userId = userId,
                bonusItemId = bonusItemId,
                predictionText = cleaned,
                isSubmitted = true
            )
            appDao.insertUserBonusPrediction(created)
        }

        // If bonus item was already evaluated, re-evaluate to update point awards accordingly
        val item = appDao.getBonusItemById(bonusItemId)
        if (item != null && item.isEvaluated && !item.actualWinner.isNullOrBlank()) {
            evaluateBonusItemWinner(bonusItemId, item.actualWinner)
        }
    }

    suspend fun toggleBonusItemPublished(bonusItemId: Int, isPublished: Boolean) {
        val item = appDao.getBonusItemById(bonusItemId) ?: return
        appDao.updateBonusItem(item.copy(isPublished = isPublished))
    }

    suspend fun updateBannerImageUrl(bannerUrl: String?) {
        val settings = getAppSettingsDirect()
        appDao.insertSettings(settings.copy(bannerImageUrl = bannerUrl?.trim()))
    }

    suspend fun resetTournamentSeasonKeepUsers() {
        appDao.clearPredictions()
        appDao.clearStageSubmissions()
        appDao.deleteAllUserBonusPredictions()
        appDao.deleteAllBonusItems()
        appDao.deleteAllEliminatedItems()
        appDao.clearAnnouncements()
        appDao.clearMatches()

        val users = appDao.getAllUsersDirect()
        for (u in users) {
            appDao.updateUser(
                u.copy(
                    totalPoints = 0,
                    penaltyPoints = 0,
                    championFirstChoice = null,
                    championSecondChoice = null,
                    championSubmitted = false,
                    topScorerChoice = null,
                    topScorerSubmitted = false,
                    championPointsEarned = 0,
                    topScorerPointsEarned = 0
                )
            )
        }

        appDao.insertSettings(AppSettings())
    }

    // -------------------------------------------------------------
    // Remote Server Synchronization (https://footballfun.ir)
    // -------------------------------------------------------------

    suspend fun syncMatchesFromRemote(stage: String? = null): Boolean {
        return try {
            val response = ApiClient.apiService.getMatches(stage)
            if (response.isSuccessful && response.body() != null) {
                val remoteMatches = response.body()!!
                for (dto in remoteMatches) {
                    val existing = appDao.getMatchById(dto.id)
                    val entity = MatchEntity(
                        id = dto.id,
                        homeTeam = dto.homeTeam,
                        awayTeam = dto.awayTeam,
                        matchTime = dto.matchTime,
                        stageName = dto.stageName,
                        homeScore = dto.homeScore,
                        awayScore = dto.awayScore,
                        isFinished = dto.isFinished,
                        pointsExactScore = dto.pointsExactScore,
                        pointsWinnerAndGd = dto.pointsWinnerAndGd,
                        pointsWinnerOnly = dto.pointsWinnerOnly,
                        pointsWrong = dto.pointsWrong,
                        isPublished = true
                    )
                    if (existing != null) {
                        appDao.updateMatch(entity)
                    } else {
                        appDao.insertMatch(entity)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun submitPredictionsToRemote(userId: Int, items: List<PredictionSubmissionItem>): Boolean {
        return try {
            val request = SubmitPredictionsRequest(userId = userId, predictions = items)
            val response = ApiClient.apiService.submitPredictions(request)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginRemote(username: String, password: String): LoginResponse? {
        return try {
            val response = ApiClient.apiService.login(LoginRequest(username = username, password = password))
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun registerRemote(username: String, displayName: String, password: String): LoginResponse? {
        return try {
            val response = ApiClient.apiService.register(RegisterRequest(username = username, displayName = displayName, password = password))
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
