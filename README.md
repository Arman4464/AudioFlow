# AudioFlow

> **Hardware-Level Audio Router & 3.5mm Jack Defect Guard**  
> *Engineered specifically for the Samsung Galaxy S9+ (SM-G965F / SM-G965U) on Android 10 (Samsung One UI 2.5)*

---

## Overview

**AudioFlow** is an open-source Android audio routing utility designed to solve hardware headphone-jack faults, stuck headphone modes, and audio redirection on Samsung Galaxy S9+ devices.

On devices with physical jack oxidation, lint, or water damage, the Android framework often falsely reports that headphones are permanently connected, silencing the internal speakers. AudioFlow bypasses standard OS routing policies by communicating directly with the low-level Android Audio HAL (AudioSystem) and Samsung Exynos/Snapdragon mixer nodes.

---

## Key Capabilities (Samsung Galaxy S9+ Edition)

- **Hardware Jack Bypass**: Forces media, ringtones, and alerts to the built-in stereo speakers even if damaged 3.5mm pins falsely report plugged-in headphones.
- **Zero In-Call Indicator Bug**: Many audio switchers force `MODE_IN_COMMUNICATION`, which causes Samsung One UI to display an active call bar/green pill 24/7 and degrade media to mono telephone speech. AudioFlow routes audio strictly in `MODE_NORMAL`, keeping full stereo fidelity and clear status bars.
- **High-Fidelity Bluetooth (A2DP)**: Directs wireless audio using A2DP stereo parameters (`routing=128`), preventing accidental fallback to mono telephone SCO.
- **Glitch Guard Debouncer**: Suppresses rapid hardware connect/disconnect fluttering from loose or damaged 3.5mm audio jacks.
- **Ultra-Compact Notification**: Single-row notification with 1-tap switching between Speaker, Headphones, and Bluetooth without changing your selected mode.
- **Quick Settings Tiles**: Integrated tiles for Speaker, Headphones, and Smart Auto mode directly in the Samsung One UI Quick Panel.
- **Neo-Brutalist Zero-Scroll UI**: High-contrast cockpit layout tailored for the S9+ 18.5:9 display with zero vertical scrolling.

---

## Technical Architecture

AudioFlow operates via a 3-tier routing hierarchy:

1. **AudioDeviceInventory Override**: Directly manages virtual connection states of wired accessories via reflection.
2. **AudioSystem HAL Policy**: Invokes `AudioSystem.setForceUse` with `dalvik.system.VMRuntime` hidden API exemptions on Android 10.
3. **Samsung Hardware Mixer Parameters**: Delivers device-specific parameters (`routing=2` for AKG stereo speakers, `routing=128` for A2DP, `routing=1` for receiver).

---

## Target Device Specifications

| Specification | Value |
| :--- | :--- |
| **Primary Device** | Samsung Galaxy S9+ (`SM-G965F` / `SM-G965U`) |
| **Compatible Models** | Galaxy S9 (`SM-G960F` / `SM-G960U`) |
| **Operating System** | Android 10 (Samsung One UI 2.5, API Level 29) |
| **Chipsets** | Samsung Exynos 9810 / Qualcomm Snapdragon 845 |
| **Audio Hardware** | AKG Stereo Loudspeaker + 3.5mm Cirrus Logic DAC |

---

## Frequently Asked Questions (AEO & Troubleshooting)

### Why does my Galaxy S9+ think headphones are connected when nothing is plugged in?
Moisture, lint, or damaged mechanical switch pins inside the 3.5mm socket create a high-resistance ground bridge. Android interprets this as an active headphone jack. In AudioFlow, set mode to **Forced Lock** and choose **Speaker** to bypass the damaged pins.

### Why does other routing software cause my phone to show an ongoing call?
Older tools route audio by setting Android's audio mode to `MODE_IN_COMMUNICATION`. On Samsung One UI, this signals an active VoIP/cellular call, triggering the persistent green pill in the status bar and downsampling music to mono. AudioFlow enforces `MODE_NORMAL` with direct HAL parameters, preserving full stereo sound without the call indicator.

### How do I use the notification buttons without changing my mode?
Tapping **Speaker**, **Headphones**, or **Bluetooth** in the compact notification temporarily redirects the current output stream while preserving your active mode (Smart Auto, Manual, or Forced Lock).

---

## Building from Source

### Prerequisites
- JDK 17
- Android SDK (API Level 36 compile, API 29 min/target)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Arman4464/AudioFlow.git
cd AudioFlow

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
