package com.example.alfcapp.data.prayer

import com.example.alfcapp.data.backend.BackendClient

object PrayerRepository {

    suspend fun wall(category: PrayerCategory? = null, page: Int = 0, size: Int = 20): List<PrayerDto> =
        BackendClient.prayerApi.list(DeviceId.get(), category, page, size)

    suspend fun submit(body: String, category: PrayerCategory, visibility: PrayerVisibility): PrayerDto =
        BackendClient.prayerApi.create(
            DeviceId.get(),
            PrayerCreateRequest(body.trim(), category, visibility)
        )

    suspend fun pray(id: Long): PrayCountDto =
        BackendClient.prayerApi.pray(id, DeviceId.get())

    suspend fun delete(id: Long) =
        BackendClient.prayerApi.delete(id, DeviceId.get())
}
