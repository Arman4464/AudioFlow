package com.antigravity.audioflow.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.antigravity.audioflow.model.AudioRoutingMode
import com.antigravity.audioflow.service.AudioRoutingService

@RequiresApi(Build.VERSION_CODES.N)
class AutoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val currentMode = AudioRoutingService.engineState.value.mode
        qsTile?.apply {
            state = if (currentMode == AudioRoutingMode.SMART_AUTO) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Auto Audio"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        AudioRoutingService.sendCommand(this, AudioRoutingService.ACTION_SET_MODE) {
            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.SMART_AUTO.name)
        }
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
}
