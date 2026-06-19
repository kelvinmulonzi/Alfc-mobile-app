package com.example.alfcapp.data.livestream

data class LiveStreamDto(
    val live: Boolean,
    val videoId: String?,
    val title: String?,
    val thumbnailUrl: String?,
    val startedAt: String?,
    val checkedAt: String?
)
