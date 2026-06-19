package com.example.alfcapp.data.chat

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("api/members/search")
    suspend fun searchMembers(@Query("q") query: String): List<MemberSummaryDto>

    @GET("api/chat/threads")
    suspend fun listThreads(): List<ThreadSummaryDto>

    @POST("api/chat/threads")
    suspend fun openOrCreateThread(@Body body: CreateThreadRequest): ThreadDto

    @GET("api/chat/threads/{id}/messages")
    suspend fun listMessages(
        @Path("id") threadId: Long,
        @Query("afterId") afterId: Long? = null,
        @Query("limit") limit: Int = 50
    ): List<MessageDto>

    @POST("api/chat/threads/{id}/messages")
    suspend fun sendMessage(
        @Path("id") threadId: Long,
        @Body body: SendMessageRequest
    ): MessageDto

    @DELETE("api/chat/messages/{id}")
    suspend fun deleteMessage(@Path("id") messageId: Long)

    @POST("api/chat/threads/{id}/read")
    suspend fun markRead(@Path("id") threadId: Long)
}
