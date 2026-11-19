package com.personal.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.Locale
import kotlin.concurrent.thread

class VoiceAssistantService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private var speechService: SpeechService? = null
    private var tts: TextToSpeech? = null
    private lateinit var config: AssistantConfig
    private lateinit var processor: CommandProcessor
    private var waitingForCommand = false

    override fun onCreate() {
        super.onCreate()
        config = AssistantConfig(this)
        processor = CommandProcessor(this)
        tts = TextToSpeech(this, this)
        startForeground(101, buildNotification("Listening for '${config.wakeWord}'"))

        StorageService.unpack(
            this,
            "model-en-us",
            "model",
            { model: Model ->
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f).apply {
                    startListening(this@VoiceAssistantService)
                }
            },
            { _ ->
                speak("Voice model not found in assets.")
            }
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractText(hypothesis)
        if (text.isEmpty()) return

        val wake = config.wakeWord
        if (text.contains(wake)) {
            val afterWake = text.substringAfter(wake).trim()
            if (afterWake.isNotEmpty()) {
                handleCommand(afterWake)
            } else {
                waitingForCommand = true
                speak("Yes?")
            }
        } else if (waitingForCommand) {
            waitingForCommand = false
            handleCommand(text)
        }
    }

    override fun onPartialResult(hypothesis: String?) {}
    override fun onFinalResult(hypothesis: String?) {}
    override fun onError(exception: Exception?) {}
    override fun onTimeout() {}

    private fun handleCommand(command: String) {
        thread {
            val reply = processor.process(command)
            speak(reply)
        }
    }

    private fun extractText(json: String?): String {
        if (json.isNullOrBlank()) return ""
        return try {
            JSONObject(json).optString("text", "").lowercase().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "assistant_reply")
    }

    private fun buildNotification(content: String): Notification {
        val channelId = "assistant_channel"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "Personal Assistant",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Personal Assistant Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
