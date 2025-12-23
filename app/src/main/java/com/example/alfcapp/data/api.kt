package com.example.alfcapp.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// Data models for the API response
data class BibleResponse<T>(val data: T)
data class BibleBook(val id: String, val name: String, val nameLong: String)
data class BibleChapter(val id: String, val number: String, val reference: String)
data class BibleContent(val content: String, val reference: String)

interface BibleApiService {
    @GET("v1/bibles/{bibleId}/books")
    suspend fun getBooks(
        @Path("bibleId") bibleId: String = "de4e12af7f895fca-01", // Default KJV ID
        @Header("api-key") apiKey: String = "6iR9FJJagmYKztTcZnD_b"
    ): BibleResponse<List<BibleBook>>

    @GET("v1/bibles/{bibleId}/books/{bookId}/chapters")
    suspend fun getChapters(
        @Path("bibleId") bibleId: String,
        @Path("bookId") bookId: String,
        @Header("api-key") apiKey: String = "6iR9FJJagmYKztTcZnD_b"
    ): BibleResponse<List<BibleChapter>>

    @GET("v1/bibles/{bibleId}/chapters/{chapterId}")
    suspend fun getChapterContent(
        @Path("bibleId") bibleId: String,
        @Path("chapterId") chapterId: String,
        @Query("content-type") contentType: String = "text",
        @Header("api-key") apiKey: String = "6iR9FJJagmYKztTcZnD_b"
    ): BibleResponse<BibleContent>
}