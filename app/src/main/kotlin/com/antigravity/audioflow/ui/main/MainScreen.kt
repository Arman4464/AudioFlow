package com.antigravity.audioflow.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.audioflow.R
import com.antigravity.audioflow.model.AudioEngineState
import com.antigravity.audioflow.model.AudioInputDevice
import com.antigravity.audioflow.model.AudioOutputDevice
import com.antigravity.audioflow.model.AudioRoutingMode
import com.antigravity.audioflow.service.AudioRoutingService
import com.antigravity.audioflow.ui.theme.AudioFlowColors
import com.antigravity.audioflow.ui.theme.AudioFlowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Authentic Neo-Brutalist Main Screen for AudioFlow.
 * Clean, bold, punchy cards with mechanical tactile press feedback.
 * Strictly zero AI-template chrome and 100% single-viewport (zero scrolling).
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val engineState by AudioRoutingService.engineState.collectAsState()
    var isPlayingTestTone by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val colors = AudioFlowTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --------------------------------------------------------------------
        // 1. Header: Logo, Title, Sound Test Chime, Theme Toggle
        // --------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_audioflow_logo),
                    contentDescription = "AudioFlow Logo",
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AudioFlow",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sound Test Button
                TactileButton(
                    shadowColor = colors.shadow,
                    borderColor = colors.border,
                    backgroundColor = if (isPlayingTestTone) colors.green else colors.blue,
                    depth = 2.dp,
                    onClick = {
                        isPlayingTestTone = true
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_PLAY_TEST)
                        coroutineScope.launch {
                            delay(500)
                            isPlayingTestTone = false
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlayingTestTone) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            contentDescription = "Test Chime",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlayingTestTone) "Chiming" else "Test Chime",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF121212)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Light / Dark Theme Switcher
                TactileButton(
                    shadowColor = colors.shadow,
                    borderColor = colors.border,
                    backgroundColor = colors.surface,
                    depth = 2.dp,
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_DARK_MODE) {
                            putExtra(AudioRoutingService.EXTRA_DARK_MODE, !engineState.isDarkMode)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (engineState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --------------------------------------------------------------------
        // 2. Hero Status Card: Big, bold active route indicator
        // --------------------------------------------------------------------
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            shadowColor = colors.shadow,
            borderColor = colors.border,
            backgroundColor = colors.yellow,
            cornerRadius = 8.dp,
            borderWidth = 2.5.dp,
            depth = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Big Device Icon Container
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                            .border(2.dp, Color(0xFF121212), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDeviceIcon(engineState.currentOutput),
                            contentDescription = null,
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(colors.green, shape = CircleShape)
                                    .border(1.dp, Color(0xFF121212), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active output",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF121212).copy(alpha = 0.75f)
                            )
                        }
                        Text(
                            text = engineState.currentOutput.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF121212),
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = engineState.currentOutput.subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF121212).copy(alpha = 0.7f)
                        )
                    }
                }

                // Mode Status Pill
                Box(
                    modifier = Modifier
                        .background(Color.White, shape = RoundedCornerShape(5.dp))
                        .border(1.8.dp, Color(0xFF121212), shape = RoundedCornerShape(5.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (engineState.mode) {
                            AudioRoutingMode.FORCED_TARGET -> "Locked"
                            AudioRoutingMode.SMART_AUTO -> "Auto"
                            AudioRoutingMode.MANUAL_OVERRIDE -> "Manual"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF121212)
                    )
                }
            }
        }

        // --------------------------------------------------------------------
        // 3. Routing Modes & Defective Jack Guard
        // --------------------------------------------------------------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Routing mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary
                )

                // 3.5mm Jack Defect Guard Quick Switch
                Box(
                    modifier = Modifier
                        .background(
                            if (engineState.ignoreDefectiveJackInAuto) colors.green else colors.surface,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .border(1.5.dp, colors.border, shape = RoundedCornerShape(5.dp))
                        .clickable {
                            AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_TOGGLE_IGNORE_JACK)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (engineState.ignoreDefectiveJackInAuto && colors.isDark) Color(0xFF121212) else colors.textPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (engineState.ignoreDefectiveJackInAuto) "Jack guard: ON" else "Jack guard: OFF",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (engineState.ignoreDefectiveJackInAuto && colors.isDark) Color(0xFF121212) else colors.textPrimary
                        )
                    }
                }
            }

            // 3 Mode Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeTab(
                    title = "Smart Auto",
                    isSelected = engineState.mode == AudioRoutingMode.SMART_AUTO,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.SMART_AUTO.name)
                        }
                    }
                )
                ModeTab(
                    title = "Forced Lock",
                    isSelected = engineState.mode == AudioRoutingMode.FORCED_TARGET,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.FORCED_TARGET.name)
                        }
                    }
                )
                ModeTab(
                    title = "Manual",
                    isSelected = engineState.mode == AudioRoutingMode.MANUAL_OVERRIDE,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.MANUAL_OVERRIDE.name)
                        }
                    }
                )
            }
        }

        // --------------------------------------------------------------------
        // 4. Output Device Selection: Comfortable 2x2 Grid + Auxiliary USB Card
        // --------------------------------------------------------------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Audio output",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                // Row 1: Speaker & Earpiece
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceCard(
                        device = AudioOutputDevice.SPEAKER,
                        engineState = engineState,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.SPEAKER) }
                    )
                    DeviceCard(
                        device = AudioOutputDevice.EARPIECE,
                        engineState = engineState,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.EARPIECE) }
                    )
                }

                // Row 2: Headphones & Bluetooth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceCard(
                        device = AudioOutputDevice.HEADPHONES,
                        engineState = engineState,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.HEADPHONES) }
                    )
                    DeviceCard(
                        device = AudioOutputDevice.BLUETOOTH,
                        engineState = engineState,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { selectOutput(context, engineState, AudioOutputDevice.BLUETOOTH) }
                    )
                }

                // Row 3: USB Audio (Auxiliary full-width)
                DeviceCard(
                    device = AudioOutputDevice.USB,
                    engineState = engineState,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth(),
                    height = 44.dp,
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.USB) }
                )
            }
        }

        // --------------------------------------------------------------------
        // 5. Microphone Selector
        // --------------------------------------------------------------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Microphone",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioInputDevice.values().forEach { mic ->
                    val isSelected = engineState.currentInput == mic
                    val micBg = if (isSelected) colors.pink.copy(alpha = 0.5f) else colors.surface

                    TactileCard(
                        modifier = Modifier.weight(1f),
                        shadowColor = colors.shadow,
                        borderColor = if (isSelected) colors.border else colors.borderSubtle,
                        backgroundColor = micBg,
                        cornerRadius = 6.dp,
                        borderWidth = if (isSelected) 2.dp else 1.5.dp,
                        depth = 2.dp,
                        onClick = {
                            AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_INPUT) {
                                putExtra(AudioRoutingService.EXTRA_INPUT, mic.name)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (mic) {
                                    AudioInputDevice.PHONE_MIC -> Icons.Default.Mic
                                    AudioInputDevice.HEADSET_MIC -> Icons.Default.Headphones
                                    AudioInputDevice.BLUETOOTH_MIC -> Icons.Default.Bluetooth
                                },
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (mic) {
                                    AudioInputDevice.PHONE_MIC -> "Phone"
                                    AudioInputDevice.HEADSET_MIC -> "Headset"
                                    AudioInputDevice.BLUETOOTH_MIC -> "BT Mic"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // --------------------------------------------------------------------
        // 6. Subtle Footer
        // --------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.green,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (engineState.glitchSuppressionCount > 0)
                        "Glitch Guard (${engineState.glitchSuppressionCount} filtered)"
                    else
                        "Glitch Guard active",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted
                )
            }
            Text(
                text = "v1.0.1",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted
            )
        }
    }
}

