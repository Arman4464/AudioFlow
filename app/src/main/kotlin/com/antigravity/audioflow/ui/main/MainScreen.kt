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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import com.antigravity.audioflow.ui.theme.AudioFlowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Neo-Brutalist Cockpit 2.0 Screen.
 * 100% Single Viewport guaranteed for Samsung Galaxy S9+ (18.5:9 display).
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ====================================================================
        // TIER 1: Brand Navigation & Utility Toolbar (~42dp)
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Mark & Logotype
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_audioflow_logo),
                    contentDescription = "AudioFlow Logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AudioFlow",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Hardware Target Badge
                Box(
                    modifier = Modifier
                        .background(colors.yellow, shape = RoundedCornerShape(4.dp))
                        .border(1.5.dp, colors.border, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "S9+",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF121212)
                    )
                }
            }

            // Quick Actions: Sound Chime + Theme Switcher
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Test Chime Button
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlayingTestTone) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            contentDescription = "Play Sound Test Chime",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlayingTestTone) "Chiming" else "Test",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF121212)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Light / Dark Mode Toggle
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (engineState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // ====================================================================
        // TIER 2: Live Routing Command HUD Hero (~96dp)
        // ====================================================================
        TactileCard(
            modifier = Modifier.fillMaxWidth(),
            shadowColor = colors.shadow,
            borderColor = colors.border,
            backgroundColor = colors.yellow,
            depth = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Active Device Icon Box
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White, shape = RoundedCornerShape(7.dp))
                            .border(2.dp, Color(0xFF121212), shape = RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDeviceIcon(engineState.currentOutput),
                            contentDescription = null,
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(colors.green, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ACTIVE ROUTING DESTINATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF121212).copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = engineState.currentOutput.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF121212),
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = getRoutingTelemetry(engineState.currentOutput),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF121212).copy(alpha = 0.75f)
                        )
                    }
                }

                // Mode Status Tag
                Box(
                    modifier = Modifier
                        .background(Color.White, shape = RoundedCornerShape(4.dp))
                        .border(1.5.dp, Color(0xFF121212), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (engineState.mode) {
                            AudioRoutingMode.FORCED_TARGET -> "LOCKED"
                            AudioRoutingMode.SMART_AUTO -> "AUTO"
                            AudioRoutingMode.MANUAL_OVERRIDE -> "MANUAL"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF121212)
                    )
                }
            }
        }

        // ====================================================================
        // TIER 3: Routing Modes & Defective Jack Shield (~84dp)
        // ====================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ModeButton(
                    title = "Forced Lock",
                    tag = "OVERRIDE",
                    isSelected = engineState.mode == AudioRoutingMode.FORCED_TARGET,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.FORCED_TARGET.name)
                        }
                    }
                )
                ModeButton(
                    title = "Smart Auto",
                    tag = "DYNAMIC",
                    isSelected = engineState.mode == AudioRoutingMode.SMART_AUTO,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_SET_MODE) {
                            putExtra(AudioRoutingService.EXTRA_MODE, AudioRoutingMode.SMART_AUTO.name)
                        }
                    }
                )
                ModeButton(
                    title = "Manual",
                    tag = "DIRECT",
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

            Spacer(modifier = Modifier.height(7.dp))

            // Dedicated Jack Defect Guard Card
            TactileCard(
                modifier = Modifier.fillMaxWidth(),
                shadowColor = colors.shadow,
                borderColor = colors.border,
                backgroundColor = colors.surface,
                depth = 2.dp,
                onClick = {
                    AudioRoutingService.sendCommand(context, AudioRoutingService.ACTION_TOGGLE_IGNORE_JACK)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (engineState.ignoreDefectiveJackInAuto) colors.green else colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column {
                            Text(
                                text = "3.5mm Jack Defect Guard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (engineState.ignoreDefectiveJackInAuto)
                                    "Bypassing oxidized/shorted pins"
                                else
                                    "Standard accessory sensing",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (engineState.ignoreDefectiveJackInAuto) colors.green else colors.textMuted
                            )
                        }
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (engineState.ignoreDefectiveJackInAuto) colors.green else colors.surfaceElevated,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(1.2.dp, colors.border, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (engineState.ignoreDefectiveJackInAuto) "GUARD ACTIVE" else "OFF",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF121212)
                        )
                    }
                }
            }
        }

        // ====================================================================
        // TIER 4: Balanced Output Matrix (3 Primary + 2 Secondary) (~148dp)
        // ====================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PLAY SOUND ON",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )

            // Row 1: Primary Targets (Speaker, Headphones, Bluetooth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutputCard(
                    device = AudioOutputDevice.SPEAKER,
                    engineState = engineState,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.SPEAKER) }
                )
                OutputCard(
                    device = AudioOutputDevice.HEADPHONES,
                    engineState = engineState,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.HEADPHONES) }
                )
                OutputCard(
                    device = AudioOutputDevice.BLUETOOTH,
                    engineState = engineState,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    height = 54.dp,
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.BLUETOOTH) }
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Row 2: Secondary / Auxiliary Targets (Earpiece, USB DAC) - Symmetrical 50/50
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutputCard(
                    device = AudioOutputDevice.EARPIECE,
                    engineState = engineState,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    height = 46.dp,
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.EARPIECE) }
                )
                OutputCard(
                    device = AudioOutputDevice.USB,
                    engineState = engineState,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    height = 46.dp,
                    onClick = { selectOutput(context, engineState, AudioOutputDevice.USB) }
                )
            }
        }

        // ====================================================================
        // TIER 5: Microphone Cockpit & Sentinel Telemetry (~82dp)
        // ====================================================================
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MICROPHONE SOURCE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )

            // Microphone 3-column row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AudioInputDevice.values().forEach { mic ->
                    val isSelected = engineState.currentInput == mic
                    val micBg = if (isSelected) colors.pink.copy(alpha = 0.45f) else colors.surface

                    TactileCard(
                        modifier = Modifier.weight(1f),
                        shadowColor = colors.shadow,
                        borderColor = if (isSelected) colors.border else colors.borderSubtle,
                        backgroundColor = micBg,
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
                                .padding(vertical = 7.dp),
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
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when (mic) {
                                    AudioInputDevice.PHONE_MIC -> "Phone"
                                    AudioInputDevice.HEADSET_MIC -> "Headset"
                                    AudioInputDevice.BLUETOOTH_MIC -> "BT Mic"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Hardware Sentinel Telemetry Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceElevated, shape = RoundedCornerShape(5.dp))
                    .border(1.2.dp, colors.borderSubtle, shape = RoundedCornerShape(5.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
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
                            tint = colors.textPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Glitch Guard: ${engineState.glitchSuppressionCount} flutters suppressed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = "Boot Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted
                    )
                }
            }
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
    borderWidth: Dp = 1.8.dp,
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
                .border(1.8.dp, borderColor, shape = RoundedCornerShape(6.dp))
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
private fun ModeButton(
    title: String,
    tag: String,
    isSelected: Boolean,
    colors: com.antigravity.audioflow.ui.theme.AudioFlowColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) colors.green else colors.surface

    TactileCard(
        modifier = modifier,
        shadowColor = colors.shadow,
        borderColor = if (isSelected) colors.border else colors.borderSubtle,
        backgroundColor = bgColor,
        depth = 2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected && colors.isDark) Color(0xFF121212) else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tag,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected && colors.isDark) Color(0xFF121212).copy(alpha = 0.7f) else colors.textMuted
            )
        }
    }
}

