package com.example.alfcapp.features.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alfcapp.data.chat.ChatRepository
import com.example.alfcapp.data.chat.MemberSummaryDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindUserScreen(
    onBack: () -> Unit,
    onThreadOpened: (id: Long, partnerUsername: String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MemberSummaryDto>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) { results = emptyList(); searching = false; return@LaunchedEffect }
        searching = true
        delay(SEARCH_DEBOUNCE_MS)
        try { results = ChatRepository.searchMembers(q); error = null }
        catch (t: Throwable) { error = t.message ?: "Search failed." }
        finally { searching = false }
    }

    fun openWith(username: String) {
        if (opening != null) return
        opening = username
        scope.launch {
            try {
                val thread = ChatRepository.openThreadWith(username)
                onThreadOpened(thread.id, thread.partner.username)
            } catch (t: Throwable) {
                error = friendly(t.message)
            } finally {
                opening = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New conversation", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by username") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (query.trim().length >= 2 &&
                results.none { it.username.equals(query.trim(), ignoreCase = true) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = opening == null) { openWith(query.trim()) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UsernameAvatar(query.trim(), size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start chat with \"${query.trim()}\"", fontWeight = FontWeight.Medium)
                        Text(
                            "Will fail if no such user exists.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
            }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (results.isEmpty() && query.isNotBlank() && !searching) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No matches",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results, key = { it.id }) { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = opening == null) { openWith(m.username) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UsernameAvatar(m.username, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(m.username, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (opening == m.username) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                }
            }
        }
    }
}

private fun friendly(msg: String?): String = when {
    msg == null -> "Could not start the conversation."
    msg.contains("404") -> "No user with that username."
    msg.contains("400") -> "Cannot start a chat with yourself."
    else -> msg
}
