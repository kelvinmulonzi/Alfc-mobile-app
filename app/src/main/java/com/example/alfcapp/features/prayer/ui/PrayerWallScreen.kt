package com.example.alfcapp.features.prayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alfcapp.data.prayer.PrayerCategory
import com.example.alfcapp.data.prayer.PrayerDto
import com.example.alfcapp.features.prayer.viewmodel.PrayerViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerWallScreen(onBack: () -> Unit) {
    val vm = remember { PrayerViewModel() }
    val snackbar = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PrayerDto?>(null) }
    var lastSubmitted by remember { mutableStateOf<PrayerDto?>(null) }

    LaunchedEffect(Unit) { vm.refresh() }

    LaunchedEffect(vm.error) {
        vm.error?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }
    LaunchedEffect(lastSubmitted) {
        lastSubmitted?.let {
            val msg = if (it.visibility.name == "PRAYER_TEAM_ONLY")
                "Sent to the prayer team. They'll be praying with you."
            else
                "Your request is on the wall. Anonymous."
            snackbar.showSnackbar(msg)
            lastSubmitted = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Wall", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Share") },
                shape = RoundedCornerShape(28.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CategoryFilterRow(
                selected = vm.selectedCategory,
                onSelect = vm::setCategory,
            )

            when {
                vm.loading && vm.prayers.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                vm.prayers.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No prayer requests yet. Be the first to share.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.prayers, key = { it.id }) { p ->
                        PrayerCard(
                            prayer = p,
                            onPray = { vm.togglePray(p.id) },
                            onDelete = { pendingDelete = p },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        PrayerRequestDialog(
            submitting = vm.submitting,
            onDismiss = { showDialog = false },
            onSubmit = { body, category, visibility ->
                vm.submit(body, category, visibility) { created ->
                    showDialog = false
                    lastSubmitted = created
                }
            },
        )
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove your request?") },
            text = { Text("You'll no longer see it on the wall.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMine(p.id)
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CategoryFilterRow(
    selected: PrayerCategory?,
    onSelect: (PrayerCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
                shape = RoundedCornerShape(50),
            )
        }
        items(PrayerCategory.values()) { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(c) },
                label = { Text(c.display()) },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

@Composable
private fun PrayerCard(
    prayer: PrayerDto,
    onPray: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryTag(prayer.category)
                Spacer(Modifier.size(8.dp))
                Text(
                    timeAgo(prayer.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (prayer.mine) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete my request",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                prayer.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            PrayButton(
                prayedByMe = prayer.prayedByMe,
                count = prayer.prayCount,
                onClick = onPray,
            )
        }
    }
}

@Composable
private fun CategoryTag(category: PrayerCategory) {
    val tint = category.tint()
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
    ) {
        Text(
            category.display(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PrayButton(
    prayedByMe: Boolean,
    count: Long,
    onClick: () -> Unit,
) {
    val container = if (prayedByMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (prayedByMe) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = contentColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = if (prayedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                if (prayedByMe) "I prayed for you · $count" else "I'll pray for you · $count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun PrayerCategory.tint(): Color = when (this) {
    PrayerCategory.HEALING -> Color(0xFF2E7D32)
    PrayerCategory.FAMILY -> Color(0xFFC2185B)
    PrayerCategory.SALVATION -> Color(0xFF6A1B9A)
    PrayerCategory.THANKSGIVING -> Color(0xFFE65100)
    PrayerCategory.GUIDANCE -> Color(0xFF1565C0)
    PrayerCategory.OTHER -> Color(0xFF455A64)
}

private fun timeAgo(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val then = Instant.parse(iso)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(then, now)
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m"
            minutes < 60 * 24 -> "${minutes / 60}h"
            else -> "${minutes / (60 * 24)}d"
        }
    } catch (_: Throwable) {
        ""
    }
}
