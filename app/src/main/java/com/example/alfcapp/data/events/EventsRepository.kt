package com.example.alfcapp.data.events

import com.example.alfcapp.data.backend.BackendClient

object EventsRepository {

    suspend fun fetchUpcoming(): Result<List<EventDto>> = runCatching {
        BackendClient.eventApi.getUpcomingEvents()
    }
}
