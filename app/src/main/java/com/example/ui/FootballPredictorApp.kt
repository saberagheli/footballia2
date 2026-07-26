package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballPredictorApp(viewModel: FootballPredictorViewModel) {
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val dynamicLeaderboard by viewModel.dynamicLeaderboard.collectAsStateWithLifecycle()
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val allAnnouncements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userPredictions by viewModel.userPredictions.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allStageSubmissions by viewModel.allStageSubmissions.collectAsStateWithLifecycle()
    val allPredictions by viewModel.allPredictions.collectAsStateWithLifecycle()
    val allBonusItems by viewModel.allBonusItems.collectAsStateWithLifecycle()
    val allUserBonusPredictions by viewModel.allUserBonusPredictions.collectAsStateWithLifecycle()
    val allEliminatedItems by viewModel.allEliminatedItems.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showPredictionDialogForMatch by remember { mutableStateOf<MatchEntity?>(null) }
    var showAdminScoreDialogForMatch by remember { mutableStateOf<MatchEntity?>(null) }

    // Last dismissed announcement state
    var dismissedAnnouncementId by remember { mutableStateOf<Int?>(null) }

    // Automatically navigate to Tab 0 if logged out or admin tab is active and user is not admin
    LaunchedEffect(currentUser) {
        if (activeTab == 3 && currentUser?.isAdmin != true) {
            activeTab = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = SportGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Footballia",
                            fontWeight = FontWeight.Bold,
                            color = StadiumWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.seedMockData() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = SportGold)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "ریست دیتابیس / Seed Data"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StadiumDark,
                    titleContentColor = StadiumWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = StadiumSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = null) },
                    label = { Text("جدول رده‌بندی", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StadiumDark,
                        selectedTextColor = SportGold,
                        indicatorColor = SportGold,
                        unselectedIconColor = StadiumGray,
                        unselectedTextColor = StadiumGray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.SportsSoccer, contentDescription = null) },
                    label = { Text("پیش‌بینی بازی‌ها", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StadiumDark,
                        selectedTextColor = SportGold,
                        indicatorColor = SportGold,
                        unselectedIconColor = StadiumGray,
                        unselectedTextColor = StadiumGray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("نتایج همگانی", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StadiumDark,
                        selectedTextColor = SportGold,
                        indicatorColor = SportGold,
                        unselectedIconColor = StadiumGray,
                        unselectedTextColor = StadiumGray
                    )
                )
                // Only show Admin Panel tab for admin users
                if (currentUser?.isAdmin == true) {
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("پنل مدیریت", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StadiumDark,
                            selectedTextColor = SportGold,
                            indicatorColor = SportGold,
                            unselectedIconColor = StadiumGray,
                            unselectedTextColor = StadiumGray
                        )
                    )
                }
            }
        },
        containerColor = StadiumDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Tournament Banner (1/8th screen height)
            TournamentHeaderBanner(bannerUrl = appSettings?.bannerImageUrl)

            // User Session Banner
            UserSessionBanner(
                currentUser = currentUser,
                onSwitchProfile = { showAuthDialog = true }
            )

            // SCHOLES Special Admin Activity Notifications
            val isScholesUser = currentUser?.username?.equals("scholes", ignoreCase = true) == true
            if (isScholesUser) {
                val scholesAdminLogs = allAnnouncements.filter { it.title.contains("فعالیت مدیران") || it.title.startsWith("📌") }
                if (scholesAdminLogs.isNotEmpty()) {
                    var isExpanded by remember { mutableStateOf(true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = SportGold.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SportGold)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "نوتیفیکیشن SCHOLES",
                                        tint = SportGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🔔 گزارش فعالیت مدیران (ویژه SCHOLES)",
                                        fontWeight = FontWeight.Bold,
                                        color = SportGold,
                                        fontSize = 13.sp
                                    )
                                }
                                TextButton(onClick = { isExpanded = !isExpanded }) {
                                    Text(
                                        text = if (isExpanded) "پنهان‌سازی" else "نمایش (${scholesAdminLogs.size})",
                                        color = StadiumWhite,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    scholesAdminLogs.take(5).forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(StadiumDark.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = SportGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = log.message,
                                                    color = StadiumWhite,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (log.timestamp.isNotBlank()) {
                                                    Text(
                                                        text = log.timestamp,
                                                        color = StadiumGray,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Main Content depending on Tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (currentUser?.isActive == false) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                            border = BorderStroke(1.5.dp, Color.Red),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "حساب کاربری شما غیر فعال است",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "دسترسی شما به تمام بخش‌های اپلیکیشن، مسابقات و مشاهده نتایج توسط مدیر غیرفعال شده است. جهت فعال‌سازی مجدد با مدیر سیستم تماس بگیرید.",
                                    fontSize = 13.sp,
                                    color = StadiumWhite.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Button(
                                    onClick = { showAuthDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = StadiumDark)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ورود با حساب کاربری دیگر", color = StadiumDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    when (activeTab) {
                        0 -> LeaderboardScreen(
                            leaderboard = dynamicLeaderboard,
                            currentUserId = currentUser?.id ?: -1,
                            bonusItems = allBonusItems,
                            userBonusPredictions = allUserBonusPredictions,
                            eliminatedItems = allEliminatedItems
                        )
                        1 -> MatchesScreen(
                            allMatches = allMatches,
                            userPredictions = userPredictions,
                            currentUser = currentUser,
                            stages = viewModel.stages,
                            allStageSubmissions = allStageSubmissions,
                            viewModel = viewModel,
                            onLoginRequired = { showAuthDialog = true }
                        )
                        2 -> PublicPredictionsScreen(
                            allMatches = allMatches,
                            allPredictions = allPredictions,
                            leaderboard = leaderboard,
                            stages = viewModel.stages,
                            allStageSubmissions = allStageSubmissions,
                            appSettings = appSettings,
                            currentUser = currentUser,
                            viewModel = viewModel
                        )
                        3 -> {
                            if (currentUser?.isAdmin == true) {
                                AdminScreen(
                                    allMatches = allMatches,
                                    viewModel = viewModel,
                                    onScoreMatchClick = { match ->
                                        showAdminScoreDialogForMatch = match
                                    }
                                )
                            } else {
                                // Non-admins shouldn't access admin
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "دسترسی غیرمجاز", color = StadiumGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Auth Switching Profile Dialog
    if (showAuthDialog) {
        AuthDialog(
            authError = authError,
            allUsers = leaderboard,
            onDismiss = { showAuthDialog = false },
            onLogin = { username, password ->
                viewModel.login(username, password) {
                    showAuthDialog = false
                }
            },
            onLogout = {
                viewModel.logout()
                showAuthDialog = false
            },
            currentUser = currentUser
        )
    }

    // Add/Edit prediction scores Dialog
    if (showPredictionDialogForMatch != null) {
        val match = showPredictionDialogForMatch!!
        val existing = userPredictions.find { it.prediction.matchId == match.id }?.prediction
        PredictionInputDialog(
            match = match,
            existingPrediction = existing,
            onDismiss = { showPredictionDialogForMatch = null },
            onSubmit = { home, away ->
                viewModel.submitPrediction(match.id, home, away)
                showPredictionDialogForMatch = null
            }
        )
    }

    // Admin enter final score & award manual/custom scores Dialog
    if (showAdminScoreDialogForMatch != null) {
        val match = showAdminScoreDialogForMatch!!
        var predictionsWithUsers by remember { mutableStateOf<List<MatchPredictionWithUser>>(emptyList()) }
        
        LaunchedEffect(match.id) {
            predictionsWithUsers = DatabaseProvider.getRepository(viewModel.getApplication()).getPredictionsForMatch(match.id)
        }

        AdminScoreMatchDialog(
            match = match,
            predictionsWithUsers = predictionsWithUsers,
            onDismiss = { showAdminScoreDialogForMatch = null },
            onSubmit = { home, away, customMap ->
                viewModel.adminScoreMatch(match.id, home, away, customMap)
                showAdminScoreDialogForMatch = null
            }
        )
    }
}

@Composable
fun UserSessionBanner(
    currentUser: User?,
    onSwitchProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StadiumSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(SportGreen, SportLightGreen)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentUser?.isAdmin == true) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint = StadiumWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = currentUser?.displayName ?: "کاربر مهمان",
                        fontWeight = FontWeight.Bold,
                        color = StadiumWhite,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (currentUser == null) "برای ثبت پیش‌بینی وارد شوید" 
                               else if (currentUser.isAdmin) "مدیر سیستم (امتیاز: ${currentUser.totalPoints})" 
                               else "شناسه کاربری: @${currentUser.username} | امتیاز: ${currentUser.totalPoints}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentUser?.isAdmin == true) SportGold else StadiumGray
                    )
                }
            }

            Button(
                onClick = onSwitchProfile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentUser == null) SportLightGreen else StadiumCard,
                    contentColor = if (currentUser == null) StadiumDark else SportGold
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentUser == null) "ورود / عضویت" else "تغییر پروفایل",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TournamentHeaderBanner(bannerUrl: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        border = BorderStroke(1.dp, SportGold.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = "بنر مسابقات",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0F2027),
                                    Color(0xFF203A43),
                                    Color(0xFF2C5364)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = SportGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "🏆 مسابقات پیش‌بینی فوتبال",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = SportGold
                            )
                            Text(
                                text = "لیگ برتر و تورنمنت‌های بین‌المللی",
                                fontSize = 11.sp,
                                color = StadiumWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    leaderboard: List<LeaderboardItem>,
    currentUserId: Int,
    bonusItems: List<BonusPredictionItem> = emptyList(),
    userBonusPredictions: List<UserBonusPrediction> = emptyList(),
    eliminatedItems: List<EliminatedItem> = emptyList()
) {
    val publishedBonusItems = remember(bonusItems) { bonusItems.filter { it.isPublished } }
    val tableWidth = 620.dp + (publishedBonusItems.size * 140).dp

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = StadiumCard
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = SportGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "جدول رده‌بندی مسابقات (کل)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StadiumWhite
                )
            }
        }

        if (leaderboard.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ کاربری هنوز ثبت‌نام نکرده است.",
                    color = StadiumGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.width(tableWidth)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StadiumSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "رتبه", fontWeight = FontWeight.Bold, color = StadiumGray, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "تغییر", fontWeight = FontWeight.Bold, color = StadiumGray, fontSize = 11.sp, modifier = Modifier.width(90.dp), textAlign = TextAlign.Center)
                        Text(text = "کاربر", fontWeight = FontWeight.Bold, color = StadiumGray, fontSize = 11.sp, modifier = Modifier.width(160.dp))
                        Text(text = "آخرین بازی", fontWeight = FontWeight.Bold, color = StadiumGray, fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                        Text(text = "جریمه", fontWeight = FontWeight.Bold, color = StadiumGray, fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                        Text(text = "امتیاز کل", fontWeight = FontWeight.Bold, color = StadiumGray, fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)

                        // Dynamic Bonus Items Columns Header
                        publishedBonusItems.forEach { bonus ->
                            Text(
                                text = bonus.title,
                                fontWeight = FontWeight.Bold,
                                color = SportGold,
                                fontSize = 11.sp,
                                modifier = Modifier.width(140.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(leaderboard) { index, item ->
                            val user = item.user
                            val isCurrentUser = user.id == currentUserId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isCurrentUser) StadiumCard else StadiumSurface,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isCurrentUser) 1.5.dp else 0.dp,
                                        color = if (isCurrentUser) SportGold else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank Medal / Circle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = when (index) {
                                                0 -> SportGold
                                                1 -> Color(0xFFC0C0C0) // Silver
                                                2 -> Color(0xFFCD7F32) // Bronze
                                                else -> StadiumCard
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) StadiumDark else StadiumWhite,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Rank Change (Climb / Fall Indicator)
                                val change = item.rankChange
                                Row(
                                    modifier = Modifier.width(90.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when {
                                        change > 0 -> {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "صعود",
                                                tint = SportLightGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "$change",
                                                color = SportLightGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        change < 0 -> {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "سقوط",
                                                tint = Color.Red.copy(alpha = 0.85f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${Math.abs(change)}",
                                                color = Color.Red.copy(alpha = 0.85f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "—",
                                                color = StadiumGray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                // User Info
                                Column(modifier = Modifier.width(160.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.username,
                                            fontWeight = FontWeight.Bold,
                                            color = StadiumWhite,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (user.isAdmin) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(SportGold, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "مدیر",
                                                    color = StadiumDark,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Last Game Points
                                val lastPointsText = if (item.lastMatchPoints != null) {
                                    val pts = item.lastMatchPoints
                                    if (pts >= 0) "+$pts" else "$pts"
                                } else {
                                    "—"
                                }
                                val lastPointsColor = if (item.lastMatchPoints != null) {
                                    if (item.lastMatchPoints > 0) SportLightGreen else if (item.lastMatchPoints < 0) Color.Red.copy(alpha = 0.85f) else StadiumWhite
                                } else {
                                    StadiumGray
                                }
                                Text(
                                    text = lastPointsText,
                                    fontWeight = FontWeight.Bold,
                                    color = lastPointsColor,
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )

                                // Disciplinary Penalty Points (جریمه انضباطی)
                                val penaltyPointsText = if (user.penaltyPoints > 0) "-${user.penaltyPoints}" else "۰"
                                Text(
                                    text = penaltyPointsText,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.penaltyPoints > 0) Color.Red.copy(alpha = 0.85f) else StadiumGray,
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )

                                // Total Points
                                Text(
                                    text = "${user.totalPoints}",
                                    fontWeight = FontWeight.Black,
                                    color = if (index == 0) SportGold else SportLightGreen,
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )

                                // Dynamic Bonus Predictions Columns per User
                                publishedBonusItems.forEach { bonusItem ->
                                    val userPred = userBonusPredictions.find { it.userId == user.id && it.bonusItemId == bonusItem.id }
                                    val textVal = userPred?.predictionText?.trim() ?: ""

                                    val isEliminated = textVal.isNotEmpty() && eliminatedItems.any { it.name.trim().equals(textVal, ignoreCase = true) }
                                    val isWinner = textVal.isNotEmpty() && bonusItem.actualWinner != null && bonusItem.actualWinner.trim().equals(textVal, ignoreCase = true)

                                    val (displayText, textColor, textDec) = when {
                                        textVal.isEmpty() -> Triple("—", StadiumGray, TextDecoration.None)
                                        isEliminated -> Triple(textVal, Color.Red, TextDecoration.LineThrough)
                                        isWinner -> Triple("$textVal ✓", SportLightGreen, TextDecoration.None)
                                        else -> Triple(textVal, StadiumWhite, TextDecoration.None)
                                    }

                                    Text(
                                        text = displayText,
                                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor,
                                        style = TextStyle(textDecoration = textDec),
                                        modifier = Modifier.width(140.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean
) {
    var textState by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = textState,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }.take(2)
            textState = digitsOnly
            val parsed = digitsOnly.toIntOrNull() ?: 0
            onValueChange(parsed)
        },
        enabled = enabled,
        modifier = Modifier
            .width(52.dp)
            .height(48.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            textAlign = TextAlign.Center,
            color = if (enabled) StadiumWhite else StadiumGray,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportLightGreen,
            unfocusedBorderColor = StadiumCard,
            disabledBorderColor = StadiumCard.copy(alpha = 0.4f),
            focusedContainerColor = StadiumDark,
            unfocusedContainerColor = StadiumDark,
            disabledContainerColor = StadiumDark
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun MatchesScreen(
    allMatches: List<MatchEntity>,
    userPredictions: List<UserPredictionWithMatch>,
    currentUser: User?,
    stages: List<String>,
    allStageSubmissions: List<StageSubmission>,
    viewModel: FootballPredictorViewModel,
    onLoginRequired: () -> Unit
) {
    val allBonusItems by viewModel.allBonusItems.collectAsStateWithLifecycle()
    val allUserBonusPredictions by viewModel.allUserBonusPredictions.collectAsStateWithLifecycle()

    var showActiveMatchesOnly by remember { mutableStateOf(true) }
    var selectedStage by remember { mutableStateOf("مرحله اول گروهی") }

    // Temporary map of predictions being edited: matchId -> Pair(homeScore, awayScore)
    var tempPredictions by remember { mutableStateOf(mapOf<Int, Pair<Int, Int>>()) }

    // Dialog state for batch stage prediction submission
    var showConfirmStageSubmitDialog by remember { mutableStateOf(false) }

    // Sync tempPredictions with existing user predictions when stage or user changes
    LaunchedEffect(selectedStage, userPredictions) {
        val initialMap = mutableMapOf<Int, Pair<Int, Int>>()
        userPredictions.forEach { up ->
            if (up.match.stageName == selectedStage) {
                initialMap[up.prediction.matchId] = Pair(up.prediction.predictedHomeScore, up.prediction.predictedAwayScore)
            }
        }
        tempPredictions = initialMap
    }

    val isSubmitted = currentUser != null && allStageSubmissions.any { 
        it.userId == currentUser.id && it.stageName == selectedStage && it.isSubmitted 
    }

    // Filter matches by selected stage and active/finished status
    val filteredMatches = remember(allMatches, selectedStage, showActiveMatchesOnly) {
        allMatches.filter { match ->
            match.stageName == selectedStage && match.isPublished && (
                if (showActiveMatchesOnly) (!match.isFinished && match.homeScore == null)
                else (match.isFinished || match.homeScore != null)
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stage Selector as the first item
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    text = "انتخاب مرحله بازی:",
                    color = StadiumWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stages) { stage ->
                        val isSelected = selectedStage == stage
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) SportGold else StadiumSurface,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SportGold else StadiumCard,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedStage = stage }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stage,
                                color = if (isSelected) StadiumDark else StadiumWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Bonus Predictions Section for Logged-In User
        if (currentUser != null && allBonusItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SportGold.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SportGold)
                            Text(
                                text = "🏆 پیش‌بینی‌های تشویقی مسابقات",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SportGold
                            )
                        }

                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        val userPreds = allUserBonusPredictions.filter { it.userId == currentUser.id }
                        val allSubmitted = allBonusItems.all { b ->
                            userPreds.any { it.bonusItemId == b.id && it.isSubmitted }
                        }

                        if (allSubmitted) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(SportLightGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                        .fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SportLightGreen, modifier = Modifier.size(18.dp))
                                    Text("پیش‌بینی‌های تشویقی شما ثبت و قفل شده است:", color = SportLightGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                allBonusItems.forEach { bonusItem ->
                                    val pred = userPreds.find { it.bonusItemId == bonusItem.id }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StadiumDark, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${bonusItem.title} (${bonusItem.points} امتیاز):",
                                            color = StadiumGray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = pred?.predictionText ?: "—",
                                            color = StadiumWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            var bonusInputs by remember(currentUser.id, userPreds) {
                                mutableStateOf(
                                    allBonusItems.associate { b ->
                                        val existing = userPreds.find { it.bonusItemId == b.id }
                                        b.id to (existing?.predictionText ?: "")
                                    }
                                )
                            }
                            var showConfirmBonusDialog by remember { mutableStateOf(false) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                allBonusItems.forEach { bonusItem ->
                                    val currentText = bonusInputs[bonusItem.id] ?: ""
                                    OutlinedTextField(
                                        value = currentText,
                                        onValueChange = { newVal ->
                                            bonusInputs = bonusInputs.toMutableMap().apply { put(bonusItem.id, newVal) }
                                        },
                                        label = { Text("${bonusItem.title} (${bonusItem.points} امتیاز)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SportLightGreen,
                                            focusedLabelColor = SportLightGreen
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                val canSubmit = bonusInputs.values.all { it.isNotBlank() }

                                Button(
                                    onClick = { showConfirmBonusDialog = true },
                                    enabled = canSubmit,
                                    colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ثبت نهایی پیش‌بینی‌های تشویقی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (showConfirmBonusDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showConfirmBonusDialog = false },
                                        title = { Text("ثبت نهایی پیش‌بینی‌های تشویقی", color = SportGold, fontWeight = FontWeight.Bold) },
                                        text = {
                                            Text("آیا از انتخاب‌های خود مطمئن هستید؟ پس از ثبت نهایی، امکان تغییر پیش‌بینی‌های تشویقی وجود ندارد.", color = StadiumWhite, fontSize = 13.sp)
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.submitUserBonusPredictions(bonusInputs) {
                                                        showConfirmBonusDialog = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen)
                                            ) {
                                                Text("بله، ثبت نهایی کن", color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showConfirmBonusDialog = false }) {
                                                Text("انصراف", color = StadiumGray)
                                            }
                                        },
                                        containerColor = StadiumSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active/Finished Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(StadiumSurface, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showActiveMatchesOnly) SportGreen else Color.Transparent)
                        .clickable { showActiveMatchesOnly = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بازی‌های فعال",
                        color = if (showActiveMatchesOnly) StadiumWhite else StadiumGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!showActiveMatchesOnly) SportGreen else Color.Transparent)
                        .clickable { showActiveMatchesOnly = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بازی‌های پایان‌یافته",
                        color = if (!showActiveMatchesOnly) StadiumWhite else StadiumGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Status Banner for stage submission
        if (showActiveMatchesOnly && currentUser != null && filteredMatches.isNotEmpty()) {
            item {
                if (isSubmitted) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(SportLightGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SportLightGreen)
                        Text(
                            text = "🔒 پیش‌بینی‌های شما در مرحله «$selectedStage» ثبت نهایی شده و غیر قابل تغییر است.",
                            color = SportLightGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(SportGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = SportGold)
                        Text(
                            text = "✍️ در حال ویرایش پیش‌بینی‌ها. پس از وارد کردن نتایج، حتماً روی دکمه ثبت نهایی در انتهای صفحه کلیک کنید.",
                            color = SportGold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Matches list
        if (filteredMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showActiveMatchesOnly) "هیچ بازی فعال منتشر شده‌ای در این مرحله وجود ندارد." 
                               else "هیچ بازی پایان‌یافته‌ای در این مرحله ثبت نشده است.",
                        color = StadiumGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredMatches) { match ->
                val userPred = userPredictions.find { it.prediction.matchId == match.id }
                val scorePair = tempPredictions[match.id] ?: Pair(0, 0)
                
                MatchCard(
                    match = match,
                    userPred = userPred?.prediction,
                    currentUser = currentUser,
                    isSubmitted = isSubmitted,
                    tempHomeScore = scorePair.first,
                    tempAwayScore = scorePair.second,
                    onTempPredictionChange = { home, away ->
                        tempPredictions = tempPredictions.toMutableMap().apply {
                            put(match.id, Pair(home, away))
                        }
                    },
                    onPredictClick = {
                        if (currentUser == null) {
                            onLoginRequired()
                        }
                    }
                )
            }

            // Big Sticky Batch Submit Button at the bottom of the list for Active matches
            if (showActiveMatchesOnly && currentUser != null && !isSubmitted) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showConfirmStageSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen, contentColor = StadiumDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ثبت نهایی پیش‌بینی‌های $selectedStage",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialogs (MANDATORY Persian Wording)
    if (showConfirmStageSubmitDialog) {
        Dialog(onDismissRequest = { showConfirmStageSubmitDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SportGold)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = SportGold, modifier = Modifier.size(48.dp))
                    
                    Text(
                        text = "آیا از ثبت نتیجه‌ها مطمئنی؟",
                        color = StadiumWhite,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "با تایید نهایی پیش‌بینی‌ها، دیگر قادر به تغییر یا ثبت مجدد برای بازی‌های فعال در این مرحله نخواهی بود.",
                        color = StadiumGray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                // 1. Save all edited predictions to the DB
                                tempPredictions.forEach { (matchId, scores) ->
                                    viewModel.submitPrediction(matchId, scores.first, scores.second)
                                }
                                // 2. Mark this stage as finalized
                                viewModel.submitStagePredictions(selectedStage)
                                showConfirmStageSubmitDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen, contentColor = StadiumDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("بله مطمئنم", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { showConfirmStageSubmitDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumCard, contentColor = StadiumWhite),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("نیاز به فکر دارم", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: MatchEntity,
    userPred: Prediction?,
    currentUser: User?,
    isSubmitted: Boolean,
    tempHomeScore: Int,
    tempAwayScore: Int,
    onTempPredictionChange: (Int, Int) -> Unit,
    onPredictClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Match Time & Stage Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(SportGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = match.stageName, color = SportGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = match.matchTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = StadiumWhite.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = if (match.isFinished) StadiumDark else SportLightGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (match.isFinished) "پایان‌یافته" else if (isSubmitted) "قفل شده" else "در حال پیش‌بینی",
                        color = if (match.isFinished) StadiumGray else if (isSubmitted) Color.Red else SportLightGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Board Grid Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Home Team Name
                Text(
                    text = match.homeTeam,
                    fontWeight = FontWeight.Bold,
                    color = StadiumWhite,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Inline Editors / Actual scores / vs
                Box(
                    modifier = Modifier.weight(1.5f),
                    contentAlignment = Alignment.Center
                ) {
                    if (match.isFinished) {
                        Row(
                            modifier = Modifier
                                .background(StadiumDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.homeScore?.toString() ?: "0",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = SportGold
                            )
                            Text(text = "-", color = StadiumGray, fontSize = 16.sp)
                            Text(
                                text = match.awayScore?.toString() ?: "0",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = SportGold
                            )
                        }
                    } else {
                        if (currentUser != null && !isSubmitted) {
                            // Display the beautiful inline steppers directly!
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ScoreStepper(
                                    value = tempHomeScore,
                                    onValueChange = { newHome -> onTempPredictionChange(newHome, tempAwayScore) },
                                    enabled = true
                                )
                                Text(text = ":", color = StadiumGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                ScoreStepper(
                                    value = tempAwayScore,
                                    onValueChange = { newAway -> onTempPredictionChange(tempHomeScore, newAway) },
                                    enabled = true
                                )
                            }
                        } else {
                            // Static display when locked or logged out
                            Row(
                                modifier = Modifier
                                    .background(StadiumDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentUser != null) tempHomeScore.toString() else "0",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = StadiumWhite
                                )
                                Text(text = "-", color = StadiumGray, fontSize = 14.sp)
                                Text(
                                    text = if (currentUser != null) tempAwayScore.toString() else "0",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = StadiumWhite
                                )
                            }
                        }
                    }
                }

                // Away Team Name
                Text(
                    text = match.awayTeam,
                    fontWeight = FontWeight.Bold,
                    color = StadiumWhite,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prediction Status Section
            Divider(color = StadiumCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (!match.isFinished) {
                if (currentUser == null) {
                    Button(
                        onClick = onPredictClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text("ورود و پیش‌بینی بازی", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSubmitted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = StadiumGray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "پیش‌بینی نهایی:  $tempHomeScore - $tempAwayScore",
                                    color = StadiumGray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(StadiumDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("ثبت قطعی شده", color = StadiumGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SportGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (userPred != null) "پیش‌بینی موقت شما: $tempHomeScore - $tempAwayScore" else "پیش‌بینی ثبت نشده است",
                                    color = SportGold,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "دقیق: ${match.pointsExactScore} | تفاضل: ${match.pointsWinnerAndGd} | برد: ${match.pointsWinnerOnly}",
                                color = StadiumGray,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            } else {
                // Finished Match Prediction Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (userPred != null) {
                        Column {
                            Text(
                                text = "پیش‌بینی شما:  ${userPred.predictedHomeScore} - ${userPred.predictedAwayScore}",
                                color = StadiumGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "نتیجه واقعی: ${match.homeScore} - ${match.awayScore}",
                                color = StadiumWhite,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Points Won Badge
                        val points = userPred.pointsEarned ?: 0
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (points > 0) SportLightGreen else if (points < 0) Color.Red.copy(alpha = 0.8f) else StadiumDark,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (points >= 0) "+$points امتیاز" else "$points امتیاز",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = "شما برای این بازی پیش‌بینی ثبت نکرده بودید.",
                            color = StadiumGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box(
                            modifier = Modifier
                                .background(StadiumDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "۰ امتیاز",
                                color = StadiumGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminScreen(
    allMatches: List<MatchEntity>,
    viewModel: FootballPredictorViewModel,
    onScoreMatchClick: (MatchEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "👥 مدیریت کاربران",
        "⚽ بازی‌ها و نتایج",
        "🏆 امتیازات تشویقی",
        "📋 پیش‌بینی نیابتی",
        "⚙️ تنظیمات و اطلاعیه‌ها"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = StadiumSurface,
            contentColor = SportGold,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    selectedContentColor = SportGold,
                    unselectedContentColor = StadiumGray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> UserManagementTab(viewModel = viewModel)
            1 -> MatchManagementTab(allMatches = allMatches, viewModel = viewModel, onScoreMatchClick = onScoreMatchClick)
            2 -> BonusPredictionsAdminTab(viewModel = viewModel)
            3 -> ProxyPredictionsTab(allMatches = allMatches, viewModel = viewModel)
            4 -> SettingsAndAnnouncementsTab(viewModel = viewModel)
        }
    }
}

@Composable
fun BonusPredictionsAdminTab(viewModel: FootballPredictorViewModel) {
    val bonusItems by viewModel.allBonusItems.collectAsStateWithLifecycle()
    val eliminatedItems by viewModel.allEliminatedItems.collectAsStateWithLifecycle()
    val allUserBonusPredictions by viewModel.allUserBonusPredictions.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    var showAddBonusDialog by remember { mutableStateOf(false) }
    var bonusTitleInput by remember { mutableStateOf("") }
    var bonusPointsInput by remember { mutableStateOf("") }

    var eliminatedNameInput by remember { mutableStateOf("") }

    var setWinnerForBonusId by remember { mutableStateOf<Int?>(null) }
    var actualWinnerInput by remember { mutableStateOf("") }

    var showEditSpellingDialog by remember { mutableStateOf(false) }
    var editingUserId by remember { mutableStateOf<Int?>(null) }
    var editingBonusItemId by remember { mutableStateOf<Int?>(null) }
    var editingTextValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Bonus Prediction Items Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SportGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SportGold)
                        Text(
                            text = "مدیریت امتیازات تشویقی (قهرمان، آقای گل و ...)",
                            fontWeight = FontWeight.Bold,
                            color = StadiumWhite,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { showAddBonusDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("افزودن امتیاز تشویقی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                if (bonusItems.isEmpty()) {
                    Text(
                        text = "هنوز هیچ آیتم پیش‌بینی تشویقی ایجاد نشده است.",
                        color = StadiumGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        bonusItems.forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = StadiumDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                color = SportGold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "مقدار امتیاز: ${item.points} امتیاز",
                                                color = StadiumGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deleteBonusPredictionItem(item.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                    }

                                    HorizontalDivider(color = StadiumCard, thickness = 0.5.dp)

                                    // Toggle Publish Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (item.isPublished) "وضعیت: 👁️ منتشر در جدول عمومی" else "وضعیت: 🔒 مخفی از جدول عمومی",
                                            color = if (item.isPublished) SportLightGreen else SportGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = { viewModel.toggleBonusItemPublished(item.id, !item.isPublished) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (item.isPublished) StadiumCard else SportGold,
                                                contentColor = if (item.isPublished) StadiumWhite else StadiumDark
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(if (item.isPublished) "مخفی کردن" else "انتشار در جدول", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(color = StadiumCard, thickness = 0.5.dp)

                                    if (item.actualWinner != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SportLightGreen, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "برنده واقعی مشخص شده: ${item.actualWinner}",
                                                color = SportLightGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    setWinnerForBonusId = item.id
                                                    actualWinnerInput = ""
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SportGold),
                                                border = BorderStroke(1.dp, SportGold),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("ثبت برنده واقعی و اعطای امتیاز", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // User Predictions Spelling Edit Section
                                    HorizontalDivider(color = StadiumCard, thickness = 0.5.dp)

                                    var isExpandedUserPreds by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isExpandedUserPreds = !isExpandedUserPreds },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✏️ ویرایش املایی پیش‌بینی ثبت‌شده کاربران",
                                            color = SportGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isExpandedUserPreds) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = SportGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (isExpandedUserPreds) {
                                        val itemPreds = allUserBonusPredictions.filter { it.bonusItemId == item.id }
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            leaderboard.forEach { u ->
                                                val pred = itemPreds.find { it.userId == u.id }
                                                val currentText = pred?.predictionText ?: "ثبت نشده"

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(StadiumSurface, RoundedCornerShape(6.dp))
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(u.username, color = StadiumWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("ثبت شده: $currentText", color = if (pred != null) SportGold else StadiumGray, fontSize = 11.sp)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            editingUserId = u.id
                                                            editingBonusItemId = item.id
                                                            editingTextValue = pred?.predictionText ?: ""
                                                            showEditSpellingDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = StadiumCard, contentColor = StadiumWhite),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("ویرایش املا", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Eliminated Teams / Players Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color.Red.copy(alpha = 0.85f))
                    Text(
                        text = "مدیریت تیم‌ها و بازیکنان حذف شده",
                        fontWeight = FontWeight.Bold,
                        color = StadiumWhite,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "نام تیم‌ها یا بازیکنان حذف شده را وارد کنید تا در جدول رده‌بندی به رنگ قرمز و با خط روی نام نشان داده شوند.",
                    color = StadiumGray,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = eliminatedNameInput,
                        onValueChange = { eliminatedNameInput = it },
                        label = { Text("نام تیم / بازیکن (مثلاً Scholes یا آلمان)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red.copy(alpha = 0.8f),
                            focusedLabelColor = Color.Red.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (eliminatedNameInput.isNotBlank()) {
                                viewModel.addEliminatedItem(eliminatedNameInput.trim())
                                eliminatedNameInput = ""
                            }
                        },
                        enabled = eliminatedNameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("افزودن به حذف شده‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (eliminatedItems.isNotEmpty()) {
                    Text("لیست موارد حذف شده فعلی:", color = StadiumWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        eliminatedItems.forEach { item ->
                            AssistChip(
                                onClick = { viewModel.deleteEliminatedItem(item.id) },
                                label = { Text(item.name, color = Color.White, style = TextStyle(textDecoration = TextDecoration.LineThrough), fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Red.copy(alpha = 0.6f)),
                                border = null
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Add Bonus Item
    if (showAddBonusDialog) {
        AlertDialog(
            onDismissRequest = { showAddBonusDialog = false },
            title = { Text("افزودن امتیاز تشویقی جدید", color = SportGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bonusTitleInput,
                        onValueChange = { bonusTitleInput = it },
                        label = { Text("عنوان امتیاز تشویقی (مثلاً: قهرمان اول، آقای گل)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportLightGreen)
                    )
                    OutlinedTextField(
                        value = bonusPointsInput,
                        onValueChange = { bonusPointsInput = it },
                        label = { Text("مقدار امتیاز تشویقی (مثلاً: 20 یا 30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportLightGreen)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pts = bonusPointsInput.toIntOrNull() ?: 0
                        if (bonusTitleInput.isNotBlank() && pts > 0) {
                            viewModel.addBonusPredictionItem(bonusTitleInput.trim(), pts)
                            showAddBonusDialog = false
                            bonusTitleInput = ""
                            bonusPointsInput = ""
                        }
                    },
                    enabled = bonusTitleInput.isNotBlank() && (bonusPointsInput.toIntOrNull() ?: 0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen)
                ) {
                    Text("ذخیره", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBonusDialog = false }) {
                    Text("انصراف", color = StadiumGray)
                }
            },
            containerColor = StadiumSurface
        )
    }

    // Dialog 2: Set Actual Winner & Award Points
    if (setWinnerForBonusId != null) {
        val targetItem = bonusItems.find { it.id == setWinnerForBonusId }
        AlertDialog(
            onDismissRequest = { setWinnerForBonusId = null },
            title = { Text("ثبت برنده واقعی ${targetItem?.title ?: ""}", color = SportGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "با ثبت نام برنده، تمامی کاربرانی که این گزینه را درست پیش‌بینی کرده‌اند به مقدار ${targetItem?.points ?: 0} امتیاز دریافت می‌کنند.",
                        color = StadiumWhite,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = actualWinnerInput,
                        onValueChange = { actualWinnerInput = it },
                        label = { Text("نام دقیق تیم / بازیکن برنده") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportGold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (actualWinnerInput.isNotBlank() && targetItem != null) {
                            viewModel.evaluateBonusItemWinner(targetItem.id, actualWinnerInput.trim())
                            setWinnerForBonusId = null
                            actualWinnerInput = ""
                        }
                    },
                    enabled = actualWinnerInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark)
                ) {
                    Text("ثبت و اعطای امتیازات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { setWinnerForBonusId = null }) {
                    Text("انصراف", color = StadiumGray)
                }
            },
            containerColor = StadiumSurface
        )
    }

    // Dialog 3: Edit User Prediction Spelling
    if (showEditSpellingDialog && editingUserId != null && editingBonusItemId != null) {
        AlertDialog(
            onDismissRequest = { showEditSpellingDialog = false },
            title = { Text("ویرایش املایی پیش‌بینی کاربر", color = SportGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("متن صحیح را وارد کنید تا در جدول و محاسبه امتیازات به‌روزرسانی شود:", color = StadiumWhite, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editingTextValue,
                        onValueChange = { editingTextValue = it },
                        label = { Text("متن صحیح پیش‌بینی (مثلاً فرانسه)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportGold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserBonusPredictionText(editingUserId!!, editingBonusItemId!!, editingTextValue.trim())
                        showEditSpellingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen)
                ) {
                    Text("ذخیره تغییرات", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSpellingDialog = false }) {
                    Text("انصراف", color = StadiumGray)
                }
            },
            containerColor = StadiumSurface
        )
    }
}

@Composable
fun UserManagementTab(viewModel: FootballPredictorViewModel) {
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    var showAddUserDialog by remember { mutableStateOf(false) }

    val sortedUsers = remember(leaderboard) {
        leaderboard.sortedWith(
            compareByDescending<User> { it.isActive }
                .thenBy { it.id }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SportLightGreen.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "👥 کاربران سیستم",
                        fontWeight = FontWeight.Bold,
                        color = SportGold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${leaderboard.size} کاربر (${sortedUsers.count { it.isActive }} فعال)",
                        color = StadiumGray,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = StadiumDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن کاربر جدید", color = StadiumDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (sortedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("هیچ کاربری یافت نشد.", color = StadiumGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedUsers, key = { it.id }) { user ->
                    UserAdminCard(
                        user = user,
                        onToggleActive = { isActive ->
                            viewModel.adminToggleUserActiveStatus(user.id, isActive)
                        },
                        onToggleAdmin = { isAdmin ->
                            viewModel.adminToggleUserAdminStatus(user.id, isAdmin)
                        },
                        onUpdatePenalty = { newPenalty ->
                            viewModel.adminUpdatePenaltyPoints(user.id, newPenalty)
                        },
                        onDeleteUser = {
                            viewModel.adminDeleteUser(user.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(
            error = authError,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, displayName, password, isAdmin ->
                viewModel.adminAddUser(username, displayName, password, isAdmin) {
                    showAddUserDialog = false
                }
            }
        )
    }
}

@Composable
fun UserAdminCard(
    user: User,
    onToggleActive: (Boolean) -> Unit,
    onToggleAdmin: (Boolean) -> Unit,
    onUpdatePenalty: (Int) -> Unit,
    onDeleteUser: () -> Unit
) {
    val isScholes = user.username.equals("scholes", ignoreCase = true)
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) StadiumSurface else Color(0xFF2B1C1C)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (user.isActive) StadiumCard else Color.Red.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (user.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isAdmin) SportGold else StadiumGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = user.displayName,
                            fontWeight = FontWeight.Bold,
                            color = StadiumWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "@${user.username}",
                            color = StadiumGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (user.isActive) SportLightGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (user.isActive) SportLightGreen else Color.Red)
                    ) {
                        Text(
                            text = if (user.isActive) "حساب فعال" else "غیر فعال",
                            color = if (user.isActive) SportLightGreen else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (!isScholes) {
                        IconButton(
                            onClick = { showConfirmDeleteDialog = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف کاربر",
                                tint = Color.Red.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = StadiumCard.copy(alpha = 0.6f), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = SportGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "رمز عبور: ",
                        color = StadiumGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isScholes) "••••••••" else user.password,
                        color = SportGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "امتیاز کل: ${user.totalPoints}",
                    color = SportLightGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = StadiumCard.copy(alpha = 0.6f), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = user.isActive,
                        onCheckedChange = if (isScholes) { {} } else onToggleActive,
                        enabled = !isScholes,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SportLightGreen,
                            checkedTrackColor = SportLightGreen.copy(alpha = 0.4f),
                            disabledCheckedThumbColor = SportLightGreen.copy(alpha = 0.6f),
                            disabledCheckedTrackColor = SportLightGreen.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "فعال",
                        color = if (user.isActive) SportLightGreen else StadiumGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = user.isAdmin,
                        onCheckedChange = if (isScholes) { {} } else onToggleAdmin,
                        enabled = !isScholes,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SportGold,
                            checkedTrackColor = SportGold.copy(alpha = 0.4f),
                            disabledCheckedThumbColor = SportGold.copy(alpha = 0.6f),
                            disabledCheckedTrackColor = SportGold.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مدیر",
                        color = if (user.isAdmin) SportGold else StadiumGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(StadiumCard, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "جریمه: ",
                        color = StadiumGray,
                        fontSize = 11.sp
                    )
                    if (isScholes) {
                        Text(
                            text = "غیرفعال",
                            color = StadiumGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { if (user.penaltyPoints > 0) onUpdatePenalty(user.penaltyPoints - 1) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("-", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "${user.penaltyPoints}",
                            color = if (user.penaltyPoints > 0) Color.Red else StadiumWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { onUpdatePenalty(user.penaltyPoints + 1) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("+", color = SportLightGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = {
                Text(
                    text = "تأیید حذف حساب کاربری",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف حساب کاربری «${user.displayName} (@${user.username})» اطمینان دارید؟ تمامی پیش‌بینی‌ها، امتیازات و سوابق این کاربر کاملاً از سیستم پاک خواهد شد.",
                    color = StadiumWhite,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        onDeleteUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("بله، حساب را حذف کن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("انصراف", color = StadiumGray)
                }
            },
            containerColor = StadiumSurface
        )
    }
}

@Composable
fun AddUserDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (username: String, displayName: String, password: String, isAdmin: Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var isAdmin by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "➕ افزودن کاربر جدید به سیستم",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("نام و نام خانوادگی (مثلاً محمد رضایی)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportLightGreen,
                        focusedLabelColor = SportLightGreen
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("نام کاربری انگلیسی (Username)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportLightGreen,
                        focusedLabelColor = SportLightGreen
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("رمز عبور (Password)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportLightGreen,
                        focusedLabelColor = SportLightGreen
                    ),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it },
                        colors = CheckboxDefaults.colors(checkedColor = SportGold)
                    )
                    Text(
                        text = "دسترسی مدیر سیستم (Admin)",
                        color = StadiumWhite,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = {
                            onConfirm(username, displayName, password, isAdmin)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("افزودن کاربر", color = StadiumDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchManagementTab(
    allMatches: List<MatchEntity>,
    viewModel: FootballPredictorViewModel,
    onScoreMatchClick: (MatchEntity) -> Unit
) {
    var stageName by remember { mutableStateOf("مرحله اول گروهی") }

    var pointsExact by remember { mutableStateOf("5") }
    var pointsWinnerAndGd by remember { mutableStateOf("3") }
    var pointsWinnerOnly by remember { mutableStateOf("2") }
    var pointsWrong by remember { mutableStateOf("0") }

    var showStageDropdown by remember { mutableStateOf(false) }

    var matchCountInput by remember { mutableStateOf("1") }
    val matchSlots = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            repeat(1) { add(Pair("", "")) }
        }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddMoreForStage by remember { mutableStateOf(false) }
    var saveSuccessMsg by remember { mutableStateOf<String?>(null) }

    val existingMatchesForStage = remember(allMatches, stageName) {
        allMatches.filter { it.stageName == stageName }
    }

    // Editable map for registered stage matches: match.id -> Pair(homeTeam, awayTeam)
    val editableTeams = remember { mutableStateMapOf<Int, Pair<String, String>>() }

    LaunchedEffect(existingMatchesForStage) {
        existingMatchesForStage.forEach { m ->
            if (!editableTeams.containsKey(m.id)) {
                editableTeams[m.id] = Pair(m.homeTeam, m.awayTeam)
            }
        }
    }

    fun setSlotsCount(count: Int) {
        val target = count.coerceIn(1, 100)
        while (matchSlots.size < target) {
            matchSlots.add(Pair("", ""))
        }
        while (matchSlots.size > target) {
            matchSlots.removeAt(matchSlots.lastIndex)
        }
    }

    val isAllSlotsFilled = matchSlots.isNotEmpty() && matchSlots.all { it.first.trim().isNotBlank() && it.second.trim().isNotBlank() }
    val isPointsValid = pointsExact.isNotBlank() && pointsWinnerAndGd.isNotBlank() && pointsWinnerOnly.isNotBlank() && pointsWrong.isNotBlank()
    val isFormValid = isAllSlotsFilled && isPointsValid

    // Sorted matches for scoring section: Unfinished first, Finished last
    val sortedMatches = remember(allMatches) {
        allMatches.sortedWith(
            compareBy<MatchEntity> { it.isFinished }
                .thenBy { it.id }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = StadiumSurface,
            title = {
                Text(
                    text = "پیش‌نمایش و تأیید ثبت بازی‌ها",
                    fontWeight = FontWeight.Bold,
                    color = SportGold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "آیا از ثبت ${matchSlots.size} بازی برای «$stageName» مطمئن هستید؟",
                        color = StadiumWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "با ثبت نهایی، تمامی بازی‌ها برای همه کاربران منتشر شده و نوتیفیکیشن اطلاع‌رسانی ارسال می‌گردد.",
                        color = StadiumGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(matchSlots) { index, slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StadiumDark, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${slot.first.trim()}",
                                        color = StadiumWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "VS",
                                        color = SportGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = slot.second.trim(),
                                        color = StadiumWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                    Text(
                        text = "امتیازات: دقیق ($pointsExact) | تفاضل ($pointsWinnerAndGd) | برنده ($pointsWinnerOnly) | اشتباه ($pointsWrong)",
                        color = SportLightGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val exactVal = pointsExact.toIntOrNull() ?: 5
                        val winnerGdVal = pointsWinnerAndGd.toIntOrNull() ?: 3
                        val winnerOnlyVal = pointsWinnerOnly.toIntOrNull() ?: 2
                        val wrongVal = pointsWrong.toIntOrNull() ?: 0

                        matchSlots.forEach { (home, away) ->
                            if (home.isNotBlank() && away.isNotBlank()) {
                                viewModel.adminCreateMatch(
                                    homeTeam = home.trim(),
                                    awayTeam = away.trim(),
                                    matchTime = "",
                                    stageName = stageName,
                                    pointsExact = exactVal,
                                    pointsWinnerAndGd = winnerGdVal,
                                    pointsWinnerOnly = winnerOnlyVal,
                                    pointsWrong = wrongVal,
                                    isPublished = true
                                )
                            }
                        }

                        viewModel.adminAddAnnouncement(
                            title = "باز شدن بازی‌های $stageName",
                            message = "بازی‌های $stageName باز شد، لطفاً پیش‌بینی خود را انجام دهید."
                        )

                        for (i in 0 until matchSlots.size) {
                            matchSlots[i] = Pair("", "")
                        }
                        showConfirmDialog = false
                        showAddMoreForStage = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen)
                ) {
                    Text("بله، ثبت و ارسال شود", color = StadiumDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("خیر", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ مدیریت بازی‌های مرحله",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SportGold
                        )

                        Box {
                            Button(
                                onClick = { showStageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = stageName, color = SportGold, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showStageDropdown,
                                onDismissRequest = { showStageDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                viewModel.stages.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(text = st, color = StadiumWhite) },
                                        onClick = {
                                            stageName = st
                                            showStageDropdown = false
                                            showAddMoreForStage = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    saveSuccessMsg?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SportLightGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = msg, color = SportLightGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (existingMatchesForStage.isNotEmpty() && !showAddMoreForStage) {
                        // Display registered matches with editable team names
                        Text(
                            text = "بازی‌های ثبت‌شده مرحله $stageName (${existingMatchesForStage.size} بازی):",
                            color = StadiumWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "💡 اسامی تیم‌ها قابل ویرایش است. با زدن دکمه «ذخیره تغییرات»، نام جدید برای تمام کاربران اعمال می‌شود.",
                            color = StadiumGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            existingMatchesForStage.forEachIndexed { index, match ->
                                val pair = editableTeams[match.id] ?: Pair(match.homeTeam, match.awayTeam)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = StadiumDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = SportGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.width(22.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        OutlinedTextField(
                                            value = pair.first,
                                            onValueChange = { newHome ->
                                                editableTeams[match.id] = Pair(newHome, pair.second)
                                            },
                                            label = { Text("میزبان", fontSize = 10.sp, color = StadiumGray) },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SportLightGreen,
                                                unfocusedBorderColor = StadiumCard
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )

                                        Text(text = "VS", color = SportGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                        OutlinedTextField(
                                            value = pair.second,
                                            onValueChange = { newAway ->
                                                editableTeams[match.id] = Pair(pair.first, newAway)
                                            },
                                            label = { Text("میهمان", fontSize = 10.sp, color = StadiumGray) },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SportLightGreen,
                                                unfocusedBorderColor = StadiumCard
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    existingMatchesForStage.forEach { m ->
                                        val pair = editableTeams[m.id]
                                        if (pair != null) {
                                            val h = pair.first.trim()
                                            val a = pair.second.trim()
                                            if (h.isNotBlank() && a.isNotBlank() && (h != m.homeTeam || a != m.awayTeam)) {
                                                viewModel.adminUpdateMatchTeams(m.id, h, a)
                                            }
                                        }
                                    }
                                    saveSuccessMsg = "تغییرات اسامی تیم‌ها با موفقیت بروزرسانی شد."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                                modifier = Modifier.weight(1.2f).height(46.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("✏️ ویرایش اسامی تیم‌ها", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showAddMoreForStage = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard, contentColor = StadiumWhite),
                                modifier = Modifier.weight(0.8f).height(46.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("➕ افزودن بازی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Display Creation Form for Stage Matches
                        if (existingMatchesForStage.isNotEmpty() && showAddMoreForStage) {
                            TextButton(
                                onClick = { showAddMoreForStage = false }
                            ) {
                                Text("⬅️ بازگشت به لیست بازی‌های ثبت‌شده ($stageName)", color = SportGold, fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "تعداد بازی‌های جدید این مرحله:", color = StadiumWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = matchCountInput,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    matchCountInput = digits
                                    val count = digits.toIntOrNull() ?: 0
                                    if (count > 0) {
                                        setSlotsCount(count)
                                    }
                                },
                                modifier = Modifier.width(75.dp).height(48.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    textAlign = TextAlign.Center,
                                    color = SportGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SportGold,
                                    unfocusedBorderColor = StadiumCard
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Text(
                            text = "تنظیم امتیازات بازی‌های این ثبت:",
                            color = StadiumWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = pointsExact,
                                onValueChange = { pointsExact = it.filter { c -> c.isDigit() || c == '-' } },
                                label = { Text("دقیق", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pointsWinnerAndGd,
                                onValueChange = { pointsWinnerAndGd = it.filter { c -> c.isDigit() || c == '-' } },
                                label = { Text("تفاضل", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pointsWinnerOnly,
                                onValueChange = { pointsWinnerOnly = it.filter { c -> c.isDigit() || c == '-' } },
                                label = { Text("برنده", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pointsWrong,
                                onValueChange = { pointsWrong = it.filter { c -> c.isDigit() || c == '-' } },
                                label = { Text("اشتباه", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "لیست ${matchSlots.size} تایی بازی‌ها ($stageName):",
                                color = SportGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    for (i in 0 until matchSlots.size) matchSlots[i] = Pair("", "")
                                }
                            ) {
                                Text("پاک‌سازی جایگاه‌ها", color = Color.Red, fontSize = 11.sp)
                            }
                        }

                        // Match Rows
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until matchSlots.size) {
                                val slot = matchSlots[i]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = StadiumDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${i + 1}",
                                            color = SportGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.width(22.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        OutlinedTextField(
                                            value = slot.first,
                                            onValueChange = { newHome ->
                                                matchSlots[i] = Pair(newHome, slot.second)
                                            },
                                            placeholder = { Text("تیم میزبان", fontSize = 11.sp, color = StadiumGray) },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SportLightGreen,
                                                unfocusedBorderColor = StadiumCard
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )

                                        Text(text = "VS", color = StadiumGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                        OutlinedTextField(
                                            value = slot.second,
                                            onValueChange = { newAway ->
                                                matchSlots[i] = Pair(slot.first, newAway)
                                            },
                                            placeholder = { Text("تیم میهمان", fontSize = 11.sp, color = StadiumGray) },
                                            modifier = Modifier.weight(1f).height(50.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SportLightGreen,
                                                unfocusedBorderColor = StadiumCard
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                showConfirmDialog = true
                            },
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StadiumDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت و ارسال همزمان ${matchSlots.size} بازی", color = StadiumDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "⚽ ثبت نتایج و مدیریت امتیازدهی بازی‌ها",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StadiumWhite,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (sortedMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "هیچ بازی ثبت‌شده‌ای موجود نیست.", color = StadiumGray)
                }
            }
        } else {
            items(sortedMatches) { match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StadiumSurface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(SportGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = match.stageName, color = SportGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            if (match.isFinished) {
                                Box(
                                    modifier = Modifier
                                        .background(SportLightGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "پایان‌یافته", color = SportLightGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (!match.isPublished) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "پیش‌نویس", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (match.isFinished) {
                            Text(
                                text = "${match.homeTeam} (${match.homeScore ?: 0}) - (${match.awayScore ?: 0}) ${match.awayTeam}",
                                fontWeight = FontWeight.Bold,
                                color = SportGold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(
                                text = "${match.homeTeam} VS ${match.awayTeam}",
                                fontWeight = FontWeight.Bold,
                                color = StadiumWhite,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (match.matchTime.isNotBlank()) {
                            Text(
                                text = match.matchTime,
                                color = StadiumGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onScoreMatchClick(match) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (match.isFinished) StadiumCard else SportGold,
                                contentColor = if (match.isFinished) SportGold else StadiumDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(if (match.isFinished) "ویرایش امتیاز" else "امتیازدهی", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyPredictionsTab(
    allMatches: List<MatchEntity>,
    viewModel: FootballPredictorViewModel
) {
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val allStageSubmissions by viewModel.allStageSubmissions.collectAsStateWithLifecycle()

    var trackingSelectedStage by remember { mutableStateOf("مرحله اول گروهی") }
    var showTrackingStageDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📋 رهگیری ثبت نهایی پیش‌بینی شرکت‌کنندگان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "انتخاب مرحله:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showTrackingStageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = trackingSelectedStage, color = SportGold, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showTrackingStageDropdown,
                                onDismissRequest = { showTrackingStageDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                viewModel.stages.forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(text = stage, color = StadiumWhite) },
                                        onClick = {
                                            trackingSelectedStage = stage
                                            showTrackingStageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeTrackingUsers = leaderboard.filter { it.isActive }.map { u ->
                            val hasSubmitted = allStageSubmissions.any { 
                                it.userId == u.id && it.stageName == trackingSelectedStage && it.isSubmitted 
                            }
                            Pair(u, hasSubmitted)
                        }.sortedWith(
                            compareBy<Pair<User, Boolean>> { it.second } // false (not submitted) first, true (submitted) second
                                .thenBy { it.first.displayName }
                        )

                        if (activeTrackingUsers.isEmpty()) {
                            Text(text = "هیچ کاربر فعالی یافت نشد.", color = StadiumGray, fontSize = 12.sp)
                        } else {
                            activeTrackingUsers.forEach { (u, hasSubmitted) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StadiumDark, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = u.displayName,
                                            color = StadiumWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "@${u.username}",
                                            color = StadiumGray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (hasSubmitted) {
                                        Box(
                                            modifier = Modifier
                                                .background(SportLightGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("ثبت نهایی شد ✅", color = SportLightGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("ثبت نکرده ❌", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "✍️ ثبت پیش‌بینی به جای کاربر (درخواست کمک)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    var selectedUserForBehalf by remember { mutableStateOf<User?>(null) }
                    var showUserDropdown by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "انتخاب کاربر:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showUserDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedUserForBehalf?.displayName ?: "انتخاب کاربر...",
                                    color = if (selectedUserForBehalf != null) SportGold else StadiumGray,
                                    fontSize = 12.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showUserDropdown,
                                onDismissRequest = { showUserDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                leaderboard.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(text = u.displayName, color = StadiumWhite) },
                                        onClick = {
                                            selectedUserForBehalf = u
                                            showUserDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedUserForBehalf != null) {
                        val targetUser = selectedUserForBehalf!!
                        
                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Text(
                            text = "۱. پیش‌بینی قهرمان و آقای گل به جای ${targetUser.displayName}",
                            color = SportGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        var champ1 by remember(targetUser) { mutableStateOf(targetUser.championFirstChoice ?: "") }
                        var champ2 by remember(targetUser) { mutableStateOf(targetUser.championSecondChoice ?: "") }
                        var topScorer by remember(targetUser) { mutableStateOf(targetUser.topScorerChoice ?: "") }

                        OutlinedTextField(
                            value = champ1,
                            onValueChange = { champ1 = it },
                            label = { Text("قهرمان اول") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = champ2,
                            onValueChange = { champ2 = it },
                            label = { Text("قهرمان دوم") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = topScorer,
                            onValueChange = { topScorer = it },
                            label = { Text("آقای گل") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.adminSubmitSpecialOnBehalf(targetUser.id, champ1, champ2, topScorer)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ثبت پیش‌بینی‌های ویژه به جای کاربر", color = StadiumDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Text(
                            text = "۲. پیش‌بینی بازی‌ها به جای ${targetUser.displayName}",
                            color = SportGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        var behalfStage by remember { mutableStateOf("مرحله اول گروهی") }
                        var showBehalfStageDropdown by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "انتخاب مرحله بازی‌ها:", color = StadiumWhite, fontSize = 12.sp)
                            Box {
                                Button(
                                    onClick = { showBehalfStageDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = behalfStage, color = SportGold, fontSize = 11.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                                }

                                DropdownMenu(
                                    expanded = showBehalfStageDropdown,
                                    onDismissRequest = { showBehalfStageDropdown = false },
                                    modifier = Modifier.background(StadiumSurface)
                                ) {
                                    viewModel.stages.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(text = st, color = StadiumWhite) },
                                            onClick = {
                                                behalfStage = st
                                                showBehalfStageDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        val behalfMatches = allMatches.filter { it.stageName == behalfStage && it.isPublished && !it.isFinished }
                        
                        if (behalfMatches.isEmpty()) {
                            Text(
                                text = "هیچ بازی فعال و منتشرشده‌ای در این مرحله وجود ندارد.",
                                color = StadiumGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            val targetUserPredictions by viewModel.allPredictions.collectAsStateWithLifecycle()
                            val predictionsByMatch = remember(targetUserPredictions, targetUser) {
                                targetUserPredictions.filter { it.userId == targetUser.id }.associateBy { it.matchId }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                behalfMatches.forEach { match ->
                                    val existingPred = predictionsByMatch[match.id]
                                    var homeScoreInput by remember(match, existingPred) { mutableStateOf(existingPred?.predictedHomeScore?.toString() ?: "0") }
                                    var awayScoreInput by remember(match, existingPred) { mutableStateOf(existingPred?.predictedAwayScore?.toString() ?: "0") }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StadiumDark, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text(
                                                text = "${match.homeTeam} - ${match.awayTeam}",
                                                color = StadiumWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(text = match.matchTime, color = StadiumGray, fontSize = 10.sp)
                                        }

                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = homeScoreInput,
                                                onValueChange = { homeScoreInput = it.filter { char -> char.isDigit() } },
                                                modifier = Modifier.width(45.dp).height(48.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = StadiumWhite, fontSize = 13.sp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            Text("-", color = StadiumWhite)
                                            OutlinedTextField(
                                                value = awayScoreInput,
                                                onValueChange = { awayScoreInput = it.filter { char -> char.isDigit() } },
                                                modifier = Modifier.width(45.dp).height(48.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = StadiumWhite, fontSize = 13.sp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            Button(
                                                onClick = {
                                                    val home = homeScoreInput.toIntOrNull() ?: 0
                                                    val away = awayScoreInput.toIntOrNull() ?: 0
                                                    viewModel.adminSubmitOnBehalf(targetUser.id, match.id, home, away)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                                                contentPadding = PaddingValues(horizontal = 6.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("ثبت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.adminSubmitStageSubmissionOnBehalf(targetUser.id, behalfStage)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SportGold.copy(alpha = 0.8f), contentColor = StadiumDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ثبت نهایی پیش‌بینی‌های $behalfStage کاربر", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndAnnouncementsTab(viewModel: FootballPredictorViewModel) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allAnnouncements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var newAnnouncementTitle by remember { mutableStateOf("") }
    var newAnnouncementMsg by remember { mutableStateOf("") }

    // Targeted Announcements State
    var sendToAll by remember { mutableStateOf(true) }
    var selectedUserIds by remember { mutableStateOf(setOf<Int>()) }

    // Banner Image URL State
    var bannerUrlInput by remember { mutableStateOf("") }

    // Gallery Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val destFile = File(context.filesDir, "custom_banner_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(destFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                val localPath = destFile.absolutePath
                bannerUrlInput = localPath
                viewModel.updateBannerImageUrl(localPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Season Reset Confirmation Dialog State
    var showConfirmResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appSettings) {
        val s = appSettings
        if (s != null) {
            bannerUrlInput = s.bannerImageUrl ?: ""
        }
    }

    val activeUsers = remember(leaderboard) { leaderboard.filter { it.isActive } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Banner Image Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🖼️ تصویر بنر بالای صفحات مسابقات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    Text(
                        text = "می‌توانید مستقیماً عکسی از داخل حافظه/گالری گوشی خود انتخاب و آپلود کنید یا آدرس اینترنتی (URL) بنر را وارد نمایید.",
                        color = StadiumWhite,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen, contentColor = StadiumDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📱 انتخاب و آپلود عکس از گالری گوشی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                    OutlinedTextField(
                        value = bannerUrlInput,
                        onValueChange = { bannerUrlInput = it },
                        label = { Text("آدرس یا مسیر بنر (https://... یا مسیر فایل)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportGold)
                    )

                    Text("نمونه‌های پیشنهادی جهت تست سریع:", color = StadiumGray, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { bannerUrlInput = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=1200" },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SportGold),
                            border = BorderStroke(1.dp, SportGold.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("استادیوم شب", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { bannerUrlInput = "https://images.unsplash.com/photo-1522778119026-d647f0596c20?w=1200" },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SportGold),
                            border = BorderStroke(1.dp, SportGold.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("هیجان فوتبال", fontSize = 10.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.updateBannerImageUrl(bannerUrlInput.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره آدرس بنر", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 3: Announcements with Target Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📣 ایجاد و مدیریت اطلاعیه‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    OutlinedTextField(
                        value = newAnnouncementTitle,
                        onValueChange = { newAnnouncementTitle = it },
                        label = { Text("عنوان اطلاعیه") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAnnouncementMsg,
                        onValueChange = { newAnnouncementMsg = it },
                        label = { Text("متن کامل اطلاعیه") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target Selector: All Active Users vs Specific Users
                    Text("دریافت‌کنندگان اطلاعیه:", color = StadiumWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { sendToAll = true }
                        ) {
                            RadioButton(selected = sendToAll, onClick = { sendToAll = true }, colors = RadioButtonDefaults.colors(selectedColor = SportGold))
                            Text("همه کاربران فعال", color = StadiumWhite, fontSize = 12.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { sendToAll = false }
                        ) {
                            RadioButton(selected = !sendToAll, onClick = { sendToAll = false }, colors = RadioButtonDefaults.colors(selectedColor = SportGold))
                            Text("انتخاب کاربران خاص", color = StadiumWhite, fontSize = 12.sp)
                        }
                    }

                    if (!sendToAll) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StadiumDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("کاربران فعال دریافت‌کننده را انتخاب کنید:", color = SportGold, fontSize = 11.sp)
                                activeUsers.forEach { user ->
                                    val isSelected = selectedUserIds.contains(user.id)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedUserIds = if (isSelected) selectedUserIds - user.id else selectedUserIds + user.id
                                            }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedUserIds = if (checked == true) selectedUserIds + user.id else selectedUserIds - user.id
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = SportGold)
                                        )
                                        Text(user.username, color = StadiumWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    val canSend = newAnnouncementTitle.isNotBlank() && newAnnouncementMsg.isNotBlank() && (sendToAll || selectedUserIds.isNotEmpty())

                    Button(
                        onClick = {
                            if (canSend) {
                                val targetStr = if (sendToAll) "ALL" else selectedUserIds.joinToString(",")
                                viewModel.adminAddAnnouncement(newAnnouncementTitle, newAnnouncementMsg, targetStr)
                                newAnnouncementTitle = ""
                                newAnnouncementMsg = ""
                                selectedUserIds = emptySet()
                            }
                        },
                        enabled = canSend,
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ارسال اطلاعیه", color = StadiumDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (allAnnouncements.isNotEmpty()) {
                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("اطلاعیه‌های فعلی (${allAnnouncements.size})", color = StadiumWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.adminClearAnnouncements() }) {
                                Text("پاکسازی همه", color = Color.Red, fontSize = 11.sp)
                            }
                        }

                        allAnnouncements.forEach { ann ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StadiumDark, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(ann.title, color = SportGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(ann.message, color = StadiumWhite, fontSize = 11.sp)
                                Text("گیرندگان: ${ann.targetUserIds ?: "ALL"}", color = StadiumGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Season Reset (Keep Users)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⚠️ بازنشانی پایگاه داده دوره مسابقات (حفظ حساب کاربران)",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "با انجام این عملیات، تمام بازی‌ها، پیش‌بینی‌ها، مراحل ثبت‌شده، آیتم‌های امتیاز تشویقی و اطلاعیه‌های دوره جاری کاملاً پاک شده و امتیاز کل همه کاربران صفر می‌شود، اما اکانت و اطلاعات کاربران حفظ باقی خواهد ماند.",
                        color = StadiumWhite,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { showConfirmResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("پاکسازی و بازنشانی این دوره (حفظ کاربران)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = StadiumCard, thickness = 0.5.dp)

                    Button(
                        onClick = { viewModel.seedMockData() },
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumCard, contentColor = StadiumWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ریست کامل اپلیکیشن به داده‌های نمونه اولیه", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text("تأیید بازنشانی دوره مسابقات", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "آیا اطمینان دارید؟ تمام مسابقات، پیش‌بینی‌ها و امتیازات این دوره حذف خواهند شد اما کاربران شما باقی می‌مانند.",
                    color = StadiumWhite,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetTournamentSeasonKeepUsers()
                        showConfirmResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("بله، دوره جدید را آغاز کن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) {
                    Text("انصراف", color = StadiumGray)
                }
            },
            containerColor = StadiumSurface
        )
    }
}

/* Unused Legacy Admin Code Disabled */
/*
@Composable
private fun LegacyAdminSection(
    viewModel: FootballPredictorViewModel,
    leaderboard: List<User>,
    allMatches: List<MatchEntity>,
    allStageSubmissions: List<StageSubmission>,
    onScoreMatchClick: (MatchEntity) -> Unit
) {
    var showPublishStageDropdown by remember { mutableStateOf(false) }
    var publishSelectedStage by remember { mutableStateOf("مرحله اول گروهی") }
    var draftMatchesCount by remember { mutableStateOf(0) }
    var showTrackingStageDropdown by remember { mutableStateOf(false) }
    var trackingSelectedStage by remember { mutableStateOf("مرحله اول گروهی") }
    var homeTeam by remember { mutableStateOf("") }
    var awayTeam by remember { mutableStateOf("") }
    var matchTime by remember { mutableStateOf("") }
    var stageName by remember { mutableStateOf("مرحله اول گروهی") }
    var pointsExact by remember { mutableStateOf("5") }
    var pointsWinnerAndGd by remember { mutableStateOf("3") }
    var pointsWinnerOnly by remember { mutableStateOf("2") }
    var pointsWrong by remember { mutableStateOf("0") }
    var isPublishedOnCreation by remember { mutableStateOf(false) }
    var showStageDropdown by remember { mutableStateOf(false) }
    val activeMatches = allMatches.filter { !it.isFinished }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section A: Publish and Submit Stage Games Batch (MANDATORY REQUIREMENT)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📢 انتشار گروهی بازی‌های مرحله (بچ)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    Text(
                        text = "بازی‌های تعریف‌شده را به صورت پیش‌نویس ذخیره کرده و با دکمه زیر برای همه کاربران فعال نمایید و نوتیفیکیشن بفرستید.",
                        color = StadiumGray,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "مرحله جهت فعال‌سازی:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showPublishStageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = publishSelectedStage, color = SportGold, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showPublishStageDropdown,
                                onDismissRequest = { showPublishStageDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                viewModel.stages.forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(text = stage, color = StadiumWhite) },
                                        onClick = {
                                            publishSelectedStage = stage
                                            showPublishStageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(StadiumDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "بازی‌های پیش‌نویس این مرحله: $draftMatchesCount",
                                color = if (draftMatchesCount > 0) SportGold else StadiumGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.adminPublishStageMatches(publishSelectedStage)
                        },
                        enabled = draftMatchesCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("انتشار رسمی مسابقات مرحله (ارسال نوتیفیکیشن)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section B: User Submission Tracking Status List (MANDATORY REQUIREMENT)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📋 رهگیری ثبت نهایی پیش‌بینی شرکت‌کنندگان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    Text(
                        text = "با انتخاب هر مرحله، لیست کاربران را به همراه علامت وضعیت پیش‌بینی‌هایشان مشاهده کنید.",
                        color = StadiumGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "انتخاب مرحله رهگیری:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showTrackingStageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = trackingSelectedStage, color = SportGold, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showTrackingStageDropdown,
                                onDismissRequest = { showTrackingStageDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                viewModel.stages.forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(text = stage, color = StadiumWhite) },
                                        onClick = {
                                            trackingSelectedStage = stage
                                            showTrackingStageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                    // Table/List of users
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeTrackingUsers = leaderboard.filter { it.isActive }.map { u ->
                            val hasSubmitted = allStageSubmissions.any { 
                                it.userId == u.id && it.stageName == trackingSelectedStage && it.isSubmitted 
                            }
                            Pair(u, hasSubmitted)
                        }.sortedWith(
                            compareBy<Pair<User, Boolean>> { it.second } // false (not submitted) first, true (submitted) second
                                .thenBy { it.first.displayName }
                        )

                        if (activeTrackingUsers.isEmpty()) {
                            Text(text = "هیچ کاربر فعالی یافت نشد.", color = StadiumGray, fontSize = 12.sp)
                        } else {
                            activeTrackingUsers.forEach { (u, hasSubmitted) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StadiumDark, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = u.displayName,
                                            color = StadiumWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "@${u.username}",
                                            color = StadiumGray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (hasSubmitted) {
                                            Box(
                                                modifier = Modifier
                                                    .background(SportLightGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = SportLightGreen, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("ثبت نهایی شد ✅", color = SportLightGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("ثبت نکرده ❌", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section C: Special Champion and Top Scorer Admin scoring (MANDATORY REQUIREMENT)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏆 ثبت نتایج نهایی قهرمان و آقای گل مسابقات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    Text(
                        text = "پس از مشخص شدن نتایج واقعی تورنمنت، اسامی واقعی را اینجا ثبت کنید تا امتیازهای کاربران به طور خودکار محاسبه و ذخیره گردند.",
                        color = StadiumGray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = champActual,
                        onValueChange = { champActual = it },
                        label = { Text("تیم قهرمان واقعی (۳۰+ یا ۱۵+ امتیاز بر اساس اولویت اول/دوم کاربر)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportLightGreen,
                            focusedLabelColor = SportLightGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = topScorerActual,
                        onValueChange = { topScorerActual = it },
                        label = { Text("بازیکن آقای گل واقعی (۳۰+ امتیاز)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportLightGreen,
                            focusedLabelColor = SportLightGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            val currentSettings = appSettings ?: AppSettings(id = 1)
                            val updatedSettings = currentSettings.copy(
                                actualChampion = champActual.trim(),
                                actualTopScorer = topScorerActual.trim()
                            )
                            viewModel.adminSaveAppSettings(updatedSettings)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره نتایج واقعی و بازمحاسبه امتیازات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section D: Create a Match Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تعریف مسابقه جدید (Scheduled New Match)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )

                    // Stage Selector Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "مرحله رقابت:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showStageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = stageName, color = SportGold, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showStageDropdown,
                                onDismissRequest = { showStageDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                viewModel.stages.forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(text = stage, color = StadiumWhite) },
                                        onClick = {
                                            stageName = stage
                                            showStageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = homeTeam,
                        onValueChange = { homeTeam = it },
                        label = { Text("تیم میزبان (Home Team)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportLightGreen,
                            focusedLabelColor = SportLightGreen,
                            unfocusedLabelColor = StadiumGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = awayTeam,
                        onValueChange = { awayTeam = it },
                        label = { Text("تیم میهمان (Away Team)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportLightGreen,
                            focusedLabelColor = SportLightGreen,
                            unfocusedLabelColor = StadiumGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = matchTime,
                        onValueChange = { matchTime = it },
                        label = { Text("زمان برگزاری (مثلا: فردا ساعت ۲۱)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportLightGreen,
                            focusedLabelColor = SportLightGreen,
                            unfocusedLabelColor = StadiumGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 4 custom points fields
                    Text(
                        text = "پیکربندی امتیازات این مسابقه (۴ نوع امتیاز):",
                        style = MaterialTheme.typography.labelMedium,
                        color = SportGold,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pointsExact,
                            onValueChange = { pointsExact = it },
                            label = { Text("نتیجه دقیق", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pointsWinnerAndGd,
                            onValueChange = { pointsWinnerAndGd = it },
                            label = { Text("تفاضل گل", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pointsWinnerOnly,
                            onValueChange = { pointsWinnerOnly = it },
                            label = { Text("برد خالی", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pointsWrong,
                            onValueChange = { pointsWrong = it },
                            label = { Text("پیش‌بینی غلط", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = !isPublishedOnCreation,
                            onCheckedChange = { isPublishedOnCreation = !it },
                            colors = CheckboxDefaults.colors(checkedColor = SportGold)
                        )
                        Text(
                            text = "ذخیره به عنوان پیش‌نویس (فعال‌سازی در بچ ۲۴ تایی)",
                            color = StadiumWhite,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (homeTeam.isNotBlank() && awayTeam.isNotBlank() && matchTime.isNotBlank()) {
                                val exactVal = pointsExact.toIntOrNull() ?: 5
                                val winnerGdVal = pointsWinnerAndGd.toIntOrNull() ?: 3
                                val winnerOnlyVal = pointsWinnerOnly.toIntOrNull() ?: 2
                                val wrongVal = pointsWrong.toIntOrNull() ?: 0
                                viewModel.adminCreateMatch(
                                    homeTeam = homeTeam,
                                    awayTeam = awayTeam,
                                    matchTime = matchTime,
                                    stageName = stageName,
                                    pointsExact = exactVal,
                                    pointsWinnerAndGd = winnerGdVal,
                                    pointsWinnerOnly = winnerOnlyVal,
                                    pointsWrong = wrongVal,
                                    isPublished = !isPublishedOnCreation
                                )
                                // Clear inputs
                                homeTeam = ""
                                awayTeam = ""
                                matchTime = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت و زمان‌بندی بازی", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section C: Scheduled matches list for editing and scoring
        item {
            Text(
                text = "ثبت نتایج و محاسبه امتیاز بازی‌های تعریف‌شده",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StadiumWhite,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (activeMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "هیچ بازی فعال ثبت‌نشده‌ای موجود نیست.", color = StadiumGray)
                }
            }
        } else {
            items(activeMatches) { match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StadiumSurface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(SportGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = match.stageName, color = SportGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            if (!match.isPublished) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "پیش‌نویس", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${match.homeTeam} VS ${match.awayTeam}",
                            fontWeight = FontWeight.Bold,
                            color = StadiumWhite,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = match.matchTime,
                            color = SportGold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onScoreMatchClick(match) },
                            colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("امتیازدهی", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { viewModel.adminDeleteMatch(match) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    }
                }
            }
        }

        // Section E: Submit Predictions on Behalf of User
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "✍️ ثبت پیش‌بینی به جای کاربر (درخواست کمک)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )
                    
                    Text(
                        text = "اگر کاربری نتوانست وارد سیستم شود، کاربر را انتخاب کرده و به جای او پیش‌بینی بازی‌ها و یا قهرمان و آقای گل را ثبت کنید.",
                        color = StadiumGray,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )

                    // User Selector
                    var selectedUserForBehalf by remember { mutableStateOf<User?>(null) }
                    var showUserDropdown by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "انتخاب کاربر:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showUserDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedUserForBehalf?.displayName ?: "انتخاب کاربر...",
                                    color = if (selectedUserForBehalf != null) SportGold else StadiumGray,
                                    fontSize = 12.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showUserDropdown,
                                onDismissRequest = { showUserDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                leaderboard.filter { !it.isAdmin }.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(text = u.displayName, color = StadiumWhite) },
                                        onClick = {
                                            selectedUserForBehalf = u
                                            showUserDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedUserForBehalf != null) {
                        val targetUser = selectedUserForBehalf!!
                        
                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Text(
                            text = "۱. پیش‌بینی قهرمان و آقای گل به جای ${targetUser.displayName}",
                            color = SportGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        var champ1Behalf by remember(targetUser) { mutableStateOf(targetUser.championFirstChoice ?: "") }
                        var champ2Behalf by remember(targetUser) { mutableStateOf(targetUser.championSecondChoice ?: "") }
                        var topScorerBehalf by remember(targetUser) { mutableStateOf(targetUser.topScorerChoice ?: "") }

                        OutlinedTextField(
                            value = champ1Behalf,
                            onValueChange = { champ1Behalf = it },
                            label = { Text("قهرمان اول (۳۰+ امتیاز)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = champ2Behalf,
                            onValueChange = { champ2Behalf = it },
                            label = { Text("قهرمان دوم (۱۵+ امتیاز)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = topScorerBehalf,
                            onValueChange = { topScorerBehalf = it },
                            label = { Text("بازیکن آقای گل (۳۰+ امتیاز)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportLightGreen,
                                focusedLabelColor = SportLightGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.adminSubmitSpecialOnBehalf(
                                    userId = targetUser.id,
                                    championFirstChoice = champ1Behalf.trim(),
                                    championSecondChoice = champ2Behalf.trim(),
                                    topScorerChoice = topScorerBehalf.trim()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen, contentColor = StadiumDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ذخیره قهرمان/آقای گل کاربر", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        Text(
                            text = "۲. پیش‌بینی مسابقات به جای ${targetUser.displayName}",
                            color = SportGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        // Select a stage to view its matches for behalf prediction
                        var behalfStage by remember { mutableStateOf("مرحله اول گروهی") }
                        var showBehalfStageDropdown by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "مرحله بازی‌ها:", color = StadiumWhite, fontSize = 12.sp)
                            Box {
                                Button(
                                    onClick = { showBehalfStageDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = behalfStage, color = SportGold, fontSize = 11.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                                }

                                DropdownMenu(
                                    expanded = showBehalfStageDropdown,
                                    onDismissRequest = { showBehalfStageDropdown = false },
                                    modifier = Modifier.background(StadiumSurface)
                                ) {
                                    viewModel.stages.forEach { stage ->
                                        DropdownMenuItem(
                                            text = { Text(text = stage, color = StadiumWhite) },
                                            onClick = {
                                                behalfStage = stage
                                                showBehalfStageDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Get matches of selected behalf stage
                        val behalfMatches = allMatches.filter { it.stageName == behalfStage && it.isPublished && !it.isFinished }
                        
                        if (behalfMatches.isEmpty()) {
                            Text(
                                text = "هیچ بازی فعال و منتشرشده‌ای در این مرحله وجود ندارد.",
                                color = StadiumGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // Fetch predictions for target user to prefill
                            val targetUserPredictions by viewModel.allPredictions.collectAsStateWithLifecycle()
                            val predictionsByMatch = remember(targetUserPredictions, targetUser) {
                                targetUserPredictions.filter { it.userId == targetUser.id }.associateBy { it.matchId }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                behalfMatches.forEach { match ->
                                    val existingPred = predictionsByMatch[match.id]
                                    var homeScoreInput by remember(match, existingPred) { mutableStateOf(existingPred?.predictedHomeScore?.toString() ?: "0") }
                                    var awayScoreInput by remember(match, existingPred) { mutableStateOf(existingPred?.predictedAwayScore?.toString() ?: "0") }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StadiumDark, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text(
                                                text = "${match.homeTeam} - ${match.awayTeam}",
                                                color = StadiumWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(text = match.matchTime, color = StadiumGray, fontSize = 10.sp)
                                        }

                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = homeScoreInput,
                                                onValueChange = { homeScoreInput = it.filter { char -> char.isDigit() } },
                                                modifier = Modifier.width(45.dp).height(48.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = StadiumWhite, fontSize = 13.sp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            Text("-", color = StadiumWhite)
                                            OutlinedTextField(
                                                value = awayScoreInput,
                                                onValueChange = { awayScoreInput = it.filter { char -> char.isDigit() } },
                                                modifier = Modifier.width(45.dp).height(48.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = StadiumWhite, fontSize = 13.sp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(4.dp)
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            Button(
                                                onClick = {
                                                    val home = homeScoreInput.toIntOrNull() ?: 0
                                                    val away = awayScoreInput.toIntOrNull() ?: 0
                                                    viewModel.adminSubmitOnBehalf(targetUser.id, match.id, home, away)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                                                contentPadding = PaddingValues(horizontal = 6.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("ثبت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.adminSubmitStageSubmissionOnBehalf(targetUser.id, behalfStage)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SportGold.copy(alpha = 0.8f), contentColor = StadiumDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ثبت نهایی پیش‌بینی‌های ${behalfStage} کاربر", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section F: Disciplinary Penalty Points Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SportGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚖️ ثبت امتیاز منفی انضباطی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )
                    
                    Text(
                        text = "در این بخش می‌توانید به صورت دستی برای کاربران امتیاز منفی (جریمه انضباطی) ثبت کنید. این امتیاز از امتیاز کل آنها کسر خواهد شد.",
                        color = StadiumGray,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )

                    // User Selector
                    var selectedUserForPenalty by remember { mutableStateOf<User?>(null) }
                    var showPenaltyUserDropdown by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "انتخاب کاربر:", color = StadiumWhite, fontSize = 13.sp)
                        Box {
                            Button(
                                onClick = { showPenaltyUserDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = selectedUserForPenalty?.displayName ?: "انتخاب کاربر...",
                                    color = if (selectedUserForPenalty != null) SportGold else StadiumGray,
                                    fontSize = 12.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SportGold)
                            }

                            DropdownMenu(
                                expanded = showPenaltyUserDropdown,
                                onDismissRequest = { showPenaltyUserDropdown = false },
                                modifier = Modifier.background(StadiumSurface)
                            ) {
                                leaderboard.filter { !it.isAdmin }.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(text = u.displayName, color = StadiumWhite) },
                                        onClick = {
                                            selectedUserForPenalty = u
                                            showPenaltyUserDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedUserForPenalty != null) {
                        val targetUser = selectedUserForPenalty!!
                        
                        HorizontalDivider(color = StadiumCard, thickness = 1.dp)

                        var penaltyInput by remember(targetUser) { mutableStateOf(targetUser.penaltyPoints.toString()) }

                        OutlinedTextField(
                            value = penaltyInput,
                            onValueChange = { penaltyInput = it.filter { char -> char.isDigit() } },
                            label = { Text("میزان امتیاز منفی (جریمه)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Red,
                                focusedLabelColor = Color.Red
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                val pts = penaltyInput.toIntOrNull() ?: 0
                                viewModel.adminUpdatePenaltyPoints(targetUser.id, pts)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f), contentColor = StadiumWhite),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ثبت جریمه انضباطی برای ${targetUser.displayName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
*/

@Composable
fun AuthDialog(
    authError: String?,
    allUsers: List<User>,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    currentUser: User?
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🔐 ورود به اپلیکیشن Footballia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (authError != null) {
                    Text(
                        text = authError,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("نام کاربری انگلیسی (Username)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportLightGreen,
                        focusedLabelColor = SportLightGreen
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("رمز عبور (Password)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportLightGreen,
                        focusedLabelColor = SportLightGreen
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onLogin(username, password)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ورود به حساب",
                            color = StadiumDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (currentUser != null) {
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("خروج", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "ℹ️ ثبت نام کاربران جدید فقط توسط مدیر سیستم از طریق پنل مدیریت امکان‌پذیر می‌باشد.",
                    color = StadiumGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PredictionInputDialog(
    match: MatchEntity,
    existingPrediction: Prediction?,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int) -> Unit
) {
    var homeScore by remember { mutableStateOf(existingPrediction?.predictedHomeScore ?: 0) }
    var awayScore by remember { mutableStateOf(existingPrediction?.predictedAwayScore ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "پیش‌بینی نتیجه مسابقه",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportGold
                )

                Text(
                    text = "${match.homeTeam} - ${match.awayTeam}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = StadiumWhite
                )

                Divider(color = StadiumCard, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Score Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = match.homeTeam, color = StadiumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        var homeText by remember(homeScore) { mutableStateOf(homeScore.toString()) }
                        OutlinedTextField(
                            value = homeText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(2)
                                homeText = digits
                                homeScore = digits.toIntOrNull() ?: 0
                            },
                            modifier = Modifier.width(60.dp).height(50.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = StadiumWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportLightGreen, unfocusedBorderColor = StadiumCard),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Text(text = "vs", color = SportGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    // Away Score Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = match.awayTeam, color = StadiumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        var awayText by remember(awayScore) { mutableStateOf(awayScore.toString()) }
                        OutlinedTextField(
                            value = awayText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(2)
                                awayText = digits
                                awayScore = digits.toIntOrNull() ?: 0
                            },
                            modifier = Modifier.width(60.dp).height(50.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = StadiumWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportLightGreen, unfocusedBorderColor = StadiumCard),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Button(
                    onClick = { onSubmit(homeScore, awayScore) },
                    colors = ButtonDefaults.buttonColors(containerColor = SportLightGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ثبت پیش‌بینی", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminScoreMatchDialog(
    match: MatchEntity,
    predictionsWithUsers: List<MatchPredictionWithUser>,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, Map<Int, Int>) -> Unit
) {
    var homeScore by remember { mutableStateOf(0) }
    var awayScore by remember { mutableStateOf(0) }

    // Map of Prediction ID to custom points to award
    val customPointsMap = remember { mutableStateMapOf<Int, Int>() }

    // Recalculate based on 4-tier customized rules when home/away score changes
    LaunchedEffect(homeScore, awayScore, predictionsWithUsers) {
        for (predUser in predictionsWithUsers) {
            val pred = predUser.prediction
            
            // 1. Exact Score Match
            val calc = if (homeScore == pred.predictedHomeScore && awayScore == pred.predictedAwayScore) {
                match.pointsExactScore
            } else {
                val actualOutcome = homeScore.compareTo(awayScore)
                val predOutcome = pred.predictedHomeScore.compareTo(pred.predictedAwayScore)
                
                if (actualOutcome == predOutcome) {
                    val actualGd = homeScore - awayScore
                    val predGd = pred.predictedHomeScore - pred.predictedAwayScore
                    
                    if (actualGd == predGd) {
                        match.pointsWinnerAndGd // Correct Winner and Goal Difference
                    } else {
                        match.pointsWinnerOnly // Correct Winner Only
                    }
                } else {
                    match.pointsWrong // Wrong prediction (can be negative score)
                }
            }
            customPointsMap[pred.id] = calc
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ثبت نتیجه نهایی و تعیین امتیازات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportGold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = StadiumGray
                        )
                    }
                }

                Text(
                    text = "${match.homeTeam} - ${match.awayTeam}",
                    fontWeight = FontWeight.Bold,
                    color = StadiumWhite
                )

                // Enter Final Scores Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StadiumDark, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Score Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "نتیجه ${match.homeTeam}", color = StadiumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        var homeText by remember(homeScore) { mutableStateOf(homeScore.toString()) }
                        OutlinedTextField(
                            value = homeText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(2)
                                homeText = digits
                                homeScore = digits.toIntOrNull() ?: 0
                            },
                            modifier = Modifier.width(60.dp).height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = SportGold, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportGold, unfocusedBorderColor = StadiumCard),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Text(text = "-", color = StadiumGray, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    // Away Score Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "نتیجه ${match.awayTeam}", color = StadiumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        var awayText by remember(awayScore) { mutableStateOf(awayScore.toString()) }
                        OutlinedTextField(
                            value = awayText,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(2)
                                awayText = digits
                                awayScore = digits.toIntOrNull() ?: 0
                            },
                            modifier = Modifier.width(60.dp).height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = SportGold, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportGold, unfocusedBorderColor = StadiumCard),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Divider(color = StadiumCard, thickness = 1.dp)

                // User Predictions List with customizable points to award
                Text(
                    text = "تعیین امتیاز کاربران (بر اساس فرمول یا به دلخواه):",
                    style = MaterialTheme.typography.labelMedium,
                    color = StadiumGray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                if (predictionsWithUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "هیچ کاربری پیش‌بینی برای این بازی ثبت نکرده است.", color = StadiumGray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(predictionsWithUsers) { predUser ->
                            val pred = predUser.prediction
                            val user = predUser.user
                            val pointVal = customPointsMap[pred.id] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StadiumCard, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = user.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = StadiumWhite,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "پیش‌بینی: ${pred.predictedHomeScore} - ${pred.predictedAwayScore}",
                                        color = SportGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Interactive point editor for admin to edit/fine tune score manually
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "امتیاز:", color = StadiumWhite, fontSize = 11.sp)
                                    
                                    var pointText by remember(pointVal) { mutableStateOf(pointVal.toString()) }
                                    OutlinedTextField(
                                        value = pointText,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() || it == '-' }.take(3)
                                            pointText = digits
                                            val parsed = digits.toIntOrNull() ?: 0
                                            customPointsMap[pred.id] = parsed
                                        },
                                        modifier = Modifier.width(55.dp).height(42.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = SportLightGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SportLightGreen, unfocusedBorderColor = StadiumCard),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onSubmit(homeScore, awayScore, customPointsMap.toMap())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SportGold, contentColor = StadiumDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت نتایج و واریز امتیازها", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PublicPredictionsScreen(
    allMatches: List<MatchEntity>,
    allPredictions: List<Prediction>,
    leaderboard: List<User>,
    stages: List<String>,
    allStageSubmissions: List<StageSubmission>,
    appSettings: AppSettings?,
    currentUser: User?,
    viewModel: FootballPredictorViewModel
) {
    var selectedStage by remember { mutableStateOf("مرحله اول گروهی") }

    val isStagePublished = remember(appSettings, selectedStage) {
        appSettings?.publishedPredictionStages?.split(",")?.map { it.trim() }?.contains(selectedStage) == true
    }

    val showPredictions = isStagePublished || (currentUser?.isAdmin == true)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stage Selector
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    text = "مشاهده پیش‌بینی همگانی مرحله:",
                    color = StadiumWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stages) { stage ->
                        val isSelected = selectedStage == stage
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) SportGold else StadiumSurface,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SportGold else StadiumCard,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedStage = stage }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stage,
                                color = if (isSelected) StadiumDark else StadiumWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Admin Toggle / Publish Control
        if (currentUser?.isAdmin == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SportGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "📢 مدیریت انتشار پیش‌بینی‌های عمومی",
                                color = SportGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isStagePublished) 
                                    "پیش‌بینی‌ها برای همه منتشر شده است (همه می‌توانند پیش‌بینی بقیه را ببینند)." 
                                    else "پیش‌بینی‌ها هم‌اکنون فقط برای مدیر قابل مشاهده است (بقیه قفل می‌بینند).",
                                color = StadiumGray,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.adminToggleStagePredictionPublish(selectedStage, !isStagePublished)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStagePublished) Color.Red else SportLightGreen,
                                contentColor = StadiumDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isStagePublished) "لغو انتشار" else "انتشار",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        if (!showPredictions) {
            // Locked view for non-admins when predictions not published
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StadiumCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل",
                            tint = SportGold,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "پیش‌بینی‌های همگانی قفل است",
                            color = StadiumWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "پیش‌بینی‌های ثبت‌شده سایر کاربران برای $selectedStage هنوز توسط مدیریت منتشر نشده است. پس از اتمام زمان ثبت پیش‌بینی‌ها و فیکس شدن همه چیز، مدیر دکمه انتشار را خواهد زد تا بتوانید پیش‌بینی بقیه را با مال خود مقایسه کنید.",
                            color = StadiumGray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        } else {
            // Stage Matches to display
            val stageMatches = allMatches.filter { it.stageName == selectedStage && it.isPublished }
            val nonAdminUsers = leaderboard.filter { !it.isAdmin }

            if (nonAdminUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "هیچ کاربری ثبت‌نام نکرده است.", color = StadiumGray)
                    }
                }
            } else if (stageMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "هیچ مسابقه منتشرشده‌ای برای این مرحله تعریف نشده است.", color = StadiumGray)
                    }
                }
            } else {
                items(nonAdminUsers) { user ->
                    val userSubmitted = allStageSubmissions.any { 
                        it.userId == user.id && it.stageName == selectedStage && it.isSubmitted 
                    }
                    val userPredictionsOfSelectedUser = allPredictions.filter { it.userId == user.id }
                    val predictionsByMatch = userPredictionsOfSelectedUser.associateBy { it.matchId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (userSubmitted) SportLightGreen.copy(alpha = 0.3f) else StadiumCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // User card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(SportGold.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.username.take(1).uppercase(),
                                            color = SportGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = user.username,
                                            color = StadiumWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${user.totalPoints} امتیاز کل",
                                            color = SportGold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (userSubmitted) {
                                    Box(
                                        modifier = Modifier
                                            .background(SportLightGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "ثبت نهایی شده",
                                            color = SportLightGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(StadiumCard, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "پیش‌نویس / غیرنهایی",
                                            color = StadiumGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = StadiumCard, 
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            // Predictions List
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stageMatches.forEach { match ->
                                    val pred = predictionsByMatch[match.id]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StadiumDark, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (pred != null) {
                                            Text(
                                                text = "${match.homeTeam}  ${pred.predictedHomeScore} - ${pred.predictedAwayScore}  ${match.awayTeam}",
                                                color = SportGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Text(
                                                text = "${match.homeTeam} - ${match.awayTeam} (ثبت نشده)",
                                                color = StadiumGray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (match.isFinished && pred != null) {
                                            val earned = pred.pointsEarned ?: 0
                                            Text(
                                                text = "واقعی: ${match.homeScore}-${match.awayScore} (${if (earned >= 0) "+$earned" else "$earned"})",
                                                color = if (earned > 0) SportLightGreen else StadiumGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
