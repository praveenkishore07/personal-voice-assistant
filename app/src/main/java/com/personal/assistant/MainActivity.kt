package com.personal.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var config: AssistantConfig
    private lateinit var processor: CommandProcessor
    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        config = AssistantConfig(this)
        processor = CommandProcessor(this)

        val etWakeWord = findViewById<EditText>(R.id.etWakeWord)
        val etFavFood = findViewById<EditText>(R.id.etFavFood)
        val etFavLine = findViewById<EditText>(R.id.etFavLine)
        val etTodoList = findViewById<EditText>(R.id.etTodoList)
        val etTestCommand = findViewById<EditText>(R.id.etTestCommand)
        val tvOutput = findViewById<TextView>(R.id.tvOutput)
        val btnToggle = findViewById<Button>(R.id.btnToggleService)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnTest = findViewById<Button>(R.id.btnRunTest)

        etWakeWord.setText(config.wakeWord)
        etFavFood.setText(config.favFood)
        etFavLine.setText(config.favLine)
        etTodoList.setText(config.getTodoList().joinToString("\n"))

        btnSave.setOnClickListener {
            config.wakeWord = etWakeWord.text.toString()
            config.favFood = etFavFood.text.toString()
            config.favLine = etFavLine.text.toString()
            config.saveTodoList(etTodoList.text.toString().lines())
            tvOutput.text = "Saved! Wake word is now '${config.wakeWord}'."
        }

        btnToggle.setOnClickListener {
            if (!hasMicPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    100
                )
                return@setOnClickListener
            }
            val intent = Intent(this, VoiceAssistantService::class.java)
            if (!serviceRunning) {
                ContextCompat.startForegroundService(this, intent)
                serviceRunning = true
                btnToggle.text = "Stop Always-On Listener"
            } else {
                stopService(intent)
                serviceRunning = false
                btnToggle.text = "Start Always-On Listener"
            }
        }

        btnTest.setOnClickListener {
            val cmd = etTestCommand.text.toString()
            thread {
                val reply = processor.process(cmd)
                runOnUiThread {
                    tvOutput.text = reply
                    etTodoList.setText(config.getTodoList().joinToString("\n"))
                }
            }
        }
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
