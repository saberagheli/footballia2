package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val displayName: String,
    val password: String = "123456",
    val isActive: Boolean = true,
    val totalPoints: Int = 0,
    val isAdmin: Boolean = false,
    val championFirstChoice: String? = null,
    val championSecondChoice: String? = null,
    val championSubmitted: Boolean = false,
    val topScorerChoice: String? = null,
    val topScorerSubmitted: Boolean = false,
    val championPointsEarned: Int = 0,
    val topScorerPointsEarned: Int = 0,
    val penaltyPoints: Int = 0
)

@Entity(tableName = "matches", indices = [Index(value = ["stageName"])])
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val homeTeam: String,
    val awayTeam: String,
    val matchTime: String, // e.g., "Tonight, 21:00" or "Sunday, 18:30"
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val isFinished: Boolean = false,
    val isPublished: Boolean = false, // If false, only visible to Admin. If true, visible to everyone.
    val stageName: String = "مرحله اول گروهی", // One of the 9 stages
    val pointsExactScore: Int = 5,
    val pointsWinnerAndGd: Int = 3,
    val pointsWinnerOnly: Int = 2,
    val pointsWrong: Int = 0
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val targetUserIds: String? = null // null or "ALL" for all users, or comma-separated user IDs e.g. "1,3"
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val matchId: Int,
    val predictedHomeScore: Int,
    val predictedAwayScore: Int,
    val pointsEarned: Int? = null,
    val isScored: Boolean = false
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

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users ORDER BY totalPoints DESC, displayName ASC")
    fun getLeaderboard(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: Int): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM predictions WHERE userId = :userId")
    suspend fun deletePredictionsForUser(userId: Int)

    @Query("DELETE FROM user_bonus_predictions WHERE userId = :userId")
    suspend fun deleteUserBonusPredictionsForUser(userId: Int)

    @Query("DELETE FROM stage_submissions WHERE userId = :userId")
    suspend fun deleteStageSubmissionsForUser(userId: Int)

    // Matches
    @Query("SELECT * FROM matches ORDER BY isFinished ASC, id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE stageName = :stageName ORDER BY id DESC")
    fun getMatchesByStage(stageName: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    // Predictions
    @Query("SELECT * FROM predictions")
    fun getAllPredictionsFlow(): Flow<List<Prediction>>

    @Transaction
    @Query("SELECT * FROM predictions WHERE userId = :userId")
    fun getPredictionsForUser(userId: Int): Flow<List<UserPredictionWithMatch>>

    @Transaction
    @Query("SELECT * FROM predictions WHERE matchId = :matchId")
    suspend fun getPredictionsForMatch(matchId: Int): List<MatchPredictionWithUser>

    @Query("SELECT * FROM predictions WHERE userId = :userId AND matchId = :matchId LIMIT 1")
    suspend fun getPredictionByUserAndMatch(userId: Int, matchId: Int): Prediction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: Prediction): Long

    @Update
    suspend fun updatePrediction(prediction: Prediction)

    // Announcements / System Notifications
    @Query("SELECT * FROM announcements ORDER BY id DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement): Long

    @Query("DELETE FROM announcements")
    suspend fun clearAnnouncements()

    // Reset/Setup helper
    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM matches")
    suspend fun clearMatches()

    @Query("DELETE FROM predictions")
    suspend fun clearPredictions()

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)

    // Stage Submissions
    @Query("SELECT * FROM stage_submissions")
    fun getAllStageSubmissionsFlow(): Flow<List<StageSubmission>>

    @Query("SELECT * FROM stage_submissions WHERE userId = :userId AND stageName = :stageName LIMIT 1")
    suspend fun getStageSubmission(userId: Int, stageName: String): StageSubmission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageSubmission(submission: StageSubmission)

    @Query("DELETE FROM stage_submissions")
    suspend fun clearStageSubmissions()

    // Bonus Prediction Items
    @Query("SELECT * FROM bonus_items ORDER BY id ASC")
    fun getAllBonusItemsFlow(): Flow<List<BonusPredictionItem>>

    @Query("SELECT * FROM bonus_items ORDER BY id ASC")
    suspend fun getAllBonusItems(): List<BonusPredictionItem>

    @Query("SELECT * FROM bonus_items WHERE id = :id LIMIT 1")
    suspend fun getBonusItemById(id: Int): BonusPredictionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonusItem(item: BonusPredictionItem): Long

    @Update
    suspend fun updateBonusItem(item: BonusPredictionItem)

    @Delete
    suspend fun deleteBonusItem(item: BonusPredictionItem)

    // User Bonus Predictions
    @Query("SELECT * FROM user_bonus_predictions")
    fun getAllUserBonusPredictionsFlow(): Flow<List<UserBonusPrediction>>

    @Query("SELECT * FROM user_bonus_predictions")
    suspend fun getAllUserBonusPredictions(): List<UserBonusPrediction>

    @Query("SELECT * FROM user_bonus_predictions WHERE userId = :userId")
    fun getUserBonusPredictionsFlow(userId: Int): Flow<List<UserBonusPrediction>>

    @Query("SELECT * FROM user_bonus_predictions WHERE userId = :userId")
    suspend fun getUserBonusPredictions(userId: Int): List<UserBonusPrediction>

    @Query("SELECT * FROM user_bonus_predictions WHERE userId = :userId AND bonusItemId = :bonusItemId LIMIT 1")
    suspend fun getUserBonusPrediction(userId: Int, bonusItemId: Int): UserBonusPrediction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserBonusPrediction(prediction: UserBonusPrediction): Long

    @Query("DELETE FROM user_bonus_predictions WHERE bonusItemId = :bonusItemId")
    suspend fun deleteUserBonusPredictionsForItem(bonusItemId: Int)

    // Eliminated Items
    @Query("SELECT * FROM eliminated_items ORDER BY id ASC")
    fun getAllEliminatedItemsFlow(): Flow<List<EliminatedItem>>

    @Query("SELECT * FROM eliminated_items ORDER BY id ASC")
    suspend fun getAllEliminatedItems(): List<EliminatedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEliminatedItem(item: EliminatedItem): Long

    @Delete
    suspend fun deleteEliminatedItem(item: EliminatedItem)

    // Clear queries for season reset
    @Query("SELECT * FROM users")
    suspend fun getAllUsersDirect(): List<User>

    @Query("DELETE FROM bonus_items")
    suspend fun deleteAllBonusItems()

    @Query("DELETE FROM user_bonus_predictions")
    suspend fun deleteAllUserBonusPredictions()

    @Query("DELETE FROM eliminated_items")
    suspend fun deleteAllEliminatedItems()
}

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val championFirstPoints: Int = 30,
    val championSecondPoints: Int = 15,
    val championWrongPoints: Int = -5,
    val topScorerPoints: Int = 30,
    val actualChampion: String? = null,
    val actualTopScorer: String? = null,
    val publishedPredictionStages: String = "", // Comma-separated list of stages whose predictions are public
    val bannerImageUrl: String? = null
)

@Entity(tableName = "stage_submissions", primaryKeys = ["userId", "stageName"])
data class StageSubmission(
    val userId: Int,
    val stageName: String,
    val isSubmitted: Boolean = false
)

@Entity(tableName = "bonus_items")
data class BonusPredictionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val points: Int,
    val actualWinner: String? = null,
    val isEvaluated: Boolean = false,
    val isPublished: Boolean = false // Controls whether bonus predictions for this item are published to leaderboard
)

@Entity(
    tableName = "user_bonus_predictions",
    primaryKeys = ["userId", "bonusItemId"]
)
data class UserBonusPrediction(
    val userId: Int,
    val bonusItemId: Int,
    val predictionText: String,
    val isSubmitted: Boolean = false,
    val pointsEarned: Int = 0
)

@Entity(tableName = "eliminated_items")
data class EliminatedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

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
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
