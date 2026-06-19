package com.example.alfcapp.data.chat

data class PartnerDto(
    val id: Long,
    val username: String
)

data class MemberSummaryDto(
    val id: Long,
    val username: String
)

data class ThreadSummaryDto(
    val id: Long,
    val partner: PartnerDto,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val unread: Boolean = false
)

data class ThreadDto(
    val id: Long,
    val partner: PartnerDto
)

data class MessageDto(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val text: String,
    val sentAt: String
)

data class CreateThreadRequest(val username: String)

data class SendMessageRequest(val text: String)
