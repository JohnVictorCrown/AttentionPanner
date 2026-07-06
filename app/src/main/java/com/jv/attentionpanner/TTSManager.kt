package com.jv.attentionpanner

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

class TTSManager(context: Context) {
    private var tts: TextToSpeech? = null
    var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                    val bestVoice = tts?.voices?.sortedWith(
                        compareByDescending<Voice> { it.name.contains("network", ignoreCase = true) }
                            .thenByDescending { it.quality }
                    )?.firstOrNull { it.locale == Locale.US || it.locale.language == "en" }
                    bestVoice?.let { tts?.voice = it }
                }
            }
        }
    }

    fun speak(text: String) {
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
