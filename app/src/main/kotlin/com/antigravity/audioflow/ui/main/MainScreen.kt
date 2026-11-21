package com.antigravity.audioflow.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.audioflow.R
import com.antigravity.audioflow.model.AudioEngineState
import com.antigravity.audioflow.model.AudioInputDevice
import com.antigravity.audioflow.model.AudioOutputDevice
import com.antigravity.audioflow.model.AudioRoutingMode
import com.antigravity.audioflow.service.AudioRoutingService

// Theme Color Palettes: Light Neo-Brutalism vs Dark Cyber-Brutalism
private data class BrutalPalette(
    val canvas: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val shadow: Color,
    val yellow: Color,
    val green: Color,
    val blue: Color,
    val pink: Color
)

private val LightPalette = BrutalPalette(
    canvas = Color(0xFFF9F7F2),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF121212),
    muted = Color(0xFF5A5A5A),
    border = Color(0xFF121212),
    shadow = Color(0xFF121212),
    yellow = Color(0xFFFFDE59),
    green = Color(0xFF55E6A5),
    blue = Color(0xFF70B4FF),
    pink = Color(0xFFFF7E95)
)

private val DarkPalette = BrutalPalette(
    canvas = Color(0xFF101014),
    surface = Color(0xFF1C1C24),
    text = Color(0xFFF5F5F7),
    muted = Color(0xFFA2A2B4),
    border = Color(0xFFFFFFFF),
    shadow = Color(0xFF000000),
    yellow = Color(0xFFFFDE59),
    green = Color(0xFF00FF88),
    blue = Color(0xFF00F0FF),
    pink = Color(0xFFFF2A85)
)

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val engineState by AudioRoutingService.engineState.collectAsState()
    var isPlayingTestTone by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val palette = if (engineState.isDarkMode) DarkPalette else LightPalette

    // 100% Single Viewport - Uniform, Zero Scrolling Bottleneck
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Header: Freestanding Logo + Title + Theme Toggle + Sound Test Chime
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Freestanding Vector Logo (No enclosing box)
                Image(
                    painter = painterResource(
                        id = if (engineState.isDarkMode) R.drawable.logo_dark else R.drawable.logo_light
                    ),
                    contentDescription = "AudioFlow Freestanding Logo",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AudioFlow",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.text
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Theme Toggle Button (Light <-> Dark)
                Box(
                    modifier = Modifier
                        .background(palette.shadow, shape = RoundedCornerShape(6.dp))
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-2).dp, y = (-2).dp)
                            .background(palette.surface, shape = RoundedCornerShape(6.dp))
                            .border(2.dp, palette.border, shape = RoundedCornerShape(6.dp))
                            .clickable {
                                AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_DARK_MODE) {
                                    putExtra(AudioRoutingService.EXTRA_DARK_MODE, !engineState.isDarkMode)
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (engineState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = palette.text,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Minimalist 1-Tap Sound Test Chime
                Box(
                    modifier = Modifier
                        .background(palette.shadow, shape = RoundedCornerShape(6.dp))
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-2).dp, y = (-2).dp)
                            .background(if (isPlayingTestTone) palette.green else palette.blue, shape = RoundedCornerShape(6.dp))
                            .border(2.dp, palette.border, shape = RoundedCornerShape(6.dp))
                            .clickable {
                                isPlayingTestTone = true
                                AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_PLAY_TEST)
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(500)
                                    isPlayingTestTone = false
                                }
                            }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPlayingTestTone) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (engineState.isDarkMode) Color.Black else palette.text,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlayingTestTone) "Chiming..." else "Test Chime",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (engineState.isDarkMode) Color.Black else palette.text
                            )
                        }
                    }
                }
            }
        }

        // 2. Cockpit Status Hero: Big Bold Active Target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.shadow, shape = RoundedCornerShape(8.dp))
                .offset(x = 2.5.dp, y = 2.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-2.5).dp, y = (-2.5).dp)
                    .background(palette.yellow, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, palette.border, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, shape = RoundedCornerShape(6.dp))
                                .border(2.dp, Color(0xFF121212), shape = RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (engineState.currentOutput) {
                                    AudioOutputDevice.SPEAKER -> Icons.Default.VolumeUp
                                    AudioOutputDevice.EARPIECE -> Icons.Default.Hearing
                                    AudioOutputDevice.HEADPHONES -> Icons.Default.Headphones
                                    AudioOutputDevice.BLUETOOTH -> Icons.Default.Bluetooth
                                    AudioOutputDevice.USB -> Icons.Default.Usb
                                },
                                contentDescription = null,
                                tint = Color(0xFF121212),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "ACTIVE OUTPUT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF121212).copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = engineState.currentOutput.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF121212)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White, shape = RoundedCornerShape(4.dp))
                            .border(1.5.dp, Color(0xFF121212), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (engineState.mode) {
                                AudioRoutingMode.FORCED_TARGET -> "FORCED"
                                AudioRoutingMode.SMART_AUTO -> "AUTO"
                                AudioRoutingMode.MANUAL_OVERRIDE -> "MANUAL"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF121212)
                        )
                    }
                }
            }
        }

        // 3. Routing Mode Selector + Auto Shield Jack Toggle
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.text,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Defective Jack Shield Pill (Solves Auto defaulting to Headphones)
                if (engineState.mode == AudioRoutingMode.SMART_AUTO) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_TOGGLE_IGNORE_JACK)
                            }
                            .padding(bottom = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (engineState.ignoreDefectiveJackInAuto) palette.green else palette.muted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (engineState.ignoreDefectiveJackInAuto) "Phantom Jack Shield: ON" else "Phantom Jack Shield: OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (engineState.ignoreDefectiveJackInAuto) palette.green else palette.muted
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeTab(
                    title = "Forced Lock",
                    isSelected = engineState.mode == AudioRoutingMode.FORCED_TARGET,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.FORCED_TARGET.name)
                        }
                    }
                )
                ModeTab(
                    title = "Smart Auto",
                    isSelected = engineState.mode == AudioRoutingMode.SMART_AUTO,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.SMART_AUTO.name)
                        }
                    }
                )
                ModeTab(
                    title = "Manual",
                    isSelected = engineState.mode == AudioRoutingMode.MANUAL_OVERRIDE,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.MANUAL_OVERRIDE.name)
                        }
                    }
                )
            }
        }

        // 4. Output Device Selection: Clean 2x2 + 1 Uniform Grid
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PLAY SOUND ON",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = palette.text,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Speaker & Earpiece
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalDeviceCard(
                        device = AudioOutputDevice.SPEAKER,
                        engineState = engineState,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.SPEAKER) }
                    )
                    MinimalDeviceCard(
                        device = AudioOutputDevice.EARPIECE,
                        engineState = engineState,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.EARPIECE) }
                    )
                }

                // Row 2: Headphones & Bluetooth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalDeviceCard(
                        device = AudioOutputDevice.HEADPHONES,
                        engineState = engineState,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.HEADPHONES) }
                    )
                    MinimalDeviceCard(
                        device = AudioOutputDevice.BLUETOOTH,
                        engineState = engineState,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.BLUETOOTH) }
                    )
                }

                // Row 3: USB Audio
                MinimalDeviceCard(
                    device = AudioOutputDevice.USB,
                    engineState = engineState,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.USB) }
                )
            }
        }

        // 5. Microphone Input: Uniform 3-Pill Row
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MICROPHONE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = palette.text,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioInputDevice.values().forEach { mic ->
                    val isSelected = engineState.currentInput == mic
                    val bgColor = if (isSelected) palette.pink.copy(alpha = 0.45f) else palette.surface

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(palette.shadow, shape = RoundedCornerShape(6.dp))
                            .offset(x = 2.dp, y = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = (-2).dp, y = (-2).dp)
                                .background(bgColor, shape = RoundedCornerShape(6.dp))
                                .border(
                                    2.dp,
                                    if (isSelected) palette.border else palette.border.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_INPUT) {
                                        putExtra(AudioRoutingService.EXTRA_INPUT, mic.name)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (mic) {
                                        AudioInputDevice.PHONE_MIC -> Icons.Default.Mic
                                        AudioInputDevice.HEADSET_MIC -> Icons.Default.Headphones
                                        AudioInputDevice.BLUETOOTH_MIC -> Icons.Default.Bluetooth
                                    },
                                    contentDescription = null,
                                    tint = palette.text,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (mic) {
                                        AudioInputDevice.PHONE_MIC -> "Phone"
                                        AudioInputDevice.HEADSET_MIC -> "Headset"
                                        AudioInputDevice.BLUETOOTH_MIC -> "Bluetooth"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = palette.text
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Minimalist Telemetry Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, shape = RoundedCornerShape(6.dp))
                .border(1.5.dp, palette.border, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = palette.text,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Glitch Guard: ${engineState.glitchSuppressionCount} flutters blocked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = palette.text
                    )
                }
                Text(
                    text = "Boot Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.muted
                )
            }
        }
    }
}

