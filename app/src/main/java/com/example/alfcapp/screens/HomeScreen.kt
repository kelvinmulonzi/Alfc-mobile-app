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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingPage() {
    // State to track which screen is active
    var currentScreen by remember { mutableStateOf("home") }
    var currentVideoId by remember { mutableStateOf<String?>(null) }
    var isLiveStream by remember { mutableStateOf(false) }

    if (currentVideoId != null) {
        YouTubePlayerScreen(
            videoId = currentVideoId!!,
            isLive = isLiveStream,
            onClose = { currentVideoId = null }
        )
    } else {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ALFC",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Profile/Settings */ }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
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
                "bible" -> BibleScreen() // Displays the Bible API implementation
                "media" -> MediaScreen()
                "notes" -> NotesScreen()
                "give" -> GiveScreen()
                "admin_live" -> AdminLiveStreamScreen()
                else -> {
                    // Placeholder for Media and Give screens
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            MediaHeroSection(onPlayVideo)
        }

        item {
            QuickActionGrid(onNavigate)
        }

        item {
            DailyDevotionalCard()
        }

        item {
            SectionHeader("Upcoming Events")
            UpcomingEventsRow()
        }
    }
}

// --- Supporting Composables remain the same ---

@Composable
fun MediaHeroSection(onPlayVideo: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val apiKey = "AIzaSyDda7HNFvzGXGZMH8LJhR3pLhGE9RA9O1o"
    val channelId = "UCL_RqdXTMyCyThK8Ub0iqXw"
    var liveVideo by remember { mutableStateOf<YouTubeVideoItem?>(null) }

    LaunchedEffect(Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(YouTubeApiService::class.java)

        while (isActive) {
            try {
                val response = apiService.getChannelVideos(apiKey, channelId, eventType = "live")
                liveVideo = response.items.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(15000) // Poll every 15 seconds to check for live status
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (liveVideo != null && liveVideo!!.id.videoId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Red,
                        shape = CircleShape,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text("LIVE NOW", style = MaterialTheme.typography.labelLarge, color = Color.Red)
                }
                Spacer(Modifier.height(8.dp))
                Text(liveVideo!!.snippet.title, style = MaterialTheme.typography.titleLarge)
            } else {
                Text("Sunday Morning Service", style = MaterialTheme.typography.titleLarge)
                Text("Building Faith in 2025", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (liveVideo?.id?.videoId != null) {
                        onPlayVideo(liveVideo!!.id.videoId!!, true)
                    } else {
                        Toast.makeText(context, "You'll see the stream when someone starts it.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Watch Stream")
            }
        }
    }
}

@Composable
fun QuickActionGrid(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionIconButton(Icons.Default.VolunteerActivism, "Give", Modifier.weight(1f), onClick = { onNavigate("give") })
            ActionIconButton(Icons.Default.EditNote, "Notes", Modifier.weight(1f), onClick = { onNavigate("notes") })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionIconButton(Icons.Default.Event, "Events", Modifier.weight(1f))
            ActionIconButton(Icons.Default.Videocam, "Go Live", Modifier.weight(1f), onClick = { onNavigate("admin_live") })
        }
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit = {}) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.secondary)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

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