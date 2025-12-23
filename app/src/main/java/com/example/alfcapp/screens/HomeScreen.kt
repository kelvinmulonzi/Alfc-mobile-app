package com.example.alfcapp

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alfcapp.screens.BibleScreen // Ensure you have implemented this screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingPage() {
    // State to track which screen is active
    var currentScreen by remember { mutableStateOf("home") }

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
                "home" -> HomeContent()
                "bible" -> BibleScreen() // Displays the Bible API implementation
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

@Composable
fun HomeContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            MediaHeroSection()
        }

        item {
            QuickActionGrid()
        }

        item {
            DailyDevotionalCard()
        }

        item {
            SectionHeader("Community Prayer Wall")
            PrayerWallPreview()
        }

        item {
            SectionHeader("Upcoming Events")
            UpcomingEventsRow()
        }
    }
}

// --- Supporting Composables remain the same ---

@Composable
fun MediaHeroSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            Text("Sunday Morning Service", style = MaterialTheme.typography.titleLarge)
            Text("Building Faith in 2025", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* Open Media Player */ },
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
fun QuickActionGrid() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionIconButton(Icons.Default.VolunteerActivism, "Give", Modifier.weight(1f))
        ActionIconButton(Icons.Default.EditNote, "Notes", Modifier.weight(1f))
        ActionIconButton(Icons.Default.Event, "Events", Modifier.weight(1f))
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, label: String, modifier: Modifier) {
    ElevatedCard(
        modifier = modifier,
        onClick = { /* Action */ },
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily Nugget of Wisdom", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text("Psalm 23:1 - The Lord is my shepherd...", style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun PrayerWallPreview() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Sarah M. requested prayer for healing.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { /* Notify user */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.PanTool, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("I Prayed for This", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun UpcomingEventsRow() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(3) { index ->
            Card(modifier = Modifier.width(200.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Box(Modifier.fillMaxWidth().height(100.dp).background(Color.LightGray))
                    Spacer(Modifier.height(8.dp))
                    Text("Youth Night", style = MaterialTheme.typography.titleMedium)
                    Text("Fri, Dec 27 • 7:00 PM", style = MaterialTheme.typography.labelMedium)
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