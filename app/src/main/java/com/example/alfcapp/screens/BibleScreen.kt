package com.example.alfcapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

@Composable
fun BibleScreen() {
    val retrofit = remember {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://bible.helloao.org/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService = remember { retrofit.create(BibleApiService::class.java) }

    var books by remember { mutableStateOf<List<BibleBook>>(emptyList()) }
    var isLoadingBooks by remember { mutableStateOf(true) }

    var selectedBook by remember { mutableStateOf<BibleBook?>(null) }
    var currentChapter by remember { mutableIntStateOf(1) }
    var chapterContent by remember { mutableStateOf<BibleChapter?>(null) }
    var isLoadingChapter by remember { mutableStateOf(false) }
    var showChapterSelection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            // Using BSB (Berean Study Bible) as the default translation
            val response = apiService.getBooks("BSB")
            books = response.books
        } catch (e: Exception) {
            // Handle error
            e.printStackTrace()
        } finally {
            isLoadingBooks = false
        }
    }

    LaunchedEffect(selectedBook, currentChapter) {
        val book = selectedBook
        if (book != null) {
            isLoadingChapter = true
            try {
                val response = apiService.getChapter("BSB", book.id, currentChapter)
                chapterContent = response.chapter
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingChapter = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedBook == null) {
            Text(
                text = "Holy Bible",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary // Using project brand color
            )

            if (isLoadingBooks) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(books) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedBook = book
                                currentChapter = 1
                            }
                        ) {
                            Text(
                                text = book.name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        } else {
            Button(onClick = { selectedBook = null }) {
                Text("Back to Books")
            }

            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                TextButton(onClick = { showChapterSelection = true }) {
                    Text(
                        text = "${selectedBook!!.name} - Chapter $currentChapter ▼",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                DropdownMenu(
                    expanded = showChapterSelection,
                    onDismissRequest = { showChapterSelection = false },
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    val maxChapter = selectedBook!!.chapters?.maxOrNull() ?: bibleChapterCounts[selectedBook!!.id] ?: 50
                    val chapters = (1..maxChapter).toList()
                    chapters.forEach { chapterNum ->
                        DropdownMenuItem(
                            text = { Text("Chapter $chapterNum") },
                            onClick = {
                                currentChapter = chapterNum
                                showChapterSelection = false
                            }
                        )
                    }
                }
            }

            if (isLoadingChapter) {
                CircularProgressIndicator()
            } else {
                chapterContent?.let { chapter ->
                    LazyColumn {
                        items(chapter.content ?: emptyList()) { item ->
                            val text = item.content?.joinToString("") { extractText(it) } ?: ""
                            val prefix = if (item.number != null) "${item.number} " else ""
                            Text(
                                text = "$prefix$text",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { if (currentChapter > 1) currentChapter-- },
                                    enabled = currentChapter > 1
                                ) {
                                    Text("Previous")
                                }
                                Button(
                                    onClick = { currentChapter++ },
                                    enabled = currentChapter < (selectedBook!!.chapters?.maxOrNull() ?: bibleChapterCounts[selectedBook!!.id] ?: 50)
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun extractText(element: JsonElement): String {
    if (element.isJsonPrimitive) {
        return element.asString
    }
    if (element.isJsonObject) {
        val obj = element.asJsonObject
        if (obj.has("text")) return obj.get("text").asString
        if (obj.has("content")) {
            val content = obj.get("content")
            if (content.isJsonArray) {
                return content.asJsonArray.joinToString("") { extractText(it) }
            } else if (content.isJsonPrimitive) {
                return content.asString
            }
        }
    }
    return ""
}

data class BibleBook(val id: String, val name: String, val chapters: List<Int>?)

data class BibleBooksResponse(val books: List<BibleBook>)

data class BibleChapterResponse(val chapter: BibleChapter)

data class BibleChapter(
    val number: Int,
    val content: List<BibleContentItem>?
)

data class BibleContentItem(
    val type: String?,
    val number: String?,
    val content: List<JsonElement>?
)

interface BibleApiService {
    @GET("{translation}/books.json")
    suspend fun getBooks(@Path("translation") translation: String): BibleBooksResponse

    @GET("{translation}/{book}/{chapter}.json")
    suspend fun getChapter(
        @Path("translation") translation: String,
        @Path("book") bookId: String,
        @Path("chapter") chapter: Int
    ): BibleChapterResponse
}

val bibleChapterCounts = mapOf(
    "GEN" to 50, "EXO" to 40, "LEV" to 27, "NUM" to 36, "DEU" to 34,
    "JOS" to 24, "JDG" to 21, "RUT" to 4, "1SA" to 31, "2SA" to 24,
    "1KI" to 22, "2KI" to 25, "1CH" to 29, "2CH" to 36, "EZR" to 10,
    "NEH" to 13, "EST" to 10, "JOB" to 42, "PSA" to 150, "PRO" to 31,
    "ECC" to 12, "SNG" to 8, "ISA" to 66, "JER" to 52, "LAM" to 5,
    "EZK" to 48, "DAN" to 12, "HOS" to 14, "JOL" to 3, "AMO" to 9,
    "OBA" to 1, "JON" to 4, "MIC" to 7, "NAM" to 3, "HAB" to 3,
    "ZEP" to 3, "HAG" to 2, "ZEC" to 14, "MAL" to 4,
    "MAT" to 28, "MRK" to 16, "LUK" to 24, "JHN" to 21, "ACT" to 28,
    "ROM" to 16, "1CO" to 16, "2CO" to 13, "GAL" to 6, "EPH" to 6,
    "PHP" to 4, "COL" to 4, "1TH" to 5, "2TH" to 3, "1TI" to 6,
    "2TI" to 4, "TIT" to 3, "PHM" to 1, "HEB" to 13, "JAS" to 5,
    "1PE" to 5, "2PE" to 3, "1JN" to 5, "2JN" to 1, "3JN" to 1,
    "JUD" to 1, "REV" to 22
)