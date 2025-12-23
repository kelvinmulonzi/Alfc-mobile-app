package com.example.alfcapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alfcapp.data.BibleBook
import com.example.alfcapp.data.BibleApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun BibleScreen() {
    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://api.scripture.api.bible/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService = remember { retrofit.create(BibleApiService::class.java) }

    var books by remember { mutableStateOf<List<BibleBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = apiService.getBooks()
            books = response.data
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Holy Bible",
            style = MaterialTheme.typography.headlineLarge,
            color = ChurchPrimary // Using project brand color
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* Navigate to Chapters */ }
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
    }
}