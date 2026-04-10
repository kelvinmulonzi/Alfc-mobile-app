package com.example.alfcapp.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// --- API DATA MODELS ---
data class YouTubeSearchResponse(val items: List<YouTubeVideoItem>)
data class YouTubeVideoItem(val id: VideoId, val snippet: VideoSnippet)
data class VideoId(val videoId: String?)
data class VideoSnippet(
    val title: String,
    val thumbnails: VideoThumbnails,
    val liveBroadcastContent: String?,
    val publishedAt: String? = null,
    val channelTitle: String? = null
)
data class VideoThumbnails(val high: ThumbnailUrl)
data class ThumbnailUrl(val url: String)

data class YouTubeVideoDetailsResponse(val items: List<YouTubeVideoDetail>)
data class YouTubeVideoDetail(val id: String, val status: VideoStatus)
data class VideoStatus(val embeddable: Boolean)

interface YouTubeApiService {
    @GET("search")
    suspend fun getChannelVideos(
        @Query("key") apiKey: String,
        @Query("channelId") channelId: String,
        @Query("part") part: String = "snippet,id",
        @Query("order") order: String = "date",
        @Query("maxResults") maxResults: Int = 15,
        @Query("type") type: String = "video",
        @Query("eventType") eventType: String? = null
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("key") apiKey: String,
        @Query("id") videoIds: String,
        @Query("part") part: String = "status"
    ): YouTubeVideoDetailsResponse
}

@Composable
fun MediaScreen() {
    var videos by remember { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var isCurrentLive by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    val apiKey = "AIzaSyDda7HNFvzGXGZMH8LJhR3pLhGE9RA9O1o"
    val channelId = "UCL_RqdXTMyCyThK8Ub0iqXw"

    val apiService = remember {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)
    }

    LaunchedEffect(refreshKey) {
        isLoading = true
        error = null
        try {
            val searchResponse = apiService.getChannelVideos(apiKey, channelId)
            val allVideos = searchResponse.items
            val ids = allVideos.mapNotNull { it.id.videoId }.joinToString(",")

            videos = if (ids.isNotEmpty()) {
                val detailsResponse = apiService.getVideoDetails(apiKey, ids)
                val embeddable = detailsResponse.items
                    .filter { it.status.embeddable }
                    .map { it.id }
                    .toSet()
                allVideos.filter { it.id.videoId in embeddable }
            } else {
                allVideos
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    val liveVideo = videos.firstOrNull { it.snippet.liveBroadcastContent == "live" }
    val pastVideos = videos.filter { it.snippet.liveBroadcastContent != "live" && it.id.videoId != null }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { MediaHeader(onRefresh = { refreshKey++ }) }

            when {
                isLoading -> item { LoadingState() }
                error != null -> item { ErrorState(onRetry = { refreshKey++ }) }
                videos.isEmpty() -> item { EmptyState() }
                else -> {
                    if (liveVideo != null) {
                        item {
                            LiveBanner(
                                video = liveVideo,
                                onClick = {
                                    selectedVideoId = liveVideo.id.videoId
                                    isCurrentLive = true
                                }
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            title = if (liveVideo != null) "Past Streams" else "Latest Videos",
                            count = pastVideos.size
                        )
                    }

                    items(pastVideos, key = { it.id.videoId ?: it.snippet.title }) { video ->
                        VideoCard(
                            video = video,
                            onClick = {
                                selectedVideoId = video.id.videoId
                                isCurrentLive = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedVideoId != null) {
            YouTubePlayerScreen(
                videoId = selectedVideoId!!,
                isLive = isCurrentLive,
                title = videos.firstOrNull { it.id.videoId == selectedVideoId }?.snippet?.title.orEmpty(),
                onClose = { selectedVideoId = null }
            )
        }
    }
}

@Composable
private fun MediaHeader(onRefresh: () -> Unit) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Watch & Listen",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sermons, services, and live moments from ALFC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }
    }
}

@Composable
private fun LiveBanner(video: YouTubeVideoItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box {
            AsyncImage(
                model = video.snippet.thumbnails.high.url,
                contentDescription = video.snippet.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            // LIVE pill (top-left)
            Surface(
                color = Color.Red,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    PulsingDot()
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "LIVE NOW",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            // Play button center
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play live",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            // Title bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = video.snippet.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                video.snippet.channelTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun VideoCard(video: YouTubeVideoItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = video.snippet.thumbnails.high.url,
                    contentDescription = video.snippet.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                ),
                                startY = 100f
                            )
                        )
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = video.snippet.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = listOfNotNull(
                    video.snippet.channelTitle,
                    formatPublishedAt(video.snippet.publishedAt)
                ).joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Loading videos...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Can't load videos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Check your connection and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.PlayCircleFilled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No videos yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "New sermons will appear here as they're posted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun YouTubePlayerScreen(
    videoId: String,
    isLive: Boolean,
    title: String = "",
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                if (isLive) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            PulsingDot()
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context -> YouTubePlayerView(context) },
                    modifier = Modifier.fillMaxSize(),
                    update = { playerView ->
                        lifecycleOwner.lifecycle.addObserver(playerView)
                        playerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youTubePlayer.loadVideo(videoId, 0f)
                            }
                        })
                    }
                )
            }

            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(20.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatPublishedAt(publishedAt: String?): String? {
    if (publishedAt.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(publishedAt) ?: return null
        val diff = System.currentTimeMillis() - date.time
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        when {
            diff < hour -> "${(diff / minute).coerceAtLeast(1)}m ago"
            diff < day -> "${diff / hour}h ago"
            diff < 7 * day -> "${diff / day}d ago"
            diff < 30 * day -> "${diff / (7 * day)}w ago"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        null
    }
}
