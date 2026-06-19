package com.example.alfcapp

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alfcapp.screens.BibleScreen // Ensure you have implemented this screen
import com.example.alfcapp.screens.NotesScreen
import com.example.alfcapp.screens.GiveScreen
import com.example.alfcapp.screens.MediaScreen
import com.example.alfcapp.screens.YouTubePlayerScreen
import com.example.alfcapp.screens.AdminLiveStreamScreen
import com.example.alfcapp.screens.YouTubeApiService
import com.example.alfcapp.screens.YouTubeVideoItem
import com.example.alfcapp.features.chat.ui.ChatNav
import com.example.alfcapp.features.prayer.ui.PrayerWallScreen
import com.example.alfcapp.data.chat.ChatNotifications
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LandingPage(
    onSignOut: () -> Unit = {},
) {
    // State to track which screen is active
    var currentScreen by remember { mutableStateOf("home") }
    var currentVideoId by remember { mutableStateOf<String?>(null) }
    var isLiveStream by remember { mutableStateOf(false) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var openChatThread by remember { mutableStateOf<com.example.alfcapp.features.chat.ui.ChatThreadHandle?>(null) }

    // Start the global unread-thread poller (lazy, idempotent).
    LaunchedEffect(Unit) { ChatNotifications.start() }
    val unreadThreads by ChatNotifications.unreadThreads.collectAsState()
    // Refresh the count quickly whenever we leave a chat thread.
    LaunchedEffect(openChatThread) {
        if (openChatThread == null) ChatNotifications.refreshNow()
    }

    if (currentVideoId != null) {
        YouTubePlayerScreen(
            videoId = currentVideoId!!,
            isLive = isLiveStream,
            onClose = { currentVideoId = null }
        )
    } else if (openChatThread != null) {
        // Fullscreen chat — bottom nav hidden.
        com.example.alfcapp.features.chat.ui.ChatScreen(
            threadId = openChatThread!!.id,
            title = openChatThread!!.partnerUsername,
            onBack = { openChatThread = null }
        )
    } else {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Long-press the logo to reach the hidden broadcaster screen.
                    // Regular taps do nothing, so ordinary users never stumble in.
                    Text(
                        "ALFC",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { currentScreen = "admin_live" }
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    Box {
                        IconButton(onClick = { accountMenuOpen = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = accountMenuOpen,
                            onDismissRequest = { accountMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                onClick = {
                                    accountMenuOpen = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == "home",
                    onClick = { currentScreen = "home" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PlayArrow, "Media") },
                    label = { Text("Media") },
                    selected = currentScreen == "media",
                    onClick = { currentScreen = "media" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MenuBook, "Bible") },
                    label = { Text("Bible") },
                    selected = currentScreen == "bible",
                    onClick = { currentScreen = "bible" } // Navigation logic corrected
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadThreads > 0) {
                                    Badge { Text(unreadThreads.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, "Chat")
                        }
                    },
                    label = { Text("Chat") },
                    selected = currentScreen == "chat",
                    onClick = { currentScreen = "chat" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, "Give") },
                    label = { Text("Give") },
                    selected = currentScreen == "give",
                    onClick = { currentScreen = "give" }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "home" -> HomeContent(
                    onNavigate = { screen -> currentScreen = screen },
                    onPlayVideo = { id, live ->
                        currentVideoId = id
                        isLiveStream = live
                    }
                )
                "bible" -> BibleScreen()
                "media" -> MediaScreen()
                "notes" -> NotesScreen()
                "give" -> GiveScreen()
                "prayer" -> PrayerWallScreen(onBack = { currentScreen = "home" })
                "events" -> EventsListScreen(onBack = { currentScreen = "home" })
                "admin_live" -> AdminLiveStreamScreen()
                "chat" -> ChatNav(
                    onExit = { currentScreen = "home" },
                    onOpenThread = { id, partner -> openChatThread = com.example.alfcapp.features.chat.ui.ChatThreadHandle(id, partner) }
                )
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Coming Soon: ${currentScreen.replaceFirstChar { it.uppercase() }}")
                    }
                }
            }
        }
    }
    }
}

