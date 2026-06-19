package com.example.alfcapp.features.chat.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alfcapp.data.auth.TokenStore
import com.example.alfcapp.data.chat.ActiveThreadTracker
import com.example.alfcapp.data.chat.ChatEvent
import com.example.alfcapp.data.chat.ChatRepository
import com.example.alfcapp.data.chat.ChatWebSocket
import com.example.alfcapp.data.chat.MessageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: Long,
    title: String?,
    onBack: () -> Unit,
) {
    val myUserId by produceState(initialValue = 0L) {
        value = TokenStore.current()?.userId ?: 0L
    }
    val partnerName = title ?: "Conversation"

    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedMessage by remember { mutableStateOf<MessageDto?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Tell the notifier this thread is on-screen so it suppresses pings for it.
    DisposableEffect(threadId) {
        ActiveThreadTracker.setActive(threadId)
        onDispose { ActiveThreadTracker.clearActive(threadId) }
    }

    // Initial load + mark-read. New messages and deletions arrive via WebSocket
    // (see the LaunchedEffect below); no polling.
    LaunchedEffect(threadId) {
        // Make sure the WebSocket is up. start() is idempotent.
        ChatWebSocket.start()

        try { ChatRepository.markRead(threadId) }
        catch (t: CancellationException) { throw t }
        catch (_: Throwable) { /* non-fatal */ }

        try { messages = ChatRepository.messages(threadId) }
        catch (t: CancellationException) { throw t }
        catch (t: Throwable) { error = t.message ?: "Could not load messages." }
    }

    // React to real-time events for THIS thread.
    LaunchedEffect(threadId) {
        ChatWebSocket.events.collect { ev ->
            if (ev.threadId != threadId) return@collect
            when (ev.kind) {
                ChatEvent.MESSAGE_NEW -> {
                    val m = ev.message ?: return@collect
                    // Sender's own send already appended optimistically below; skip duplicates.
                    if (messages.none { it.id == m.id }) {
                        messages = messages + m
                        if (m.senderId != myUserId) {
                            try { ChatRepository.markRead(threadId) } catch (_: Throwable) {}
                        }
                    }
                }
                ChatEvent.MESSAGE_DELETED -> {
                    val id = ev.messageId ?: return@collect
                    messages = messages.filterNot { it.id == id }
                }
                ChatEvent.READ -> {
                    /* read receipts: no per-message UI yet; ignore */
                }
            }
        }
    }

    // Single source of truth for auto-scrolling to the newest message.
    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        try { listState.animateScrollToItem(messages.size - 1) }
        catch (_: CancellationException) { /* expected when size changes again */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UsernameAvatar(partnerName, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(partnerName, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No messages yet — say hi.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    messages.forEachIndexed { index, m ->
                        val prev = messages.getOrNull(index - 1)
                        val showDate = prev == null || !sameLocalDay(prev.sentAt, m.sentAt)
                        if (showDate) {
                            item("d-${m.id}") { DateChip(m.sentAt) }
                        }
                        val isMine = m.senderId == myUserId
                        val showSender = !isMine && (prev == null || prev.senderId != m.senderId || showDate)
                        item(m.id) {
                            MessageBubble(
                                m,
                                isMine = isMine,
                                showSender = showSender,
                                onLongPress = if (isMine) {
                                    { selectedMessage = m }
                                } else null
                            )
                        }
                    }
                }
            }

            selectedMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { if (!deleting) selectedMessage = null },
                    title = { Text("Delete message?") },
                    text = { Text("This will remove the message for everyone in this conversation.") },
                    confirmButton = {
                        TextButton(
                            enabled = !deleting,
                            onClick = {
                                scope.launch {
                                    deleting = true
                                    try {
                                        ChatRepository.deleteMessage(msg.id)
                                        messages = messages.filterNot { it.id == msg.id }
                                        selectedMessage = null
                                    } catch (t: CancellationException) { throw t }
                                    catch (t: Throwable) { error = t.message ?: "Could not delete." }
                                    finally { deleting = false }
                                }
                            }
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !deleting,
                            onClick = { selectedMessage = null }
                        ) { Text("Cancel") }
                    }
                )
            }

            HorizontalDivider()
            Composer(
                value = input,
                onChange = { input = it },
                sending = sending,
                onSend = {
                    val text = input.trim()
                    if (text.isEmpty()) return@Composer
                    scope.launch {
                        sending = true
                        try {
                            val msg = ChatRepository.send(threadId, text)
                            messages = messages + msg
                            input = ""
                        } catch (t: CancellationException) {
                            throw t
                        } catch (t: Throwable) {
                            error = t.message ?: "Could not send."
                        } finally {
                            sending = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DateChip(iso: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
        ) {
            Text(
                dateHeader(iso),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    m: MessageDto,
    isMine: Boolean,
    showSender: Boolean,
    onLongPress: (() -> Unit)? = null,
) {
    val align = if (isMine) Alignment.End else Alignment.Start
    val bg = if (isMine) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isMine) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMine)
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    else
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalAlignment = align
    ) {
        if (showSender && !isMine) {
            Text(
                m.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .wrapContentWidth(if (isMine) Alignment.End else Alignment.Start)
        ) {
            val bubbleModifier = Modifier
                .clip(shape)
                .background(bg)
                .let {
                    if (onLongPress != null)
                        it.combinedClickable(onClick = {}, onLongClick = onLongPress)
                    else it
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            Box(modifier = bubbleModifier) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(m.text, color = fg)
                    Text(
                        timeOfDay(m.sentAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onChange: (String) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("Type a message…") },
            shape = RoundedCornerShape(24.dp),
            singleLine = false,
            maxLines = 4,
            enabled = !sending,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        val canSend = !sending && value.isNotBlank()
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            shape = CircleShape,
            modifier = Modifier.size(48.dp).padding(bottom = 4.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (sending) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
