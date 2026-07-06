package com.jv.attentionpanner

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun MediaContent(uri: Uri?, verse: Verse?, player: ExoPlayer?, onClose: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight().heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF130433))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (uri != null) {
                    val mime = context.contentResolver.getType(uri) ?: ""
                    if (mime.startsWith("image/")) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )
                    } else if (mime.startsWith("video/") && player != null) {
                        DisposableEffect(Unit) { onDispose { } }
                        AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
                            modifier = Modifier.fillMaxWidth().height(400.dp)
                        )
                    }
                } else if (verse != null) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = verse.text, style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic), color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "— ${verse.reference}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Cyan, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), shape = CircleShape).size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
