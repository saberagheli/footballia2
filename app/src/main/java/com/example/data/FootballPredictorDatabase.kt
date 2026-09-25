package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String = "",
    val password: String = "123456",
    val isActive: Boolean = true,
    val totalPoints: Int = 0,
    val bonusPoints: Int = 0,
    val customBonusPoints: Int = 0,
    val isAdmin: Boolean = false,
    val championFirstChoice: String? = null,
    val championSecondChoice: String? = null,
    val championSubmitted: Boolean = false,
    val topScorerChoice: String? = null,
    val topScorerSubmitted: Boolean = false,
    val championPointsEarned: Int = 0,
    val topScorerPointsEarned: Int = 0,
    val penaltyPoints: Int = 0,
    val predictedChampion: String? = null,
    val predictedTopScorer: String? = null
)

@Entity(tableName = "matches", indices = [Index(value = ["stageName"])])
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val homeTeam: String,
    val awayTeam: String,
    val matchTime: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val isFinished: Boolean = false,
    val isPublished: Boolean = false,
    val stageName: String = "مرحله اول گروهی",
    val pointsExactScore: Int = 5,
    val pointsWinnerAndGd: Int = 3,
    val pointsWinnerOnly: Int = 2,
    val pointsWrong: Int = 0,
    val pointsExact: Int = 5
)

typealias Match = MatchEntity

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: String = "",
    val createdAt: String = "",
    val isRead: Boolean = false,
    val targetUserIds: String? = null
)

@Entity(
    tableName = "predictions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "matchId"], unique = true),
        Index(value = ["matchId"])
    ]
)
data class Prediction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val matchId: Long,
    val predictedHomeScore: Int,
    val predictedAwayScore: Int,
    val pointsEarned: Int? = null,
    val isScored: Boolean = false,
    val isCalculated: Boolean = false
)

data class UserPredictionWithMatch(
    @Embedded val prediction: Prediction,
    @Relation(
        parentColumn = "matchId",
        entityColumn = "id"
    )
    val match: MatchEntity
)

data class MatchPredictionWithUser(
    @Embedded val prediction: Prediction,
    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: User
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val championFirstPoints: Int = 30,
    val championSecondPoints: Int = 15,
    val championWrongPoints: Int = -5,
    val topScorerPoints: Int = 30,
    val actualChampion: String? = null,
    val actualTopScorer: String? = null,
    val publishedPredictionStages: String = "",
    val bannerImageUrl: String? = null
)

@Entity(tableName = "stage_submissions", primaryKeys = ["userId", "stageName"])
data class StageSubmission(
    val userId: Long,
    val stageName: String,
    val isSubmitted: Boolean = false
)

@Entity(tableName = "bonus_items")
data class BonusPredictionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val points: Int = 0,
    val pointsAwarded: Int = 0,
    val actualWinner: String? = null,
    val correctAnswer: String? = null,
    val isEvaluated: Boolean = false,
    val isPublished: Boolean = false
)

typealias BonusItem = BonusPredictionItem

@Entity(
    tableName = "user_bonus_predictions",
    primaryKeys = ["userId", "bonusItemId"]
)
data class UserBonusPrediction(
    val userId: Long,
    val bonusItemId: Long,
    val predictionText: String = "",
    val predictedAnswer: String = "",
    val isSubmitted: Boolean = false,
    val isEvaluated: Boolean = false,
    val pointsEarned: Int = 0
)

typealias BonusPrediction = UserBonusPrediction

@Entity(tableName = "eliminated_items")
data class EliminatedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = ""
)

