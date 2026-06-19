package com.example.alfcapp.features.chat.ui

import androidx.compose.runtime.*

private sealed interface ChatNavState {
    data object Inbox : ChatNavState
    data object FindUser : ChatNavState
}

/** Handle to a chat thread the parent can route to as a fullscreen view. */
data class ChatThreadHandle(val id: Long, val partnerUsername: String)

/**
 * Inbox + find-user only. Opening a thread escalates to a fullscreen view
 * managed by the parent (so the bottom navigation can be hidden).
 */
@Composable
fun ChatNav(
    onExit: () -> Unit,
    onOpenThread: (id: Long, partnerUsername: String) -> Unit,
) {
    var state: ChatNavState by remember { mutableStateOf(ChatNavState.Inbox) }
    when (state) {
        ChatNavState.Inbox -> ThreadListScreen(
            onBack = onExit,
            onNew = { state = ChatNavState.FindUser },
            onOpenThread = onOpenThread
        )
        ChatNavState.FindUser -> FindUserScreen(
            onBack = { state = ChatNavState.Inbox },
            onThreadOpened = onOpenThread
        )
    }
}
