package com.example.alfcapp.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class GivingCategory(
    val name: String,
    val icon: ImageVector,
    val accent: Color
)

private val givingCategories = listOf(
    GivingCategory("Offering", Icons.Filled.VolunteerActivism, Color(0xFF6D4AFF)),
    GivingCategory("Tithe", Icons.Filled.Savings, Color(0xFF00A37A)),
    GivingCategory("Thanksgiving", Icons.Filled.Celebration, Color(0xFFE8833A)),
    GivingCategory("Project", Icons.Filled.Construction, Color(0xFF1E88E5)),
    GivingCategory("First Fruit", Icons.Filled.Spa, Color(0xFF43A047)),
    GivingCategory("Welfare", Icons.Filled.Diversity3, Color(0xFFD81B60))
)

@Composable
fun GiveScreen() {
    val context = LocalContext.current
    val paybillNumber = "4083969"
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun copy(label: String, value: String) {
        copyToClipboard(context, label, value)
        scope.launch {
            snackbarHostState.showSnackbar("$label copied")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            GiveHeroHeader()

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))

                PaybillCard(
                    paybillNumber = paybillNumber,
                    onCopy = { copy("Paybill", paybillNumber) }
                )

                Spacer(Modifier.height(20.dp))
                HowToGiveCard(paybillNumber = paybillNumber)

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Purpose",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Tap to copy as your account number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                CategoryGrid(onCategoryClick = { copy("Purpose", it.name) })

                Spacer(Modifier.height(28.dp))
                ClosingVerse()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GiveHeroHeader() {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Give Generously",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Partner with the work God is doing through ALFC.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun PaybillCard(paybillNumber: String, onCopy: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-32).dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "M-PESA PAYBILL",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onCopy,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = paybillNumber,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(14.dp))
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy paybill",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Tap the number to copy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HowToGiveCard(paybillNumber: String) {
    var expanded by remember { mutableStateOf(false) }
    val steps = listOf(
        "Open M-PESA on your phone",
        "Select Lipa na M-PESA",
        "Choose Pay Bill",
        "Enter Business No: $paybillNumber",
        "Enter Account No (purpose below)",
        "Enter amount, then your PIN"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How to give",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    steps.forEachIndexed { index, step ->
                        StepRow(number = index + 1, text = step)
                        if (index != steps.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryGrid(onCategoryClick: (GivingCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        givingCategories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { category ->
                    CategoryChip(
                        category = category,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(category) }
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: GivingCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = category.accent.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = category.accent.copy(alpha = 0.35f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ClosingVerse() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\"Each of you should give what you have decided in your heart to give, " +
                "not reluctantly or under compulsion, for God loves a cheerful giver.\"",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "2 Corinthians 9:7",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