@Composable
private fun OutputCard(
    device: AudioOutputDevice,
    engineState: AudioEngineState,
    colors: com.antigravity.audioflow.ui.theme.AudioFlowColors,
    modifier: Modifier = Modifier,
    height: Dp,
    onClick: () -> Unit
) {
    val isActive = engineState.currentOutput == device
    val isForcedTarget = engineState.mode == AudioRoutingMode.FORCED_TARGET && engineState.forcedTargetOutput == device

    val bgColor = when {
        isActive -> colors.yellow
        isForcedTarget -> colors.blue.copy(alpha = 0.35f)
        else -> colors.surface
    }

    TactileCard(
        modifier = modifier.height(height),
        shadowColor = colors.shadow,
        borderColor = if (isActive) colors.border else colors.borderSubtle,
        backgroundColor = bgColor,
        depth = 2.5.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isActive) Color.White else colors.surfaceElevated,
                        shape = RoundedCornerShape(5.dp)
                    )
                    .border(
                        1.4.dp,
                        if (isActive) Color(0xFF121212) else colors.borderSubtle,
                        shape = RoundedCornerShape(5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device),
                    contentDescription = null,
                    tint = if (isActive) Color(0xFF121212) else colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = device.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) Color(0xFF121212) else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Text(
                        text = "STREAMING",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF121212).copy(alpha = 0.75f)
                    )
                }
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

private fun getRoutingTelemetry(device: AudioOutputDevice): String = when (device) {
    AudioOutputDevice.SPEAKER -> "MODE_NORMAL • 48kHz AKG Stereo • routing=2"
    AudioOutputDevice.EARPIECE -> "MODE_NORMAL • 48kHz Receiver • routing=1"
    AudioOutputDevice.HEADPHONES -> "MODE_NORMAL • 3.5mm DAC High-Fi • routing=8"
    AudioOutputDevice.BLUETOOTH -> "MODE_NORMAL • A2DP Stereo • routing=128"
    AudioOutputDevice.USB -> "MODE_NORMAL • USB-C Digital Audio • routing=16384"
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