@Dao
interface AppDao {
    // --- Users ---
    @Query("SELECT * FROM users ORDER BY totalPoints DESC, displayName ASC")
    fun getLeaderboard(): Flow<List<User>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdDirect(userId: Long): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: Long): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun updateUserActiveStatus(userId: Long, isActive: Boolean)

    @Query("UPDATE users SET isAdmin = :isAdmin WHERE id = :userId")
    suspend fun updateUserAdminStatus(userId: Long, isAdmin: Boolean)

    @Query("DELETE FROM predictions WHERE userId = :userId")
    suspend fun deletePredictionsByUserId(userId: Long)

    @Query("DELETE FROM user_bonus_predictions WHERE userId = :userId")
    suspend fun deleteBonusPredictionsByUserId(userId: Long)

    @Query("DELETE FROM stage_submissions WHERE userId = :userId")
    suspend fun deleteStageSubmissionsForUser(userId: Long)

    @Query("SELECT * FROM users")
    suspend fun getAllUsersDirect(): List<User>

    // --- Matches ---
    @Query("SELECT * FROM matches ORDER BY isFinished ASC, id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE stageName = :stageName ORDER BY id DESC")
    fun getMatchesByStage(stageName: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Long): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Long)

    @Query("DELETE FROM predictions WHERE matchId = :matchId")
    suspend fun deletePredictionsByMatchId(matchId: Long)

    @Query("DELETE FROM matches")
    suspend fun clearAllMatches()

    // --- Predictions ---
    @Query("SELECT * FROM predictions")
    fun getAllPredictions(): Flow<List<Prediction>>

    @Query("SELECT * FROM predictions WHERE userId = :userId")
    fun getPredictionsByUserId(userId: Long): Flow<List<Prediction>>

    @Query("SELECT * FROM predictions WHERE userId = :userId")
    suspend fun getPredictionsByUserIdDirect(userId: Long): List<Prediction>

    @Transaction
    @Query("SELECT * FROM predictions WHERE userId = :userId")
    fun getPredictionsForUser(userId: Int): Flow<List<UserPredictionWithMatch>>

    @Transaction
    @Query("SELECT * FROM predictions WHERE matchId = :matchId")
    suspend fun getPredictionsForMatch(matchId: Long): List<MatchPredictionWithUser>

    @Query("SELECT * FROM predictions WHERE matchId = :matchId")
    suspend fun getPredictionsForMatchDirect(matchId: Long): List<Prediction>

    @Query("SELECT * FROM predictions WHERE userId = :userId AND matchId = :matchId LIMIT 1")
    suspend fun getPredictionByUserAndMatch(userId: Long, matchId: Long): Prediction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: Prediction): Long

    @Update
    suspend fun updatePrediction(prediction: Prediction)

    @Query("SELECT SUM(pointsEarned) FROM predictions WHERE userId = :userId")
    suspend fun getUserTotalMatchPointsDirect(userId: Long): Int?

    @Query("SELECT SUM(pointsEarned) FROM user_bonus_predictions WHERE userId = :userId")
    suspend fun getUserTotalBonusPredictionsPointsDirect(userId: Long): Int?

    @Query("DELETE FROM predictions")
    suspend fun clearAllPredictions()

    // --- Announcements ---
    @Query("SELECT * FROM announcements ORDER BY id DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement): Long

    @Query("DELETE FROM announcements")
    suspend fun clearAnnouncements()

    // --- Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettings)

    // --- Stage Submissions ---
    @Query("SELECT * FROM stage_submissions")
    fun getAllStageSubmissionsFlow(): Flow<List<StageSubmission>>

    @Query("SELECT * FROM stage_submissions WHERE userId = :userId AND stageName = :stageName LIMIT 1")
    suspend fun getStageSubmission(userId: Long, stageName: String): StageSubmission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageSubmission(submission: StageSubmission)

    @Query("DELETE FROM stage_submissions")
    suspend fun clearStageSubmissions()

    // --- Bonus Items ---
    @Query("SELECT * FROM bonus_items ORDER BY id ASC")
    fun getAllBonusItems(): Flow<List<BonusPredictionItem>>

    @Query("SELECT * FROM bonus_items WHERE id = :id LIMIT 1")
    suspend fun getBonusItemById(id: Long): BonusPredictionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonusItem(item: BonusPredictionItem): Long

    @Update
    suspend fun updateBonusItem(item: BonusPredictionItem)

    @Query("DELETE FROM bonus_items WHERE id = :id")
    suspend fun deleteBonusItemById(id: Long)

    @Query("DELETE FROM bonus_items")
    suspend fun clearAllBonusItems()

    // --- User Bonus Predictions ---
    @Query("SELECT * FROM user_bonus_predictions WHERE userId = :userId")
    fun getBonusPredictionsByUser(userId: Long): Flow<List<UserBonusPrediction>>

    @Query("SELECT * FROM user_bonus_predictions WHERE bonusItemId = :itemId")
    suspend fun getBonusPredictionsByItemIdDirect(itemId: Long): List<UserBonusPrediction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonusPrediction(prediction: UserBonusPrediction): Long

    @Query("DELETE FROM user_bonus_predictions WHERE bonusItemId = :itemId")
    suspend fun deleteBonusPredictionsByItemId(itemId: Long)

    @Query("DELETE FROM user_bonus_predictions")
    suspend fun clearAllBonusPredictions()

    // --- Eliminated Items ---
    @Query("SELECT * FROM eliminated_items ORDER BY id ASC")
    fun getAllEliminatedItems(): Flow<List<EliminatedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEliminatedItem(item: EliminatedItem): Long

    @Query("DELETE FROM eliminated_items WHERE id = :id")
    suspend fun deleteEliminatedItemById(id: Long)

    @Query("DELETE FROM eliminated_items")
    suspend fun clearAllEliminatedItems()
}

@Database(
    entities = [
        User::class,
        MatchEntity::class,
        Prediction::class,
        Announcement::class,
        AppSettings::class,
        StageSubmission::class,
        BonusPredictionItem::class,
        UserBonusPrediction::class,
        EliminatedItem::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