private fun selectOutput(
    context: android.content.Context,
    engineState: AudioEngineState,
    device: AudioOutputDevice
) {
    if (engineState.mode == AudioRoutingMode.FORCED_TARGET) {
        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.FORCED_TARGET.name)
            putExtra(AudioRoutingService.EXTRA_OUTPUT, device.name)
        }
    } else {
        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_FORCE_OUTPUT) {
            putExtra(AudioRoutingService.EXTRA_OUTPUT, device.name)
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    isSelected: Boolean,
    palette: BrutalPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(palette.shadow, shape = RoundedCornerShape(6.dp))
            .offset(x = 2.dp, y = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-2).dp, y = (-2).dp)
                .background(if (isSelected) palette.green else palette.surface, shape = RoundedCornerShape(6.dp))
                .border(2.dp, palette.border, shape = RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected && palette == DarkPalette) Color.Black else palette.text
            )
        }
    }
}

@Composable
private fun MinimalDeviceCard(
    device: AudioOutputDevice,
    engineState: AudioEngineState,
    palette: BrutalPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isActive = engineState.currentOutput == device
    val isTarget = engineState.mode == AudioRoutingMode.FORCED_TARGET && engineState.forcedTargetOutput == device

    val bgColor = when {
        isActive -> palette.yellow
        isTarget -> palette.blue.copy(alpha = 0.35f)
        else -> palette.surface
    }

    val itemTextColor = if (isActive) Color(0xFF121212) else palette.text

    Box(
        modifier = modifier
            .height(50.dp)
            .background(palette.shadow, shape = RoundedCornerShape(6.dp))
            .offset(x = 2.dp, y = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = (-2).dp, y = (-2).dp)
                .background(bgColor, shape = RoundedCornerShape(6.dp))
                .border(2.dp, palette.border, shape = RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isActive) Color.White.copy(alpha = 0.9f) else palette.border.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isActive) Color(0xFF121212) else palette.border.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (device) {
                            AudioOutputDevice.SPEAKER -> Icons.Default.VolumeUp
                            AudioOutputDevice.EARPIECE -> Icons.Default.Hearing
                            AudioOutputDevice.HEADPHONES -> Icons.Default.Headphones
                            AudioOutputDevice.BLUETOOTH -> Icons.Default.Bluetooth
                            AudioOutputDevice.USB -> Icons.Default.Usb
                        },
                        contentDescription = null,
                        tint = if (isActive) Color(0xFF121212) else palette.text,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = device.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = itemTextColor,
                    maxLines = 1
                )
            }
        }
    }
}
