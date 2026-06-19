package com.example.alfcapp.data.chat

import com.example.alfcapp.data.backend.BackendClient

object ChatRepository {

    suspend fun searchMembers(query: String): List<MemberSummaryDto> =
        BackendClient.chatApi.searchMembers(query)

    suspend fun threads(): List<ThreadSummaryDto> =
        BackendClient.chatApi.listThreads()

    suspend fun openThreadWith(username: String): ThreadDto =
        BackendClient.chatApi.openOrCreateThread(CreateThreadRequest(username.trim()))

    suspend fun messages(threadId: Long, afterId: Long? = null): List<MessageDto> =
        BackendClient.chatApi.listMessages(threadId, afterId)

    suspend fun send(threadId: Long, text: String): MessageDto =
        BackendClient.chatApi.sendMessage(threadId, SendMessageRequest(text))

    suspend fun deleteMessage(messageId: Long) =
        BackendClient.chatApi.deleteMessage(messageId)

    suspend fun markRead(threadId: Long) =
        BackendClient.chatApi.markRead(threadId)
}
