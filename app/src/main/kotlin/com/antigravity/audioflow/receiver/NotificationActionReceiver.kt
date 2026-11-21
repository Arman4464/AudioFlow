package com.antigravity.audioflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antigravity.audioflow.service.AudioRoutingService

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val outputName = intent.getStringExtra(AudioRoutingService.EXTRA_OUTPUT)
        Log.i("NotificationActionReceiver", "Received 1-tap notification switch to: $outputName")
        if (outputName != null) {
            AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_FORCE_OUTPUT) {
                putExtra(AudioRoutingService.EXTRA_OUTPUT, outputName)
            }
        }
    }
}
