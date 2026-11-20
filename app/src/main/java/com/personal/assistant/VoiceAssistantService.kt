package com.personal.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.ServiceCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

class VoiceAssistantService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private var speechService: SpeechService? = null
    private var voskModel: Model? = null
    private var tts: TextToSpeech? = null
    private lateinit var config: AssistantConfig
    private lateinit var processor: CommandProcessor
    private var waitingForCommand = false

    override fun onCreate() {
        super.onCreate()
        config = AssistantConfig(this)
        processor = CommandProcessor(this)
        tts = TextToSpeech(this, this)

        val notification = buildNotification("Loading offline voice model...")
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, 101, notification, fgType)
        AssistantBus.notifyStatus(true, "LOADING MODEL")

        initOfflineModel()
    }

    private fun initOfflineModel() {
        thread {
            try {
                val targetDir = File(filesDir, "vosk-model-en-us")
                val marker = File(targetDir, "am/final.mdl")
                if (!marker.exists()) {
                    targetDir.deleteRecursively()
                    val copied = copyAssetFolder("model-en-us", targetDir)
                    if (!copied || !marker.exists()) {
                        AssistantBus.notifyStatus(false, "MODEL MISSING")
                        AssistantBus.notifyReply("Offline voice model files were not bundled in assets/model-en-us.")
                        speak("Offline voice model could not be loaded.")
                        stopSelf()
                        return@thread
                    }
                }

                val model = Model(targetDir.absolutePath)
                voskModel = model
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f).apply {
                    startListening(this@VoiceAssistantService)
                }

                val wake = config.wakeWord
                updateNotification("Listening for '$wake'")
                AssistantBus.notifyStatus(true, "LISTENING")
                AssistantBus.notifyTranscript("Listening for '$wake'...")
            } catch (e: Exception) {
                AssistantBus.notifyStatus(false, "MIC ERROR")
                AssistantBus.notifyReply("Could not start microphone: ${e.message ?: "unknown error"}")
                stopSelf()
            }
        }
    }

    private fun copyAssetFolder(assetPath: String, outDir: File): Boolean {
        val items = assets.list(assetPath) ?: return false
        if (items.isEmpty()) return false
        outDir.mkdirs()
        for (item in items) {
            val subAssetPath = "$assetPath/$item"
            val subItems = assets.list(subAssetPath)
            if (!subItems.isNullOrEmpty()) {
                copyAssetFolder(subAssetPath, File(outDir, item))
            } else {
                assets.open(subAssetPath).use { input ->
                    File(outDir, item).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        return true
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speechService?.setPause(true)
                }

                override fun onDone(utteranceId: String?) {
                    speechService?.setPause(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speechService?.setPause(false)
                }
            })
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        val partial = extractField(hypothesis, "partial")
        if (partial.isNotEmpty()) {
            AssistantBus.notifyTranscript("Hearing: '$partial'")
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = extractField(hypothesis, "text")
        if (text.isEmpty()) return

        AssistantBus.notifyTranscript("Heard: '$text'")
        val wake = config.wakeWord
        if (text.contains(wake)) {
            val afterWake = text.substringAfter(wake).trim()
            if (afterWake.isNotEmpty()) {
                handleCommand(afterWake)
            } else {
                waitingForCommand = true
                AssistantBus.notifyStatus(true, "AWAITING CMD")
                speak("Yes?")
            }
        } else if (waitingForCommand) {
            waitingForCommand = false
            AssistantBus.notifyStatus(true, "LISTENING")
            handleCommand(text)
        }
    }

    override fun onFinalResult(hypothesis: String?) {}

    override fun onError(exception: Exception?) {
        AssistantBus.notifyTranscript("Mic error: ${exception?.message ?: "retrying"}")
    }

    override fun onTimeout() {}

    private fun handleCommand(command: String) {
        thread {
            val reply = processor.process(command)
            AssistantBus.notifyReply(reply)
            speak(reply)
        }
    }

    private fun extractField(json: String?, key: String): String {
        if (json.isNullOrBlank()) return ""
        return try {
            JSONObject(json).optString(key, "").lowercase().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "assistant_reply")
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(101, buildNotification(content))
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
            .setContentTitle("Personal Assistant")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        voskModel?.close()
        tts?.stop()
        tts?.shutdown()
        AssistantBus.notifyStatus(false, "OFFLINE")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
