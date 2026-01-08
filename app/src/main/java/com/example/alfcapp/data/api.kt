package com.example.alfcapp.data

import com.example.alfcapp.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// Data models for the API response
data class BibleResponse<T>(val data: T)
data class BibleBook(val id: String, val name: String, val nameLong: String? = null)
data class BibleChapter(val id: String, val number: String, val reference: String)
data class BibleContent(val content: String, val reference: String)

interface BibleApiService {
    @GET("bibles/{bibleId}/books")
    suspend fun getBooks(
        @Path("bibleId") bibleId: String = "de4e12af7f895fca-01"
    ): BibleResponse<List<BibleBook>>

    @GET("bibles/{bibleId}/books/{bookId}/chapters")
    suspend fun getChapters(
        @Path("bibleId") bibleId: String,
        @Path("bookId") bookId: String
    ): BibleResponse<List<BibleChapter>>

    @GET("bibles/{bibleId}/chapters/{chapterId}")
    suspend fun getChapterContent(
        @Path("bibleId") bibleId: String,
        @Path("chapterId") chapterId: String,
        @Query("content-type") contentType: String = "text"
    ): BibleResponse<BibleContent>
}