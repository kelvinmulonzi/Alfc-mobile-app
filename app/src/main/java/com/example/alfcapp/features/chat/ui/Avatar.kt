package com.example.alfcapp.features.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

private val AVATAR_PALETTE = listOf(
    Color(0xFF1E88E5), Color(0xFFD81B60), Color(0xFF8E24AA),
    Color(0xFF6D4AFF), Color(0xFF00897B), Color(0xFFE8833A),
    Color(0xFF43A047), Color(0xFF5E35B1), Color(0xFFE53935),
)

private fun colorForName(name: String): Color {
    if (name.isEmpty()) return AVATAR_PALETTE[0]
    val idx = (name.hashCode() and Int.MAX_VALUE) % AVATAR_PALETTE.size
    return AVATAR_PALETTE[idx]
}

private fun initialsOf(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(" ", "_", "-", ".").filter { it.isNotEmpty() }
    val first = parts.getOrNull(0)?.firstOrNull()?.uppercase() ?: "?"
    val second = parts.getOrNull(1)?.firstOrNull()?.uppercase()
    return if (second != null) first + second else first
}

@Composable
fun UsernameAvatar(
    username: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val bg = colorForName(username.lowercase())
    val initials = initialsOf(username)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value / 2.5f).sp
        )
    }
}
