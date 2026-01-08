package com.example.alfcapp.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun NotesScreen(
    onNoteClick: (Note) -> Unit = {},
    onAddNoteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("notes_app_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    var notes by remember {
        mutableStateOf(
            try {
                val json = sharedPreferences.getString("notes_list", null)
                if (json != null) {
                    val type = object : TypeToken<List<Note>>() {}.type
                    gson.fromJson<List<Note>>(json, type)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf<Note?>(null) }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.content.contains(searchQuery, ignoreCase = true)
    }

    if (isEditing) {
        NoteEditorScreen(
            note = currentNote,
            onSave = { title, content ->
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                val date = dateFormat.format(Date())

                val updatedList = if (currentNote == null) {
                    val newNote = Note(UUID.randomUUID().toString(), title, content, date)
                    listOf(newNote) + notes
                } else {
                    notes.map {
                        if (it.id == currentNote!!.id) it.copy(title = title, content = content, date = date)
                        else it
                    }
                }

                notes = updatedList
                val json = gson.toJson(updatedList)
                sharedPreferences.edit().putString("notes_list", json).apply()

                isEditing = false
                currentNote = null
            },
            onBack = {
                isEditing = false
                currentNote = null
            }
        )
    } else {
    Scaffold(
        containerColor = Color(0xFFF2F2F2), // Light gray background similar to Samsung Notes
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentNote = null
                    isEditing = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Search Bar Look-alike
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE6E6E6),
                    unfocusedContainerColor = Color(0xFFE6E6E6),
                    disabledContainerColor = Color(0xFFE6E6E6),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                placeholder = { Text("Search notes", color = Color.Gray) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteItem(note = note, onClick = {
                        currentNote = note
                        isEditing = true
                    })
                }
            }
        }
    }
    }
}

@Composable
fun NoteEditorScreen(
    note: Note?,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                TextButton(onClick = { onSave(title, content) }) {
                    Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium.copy(color = Color.Gray)) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Start typing...", style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.date,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

data class Note(val id: String, val title: String, val content: String, val date: String)