// ============================================================================
// Tactile Neo-Brutalist Reusable Components
// ============================================================================

@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    shadowColor: Color,
    borderColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp = 6.dp,
    borderWidth: Dp = 2.dp,
    depth: Dp = 2.5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && onClick != null) depth else 0.dp,
        label = "press"
    )

    Box(
        modifier = modifier
            .background(shadowColor, shape = RoundedCornerShape(cornerRadius))
            .offset(x = depth, y = depth)
    ) {
        val clickModifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = -depth + pressOffset, y = -depth + pressOffset)
                .background(backgroundColor, shape = RoundedCornerShape(cornerRadius))
                .border(borderWidth, borderColor, shape = RoundedCornerShape(cornerRadius))
                .then(clickModifier),
            content = content
        )
    }
}

@Composable
fun TactileButton(
    modifier: Modifier = Modifier,
    shadowColor: Color,
    borderColor: Color,
    backgroundColor: Color,
    depth: Dp = 2.dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) depth else 0.dp,
        label = "press"
    )

    Box(
        modifier = modifier
            .background(shadowColor, shape = RoundedCornerShape(6.dp))
            .offset(x = depth, y = depth)
    ) {
        Box(
            modifier = Modifier
                .offset(x = -depth + pressOffset, y = -depth + pressOffset)
                .background(backgroundColor, shape = RoundedCornerShape(6.dp))
                .border(2.dp, borderColor, shape = RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            content = content
        )
    }
}

