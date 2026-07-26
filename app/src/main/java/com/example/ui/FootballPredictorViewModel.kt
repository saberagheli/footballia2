package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LeaderboardItem(
    val user: User,
    val lastMatchPoints: Int?,
    val rankChange: Int
)

class FootballPredictorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DatabaseProvider.getRepository(application)

    // Flow UI States
    val leaderboard: StateFlow<List<User>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicLeaderboard: StateFlow<List<LeaderboardItem>> = combine(
        repository.leaderboard,
        repository.allMatches,
        repository.allPredictions
    ) { users, matches, predictions ->
        val lastFinishedMatch = matches.filter { it.isFinished }.maxByOrNull { it.id }
        if (lastFinishedMatch == null) {
            val sortedUsers = users.sortedWith(
                compareByDescending<User> { it.totalPoints }
                    .thenBy { it.displayName }
            )
            sortedUsers.map { user ->
                LeaderboardItem(
                    user = user,
                    lastMatchPoints = null,
                    rankChange = 0
                )
            }
        } else {
            val lastMatchPredictions = predictions.filter { it.matchId == lastFinishedMatch.id }
            val predictionsByUser = lastMatchPredictions.associateBy { it.userId }

            val usersWithPreviousPoints = users.map { user ->
                val pred = predictionsByUser[user.id]
                val lastPoints = if (pred?.isScored == true) pred.pointsEarned ?: 0 else 0
                val prevPoints = (user.totalPoints - lastPoints).coerceAtLeast(0)
                Triple(user, lastPoints, prevPoints)
            }

            val previousSortedList = usersWithPreviousPoints.sortedWith(
                compareByDescending<Triple<User, Int, Int>> { it.third }
                    .thenBy { it.first.displayName }
            )

            val previousRankMap = previousSortedList.mapIndexed { index, triple ->
                triple.first.id to (index + 1)
            }.toMap()

            val currentSortedList = usersWithPreviousPoints.sortedWith(
                compareByDescending<Triple<User, Int, Int>> { it.first.totalPoints }
                    .thenBy { it.first.displayName }
            )

            currentSortedList.mapIndexed { currentIndex, (user, lastPoints, _) ->
                val currentRank = currentIndex + 1
                val previousRank = previousRankMap[user.id] ?: currentRank
                val rankChange = previousRank - currentRank

                LeaderboardItem(
                    user = user,
                    lastMatchPoints = if (predictionsByUser.containsKey(user.id)) lastPoints else null,
                    rankChange = rankChange
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMatches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBonusItems: StateFlow<List<BonusPredictionItem>> = repository.allBonusItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserBonusPredictions: StateFlow<List<UserBonusPrediction>> = repository.allUserBonusPredictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEliminatedItems: StateFlow<List<EliminatedItem>> = repository.allEliminatedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allStageSubmissions: StateFlow<List<StageSubmission>> = repository.allStageSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPredictions: StateFlow<List<Prediction>> = repository.allPredictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUserId = MutableStateFlow<Int?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getUserByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Predictions of the logged-in user
    private val _userPredictions = MutableStateFlow<List<UserPredictionWithMatch>>(emptyList())
    val userPredictions: StateFlow<List<UserPredictionWithMatch>> = _userPredictions.asStateFlow()

    // For signup and login screens
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Predefined 9 Stages
    val stages = listOf(
        "مرحله اول گروهی",
        "مرحله دوم گروهی",
        "مرحله سوم گروهی",
        "مرحله یک شانزدهم نهایی",
        "مرحله یک هشتم نهایی",
        "مرحله یک چهارم نهایی",
        "مرحله نیمه نهایی",
        "مرحله رده‌بندی",
        "مرحله فینال"
    )

    private val prefs = application.getSharedPreferences("football_predictor_session", Context.MODE_PRIVATE)

    init {
        // Fetch or create default settings
        viewModelScope.launch {
            repository.getAppSettingsDirect()
        }

        // Observe current user changes and reload predictions
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    repository.getPredictionsForUser(user.id).collect { preds ->
                        _userPredictions.value = preds
                    }
                } else {
                    _userPredictions.value = emptyList()
                    if (_currentUserId.value != null) {
                        _currentUserId.value = null
                        prefs.edit().remove("saved_user_id").apply()
                    }
                }
            }
        }
        
        // Auto-seed on first launch if users database is empty and restore session
        viewModelScope.launch {
            leaderboard.first { true } // Wait for first load
            if (leaderboard.value.isEmpty()) {
                seedMockData()
            } else {
                // Ensure SCHOLES user exists and has uppercase username
                val scholes = repository.getUserByUsername("SCHOLES")
                if (scholes == null) {
                    repository.registerUser("SCHOLES", "SCHOLES", "11971197", isAdmin = true)
                } else if (scholes.username != "SCHOLES") {
                    repository.updateUser(scholes.copy(username = "SCHOLES", displayName = "SCHOLES"))
                }
            }

            // Restore saved user session if available
            val savedUserId = prefs.getInt("saved_user_id", -1)
            if (savedUserId != -1) {
                val user = repository.getUserById(savedUserId)
                if (user != null) {
                    if (user.username.equals("scholes", ignoreCase = true) && user.username != "SCHOLES") {
                        val updated = user.copy(username = "SCHOLES", displayName = "SCHOLES")
                        repository.updateUser(updated)
                        _currentUserId.value = updated.id
                    } else {
                        _currentUserId.value = user.id
                    }
                } else {
                    val scholesUser = repository.getUserByUsername("SCHOLES") ?: repository.getUserByUsername("ali")
                    scholesUser?.let {
                        _currentUserId.value = it.id
                        prefs.edit().putInt("saved_user_id", it.id).apply()
                    }
                }
            } else {
                val scholesUser = repository.getUserByUsername("SCHOLES") ?: repository.getUserByUsername("ali")
                scholesUser?.let {
                    _currentUserId.value = it.id
                    prefs.edit().putInt("saved_user_id", it.id).apply()
                }
            }
        }
    }

    private suspend fun logAdminAction(actionDetail: String) {
        val manager = currentUser.value
        val managerName = manager?.username ?: "مدیر"
        val title = "📌 گزارش فعالیت مدیران"
        val message = "مدیر ($managerName) $actionDetail"
        repository.postAnnouncement(title, message)
    }

    fun login(username: String, password: String = "123456", onSuccess: () -> Unit = {}) {
        _authError.value = null
        viewModelScope.launch {
            val user = repository.getUserByUsername(username.trim())
            if (user != null) {
                if (user.password == password.trim()) {
                    _currentUserId.value = user.id
                    prefs.edit().putInt("saved_user_id", user.id).apply()
                    onSuccess()
                } else {
                    _authError.value = "رمز عبور اشتباه است!"
                }
            } else {
                _authError.value = "کاربر با این نام کاربری یافت نشد! کاربران باید توسط مدیر سیستم اضافه شوند."
            }
        }
    }

    fun adminAddUser(username: String, displayName: String, password: String, isAdmin: Boolean, onSuccess: () -> Unit = {}) {
        _authError.value = null
        viewModelScope.launch {
            if (username.isBlank() || displayName.isBlank() || password.isBlank()) {
                _authError.value = "نام، نام کاربری و رمز عبور نمی‌توانند خالی باشند."
                return@launch
            }
            val registered = repository.registerUser(username, displayName, password, isAdmin)
            if (registered != null) {
                logAdminAction("کاربر جدید با نام کاربری (${registered.username}) را اضافه کرد.")
                onSuccess()
            } else {
                _authError.value = "این نام کاربری از قبل وجود دارد!"
            }
        }
    }

    fun adminToggleUserActiveStatus(userId: Int, isActive: Boolean) {
        viewModelScope.launch {
            val targetUser = repository.getUserById(userId)
            repository.toggleUserActiveStatus(userId, isActive)
            val targetName = targetUser?.username ?: "کاربر $userId"
            val statusText = if (isActive) "فعال" else "غیرفعال"
            logAdminAction("وضعیت کاربر ($targetName) را به $statusText تغییر داد.")
        }
    }

    fun adminToggleUserAdminStatus(userId: Int, isAdmin: Boolean) {
        viewModelScope.launch {
            val targetUser = repository.getUserById(userId)
            repository.toggleUserAdminStatus(userId, isAdmin)
            val targetName = targetUser?.username ?: "کاربر $userId"
            val statusText = if (isAdmin) "مدیر" else "کاربر عادی"
            logAdminAction("دسترسی کاربر ($targetName) را به $statusText تغییر داد.")
        }
    }

    fun adminDeleteUser(userId: Int) {
        viewModelScope.launch {
            val targetUser = repository.getUserById(userId) ?: return@launch
            val targetName = targetUser.username

            repository.deleteUser(userId)

            if (_currentUserId.value == userId) {
                logout()
            }

            logAdminAction("حساب کاربری ($targetName) را کاملاً از سیستم حذف کرد.")
        }
    }

    fun logout() {
        _currentUserId.value = null
        prefs.edit().remove("saved_user_id").apply()
    }

    fun submitPrediction(matchId: Int, homeScore: Int, awayScore: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitPrediction(user.id, matchId, homeScore, awayScore)
        }
    }

    fun adminCreateMatch(
        homeTeam: String, 
        awayTeam: String, 
        matchTime: String, 
        stageName: String,
        pointsExact: Int, 
        pointsWinnerAndGd: Int,
        pointsWinnerOnly: Int,
        pointsWrong: Int,
        isPublished: Boolean
    ) {
        viewModelScope.launch {
            repository.createMatch(
                homeTeam = homeTeam, 
                awayTeam = awayTeam, 
                matchTime = matchTime, 
                stageName = stageName,
                pointsExactScore = pointsExact,
                pointsWinnerAndGd = pointsWinnerAndGd,
                pointsWinnerOnly = pointsWinnerOnly,
                pointsWrong = pointsWrong,
                isPublished = isPublished
            )
            logAdminAction("مسابقه جدید ($homeTeam - $awayTeam) را در قسمت بازی‌ها و نتائج اضافه کرد.")
        }
    }

    // Publish a batch of matches for a specific stage
    fun adminPublishStageMatches(stageName: String) {
        viewModelScope.launch {
            val unpublishedMatches = allMatches.value.filter { it.stageName == stageName && !it.isPublished }
            for (match in unpublishedMatches) {
                repository.updateMatch(match.copy(isPublished = true))
            }
            if (unpublishedMatches.isNotEmpty()) {
                // Post system announcement notification
                repository.postAnnouncement(
                    title = "مسابقات جدید منتشر شد",
                    message = "بازی‌های ${stageName} (تعداد ${unpublishedMatches.size} بازی) با موفقیت فعال شد. هم‌اکنون می‌توانید پیش‌بینی‌های خود را ثبت کنید!"
                )
            }
        }
    }

    fun adminDeleteMatch(match: MatchEntity) {
        viewModelScope.launch {
            repository.deleteMatch(match)
        }
    }

    fun adminUpdateMatchTeams(matchId: Int, homeTeam: String, awayTeam: String) {
        viewModelScope.launch {
            val existing = repository.getMatchById(matchId)
            if (existing != null) {
                repository.updateMatch(existing.copy(homeTeam = homeTeam, awayTeam = awayTeam))
                logAdminAction("اسامی تیم‌های مسابقه کد $matchId را به ($homeTeam - $awayTeam) ویرایش کرد.")
            }
        }
    }

    fun adminScoreMatch(
        matchId: Int,
        actualHomeScore: Int,
        actualAwayScore: Int,
        customPointsMap: Map<Int, Int>
    ) {
        viewModelScope.launch {
            repository.saveMatchResultsAndCustomPoints(
                matchId = matchId,
                actualHomeScore = actualHomeScore,
                actualAwayScore = actualAwayScore,
                customPointsMap = customPointsMap
            )
            val match = repository.getMatchById(matchId)
            val matchTitle = if (match != null) "${match.homeTeam} - ${match.awayTeam}" else "کد $matchId"
            logAdminAction("نتیجه مسابقه ($matchTitle) را ثبت نمود ($actualHomeScore - $actualAwayScore).")
            // Refresh current user points in VM if logged in
            val user = currentUser.value
            if (user != null) {
                _currentUserId.value = user.id
            }
        }
    }

    fun submitStagePredictions(stageName: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitStagePredictions(user.id, stageName)
            _currentUserId.value = user.id
        }
    }

    fun submitChampionPrediction(firstChoice: String, secondChoice: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitChampionPrediction(user.id, firstChoice, secondChoice)
            _currentUserId.value = user.id
        }
    }

    fun submitTopScorerPrediction(playerChoice: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitTopScorerPrediction(user.id, playerChoice)
            _currentUserId.value = user.id
        }
    }

    fun adminSaveAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveAppSettings(settings)
            val user = currentUser.value
            if (user != null) {
                _currentUserId.value = user.id
            }
        }
    }

    fun adminAddAnnouncement(title: String, message: String, targetUserIds: String? = null) {
        viewModelScope.launch {
            repository.postAnnouncement(title, message, targetUserIds)
            logAdminAction("اطلاعیه جدیدی با عنوان ($title) ثبت کرد.")
        }
    }

    fun adminClearAnnouncements() {
        viewModelScope.launch {
            repository.clearAnnouncements()
        }
    }

    fun adminToggleStagePredictionPublish(stageName: String, isPublished: Boolean) {
        viewModelScope.launch {
            val currentSettings = appSettings.value ?: AppSettings(id = 1)
            val currentStagesList = currentSettings.publishedPredictionStages
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.trim() }
                .toMutableList()
            
            if (isPublished) {
                if (!currentStagesList.contains(stageName)) {
                    currentStagesList.add(stageName)
                }
            } else {
                currentStagesList.remove(stageName)
            }
            
            val updatedSettings = currentSettings.copy(
                publishedPredictionStages = currentStagesList.joinToString(",")
            )
            repository.saveAppSettings(updatedSettings)
            
            if (isPublished) {
                repository.postAnnouncement(
                    title = "پیش‌بینی‌های همگانی منتشر شد",
                    message = "پیش‌بینی همه کاربران برای بازی‌های ${stageName} منتشر شد! از منوی نتایج همگانی می‌توانید جزئیات پیش‌بینی‌های بقیه را بررسی کنید."
                )
            }
        }
    }

    fun adminSubmitOnBehalf(
        userId: Int,
        matchId: Int,
        homeScore: Int,
        awayScore: Int
    ) {
        viewModelScope.launch {
            repository.submitPrediction(userId, matchId, homeScore, awayScore)
            val targetUser = repository.getUserById(userId)
            val targetName = targetUser?.username ?: "کاربر $userId"
            logAdminAction("پیش‌بینی بازی را به جای کاربر ($targetName) در قسمت بازی‌ها و نتایج ثبت کرد.")
            val current = currentUser.value
            if (current != null && current.id == userId) {
                _currentUserId.value = userId
            }
        }
    }

    fun adminSubmitSpecialOnBehalf(
        userId: Int,
        championFirstChoice: String,
        championSecondChoice: String,
        topScorerChoice: String
    ) {
        viewModelScope.launch {
            repository.submitChampionPrediction(userId, championFirstChoice, championSecondChoice)
            repository.submitTopScorerPrediction(userId, topScorerChoice)
            val targetUser = repository.getUserById(userId)
            val targetName = targetUser?.username ?: "کاربر $userId"
            logAdminAction("پیش‌بینی قهرمان/آقای گل را به جای کاربر ($targetName) ثبت کرد.")
            val current = currentUser.value
            if (current != null && current.id == userId) {
                _currentUserId.value = userId
            }
        }
    }

    fun adminSubmitStageSubmissionOnBehalf(userId: Int, stageName: String) {
        viewModelScope.launch {
            repository.submitStagePredictions(userId, stageName)
            val targetUser = repository.getUserById(userId)
            val targetName = targetUser?.username ?: "کاربر $userId"
            logAdminAction("ثبت نهایی پیش‌بینی‌های $stageName را به جای کاربر ($targetName) انجام داد.")
        }
    }

    fun adminUpdatePenaltyPoints(userId: Int, penaltyPoints: Int) {
        viewModelScope.launch {
            repository.adminUpdatePenaltyPoints(userId, penaltyPoints)
            val targetUser = repository.getUserById(userId)
            val targetName = targetUser?.username ?: "کاربر $userId"
            logAdminAction("به کاربر با نام کاربری ($targetName) $penaltyPoints امتیاز منفی داد.")
            val current = currentUser.value
            if (current != null && current.id == userId) {
                _currentUserId.value = userId
            }
        }
    }

    fun adminAddBonusItem(title: String, points: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val item = repository.addBonusItem(title, points)
            if (item != null) {
                logAdminAction("امتیاز تشویقی جدید با عنوان (${item.title}) و مقدار (${item.points} امتیاز) ایجاد کرد.")
                onSuccess()
            }
        }
    }

    fun addBonusPredictionItem(title: String, points: Int) {
        adminAddBonusItem(title, points)
    }

    fun adminDeleteBonusItem(item: BonusPredictionItem) {
        viewModelScope.launch {
            repository.deleteBonusItem(item)
            logAdminAction("امتیاز تشویقی (${item.title}) را حذف کرد.")
        }
    }

    fun deleteBonusPredictionItem(id: Int) {
        val item = allBonusItems.value.find { it.id == id }
        if (item != null) {
            adminDeleteBonusItem(item)
        }
    }

    fun adminEvaluateBonusItemWinner(bonusItemId: Int, actualWinner: String, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.evaluateBonusItemWinner(bonusItemId, actualWinner)
            logAdminAction("برنده امتیاز تشویقی کد $bonusItemId را ($actualWinner) ثبت و امتیازها را اعطا کرد.")
            onResult(res)
        }
    }

    fun evaluateBonusItemWinner(bonusItemId: Int, actualWinner: String) {
        adminEvaluateBonusItemWinner(bonusItemId, actualWinner)
    }

    fun adminAddEliminatedItem(name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val item = repository.addEliminatedItem(name)
            if (item != null) {
                logAdminAction("تیم/فرد حذف‌شده (${item.name}) را اضافه کرد.")
                onSuccess()
            }
        }
    }

    fun addEliminatedItem(name: String) {
        adminAddEliminatedItem(name)
    }

    fun adminDeleteEliminatedItem(item: EliminatedItem) {
        viewModelScope.launch {
            repository.deleteEliminatedItem(item)
            logAdminAction("تیم/فرد حذف‌شده (${item.name}) را از لیست حذف کرد.")
        }
    }

    fun deleteEliminatedItem(id: Int) {
        val item = allEliminatedItems.value.find { it.id == id }
        if (item != null) {
            adminDeleteEliminatedItem(item)
        }
    }

    fun submitUserBonusPredictions(predictions: Map<Int, String>, onSuccess: () -> Unit = {}) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitUserBonusPredictions(user.id, predictions)
            onSuccess()
        }
    }

    fun updateUserBonusPredictionText(userId: Int, bonusItemId: Int, newText: String) {
        viewModelScope.launch {
            repository.updateUserBonusPredictionText(userId, bonusItemId, newText)
            val targetUser = repository.getUserById(userId)
            val targetName = targetUser?.username ?: "کاربر $userId"
            logAdminAction("متن پیش‌بینی تشویقی کاربر ($targetName) را اصلاح کرد: «$newText»")
        }
    }

    fun toggleBonusItemPublished(bonusItemId: Int, isPublished: Boolean) {
        viewModelScope.launch {
            repository.toggleBonusItemPublished(bonusItemId, isPublished)
            val status = if (isPublished) "منتشر" else "مخفی"
            logAdminAction("وضعیت انتشار پیش‌بینی‌های امتیاز تشویقی کد $bonusItemId را به $status تغییر داد.")
        }
    }

    fun updateBannerImageUrl(url: String?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBannerImageUrl(url)
            logAdminAction("بنر بالای صفحات مسابقات را به‌روزرسانی کرد.")
            val user = currentUser.value
            if (user != null) {
                _currentUserId.value = user.id
            }
            onSuccess()
        }
    }

    fun resetTournamentSeasonKeepUsers(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resetTournamentSeasonKeepUsers()
            logAdminAction("پایگاه داده دوره مسابقات را بازنشانی کرد (بدون حذف کاربران).")
            val user = currentUser.value
            if (user != null) {
                _currentUserId.value = user.id
            }
            onSuccess()
        }
    }

    fun adminSubmitUserBonusPredictionsOnBehalf(userId: Int, predictions: Map<Int, String>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitUserBonusPredictions(userId, predictions)
            val targetUser = repository.getUserById(userId)
            val targetName = targetUser?.username ?: "کاربر $userId"
            logAdminAction("پیش‌بینی‌های تشویقی را به جای کاربر ($targetName) ثبت کرد.")
            onSuccess()
        }
    }

    fun seedMockData() {
        viewModelScope.launch {
            repository.seedDemoData()
            // Set current user to 'admin' or 'ali'
            val admin = repository.getUserByUsername("admin")
            if (admin != null) {
                _currentUserId.value = admin.id
            }
        }
    }
}
