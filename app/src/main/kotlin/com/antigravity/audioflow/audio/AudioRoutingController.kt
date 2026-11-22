package com.antigravity.audioflow.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.antigravity.audioflow.model.AudioInputDevice
import com.antigravity.audioflow.model.AudioOutputDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import kotlin.math.PI
import kotlin.math.sin

/**
 * Robust Low-Level Audio Routing Controller.
 * Supports Android 9/10 (Samsung Galaxy S9+ / Exynos / Qualcomm) through Android 15.
 *
 * Implements a 3-tier routing architecture:
 * 1. AudioDeviceInventory reflection: setWiredDeviceConnectionState (overrides STREAM_MUSIC/Spotify/YouTube)
 * 2. AudioPolicyManager / Communication mode: setSpeakerphoneOn + setMode (overrides STREAM_VOICE_CALL/VoIP)
 * 3. Samsung HAL parameters: setParameters("routing=...")
 * 4. Android 12+ CommunicationDevice API
 */
class AudioRoutingController(private val context: Context) {

    companion object {
        private const val TAG = "AudioRoutingController"
        private const val GLITCH_WINDOW_MS = 800L

        // Android AudioSystem / AudioDeviceInventory internal types
        const val DEVICE_OUT_EARPIECE = 0x1
        const val DEVICE_OUT_SPEAKER = 0x2
        const val DEVICE_OUT_WIRED_HEADSET = 0x4
        const val DEVICE_OUT_WIRED_HEADPHONE = 0x8
        const val DEVICE_OUT_BLUETOOTH_SCO = 0x10
        const val DEVICE_OUT_BLUETOOTH_A2DP = 0x80
        const val DEVICE_OUT_LINE = 0x20000
        const val DEVICE_OUT_AUX_LINE = 0x200000
        const val DEVICE_OUT_USB_DEVICE = 0x4000
        const val DEVICE_OUT_USB_HEADSET = 0x4000000

        const val DEVICE_STATE_UNAVAILABLE = 0
        const val DEVICE_STATE_AVAILABLE = 1

        // AudioSystem.setForceUse constants
        const val FOR_COMMUNICATION = 0
        const val FOR_MEDIA = 1
        const val FOR_RECORD = 2
        const val FOR_DOCK = 3
        const val FOR_SYSTEM = 4
        const val FOR_HDMI_SYSTEM_AUDIO = 5
        const val FOR_ENCODED_SURROUND = 6
        const val FOR_VIBRATE_RINGING = 7

        const val FORCE_NONE = 0
        const val FORCE_SPEAKER = 1
        const val FORCE_HEADPHONES = 2
        const val FORCE_BT_SCO = 3
        const val FORCE_BT_A2DP = 4
        const val FORCE_WIRED_ACCESSORY = 5
        const val FORCE_NO_BT_A2DP = 10
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var setWiredDeviceConnectionStateMethod4: Method? = null
    private var setWiredDeviceConnectionStateMethod3: Method? = null
    private var setForceUseMethod: Method? = null

    private var lastHeadsetPlugTime: Long = 0L
    private var lastPlugState: Int = -1
    var suppressedGlitchCount: Int = 0
        private set

    @Volatile
    var lastInternalRoutingTimestamp: Long = 0L
        private set

    fun markInternalRoutingAction() {
        lastInternalRoutingTimestamp = System.currentTimeMillis()
    }

    init {
        findReflectionMethods()
    }

    private fun findReflectionMethods() {
        // Double-reflection to bypass ART hidden API restrictions on Android 9/10 (One UI 2.5)
        try {
            val classArrayClass = Class.forName("[Ljava.lang.Class;")
            val stringArrayClass = Class.forName("[Ljava.lang.String;")
            val forNameMethod = java.lang.Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = java.lang.Class::class.java.getDeclaredMethod(
                "getDeclaredMethod",
                String::class.java,
                classArrayClass
            )
            val vmRuntimeClass = forNameMethod.invoke(null, "dalvik.system.VMRuntime") as java.lang.Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
            val setHiddenApiExemptions = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                arrayOf<java.lang.Class<*>>(stringArrayClass)
            ) as Method
            val vmRuntime = getRuntime.invoke(null)
            setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L"))
            Log.i(TAG, "ART Hidden API exemptions successfully granted")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to set Hidden API exemptions: ${e.message}")
        }

        try {
            val declaredMethods = AudioManager::class.java.declaredMethods
            for (m in declaredMethods) {
                if (m.name == "setWiredDeviceConnectionState") {
                    m.isAccessible = true
                    if (m.parameterTypes.size == 4) {
                        setWiredDeviceConnectionStateMethod4 = m
                    } else if (m.parameterTypes.size == 3) {
                        setWiredDeviceConnectionStateMethod3 = m
                    }
                }
            }
            Log.d(TAG, "AudioRoutingController reflection init: method4=$setWiredDeviceConnectionStateMethod4, method3=$setWiredDeviceConnectionStateMethod3")
        } catch (e: Exception) {
            Log.w(TAG, "AudioManager reflection lookup error: ${e.message}")
        }

        try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            setForceUseMethod = audioSystemClass.getDeclaredMethod("setForceUse", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            setForceUseMethod?.isAccessible = true
            Log.d(TAG, "AudioSystem.setForceUse reflection init: $setForceUseMethod")
        } catch (e: Exception) {
            Log.w(TAG, "AudioSystem.setForceUse reflection error: ${e.message}")
        }
    }

