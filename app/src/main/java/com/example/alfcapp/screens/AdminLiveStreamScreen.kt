package com.example.alfcapp.screens

import android.content.Context
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.alfcapp.data.livestream.LiveStreamRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera1
import com.pedro.library.view.OpenGlView
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AdminLiveStreamScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("ALFC_PREFS", Context.MODE_PRIVATE) }
    var isAuthenticated by remember { mutableStateOf(false) }
    // Pre-fill with a previously validated token so admins don't retype it each time.
    var tokenInput by remember { mutableStateOf(prefs.getString("ADMIN_TOKEN", "") ?: "") }
    var isValidating by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Admin Access Required", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter the church admin token to start a broadcast.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Admin Token") },
                singleLine = true,
                enabled = !isValidating,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                enabled = !isValidating && tokenInput.isNotBlank(),
                onClick = {
                    val token = tokenInput.trim()
                    isValidating = true
                    scope.launch {
                        // The backend's admin filter accepts the request only when the
                        // token matches `app.admin-token`, so a success means it's valid.
                        val result = LiveStreamRepository.validateAdminToken(token)
                        isValidating = false
                        if (result.isSuccess) {
                            prefs.edit().putString("ADMIN_TOKEN", token).apply()
                            isAuthenticated = true
                        } else {
                            Toast.makeText(context, "Invalid admin token", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }
        }
    } else {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    )

    // Load saved Stream Key from local storage
    val sharedPref = remember { context.getSharedPreferences("ALFC_PREFS", Context.MODE_PRIVATE) }

    var rtmpCamera by remember { mutableStateOf<RtmpCamera1?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var streamKey by rememberSaveable { mutableStateOf(sharedPref.getString("STREAM_KEY", "") ?: "") }
    
    // YouTube RTMP URL (Standard Ingest)
    val youtubeRtmpUrl = "rtmp://a.rtmp.youtube.com/live2"

    DisposableEffect(Unit) {
        // Keep screen on while streaming
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (rtmpCamera?.isStreaming == true) {
                rtmpCamera?.stopStream()
            }
            if (rtmpCamera?.isOnPreview == true) {
                rtmpCamera?.stopPreview()
            }
        }
    }

    if (!permissionsState.allPermissionsGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera and Audio permissions are required to stream.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    OpenGlView(ctx).apply {
                        val camera = RtmpCamera1(this, object : ConnectChecker {
                            override fun onConnectionStarted(url: String) {}
                            override fun onConnectionSuccess() {
                                (ctx as? android.app.Activity)?.runOnUiThread {
                                    Toast.makeText(ctx, "Connection Success", Toast.LENGTH_SHORT).show()
                                    isStreaming = true
                                }
                            }
                            override fun onConnectionFailed(reason: String) {
                                (ctx as? android.app.Activity)?.runOnUiThread {
                                    Toast.makeText(ctx, "Connection Failed: $reason", Toast.LENGTH_SHORT).show()
                                    if (rtmpCamera?.isStreaming == true) rtmpCamera?.stopStream()
                                    isStreaming = false
                                }
                            }
                            override fun onNewBitrate(bitrate: Long) {}
                            override fun onDisconnect() {
                                (ctx as? android.app.Activity)?.runOnUiThread {
                                    Toast.makeText(ctx, "Disconnected", Toast.LENGTH_SHORT).show()
                                    isStreaming = false
                                }
                            }
                            override fun onAuthError() {}
                            override fun onAuthSuccess() {}
                        })
                        rtmpCamera = camera
                        // Start preview immediately

                        // Fix: Wait for Surface to be ready before starting preview to prevent crashes
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
                                    if (!camera.isOnPreview) camera.startPreview()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Camera Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                if (camera.isOnPreview) camera.stopPreview()
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Controls Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isStreaming) {
                    OutlinedTextField(
                        value = streamKey,
                        onValueChange = { streamKey = it },
                        label = { Text("Enter YouTube Stream Key") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.8f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FloatingActionButton(
                        onClick = { rtmpCamera?.switchCamera() },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera")
                    }

                    FloatingActionButton(
                        onClick = {
                            if (!isStreaming) {
                                // Save the key permanently when starting the stream
                                sharedPref.edit().putString("STREAM_KEY", streamKey).apply()

                                if (rtmpCamera?.prepareAudio() == true && rtmpCamera?.prepareVideo() == true) {
                                    rtmpCamera?.startStream("$youtubeRtmpUrl/$streamKey")
                                } else {
                                    Toast.makeText(context, "Error preparing stream", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                rtmpCamera?.stopStream()
                                isStreaming = false
                            }
                        },
                        containerColor = if (isStreaming) Color.Red else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            if (isStreaming) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isStreaming) "Stop Stream" else "Start Stream"
                        )
                    }
                }
            }
        }
    }
    }
}