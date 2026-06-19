package com.example.alfcapp.features.prayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alfcapp.data.prayer.PrayerCategory
import com.example.alfcapp.data.prayer.PrayerVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerRequestDialog(
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (body: String, category: PrayerCategory, visibility: PrayerVisibility) -> Unit,
) {
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PrayerCategory.OTHER) }
    var visibility by remember { mutableStateOf(PrayerVisibility.PUBLIC_WALL) }

    val canSubmit = body.trim().length in 1..500 && !submitting

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Share a prayer request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Your name is never shown on the wall. " +
                            "The prayer team can see who submitted private requests so they can follow up.",
                    fontWeight = FontWeight.Normal,
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 500) body = it },
                    label = { Text("Your request") },
                    placeholder = { Text("What can we pray with you about?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${body.length}/500") },
                )

                Text("Category", fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(PrayerCategory.values()) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.display()) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }

                Text("Who sees this?", fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = visibility == PrayerVisibility.PUBLIC_WALL,
                        onClick = { visibility = PrayerVisibility.PUBLIC_WALL },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Share on wall") }
                    SegmentedButton(
                        selected = visibility == PrayerVisibility.PRAYER_TEAM_ONLY,
                        onClick = { visibility = PrayerVisibility.PRAYER_TEAM_ONLY },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Prayer team only") }
                }
                if (visibility == PrayerVisibility.PRAYER_TEAM_ONLY) {
                    Text(
                        "Only pastors and the prayer team will see this one.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(body.trim(), category, visibility) },
                enabled = canSubmit,
            ) { Text(if (submitting) "Sending..." else "Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancel") }
        },
    )
}

fun PrayerCategory.display(): String = when (this) {
    PrayerCategory.HEALING -> "Healing"
    PrayerCategory.FAMILY -> "Family"
    PrayerCategory.SALVATION -> "Salvation"
    PrayerCategory.THANKSGIVING -> "Thanksgiving"
    PrayerCategory.GUIDANCE -> "Guidance"
    PrayerCategory.OTHER -> "Other"
}