    /**
     * Invokes AudioSystem.setForceUse via reflection.
     */
    fun setForceUse(usage: Int, config: Int) {
        markInternalRoutingAction()
        try {
            setForceUseMethod?.invoke(null, usage, config)
            Log.d(TAG, "Invoked AudioSystem.setForceUse(usage=$usage, config=$config)")
        } catch (e: Exception) {
            Log.w(TAG, "setForceUse error: ${e.message}")
        }
    }

    /**
     * Inspects a HEADSET_PLUG broadcast.
     * Returns true if the event was recognized as phantom hardware jitter and suppressed.
     */
    fun shouldSuppressJackEvent(plugState: Int): Boolean {
        val now = System.currentTimeMillis()
        val delta = now - lastHeadsetPlugTime
        lastHeadsetPlugTime = now

        if (lastPlugState != -1 && lastPlugState != plugState && delta < GLITCH_WINDOW_MS) {
            suppressedGlitchCount++
            Log.d(TAG, "Glitch Guard: Suppressed fluttering headset plug event (delta: ${delta}ms, total: $suppressedGlitchCount)")
            lastPlugState = plugState
            return true
        }
        lastPlugState = plugState
        return false
    }

    /**
     * Dispatches low-level wired device connection state to Android AudioService.
     */
    fun setWiredDeviceState(type: Int, state: Int, name: String = "h2w") {
        markInternalRoutingAction()
        try {
            if (setWiredDeviceConnectionStateMethod4 != null) {
                setWiredDeviceConnectionStateMethod4?.invoke(audioManager, type, state, "", name)
                Log.d(TAG, "Invoked setWiredDeviceConnectionState(type=$type, state=$state, addr='', name='$name')")
            } else if (setWiredDeviceConnectionStateMethod3 != null) {
                setWiredDeviceConnectionStateMethod3?.invoke(audioManager, type, state, name)
                Log.d(TAG, "Invoked setWiredDeviceConnectionState(type=$type, state=$state, name='$name')")
            }
        } catch (e: Exception) {
            Log.w(TAG, "setWiredDeviceState error: ${e.message}")
        }
    }

