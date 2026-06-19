package com.example.alfcapp.features.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.alfcapp.data.auth.Session
import com.example.alfcapp.data.auth.TokenStore

/**
 * Top-level gate. Shows LoginScreen when there's no session, otherwise
 * renders the authenticated app.
 */
@Composable
fun AuthGate(authenticatedContent: @Composable () -> Unit) {
    val session: Session? by TokenStore.session.collectAsState(initial = null)
    var sessionLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(session) { sessionLoaded = true }

    when {
        !sessionLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        session == null -> LoginScreen()
        else -> authenticatedContent()
    }
}
