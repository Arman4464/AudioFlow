package com.antigravity.audioflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.antigravity.audioflow.MainActivity
import com.antigravity.audioflow.R
import com.antigravity.audioflow.audio.AudioRoutingController
import com.antigravity.audioflow.model.AudioEngineState
import com.antigravity.audioflow.model.AudioInputDevice
import com.antigravity.audioflow.model.AudioOutputDevice
import com.antigravity.audioflow.model.AudioRoutingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AudioRoutingService : Service() {

    companion object {
        private const val TAG = "AudioRoutingService"
        private const val CHANNEL_ID = "audioflow_compact_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.antigravity.audioflow.START"
        const val ACTION_STOP = "com.antigravity.audioflow.STOP"
        const val ACTION_FORCE_OUTPUT = "com.antigravity.audioflow.FORCE_OUTPUT"
        const val ACTION_SET_MODE = "com.antigravity.audioflow.SET_MODE"
        const val ACTION_SET_INPUT = "com.antigravity.audioflow.SET_INPUT"
        const val ACTION_PLAY_TEST = "com.antigravity.audioflow.PLAY_TEST"
        const val ACTION_TOGGLE_IGNORE_JACK = "com.antigravity.audioflow.TOGGLE_IGNORE_JACK"
        const val ACTION_SET_DARK_MODE = "com.antigravity.audioflow.SET_DARK_MODE"

        const val EXTRA_OUTPUT = "extra_output"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_INPUT = "extra_input"
        const val EXTRA_DARK_MODE = "extra_dark_mode"

        private const val PREFS_NAME = "audioflow_prefs"
        private const val KEY_MODE = "pref_mode"
        private const val KEY_TARGET_OUTPUT = "pref_target_output"
        private const val KEY_TARGET_INPUT = "pref_target_input"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_IGNORE_JACK = "pref_ignore_jack"

        private val _engineState = MutableStateFlow(AudioEngineState())
        val engineState: StateFlow<AudioEngineState> = _engineState.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, AudioRoutingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun sendCommand(context: Context, intentAction: String, configure: Intent.() -> Unit = {}) {
            val intent = Intent(context, AudioRoutingService::class.java).apply {
                action = intentAction
                configure()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var controller: AudioRoutingController
    private lateinit var audioManager: AudioManager
    private lateinit var prefs: SharedPreferences
    private var userAutoOverride: AudioOutputDevice? = null

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val now = System.currentTimeMillis()
            if (now - controller.lastInternalRoutingTimestamp < 1200L) {
                Log.d(TAG, "Suppressed onAudioDevicesAdded reflection echo")
                return
            }
            Log.d(TAG, "Audio devices added")
            updateHardwarePresence()
            reapplyCurrentPolicy()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val now = System.currentTimeMillis()
            if (now - controller.lastInternalRoutingTimestamp < 1200L) {
                Log.d(TAG, "Suppressed onAudioDevicesRemoved reflection echo")
                return
            }
            Log.d(TAG, "Audio devices removed")
            updateHardwarePresence()
            reapplyCurrentPolicy()
        }
    }

    private val hardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val now = System.currentTimeMillis()
                    val deltaSinceInternal = now - controller.lastInternalRoutingTimestamp
                    if (deltaSinceInternal < 1200L) {
                        Log.d(TAG, "Suppressed synthetic HEADSET_PLUG reflection echo (delta: ${deltaSinceInternal}ms)")
                        return
                    }

                    val state = intent.getIntExtra("state", -1)
                    val isPlugged = state == 1
                    val hasMic = intent.getIntExtra("microphone", 0) == 1
                    Log.d(TAG, "HEADSET_PLUG received: state=$state, hasMic=$hasMic")

                    if (!isPlugged && userAutoOverride == AudioOutputDevice.HEADPHONES) {
                        userAutoOverride = null
                    }

                    if (_engineState.value.isGlitchGuardEnabled && controller.shouldSuppressJackEvent(state)) {
                        _engineState.update {
                            it.copy(
                                lastGlitchSuppressedTimestamp = System.currentTimeMillis(),
                                glitchSuppressionCount = controller.suppressedGlitchCount
                            )
                        }
                        return
                    }

                    _engineState.update {
                        it.copy(
                            isHeadsetPhysicallyPlugged = isPlugged,
                            hasRealHeadsetWithMic = isPlugged && hasMic,
                            currentOutput = if (isPlugged) {
                                if (it.mode == AudioRoutingMode.SMART_AUTO) {
                                    userAutoOverride ?: if (!it.ignoreDefectiveJackInAuto || hasMic) AudioOutputDevice.HEADPHONES else it.currentOutput
                                } else it.currentOutput
                            } else {
                                if (it.currentOutput == AudioOutputDevice.HEADPHONES) AudioOutputDevice.SPEAKER else it.currentOutput
                            }
                        )
                    }

                    reapplyCurrentPolicy()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")
        controller = AudioRoutingController(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load saved preferences
        val savedModeName = prefs.getString(KEY_MODE, AudioRoutingMode.FORCED_TARGET.name)
        val savedOutputName = prefs.getString(KEY_TARGET_OUTPUT, AudioOutputDevice.SPEAKER.name)
        val savedInputName = prefs.getString(KEY_TARGET_INPUT, AudioInputDevice.PHONE_MIC.name)
        val savedDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        val savedIgnoreJack = prefs.getBoolean(KEY_IGNORE_JACK, false)

        val initMode = try { AudioRoutingMode.valueOf(savedModeName ?: "") } catch (e: Exception) { AudioRoutingMode.FORCED_TARGET }
        val initOutput = try { AudioOutputDevice.valueOf(savedOutputName ?: "") } catch (e: Exception) { AudioOutputDevice.SPEAKER }
        val initInput = try { AudioInputDevice.valueOf(savedInputName ?: "") } catch (e: Exception) { AudioInputDevice.PHONE_MIC }

        _engineState.update {
            it.copy(
                mode = initMode,
                forcedTargetOutput = initOutput,
                currentOutput = initOutput,
                currentInput = initInput,
                isDarkMode = savedDarkMode,
                ignoreDefectiveJackInAuto = savedIgnoreJack,
                isServiceRunning = true
            )
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        registerReceiver(hardwareReceiver, filter)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        updateHardwarePresence()
        reapplyCurrentPolicy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FORCE_OUTPUT -> {
                val outputName = intent.getStringExtra(EXTRA_OUTPUT)
                val target = outputName?.let { runCatching { AudioOutputDevice.valueOf(it) }.getOrNull() }
                if (target != null) {
                    userAutoOverride = target
                    _engineState.update {
                        it.copy(
                            // Preserves current mode (Smart Auto, Forced Lock, or Manual) exactly as is
                            forcedTargetOutput = target,
                            currentOutput = target
                        )
                    }
                    prefs.edit().putString(KEY_TARGET_OUTPUT, target.name).apply()
                    controller.routeToOutput(target)
                    updateNotification()
                }
            }
            ACTION_SET_MODE -> {
                val modeName = intent.getStringExtra(EXTRA_MODE)
                val newMode = modeName?.let { runCatching { AudioRoutingMode.valueOf(it) }.getOrNull() }
                if (newMode != null) {
                    userAutoOverride = null
                    val targetOutput = intent.getStringExtra(EXTRA_OUTPUT)?.let {
                        runCatching { AudioOutputDevice.valueOf(it) }.getOrNull()
                    } ?: _engineState.value.forcedTargetOutput

                    _engineState.update {
                        it.copy(
                            mode = newMode,
                            forcedTargetOutput = targetOutput
                        )
                    }
                    prefs.edit()
                        .putString(KEY_MODE, newMode.name)
                        .putString(KEY_TARGET_OUTPUT, targetOutput.name)
                        .apply()
                    reapplyCurrentPolicy()
                }
            }
            ACTION_SET_INPUT -> {
                val inputName = intent.getStringExtra(EXTRA_INPUT)
                val targetInput = inputName?.let { runCatching { AudioInputDevice.valueOf(it) }.getOrNull() }
                if (targetInput != null) {
                    _engineState.update { it.copy(currentInput = targetInput) }
                    prefs.edit().putString(KEY_TARGET_INPUT, targetInput.name).apply()
                    controller.routeToInput(targetInput)
                }
            }
            ACTION_TOGGLE_IGNORE_JACK -> {
                val newVal = !_engineState.value.ignoreDefectiveJackInAuto
                _engineState.update { it.copy(ignoreDefectiveJackInAuto = newVal) }
                prefs.edit().putBoolean(KEY_IGNORE_JACK, newVal).apply()
                reapplyCurrentPolicy()
            }
            ACTION_SET_DARK_MODE -> {
                val isDark = intent.getBooleanExtra(EXTRA_DARK_MODE, false)
                _engineState.update { it.copy(isDarkMode = isDark) }
                prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
                updateNotification()
            }
            ACTION_PLAY_TEST -> {
                controller.playTestChime()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        updateNotification()
        return START_STICKY
    }

    private fun reapplyCurrentPolicy() {
        val state = _engineState.value
        when (state.mode) {
            AudioRoutingMode.FORCED_TARGET -> {
                controller.routeToOutput(state.forcedTargetOutput)
                _engineState.update { it.copy(currentOutput = state.forcedTargetOutput) }
            }
            AudioRoutingMode.SMART_AUTO -> {
                // In Smart Auto: Bluetooth > USB > Real/Valid Headphones > Built-in Stereo Speaker
                // If user has explicitly selected an output (userAutoOverride != null), honor user selection!
                val override = userAutoOverride
                val autoTarget = if (override != null) {
                    override
                } else {
                    val hasValidHeadset = state.isHeadsetPhysicallyPlugged && (!state.ignoreDefectiveJackInAuto || state.hasRealHeadsetWithMic)
                    when {
                        state.isBluetoothConnected -> AudioOutputDevice.BLUETOOTH
                        state.isUsbConnected -> AudioOutputDevice.USB
                        hasValidHeadset -> AudioOutputDevice.HEADPHONES
                        else -> AudioOutputDevice.SPEAKER
                    }
                }
                controller.routeToOutput(autoTarget)
                _engineState.update { it.copy(currentOutput = autoTarget) }
            }
            AudioRoutingMode.MANUAL_OVERRIDE -> {
                controller.routeToOutput(state.currentOutput)
            }
        }
        updateNotification()
    }

    private fun updateHardwarePresence() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hasBt = devices.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        val hasUsb = devices.any {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        val hasHeadset = devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }

        _engineState.update {
            it.copy(
                isBluetoothConnected = hasBt,
                isUsbConnected = hasUsb,
                isHeadsetPhysicallyPlugged = hasHeadset || it.isHeadsetPhysicallyPlugged
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AudioFlow Quick Switcher",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quick 1-tap audio destination switching"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Compact Notification: Ultra-short display with exactly 3 direct options:
     * Speaker, Headphones, Bluetooth.
     * Overrides current output immediately while keeping the active mode intact.
     */
    private fun buildNotification(): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val state = _engineState.value

        val speakerPI = createOutputPendingIntent(AudioOutputDevice.SPEAKER, 101)
        val headphonesPI = createOutputPendingIntent(AudioOutputDevice.HEADPHONES, 102)
        val btPI = createOutputPendingIntent(AudioOutputDevice.BLUETOOTH, 103)

        val title = "AudioFlow • ${state.currentOutput.title}"
        val modeText = "${state.mode.title} Mode"

        val remoteViews = RemoteViews(packageName, R.layout.notification_compact).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_mode, modeText)

            val activeBg = if (state.isDarkMode) R.drawable.bg_notif_button_active_dark else R.drawable.bg_notif_button_active
            val defaultBg = R.drawable.bg_notif_button

            // Speaker Button
            val isSpeaker = state.currentOutput == AudioOutputDevice.SPEAKER
            setInt(R.id.btn_notif_speaker, "setBackgroundResource", if (isSpeaker) activeBg else defaultBg)
            setTextColor(R.id.btn_notif_speaker, if (isSpeaker) Color.BLACK else Color.WHITE)
            setOnClickPendingIntent(R.id.btn_notif_speaker, speakerPI)

            // Headphones Button
            val isHeadphones = state.currentOutput == AudioOutputDevice.HEADPHONES
            setInt(R.id.btn_notif_headphones, "setBackgroundResource", if (isHeadphones) activeBg else defaultBg)
            setTextColor(R.id.btn_notif_headphones, if (isHeadphones) Color.BLACK else Color.WHITE)
            setOnClickPendingIntent(R.id.btn_notif_headphones, headphonesPI)

            // Bluetooth Button
            val isBt = state.currentOutput == AudioOutputDevice.BLUETOOTH
            setInt(R.id.btn_notif_bluetooth, "setBackgroundResource", if (isBt) activeBg else defaultBg)
            setTextColor(R.id.btn_notif_bluetooth, if (isBt) Color.BLACK else Color.WHITE)
            setOnClickPendingIntent(R.id.btn_notif_bluetooth, btPI)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createOutputPendingIntent(device: AudioOutputDevice, requestCode: Int): PendingIntent {
        val intent = Intent(this, com.antigravity.audioflow.receiver.NotificationActionReceiver::class.java).apply {
            putExtra(EXTRA_OUTPUT, device.name)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())

        // Synchronize Samsung One UI Quick Settings Tiles
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                android.service.quicksettings.TileService.requestListeningState(
                    this,
                    ComponentName(this, com.antigravity.audioflow.tile.SpeakerTileService::class.java)
                )
                android.service.quicksettings.TileService.requestListeningState(
                    this,
                    ComponentName(this, com.antigravity.audioflow.tile.HeadphonesTileService::class.java)
                )
                android.service.quicksettings.TileService.requestListeningState(
                    this,
                    ComponentName(this, com.antigravity.audioflow.tile.AutoTileService::class.java)
                )
            } catch (ignored: Exception) {}
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")
        try {
            unregisterReceiver(hardwareReceiver)
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (ignored: Exception) {}
        controller.resetRouting()
        _engineState.update { it.copy(isServiceRunning = false) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