@Composable
private fun ModeTab(
    title: String,
    isSelected: Boolean,
    colors: AudioFlowColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) colors.green else colors.surface

    TactileCard(
        modifier = modifier,
        shadowColor = colors.shadow,
        borderColor = if (isSelected) colors.border else colors.borderSubtle,
        backgroundColor = bgColor,
        cornerRadius = 6.dp,
        borderWidth = if (isSelected) 2.dp else 1.5.dp,
        depth = 2.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected && colors.isDark) Color(0xFF121212) else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: AudioOutputDevice,
    engineState: AudioEngineState,
    colors: AudioFlowColors,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    onClick: () -> Unit
) {
    val isActive = engineState.currentOutput == device
    val isLockedTarget = engineState.mode == AudioRoutingMode.FORCED_TARGET && engineState.forcedTargetOutput == device

    val bgColor = when {
        isActive -> colors.yellow
        isLockedTarget -> colors.blue.copy(alpha = 0.35f)
        else -> colors.surface
    }

    TactileCard(
        modifier = modifier.height(height),
        shadowColor = colors.shadow,
        borderColor = if (isActive) colors.border else colors.borderSubtle,
        backgroundColor = bgColor,
        cornerRadius = 6.dp,
        borderWidth = if (isActive) 2.dp else 1.5.dp,
        depth = 2.5.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isActive) Color.White else colors.surfaceElevated,
                        shape = RoundedCornerShape(5.dp)
                    )
                    .border(
                        1.5.dp,
                        if (isActive) Color(0xFF121212) else colors.borderSubtle,
                        shape = RoundedCornerShape(5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device),
                    contentDescription = null,
                    tint = if (isActive) Color(0xFF121212) else colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = device.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) Color(0xFF121212) else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.subtitle,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF121212).copy(alpha = 0.7f) else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================

private fun getDeviceIcon(device: AudioOutputDevice): ImageVector = when (device) {
    AudioOutputDevice.SPEAKER -> Icons.Default.VolumeUp
    AudioOutputDevice.EARPIECE -> Icons.Default.Hearing
    AudioOutputDevice.HEADPHONES -> Icons.Default.Headphones
    AudioOutputDevice.BLUETOOTH -> Icons.Default.Bluetooth
    AudioOutputDevice.USB -> Icons.Default.Usb
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
