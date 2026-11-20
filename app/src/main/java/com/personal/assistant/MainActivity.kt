package com.personal.assistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), AssistantUiListener, TextToSpeech.OnInitListener {

    private lateinit var config: AssistantConfig
    private lateinit var processor: CommandProcessor
    private var localTts: TextToSpeech? = null
    private var pulseAnimator: ObjectAnimator? = null

    private lateinit var tvAssistantTitle: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvOrbHint: TextView
    private lateinit var tvLiveTranscript: TextView
    private lateinit var tvOutput: TextView
    private lateinit var tvNextClassPreview: TextView
    private lateinit var viewOrbRing: View
    private lateinit var btnOrbToggle: FrameLayout

    private lateinit var etWakeWord: EditText
    private lateinit var etFavFood: EditText
    private lateinit var etFavLine: EditText
    private lateinit var etTodoList: EditText
    private lateinit var etSchedule: EditText
    private lateinit var etTestCommand: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        config = AssistantConfig(this)
        processor = CommandProcessor(this)
        localTts = TextToSpeech(this, this)

        tvAssistantTitle = findViewById(R.id.tvAssistantTitle)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvOrbHint = findViewById(R.id.tvOrbHint)
        tvLiveTranscript = findViewById(R.id.tvLiveTranscript)
        tvOutput = findViewById(R.id.tvOutput)
        tvNextClassPreview = findViewById(R.id.tvNextClassPreview)
        viewOrbRing = findViewById(R.id.viewOrbRing)
        btnOrbToggle = findViewById(R.id.btnOrbToggle)

        etWakeWord = findViewById(R.id.etWakeWord)
        etFavFood = findViewById(R.id.etFavFood)
        etFavLine = findViewById(R.id.etFavLine)
        etTodoList = findViewById(R.id.etTodoList)
        etSchedule = findViewById(R.id.etSchedule)
        etTestCommand = findViewById(R.id.etTestCommand)

        val btnSave = findViewById<TextView>(R.id.btnSaveSettings)
        val btnTest = findViewById<TextView>(R.id.btnRunTest)

        loadFieldsFromConfig()
        refreshNextClassCard()

        btnOrbToggle.setOnClickListener {
            toggleVoiceService()
        }

        btnSave.setOnClickListener {
            config.wakeWord = etWakeWord.text.toString()
            config.favFood = etFavFood.text.toString()
            config.favLine = etFavLine.text.toString()
            config.saveTodoList(etTodoList.text.toString().lines())
            config.saveScheduleText(etSchedule.text.toString())
            tvAssistantTitle.text = "${config.wakeWord.uppercase()} AI"
            refreshNextClassCard()
            tvOutput.text = "Saved! Say "${config.wakeWord}" followed by any command."
        }

        btnTest.setOnClickListener {
            val cmd = etTestCommand.text.toString()
            etTestCommand.text.clear()
            runManualCommand(cmd)
        }

        findViewById<TextView>(R.id.chipNextClass).setOnClickListener { runManualCommand("next class") }
        findViewById<TextView>(R.id.chipTodo).setOnClickListener { runManualCommand("todo list") }
        findViewById<TextView>(R.id.chipWeather).setOnClickListener { runManualCommand("weather") }
        findViewById<TextView>(R.id.chipFavFood).setOnClickListener { runManualCommand("favorite food") }
        findViewById<TextView>(R.id.chipFavLine).setOnClickListener { runManualCommand("favorite line") }
    }

    override fun onStart() {
        super.onStart()
        AssistantBus.register(this)
    }

    override fun onStop() {
        super.onStop()
        AssistantBus.unregister()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            localTts?.language = Locale.US
        }
    }

    private fun loadFieldsFromConfig() {
        tvAssistantTitle.text = "${config.wakeWord.uppercase()} AI"
        etWakeWord.setText(config.wakeWord)
        etFavFood.setText(config.favFood)
        etFavLine.setText(config.favLine)
        etTodoList.setText(config.getTodoList().joinToString("
"))
        etSchedule.setText(config.getScheduleText())
    }

    private fun refreshNextClassCard() {
        tvNextClassPreview.text = processor.getNextClass()
    }

    private fun runManualCommand(cmd: String) {
        thread {
            val reply = processor.process(cmd)
            runOnUiThread {
                tvOutput.text = reply
                etTodoList.setText(config.getTodoList().joinToString("
"))
                refreshNextClassCard()
                if (!AssistantBus.isRunning) {
                    localTts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "manual_reply")
                }
            }
        }
    }

    private fun toggleVoiceService() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
            return
        }

        val intent = Intent(this, VoiceAssistantService::class.java)
        if (!AssistantBus.isRunning) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            stopService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            ContextCompat.startForegroundService(this, Intent(this, VoiceAssistantService::class.java))
        } else if (requestCode == 100) {
            tvOutput.text = "Microphone permission is required for voice listening."
        }
    }

    override fun onStatusChanged(running: Boolean, statusText: String) {
        tvStatusBadge.text = statusText
        if (running) {
            btnOrbToggle.setBackgroundResource(R.drawable.bg_orb_active)
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_emerald))
            tvOrbHint.text = "Listening for "${config.wakeWord}" (tap to stop)"
            startPulse()
        } else {
            btnOrbToggle.setBackgroundResource(R.drawable.bg_orb_idle)
            tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            tvOrbHint.text = "Tap mic to start always-on listening"
            stopPulse()
        }
    }

    override fun onTranscriptChanged(transcript: String) {
        tvLiveTranscript.text = transcript
    }

    override fun onAssistantReply(reply: String) {
        tvOutput.text = reply
        etTodoList.setText(config.getTodoList().joinToString("
"))
        refreshNextClassCard()
    }

    private fun startPulse() {
        if (pulseAnimator != null) return
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.92f, 1.15f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.92f, 1.15f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0.15f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(viewOrbRing, scaleX, scaleY, alpha).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        viewOrbRing.scaleX = 1f
        viewOrbRing.scaleY = 1f
        viewOrbRing.alpha = 0.35f
    }

    override fun onDestroy() {
        stopPulse()
        localTts?.stop()
        localTts?.shutdown()
        super.onDestroy()
    }
}
