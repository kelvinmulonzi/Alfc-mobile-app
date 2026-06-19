package com.example.alfcapp.data.prayer

enum class PrayerCategory { HEALING, FAMILY, SALVATION, THANKSGIVING, GUIDANCE, OTHER }

enum class PrayerVisibility { PUBLIC_WALL, PRAYER_TEAM_ONLY }

enum class PrayerStatus { ACTIVE, HIDDEN, ARCHIVED }

data class PrayerCreateRequest(
    val body: String,
    val category: PrayerCategory,
    val visibility: PrayerVisibility,
)

data class PrayerDto(
    val id: Long,
    val body: String,
    val category: PrayerCategory,
    val visibility: PrayerVisibility,
    val status: PrayerStatus,
    val prayCount: Long,
    val prayedByMe: Boolean,
    val mine: Boolean,
    val createdAt: String?,
)

data class PrayCountDto(
    val prayCount: Long,
    val prayedByMe: Boolean,
)
