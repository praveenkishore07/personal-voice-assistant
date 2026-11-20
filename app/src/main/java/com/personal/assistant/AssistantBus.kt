package com.personal.assistant

import android.os.Handler
import android.os.Looper

interface AssistantUiListener {
    fun onStatusChanged(running: Boolean, statusText: String)
    fun onTranscriptChanged(transcript: String)
    fun onAssistantReply(reply: String)
}

object AssistantBus {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: AssistantUiListener? = null

    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var lastStatus: String = "OFFLINE"

    fun register(uiListener: AssistantUiListener) {
        listener = uiListener
        notifyStatus(isRunning, lastStatus)
    }

    fun unregister() {
        listener = null
    }

    fun notifyStatus(running: Boolean, statusText: String) {
        isRunning = running
        lastStatus = statusText
        mainHandler.post {
            listener?.onStatusChanged(running, statusText)
        }
    }

    fun notifyTranscript(transcript: String) {
        mainHandler.post {
            listener?.onTranscriptChanged(transcript)
        }
    }

    fun notifyReply(reply: String) {
        mainHandler.post {
            listener?.onAssistantReply(reply)
        }
    }
}
