package com.example.alfcapp.data.events

import retrofit2.http.GET

interface EventApi {
    @GET("api/events")
    suspend fun getUpcomingEvents(): List<EventDto>
}
