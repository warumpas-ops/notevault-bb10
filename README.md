# NoteVault (BB10)

> A clean, minimalist, offline note-taking app designed for BlackBerry 10 devices (Q10, Q5, Classic, Passport) running the Android 4.3 runtime (API 18).

---

## ⚡ Features

- **Text Notes**: Fast note taking with automatic timestamps.
- **Task Lists**: Interactive to-do lists with checkbox support.
- **Audio Notes**: Record and playback voice memos via `MediaRecorder` (`.3gp` / `AMR_NB`).
- **MicroSD Card Storage**: 4-stage smart fallback storage engine (Custom path → SD Card `/storage/sdcard1` → External → Internal).
- **Monotone Aesthetic**: Minimalist, distraction-free monochrome styling with `[bracket]` conventions.
- **Theme Switcher**: Instant `[light]` / `[dark]` mode toggle.
- **Custom Typography**: Built-in Space Grotesk font.
- **Zero Bloat**: Pure Java, zero AndroidX/Jetpack overhead, fully compatible with BlackBerry 10's API 18 Android runtime.

---

## 📦 Version History & Releases

Check the [Releases tab](https://github.com/warumpas-ops/notevault-bb10/releases) for APK sideload packages:

| Version | Description | Status | APK Download |
|:---|:---|:---|:---|
| **[v1.0.0](https://github.com/warumpas-ops/notevault-bb10/releases/tag/v1.0.0)** | **Final Release** — Custom `[ N ]` squircle icon matching the Space Grotesk vibe, polished audio recording, full SD card support, light/dark themes. | 🟢 Stable | [NoteVault.apk](https://github.com/warumpas-ops/notevault-bb10/releases/download/v1.0.0/NoteVault.apk) |
| **[v0.2.0](https://github.com/warumpas-ops/notevault-bb10/releases/tag/v0.2.0)** | **UI Overhaul** — Introduction of Space Grotesk font, monochrome palette, bracket UI style, and theme toggle. | 🟡 Pre-release | [NoteVault-v0.2.0-beta.apk](https://github.com/warumpas-ops/notevault-bb10/releases/download/v0.2.0/NoteVault-v0.2.0-beta.apk) |
| **[v0.1.0](https://github.com/warumpas-ops/notevault-bb10/releases/tag/v0.1.0)** | **Initial Prototype** — The first working test. Primitive UI that borrowed heavily from Anki before the redesign. | 🔴 Deprecated | [NoteVault-v0.1.0-prototype.apk](https://github.com/warumpas-ops/notevault-bb10/releases/download/v0.1.0/NoteVault-v0.1.0-prototype.apk) |

---

## 🛠️ Building

Built using standard Android SDK tools targeting API 18:

```bash
./gradlew assembleDebug
```

Output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📲 Sideloading onto BlackBerry 10

1. Download `NoteVault.apk` from the [Latest Release](https://github.com/warumpas-ops/notevault-bb10/releases/tag/v1.0.0).
2. Transfer it to your BlackBerry via USB or copy directly onto your MicroSD card.
3. Open the File Manager on BB10, tap the APK, and hit **Install**.
