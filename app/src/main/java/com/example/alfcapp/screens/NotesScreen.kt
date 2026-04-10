package com.example.alfcapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alfcapp.data.notes.NoteEntity
import com.example.alfcapp.data.notes.NotesRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteColors = listOf(
    Color(0xFFFFFFFF), // white
    Color(0xFFFFF1C2), // butter
    Color(0xFFFFD8D8), // blush
    Color(0xFFD7F0DC), // mint
    Color(0xFFD6E6FF), // sky
    Color(0xFFE9D8FF), // lavender
    Color(0xFFFFE0B2)  // peach
)

private fun colorFor(index: Int): Color = noteColors[index.coerceIn(0, noteColors.lastIndex)]

@Composable
fun NotesScreen() {
    val context = LocalContext.current
    val repository = remember { NotesRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val notes by repository.observeNotes().collectAsState(initial = emptyList())

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var currentNoteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val currentNote = remember(currentNoteId, notes) {
        notes.firstOrNull { it.id == currentNoteId }
    }

    val filtered = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    if (editing) {
        NoteEditorScreen(
            note = currentNote,
            onSave = { title, content, colorIndex ->
                scope.launch {
                    val entity = (currentNote ?: NoteEntity(title = "", content = "")).copy(
                        title = title.ifBlank { "Untitled" },
                        content = content,
                        colorIndex = colorIndex
                    )
                    repository.upsert(entity)
                    editing = false
                    currentNoteId = null
                }
            },
            onDelete = {
                val toDelete = currentNote
                if (toDelete != null) {
                    scope.launch {
                        repository.delete(toDelete)
                        editing = false
                        currentNoteId = null
                    }
                } else {
                    editing = false
                    currentNoteId = null
                }
            },
            onBack = {
                editing = false
                currentNoteId = null
            }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    currentNoteId = null
                    editing = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (notes.isEmpty()) "Capture your sermon thoughts"
                        else "${notes.size} note${if (notes.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SearchBar(value = searchQuery, onChange = { searchQuery = it })
            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                EmptyState(hasQuery = searchQuery.isNotBlank())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = {
                                currentNoteId = note.id
                                editing = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            BasicSearchField(
                value = value,
                onChange = onChange,
                placeholder = "Search notes",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BasicSearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun NoteCard(note: NoteEntity, onClick: () -> Unit) {
    val background = colorFor(note.colorIndex)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = background,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1F1F1F),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = Color(0xFF3A3A3A),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatRelative(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1F1F1F).copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (hasQuery) "No matching notes" else "No notes yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasQuery) "Try a different keyword."
                else "Tap \"New note\" to write your first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoteEditorScreen(
    note: NoteEntity?,
    onSave: (title: String, content: String, colorIndex: Int) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var title by rememberSaveable(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by rememberSaveable(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var colorIndex by rememberSaveable(note?.id) { mutableStateOf(note?.colorIndex ?: 0) }
    var paletteOpen by remember { mutableStateOf(false) }

    val background = colorFor(colorIndex)

    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { paletteOpen = !paletteOpen }) {
                    Icon(Icons.Filled.Palette, contentDescription = "Color")
                }
                if (note != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
                FilledIconButton(
                    onClick = { onSave(title, content, colorIndex) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Save")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = paletteOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ColorPickerRow(
                    selectedIndex = colorIndex,
                    onSelect = { colorIndex = it },
                    background = background
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            EditorField(
                value = title,
                onChange = { title = it },
                placeholder = "Title",
                textSize = 28.sp,
                bold = true
            )
            Spacer(Modifier.height(8.dp))
            Divider(color = Color.Black.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))
            EditorField(
                value = content,
                onChange = { content = it },
                placeholder = "Start typing...",
                textSize = 16.sp,
                bold = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    bold: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = textSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF1F1F1F),
            lineHeight = textSize * 1.4f
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = textSize,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.4f)
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun ColorPickerRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    background: Color
) {
    Surface(
        color = background,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            noteColors.forEachIndexed { index, color ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelative(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "Just now"
        diff < hour -> "${diff / minute}m ago"
        diff < day -> "${diff / hour}h ago"
        diff < 7 * day -> "${diff / day}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
