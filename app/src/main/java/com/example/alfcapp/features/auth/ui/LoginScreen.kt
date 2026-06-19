package com.example.alfcapp.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alfcapp.data.auth.AuthRepository
import com.example.alfcapp.screens.ChurchPrimary
import com.example.alfcapp.screens.ChurchPrimaryVariant
import com.example.alfcapp.screens.ChurchSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var registering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val passwordsMatch = password == confirmPassword
    val canSubmit = !busy && username.trim().length >= 2 &&
            password.length >= (if (registering) 8 else 1) &&
            (!registering || passwordsMatch)

    fun submit() {
        if (!canSubmit) return
        scope.launch {
            busy = true
            error = null
            try {
                if (registering) AuthRepository.register(username, password)
                else AuthRepository.login(username, password)
            } catch (t: Throwable) {
                error = friendlyError(t, registering)
            } finally {
                busy = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ChurchPrimary, ChurchPrimaryVariant, Color(0xFF0D47A1))
                )
            )
    ) {
        // BoxWithConstraints gives the available height (it shrinks as the keyboard
        // opens, since the inset paddings are applied to it). The inner content is forced
        // to be at least that tall and centered — so when the keyboard appears the content
        // overflows and the scroll smoothly pushes it up (auto-scrolling the focused field
        // above the keyboard) instead of collapsing. Nothing is ever clipped.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val available = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = available)
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ---- Brand / hero ----
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Church,
                            contentDescription = "ALFC logo",
                            tint = ChurchPrimary,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "ALFC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Abundant Life Family Chapel",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(32.dp))

                    // ---- Frosted-glass form card (blends into the gradient) ----
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AnimatedContent(
                                targetState = registering,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "headline"
                            ) { reg ->
                                Column {
                                    Text(
                                        if (reg) "Create account" else "Welcome back",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (reg) "Join the community to chat, pray, and connect."
                                        else "Sign in to continue your journey with us.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it; error = null },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                enabled = !busy,
                                shape = RoundedCornerShape(14.dp),
                                colors = glassFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; error = null },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                singleLine = true,
                                enabled = !busy,
                                shape = RoundedCornerShape(14.dp),
                                colors = glassFieldColors(),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password",
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // ---- Confirm password (only when creating an account) ----
                            AnimatedVisibility(visible = registering) {
                                Column {
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it; error = null },
                                        label = { Text("Confirm password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        singleLine = true,
                                        enabled = !busy,
                                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = glassFieldColors(),
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val hint = when {
                                        confirmPassword.isNotEmpty() && !passwordsMatch -> "Passwords don't match."
                                        else -> "Password must be at least 8 characters."
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        hint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (confirmPassword.isNotEmpty() && !passwordsMatch)
                                            Color(0xFFFFCDD2) else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            error?.let {
                                Surface(
                                    color = Color(0xFFB00020).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFCDD2).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        it,
                                        color = Color(0xFFFFE0E3),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            // ---- Primary action ----
                            GradientButton(
                                text = if (registering) "Create account" else "Sign in",
                                enabled = canSubmit,
                                busy = busy,
                                onClick = { submit() }
                            )

                            TextButton(
                                onClick = {
                                    registering = !registering
                                    error = null
                                    confirmPassword = ""
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Crossfade(targetState = registering, label = "toggle") { reg ->
                                    Text(
                                        if (reg) "Already have an account?  Sign in"
                                        else "New here?  Create an account",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        "“For where two or three gather in my name,\nthere am I with them.”  — Matthew 18:20",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
    errorBorderColor = Color(0xFFFFCDD2),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
    errorLabelColor = Color(0xFFFFCDD2),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White,
    focusedLeadingIconColor = Color.White,
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
)

@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit
) {
    val brush = if (enabled)
        Brush.horizontalGradient(listOf(ChurchSecondary, Color(0xFFFB8C00)))
    else
        Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.22f))
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !busy,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            elevation = null,
            modifier = Modifier.fillMaxSize()
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun friendlyError(t: Throwable, registering: Boolean): String {
    val msg = t.message.orEmpty()
    return when {
        msg.contains("409") -> "That username is already taken."
        msg.contains("401") -> "Invalid username or password."
        msg.contains("400") && registering -> "Username or password didn't meet the requirements."
        else -> if (registering) "Could not create account." else "Could not sign in."
    }
}
