package com.jv.attentionpanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) Toast.makeText(this, "Permissions Granted! Syncing...", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        checkStoragePermissions()

        setContent {
            var minMinutes by remember { mutableStateOf("1") }
            var maxMinutes by remember { mutableStateOf("2") }

            var statusMessage by remember { mutableStateOf("Initializing...") }
            var isDownloading by remember { mutableStateOf(false) }
            var mediaCount by remember { mutableStateOf(0) }
            var verseCount by remember { mutableStateOf(0) }
            var serviceRunning by remember { mutableStateOf(false) }

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(packageName)

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val verseHelper = VerseDatabaseHelper.getInstance(applicationContext)
                    val count = verseHelper.getVerseCount()

                    if (count == 0) {
                        isDownloading = true
                        withContext(Dispatchers.Main) { statusMessage = "Loading Bible Data..." }
                        val success = VerseLoader.loadFromAssets(applicationContext, verseHelper)
                        verseCount = verseHelper.getVerseCount()
                        isDownloading = false
                        if (!success || verseCount == 0) {
                            withContext(Dispatchers.Main) { statusMessage = "Failed to load verses (check Logcat)" }
                        }
                    } else {
                        verseCount = count
                    }
                }

                if (hasStoragePermissions()) {
                    withContext(Dispatchers.Main) { statusMessage = "Syncing Media..." }
                    mediaCount = withContext(Dispatchers.IO) {
                        val helper = MediaDatabaseHelper.getInstance(applicationContext)
                        helper.syncFromMediaStore(applicationContext)
                        helper.getMediaCount()
                    }
                    withContext(Dispatchers.Main) { statusMessage = "Ready" }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        serviceRunning = OverlayService.isRunning
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("Attention Panner", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDownloading) {
                        Text(statusMessage, color = Color.Yellow, style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    } else {
                         Text("Library Status:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                         Text("Images/Videos: $mediaCount", color = Color.Green, style = MaterialTheme.typography.titleMedium)
                         Text("Verses: $verseCount", color = Color.Cyan, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Magenta, unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Magenta, unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White
                    )

                    OutlinedTextField(
                        value = minMinutes, onValueChange = { minMinutes = it },
                        label = { Text("Min Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), colors = colors
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = maxMinutes, onValueChange = { maxMinutes = it },
                        label = { Text("Max Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), colors = colors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isIgnoringBatteryOptimizations) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                @SuppressLint("BatteryLife")
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                intent.data = Uri.parse("package:$packageName")
                                startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Background Run (Important!)", color = Color.Yellow)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (serviceRunning) {
                        Button(
                            onClick = {
                                stopService(Intent(this@MainActivity, OverlayService::class.java))
                                serviceRunning = false
                                Toast.makeText(this@MainActivity, "Service Stopped", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Stop Service", color = Color(0xFFFF4444)) }
                    } else {
                        Button(
                            onClick = {
                                if (Settings.canDrawOverlays(this@MainActivity)) {
                                    val intent = Intent(this@MainActivity, OverlayService::class.java)
                                    intent.putExtra("MIN_MINUTES", minMinutes.toLongOrNull() ?: 1L)
                                    intent.putExtra("MAX_MINUTES", maxMinutes.toLongOrNull() ?: 2L)
                                    startService(intent)
                                    serviceRunning = true
                                    Toast.makeText(this@MainActivity, "Service Started!", Toast.LENGTH_SHORT).show()
                                    moveTaskToBack(true)
                                } else {
                                    Toast.makeText(this@MainActivity, "Overlay Permission required!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Start Service") }
                    }
                }
            }
        }
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS))
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun hasStoragePermissions(): Boolean = true
}
