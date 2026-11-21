package com.antigravity.audioflow.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.antigravity.audioflow.model.AudioOutputDevice
import com.antigravity.audioflow.service.AudioRoutingService

@RequiresApi(Build.VERSION_CODES.N)
class SpeakerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val currentOutput = AudioRoutingService.engineState.value.currentOutput
        qsTile?.apply {
            state = if (currentOutput == AudioOutputDevice.SPEAKER) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Speaker"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        AudioRoutingService.sendCommand(this, AudioRoutingService.ACTION_FORCE_OUTPUT) {
            putExtra(AudioRoutingService.EXTRA_OUTPUT, AudioOutputDevice.SPEAKER.name)
        }
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
}