@Composable
fun HomeContent(onNavigate: (String) -> Unit, onPlayVideo: (String, Boolean) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GreetingHeader() }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LiveOrNextHero(onPlayVideo = onPlayVideo)
            }
        }

        item {
            QuickActionRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                onNavigate = onNavigate
            )
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DailyDevotionalCard()
            }
        }

        item {
            Column {
                SectionHeaderWithAction(
                    title = "Upcoming Events",
                    actionLabel = "See all",
                    onActionClick = { onNavigate("events") }
                )
                UpcomingEventsRow()
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ConnectCard()
            }
        }
    }
}

@Composable
private fun GreetingHeader() {
    val now = java.time.LocalTime.now()
    val greeting = when (now.hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val today = java.time.LocalDate.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.getDefault()))

    // Compact greeting: a slim line at the top, not a full gradient block.
    // (The profile avatar already lives in the top app bar, so it's dropped here.)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = today,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LiveOrNextHero(onPlayVideo: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    var liveStatus by remember { mutableStateOf<com.example.alfcapp.data.livestream.LiveStreamDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (isActive) {
            com.example.alfcapp.data.livestream.LiveStreamRepository.fetchStatus()
                .onSuccess {
                    liveStatus = it
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
            // Backend already caches; polling app-side every 30s is cheap (zero YouTube cost).
            delay(30000)
        }
    }

    val isLive = liveStatus?.live == true && liveStatus?.videoId != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box {
            // Background: image when live, gradient otherwise
            if (isLive && !liveStatus?.thumbnailUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = liveStatus!!.thumbnailUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width, 0f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.width * 0.3f,
                        center = Offset(0f, size.height)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isLive) {
                    LivePill()
                } else {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "WORSHIP",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = if (isLive) liveStatus?.title ?: "We're live now"
                        else "Sunday Morning Service",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isLive) "Tap to join the stream"
                        else "Join us this Sunday in worship",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (isLive) {
                                onPlayVideo(liveStatus!!.videoId!!, true)
                            } else {
                                Toast.makeText(
                                    context,
                                    "We'll let you know when the stream starts.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLive) Color.Red else Color.White,
                            contentColor = if (isLive) Color.White else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isLive) "Watch Live" else "Notify Me",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePill() {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Surface(
        color = Color.Red,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White.copy(alpha = alpha), CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "LIVE NOW",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun QuickActionRow(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val actions = listOf(
        QuickAction("Bible", Icons.Default.MenuBook, Color(0xFF6D4AFF), "bible"),
        QuickAction("Notes", Icons.Default.EditNote, Color(0xFF00A37A), "notes"),
        QuickAction("Prayer", Icons.Default.Favorite, Color(0xFF9C27B0), "prayer"),
        QuickAction("Events", Icons.Default.Event, Color(0xFFE8833A), "events"),
        QuickAction("Give", Icons.Default.VolunteerActivism, Color(0xFFD81B60), "give")
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        actions.forEach { action ->
            QuickActionItem(
                action = action,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(action.route) }
            )
        }
    }
}

@Composable
private fun QuickActionItem(action: QuickAction, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = action.color.copy(alpha = 0.14f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = action.color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@Composable
private fun SectionHeaderWithAction(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onActionClick) {
            Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun ConnectCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stay Connected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "We're praying for you. Reach out anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Supporting Composables ---

@Composable
fun EventsListScreen(onBack: () -> Unit) {
    var events by remember { mutableStateOf<List<com.example.alfcapp.data.events.EventDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMessage = null
        com.example.alfcapp.data.events.EventsRepository.fetchUpcoming()
            .onSuccess {
                events = it
                isLoading = false
            }
            .onFailure {
                errorMessage = it.message ?: "Couldn't load events"
                isLoading = false
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            EventsListHeader(onBack = onBack, count = events.size)
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Couldn't load events",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { refreshKey++ }) { Text("Retry") }
                            }
                        }
                    }
                    events.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(96.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "No upcoming events",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Check back soon for what's coming up.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(events.size) { index ->
                                FullEventCard(event = events[index])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsListHeader(onBack: () -> Unit, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (count > 0) {
                Text(
                    text = "$count event${if (count == 1) "" else "s"} ahead",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FullEventCard(event: com.example.alfcapp.data.events.EventDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // ---- Full image with category + calendar date badge ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                if (!event.imageUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = event.imageUrl,
                        contentDescription = event.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )
                }

                if (!event.category.isNullOrBlank()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = eventDayNumber(event.startsAt),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = eventMonthShort(event.startsAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ---- Details + description ----
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${eventShortDate(event.startsAt)} • ${formatEventTime(event.startsAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!event.location.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = event.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                if (!event.description.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatFullEventDate(isoInstant: String): String = try {
    val z = java.time.Instant.parse(isoInstant).atZone(java.time.ZoneId.systemDefault())
    z.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.getDefault()))
} catch (e: Exception) { "" }

private fun eventDayNumber(isoInstant: String): String = try {
    java.time.Instant.parse(isoInstant).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("d", java.util.Locale.getDefault()))
} catch (e: Exception) { "" }

private fun eventMonthShort(isoInstant: String): String = try {
    java.time.Instant.parse(isoInstant).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.getDefault()))
        .uppercase()
} catch (e: Exception) { "" }

private fun eventShortDate(isoInstant: String): String = try {
    java.time.Instant.parse(isoInstant).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.getDefault()))
} catch (e: Exception) { "" }


@Composable
fun DailyDevotionalCard() {
    var dailyVerse by remember { mutableStateOf<VerseDetails?>(null) }

    LaunchedEffect(Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://beta.ourmanna.com/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(DevotionalApiService::class.java)
        try {
            val response = apiService.getDailyVerse()
            dailyVerse = response.verse.details
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily Verse", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                if (dailyVerse != null) {
                    Text("\"${dailyVerse!!.text}\"", style = MaterialTheme.typography.bodyLarge, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Text("- ${dailyVerse!!.reference}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text("Loading daily inspiration...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
    }
}

private val eventGradients = listOf(
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
    listOf(Color(0xFF00c6ff), Color(0xFF0072ff)),
    listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFFf7971e), Color(0xFFffd200))
)

@Composable
fun UpcomingEventsRow() {
    var events by remember { mutableStateOf<List<com.example.alfcapp.data.events.EventDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        com.example.alfcapp.data.events.EventsRepository.fetchUpcoming()
            .onSuccess {
                events = it
                isLoading = false
            }
            .onFailure {
                errorMessage = it.message ?: "Couldn't load events"
                isLoading = false
            }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Couldn't load events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        events.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(events.size) { index ->
                    val event = events[index]
                    val gradient = eventGradients[index % eventGradients.size]
                    EventCard(
                        title = event.title,
                        dateLabel = formatEventDate(event.startsAt),
                        timeLabel = formatEventTime(event.startsAt),
                        location = event.location,
                        imageUrl = event.imageUrl,
                        gradientColors = gradient
                    )
                }
            }
        }
    }
}

private fun formatEventDate(isoInstant: String): String {
    return try {
        val instant = java.time.Instant.parse(isoInstant)
        val zoned = instant.atZone(java.time.ZoneId.systemDefault())
        zoned.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.getDefault()))
    } catch (e: Exception) {
        ""
    }
}

private fun formatEventTime(isoInstant: String): String {
    return try {
        val instant = java.time.Instant.parse(isoInstant)
        val zoned = instant.atZone(java.time.ZoneId.systemDefault())
        zoned.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault()))
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun EventCard(
    title: String,
    dateLabel: String,
    timeLabel: String,
    location: String?,
    imageUrl: String?,
    gradientColors: List<Color>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cardPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            if (!imageUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width, 0f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = size.width * 0.3f,
                        center = Offset(0f, size.height)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    if (dateLabel.isNotBlank()) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = dateLabel,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        if (!location.isNullOrBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

interface DevotionalApiService {
    @GET("get")
    suspend fun getDailyVerse(
        @Query("format") format: String = "json",
        @Query("order") order: String = "daily"
    ): DevotionalResponse
}

data class DevotionalResponse(val verse: VerseData)
data class VerseData(val details: VerseDetails)
data class VerseDetails(val text: String, val reference: String)