    /**
     * Core audio routing engine supporting all Android generations.
     */
    fun routeToOutput(target: AudioOutputDevice): Boolean {
        markInternalRoutingAction()
        Log.i(TAG, "Routing audio to output target: $target")
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    routeUsingModernApi(target)
                }
                else -> {
                    routeUsingLegacyApi(target)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error routing to $target: ${e.message}", e)
            false
        }
    }

    private fun routeUsingModernApi(target: AudioOutputDevice): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

        val devices = audioManager.availableCommunicationDevices
        val targetType = when (target) {
            AudioOutputDevice.SPEAKER -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            AudioOutputDevice.EARPIECE -> AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            AudioOutputDevice.HEADPHONES -> AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            AudioOutputDevice.BLUETOOTH -> AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            AudioOutputDevice.USB -> AudioDeviceInfo.TYPE_USB_DEVICE
        }

        var matchedDevice = devices.find { it.type == targetType }
        if (matchedDevice == null && target == AudioOutputDevice.HEADPHONES) {
            matchedDevice = devices.find { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
        }
        if (matchedDevice == null && target == AudioOutputDevice.BLUETOOTH) {
            matchedDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        }
        if (matchedDevice == null && target == AudioOutputDevice.USB) {
            matchedDevice = devices.find { it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
        }

        return if (matchedDevice != null) {
            audioManager.setCommunicationDevice(matchedDevice)
        } else {
            routeUsingLegacyApi(target)
        }
    }

    private fun routeUsingLegacyApi(target: AudioOutputDevice): Boolean {
        return when (target) {
            AudioOutputDevice.SPEAKER -> {
                // 1. AudioSystem.setForceUse: force speaker for communication & reset media/ringing
                setForceUse(FOR_COMMUNICATION, FORCE_SPEAKER)
                setForceUse(FOR_MEDIA, FORCE_NONE)
                setForceUse(FOR_VIBRATE_RINGING, FORCE_NONE)

                // 2. Disconnect wired accessories in AudioDeviceInventory so STREAM_MUSIC drops back to Speaker
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADPHONE, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADPHONE, DEVICE_STATE_UNAVAILABLE, "h2w")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "h2w")
                setWiredDeviceState(DEVICE_OUT_USB_DEVICE, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_USB_HEADSET, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_LINE, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_AUX_LINE, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")

                // 3. Keep MODE_NORMAL when not in an actual call
                // CRITICAL FIX: Setting MODE_IN_COMMUNICATION when not in a call causes Samsung One UI
                // to show an ongoing call indicator/green call icon 24/7 and locks the volume keys to call volume.
                if (audioManager.mode != AudioManager.MODE_IN_CALL) {
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }

                // 4. Samsung HAL parameters
                try { audioManager.setParameters("routing=2") } catch (ignored: Exception) {}
                true
            }
            AudioOutputDevice.EARPIECE -> {
                setForceUse(FOR_COMMUNICATION, FORCE_NONE)
                setForceUse(FOR_MEDIA, FORCE_NONE)

                setWiredDeviceState(DEVICE_OUT_WIRED_HEADPHONE, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADPHONE, DEVICE_STATE_UNAVAILABLE, "h2w")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "h2w")
                setWiredDeviceState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "earpiece")

                // Keep MODE_NORMAL for media to prevent Samsung One UI persistent call pill
                if (audioManager.mode != AudioManager.MODE_IN_CALL) {
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }
                try { audioManager.setParameters("routing=1") } catch (ignored: Exception) {}
                try { audioManager.setParameters("audio_devices=1") } catch (ignored: Exception) {}
                true
            }
            AudioOutputDevice.HEADPHONES -> {
                // 1. AudioSystem.setForceUse: allow wired accessories
                setForceUse(FOR_COMMUNICATION, FORCE_NONE)
                setForceUse(FOR_MEDIA, FORCE_NONE)
                setForceUse(FOR_VIBRATE_RINGING, FORCE_NONE)

                // 2. Force wired headphone and headset as AVAILABLE for all real 3-pole and 4-pole plugs
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADPHONE, DEVICE_STATE_AVAILABLE, "h2w")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "h2w")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADPHONE, DEVICE_STATE_AVAILABLE, "las_enabled_device")
                setWiredDeviceState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "las_enabled_device")

                if (audioManager.mode != AudioManager.MODE_IN_CALL) {
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }
                try { audioManager.setParameters("routing=8") } catch (ignored: Exception) {}
                try { audioManager.setParameters("routing=4") } catch (ignored: Exception) {}
                true
            }
            AudioOutputDevice.BLUETOOTH -> {
                setForceUse(FOR_COMMUNICATION, FORCE_BT_SCO)
                setForceUse(FOR_MEDIA, FORCE_BT_A2DP)

                // High-fidelity A2DP stereo media: keep MODE_NORMAL without forcing mono SCO call mode
                if (audioManager.mode != AudioManager.MODE_IN_CALL) {
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }
                // CRITICAL FIX: Route to A2DP Stereo (128 = 0x80), NOT telephone SCO (16 = 0x10)
                try { audioManager.setParameters("routing=128") } catch (ignored: Exception) {}
                true
            }
            AudioOutputDevice.USB -> {
                setForceUse(FOR_COMMUNICATION, FORCE_NONE)
                setForceUse(FOR_MEDIA, FORCE_NONE)

                setWiredDeviceState(DEVICE_OUT_USB_DEVICE, DEVICE_STATE_AVAILABLE, "usb_device")
                setWiredDeviceState(DEVICE_OUT_USB_HEADSET, DEVICE_STATE_AVAILABLE, "usb_headset")

                if (audioManager.mode != AudioManager.MODE_IN_CALL) {
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }
                try { audioManager.setParameters("routing=16384") } catch (ignored: Exception) {}
                true
            }
        }
    }

    /**
     * Microphones routing (Phone Mic, Headset Mic, Bluetooth Mic).
     */
    fun routeToInput(target: AudioInputDevice): Boolean {
        Log.i(TAG, "Routing audio to microphone target: $target")
        return try {
            when (target) {
                AudioInputDevice.BLUETOOTH_MIC -> {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                    try { audioManager.setParameters("input_source=bluetooth") } catch (ignored: Exception) {}
                    true
                }
                AudioInputDevice.PHONE_MIC -> {
                    if (audioManager.isBluetoothScoOn) {
                        audioManager.isBluetoothScoOn = false
                        audioManager.stopBluetoothSco()
                    }
                    try { audioManager.setParameters("input_source=builtin_mic") } catch (ignored: Exception) {}
                    true
                }
                AudioInputDevice.HEADSET_MIC -> {
                    if (audioManager.isBluetoothScoOn) {
                        audioManager.isBluetoothScoOn = false
                        audioManager.stopBluetoothSco()
                    }
                    try { audioManager.setParameters("input_source=wired_headset") } catch (ignored: Exception) {}
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting microphone route to $target: ${e.message}", e)
            false
        }
    }

    /**
     * Clears overrides and resets back to normal system default.
     */
    fun resetRouting() {
        Log.i(TAG, "Resetting audio routing to OS default")
        try {
            setForceUse(FOR_COMMUNICATION, FORCE_NONE)
            setForceUse(FOR_MEDIA, FORCE_NONE)
            setForceUse(FOR_VIBRATE_RINGING, FORCE_NONE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            if (audioManager.isBluetoothScoOn) {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting audio routing: ${e.message}", e)
        }
    }

    /**
     * Synthesizes and plays a friendly test chime directly to confirm that
     * audio is properly routed to the chosen device.
     */
    fun playTestChime(onDone: () -> Unit = {}) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                val durationSeconds = 0.35
                val numSamples = (durationSeconds * sampleRate).toInt()
                val samples = ShortArray(numSamples)

                val freq1 = 587.33 // D5
                val freq2 = 880.00 // A5

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = (1.0 - (t / durationSeconds)).coerceIn(0.0, 1.0)
                    val sampleValue = 0.5 * sin(2.0 * PI * freq1 * t) + 0.5 * sin(2.0 * PI * freq2 * t)
                    samples[i] = (sampleValue * envelope * Short.MAX_VALUE * 0.7).toInt().toShort()
                }

                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuffer, samples.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                kotlinx.coroutines.delay(400)
                audioTrack.release()
                onDone()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing test chime: ${e.message}", e)
                onDone()
            }
        }
    }
}
