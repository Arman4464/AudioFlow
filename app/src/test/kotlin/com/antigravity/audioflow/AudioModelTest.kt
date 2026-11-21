package com.antigravity.audioflow

import com.antigravity.audioflow.model.AudioEngineState
import com.antigravity.audioflow.model.AudioInputDevice
import com.antigravity.audioflow.model.AudioOutputDevice
import com.antigravity.audioflow.model.AudioRoutingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioModelTest {

    @Test
    fun defaultAudioEngineState_isSpeakerAndForcedTarget() {
        val state = AudioEngineState()
        assertEquals(AudioOutputDevice.SPEAKER, state.currentOutput)
        assertEquals(AudioInputDevice.PHONE_MIC, state.currentInput)
        assertEquals(AudioRoutingMode.FORCED_TARGET, state.mode)
        assertEquals(AudioOutputDevice.SPEAKER, state.forcedTargetOutput)
        assertFalse(state.isHeadsetPhysicallyPlugged)
        assertFalse(state.hasRealHeadsetWithMic)
        assertFalse(state.ignoreDefectiveJackInAuto)
        assertTrue(state.isGlitchGuardEnabled)
    }

    @Test
    fun audioOutputDevice_titlesAndIcons() {
        assertEquals("Speaker", AudioOutputDevice.SPEAKER.title)
        assertEquals("Headphones", AudioOutputDevice.HEADPHONES.title)
        assertEquals("Bluetooth", AudioOutputDevice.BLUETOOTH.title)
        assertEquals("Earpiece", AudioOutputDevice.EARPIECE.title)
        assertEquals("USB Audio", AudioOutputDevice.USB.title)
    }

    @Test
    fun audioRoutingModes_titlesAndDescriptions() {
        assertEquals("Forced Lock", AudioRoutingMode.FORCED_TARGET.title)
        assertEquals("Smart Auto", AudioRoutingMode.SMART_AUTO.title)
        assertEquals("Manual", AudioRoutingMode.MANUAL_OVERRIDE.title)
    }
}
