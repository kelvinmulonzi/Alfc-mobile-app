package com.example.alfcapp.data.chat

/**
 * Mirror of the backend's {@code com.example.Alfc.chat.dto.ChatEvent}.
 *
 * Pushed by the server over STOMP to the authenticated user's personal queue
 * ({@code /user/queue/messages}). Only the fields relevant to {@code kind}
 * are populated; the rest are null.
 *
 * Kinds:
 *  - "MESSAGE_NEW"      → {@code message} carries the new MessageDto for {@code threadId}
 *  - "MESSAGE_DELETED"  → {@code messageId} was removed from {@code threadId}
 *  - "READ"             → {@code readerId} read {@code threadId} at {@code at}
 */
data class ChatEvent(
    val kind: String,
    val threadId: Long,
    val message: MessageDto? = null,
    val messageId: Long? = null,
    val readerId: Long? = null,
    val at: String? = null,
) {
    companion object {
        const val MESSAGE_NEW = "MESSAGE_NEW"
        const val MESSAGE_DELETED = "MESSAGE_DELETED"
        const val READ = "READ"
    }
}
