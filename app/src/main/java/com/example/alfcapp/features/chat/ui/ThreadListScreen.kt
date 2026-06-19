package com.example.alfcapp.features.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alfcapp.data.chat.ChatNotifications
import com.example.alfcapp.data.chat.ChatRepository
import com.example.alfcapp.data.chat.ThreadSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpenThread: (id: Long, partnerUsername: String) -> Unit,
) {
    var threads by remember { mutableStateOf<List<ThreadSummaryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try { threads = ChatRepository.threads() }
        catch (t: Throwable) { error = t.message ?: "Could not load conversations." }
        finally { loading = false }
    }

    // Refresh inbox every 5s while open so new conversations and unread state stay current.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            try { threads = ChatRepository.threads() } catch (_: Throwable) {}
            ChatNotifications.refreshNow()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { reloadKey++ }) { Text("Refresh") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { reloadKey++ }) { Text("Retry") }
                }
                threads.isEmpty() -> EmptyInbox(onNew)
                else -> LazyColumn {
                    items(threads, key = { it.id }) { t -> ThreadRow(t, onOpenThread) }
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(
    t: ThreadSummaryDto,
    onOpenThread: (id: Long, partnerUsername: String) -> Unit,
) {
    val nameColor = if (t.unread) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface
    val previewColor = if (t.unread) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    val previewWeight = if (t.unread) FontWeight.Medium else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenThread(t.id, t.partner.username) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UsernameAvatar(t.partner.username, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                t.partner.username,
                fontWeight = if (t.unread) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = nameColor
            )
            Text(
                t.lastMessage ?: "Tap to send a message",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = previewWeight,
                color = previewColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                relativeTime(t.lastMessageAt),
                style = MaterialTheme.typography.labelSmall,
                color = if (t.unread) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (t.unread) FontWeight.Bold else FontWeight.Normal
            )
            if (t.unread) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
}

@Composable
private fun EmptyInbox(onNew: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text("No conversations yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap New to find someone by username and start chatting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew) { Text("New conversation") }
    }
}
