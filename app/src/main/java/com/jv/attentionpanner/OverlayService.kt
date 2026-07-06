package com.jv.attentionpanner

import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.SecureRandom

class OverlayService : android.app.Service() {

    companion object {
        var isRunning = false
    }

    private val secureRandom = SecureRandom()
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayContainer: LifecycleComposeLayout? = null

    private var ttsManager: TTSManager? = null
    private var exoPlayer: ExoPlayer? = null

    private var minDelayMs: Long = 60000L
    private var maxDelayMs: Long = 120000L

    private val showRunnable = Runnable { showRandomContent() }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val minMin = it.getLongExtra("MIN_MINUTES", 1)
            val maxMin = it.getLongExtra("MAX_MINUTES", 2)

            minDelayMs = minMin * 60 * 1000L
            maxDelayMs = maxMin * 60 * 1000L
            if (maxDelayMs < minDelayMs) maxDelayMs = minDelayMs
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ttsManager = TTSManager(this)

        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_ONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("ok_overlay_channel", "OK Overlay", android.app.NotificationManager.IMPORTANCE_LOW)
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
            val notification = android.app.Notification.Builder(this, "ok_overlay_channel")
                .setContentTitle("Attention Panner")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
            startForeground(1, notification)
        }

        handler.removeCallbacks(showRunnable)
        handler.postDelayed(showRunnable, 5000L)
    }

    private fun scheduleNextShow() {
        val range = maxDelayMs - minDelayMs
        val randomExtra = if (range > 0) (secureRandom.nextDouble() * range).toLong() else 0L
        handler.postDelayed(showRunnable, minDelayMs + randomExtra)
    }

    private fun showRandomContent() {
        CoroutineScope(Dispatchers.IO).launch {
            val verseDb = VerseDatabaseHelper.getInstance(applicationContext)
            val mediaDb = MediaDatabaseHelper.getInstance(applicationContext)

            val totalCount = verseDb.getVerseCount() + mediaDb.getMediaCount()
            if (totalCount == 0) {
                handler.post { Toast.makeText(applicationContext, "Library Empty", Toast.LENGTH_SHORT).show() }
                scheduleNextShow()
                return@launch
            }

            val randomTicket = (secureRandom.nextDouble() * totalCount).toLong()

            if (randomTicket < verseDb.getVerseCount()) {
                val verse = verseDb.getRandomVerseSet(secureRandom)
                if (verse != null) handler.post {
                    createOverlay(null, verse)
                    ttsManager?.speak(verse.text)
                }
            } else {
                val uri = mediaDb.getRandomMediaUri(secureRandom)
                if (uri != null) handler.post { createOverlay(uri, null) }
            }
            scheduleNextShow()
        }
    }

    private fun createOverlay(uri: Uri?, verse: Verse?) {
        if (overlayContainer != null) hideOverlay()

        if (uri != null && contentResolver.getType(uri)?.startsWith("video/") == true) {
            exoPlayer?.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
        }

        overlayContainer = LifecycleComposeLayout(this).apply {
            setContent {
                MediaContent(
                    uri = uri,
                    verse = verse,
                    player = exoPlayer,
                    onClose = { hideOverlay() }
                )
            }
        }

        val layoutFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            layoutFlags,
            PixelFormat.TRANSLUCENT
        )

        overlayContainer?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        overlayContainer?.handleLifecycleEvent(Lifecycle.Event.ON_START)
        overlayContainer?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        windowManager?.addView(overlayContainer, params)
    }

    private fun hideOverlay() {
        overlayContainer?.let {
            it.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            windowManager?.removeView(it)
            overlayContainer = null
        }
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        ttsManager?.stop()
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(showRunnable)
        hideOverlay()
        ttsManager?.shutdown()
        exoPlayer?.release()
        super.onDestroy()
    }
}
