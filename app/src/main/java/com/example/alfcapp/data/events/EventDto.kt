package com.example.alfcapp.data.events

data class EventDto(
    val id: Long,
    val title: String,
    val description: String?,
    val startsAt: String,   // ISO-8601 instant, e.g. "2026-04-12T07:00:00Z"
    val endsAt: String?,
    val location: String?,
    val imageUrl: String?,
    val category: String?,
    val published: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)
