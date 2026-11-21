package com.antigravity.audioflow.model

enum class AudioOutputDevice(
    val title: String,
    val subtitle: String,
    val isWired: Boolean = false
) {
    SPEAKER(
        title = "Speaker",
        subtitle = "Built-in Loudspeaker"
    ),
    EARPIECE(
        title = "Earpiece",
        subtitle = "Phone Call Receiver"
    ),
    HEADPHONES(
        title = "Headphones",
        subtitle = "3.5mm Aux / Wired Headset",
        isWired = true
    ),
    BLUETOOTH(
        title = "Bluetooth",
        subtitle = "Wireless Headset / A2DP"
    ),
    USB(
        title = "USB Audio",
        subtitle = "Type-C DAC / External Soundcard",
        isWired = true
    )
}

enum class AudioInputDevice(
    val title: String,
    val subtitle: String
) {
    PHONE_MIC(
        title = "Internal Mic",
        subtitle = "Default Phone Microphone"
    ),
    HEADSET_MIC(
        title = "Headset Mic",
        subtitle = "Inline Wired Microphone"
    ),
    BLUETOOTH_MIC(
        title = "Bluetooth Mic",
        subtitle = "Wireless Headset Hands-free (SCO)"
    )
}

enum class AudioRoutingMode(
    val title: String,
    val shortDesc: String,
    val detailedDesc: String
) {
    FORCED_TARGET(
        title = "Forced Lock",
        shortDesc = "Lock to Target",
        detailedDesc = "Keeps audio permanently locked to your chosen target device. Completely overrides and suppresses phantom jack disconnects/connects."
    ),
    SMART_AUTO(
        title = "Smart Auto",
        shortDesc = "Auto + Glitch Guard",
        detailedDesc = "Normal phone detection: Headphones $\\rightarrow$ BT $\\rightarrow$ Speaker, but debounces rapid hardware flickering from damaged contacts."
    ),
    MANUAL_OVERRIDE(
        title = "Manual",
        shortDesc = "Direct Override",
        detailedDesc = "Instant 1-tap override button. Sets the hardware DAC directly and suspends background automation."
    )
}

data class AudioEngineState(
    val currentOutput: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val currentInput: AudioInputDevice = AudioInputDevice.PHONE_MIC,
    val mode: AudioRoutingMode = AudioRoutingMode.FORCED_TARGET,
    val forcedTargetOutput: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val isHeadsetPhysicallyPlugged: Boolean = false,
    val hasRealHeadsetWithMic: Boolean = false,
    val isBluetoothConnected: Boolean = false,
    val isUsbConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isGlitchGuardEnabled: Boolean = true,
    val ignoreDefectiveJackInAuto: Boolean = false,
    val isDarkMode: Boolean = false,
    val isBootRestoreEnabled: Boolean = true,
    val isServiceRunning: Boolean = false,
    val lastGlitchSuppressedTimestamp: Long = 0L,
    val glitchSuppressionCount: Int = 0
)
