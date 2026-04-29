<div align="center">

# 🧮 ArithMatrix
### Intelligent Calculator with Voice, Camera & Currency Conversion

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![ML Kit](https://img.shields.io/badge/ML_Kit-OCR-34A853?style=for-the-badge&logo=google&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-DI-1565C0?style=for-the-badge)
![Room](https://img.shields.io/badge/Room-Database-4CAF50?style=for-the-badge)

> *Three input modes. One clean app.*
> *Type it. Say it. Point your camera at it.*

</div>

---

## 📸 Screenshots

| Basic Calculator | Camera OCR | Voice Input |
|---|---|---|
| ![basic](assets/screen1.png) | ![camera](assets/screen2.png) | ![voice](assets/screen3.png) |

| Currency Converter | History |
|---|---|
| ![currency](assets/screen4.png) | ![history](assets/screen5.png) |

---

## 📌 Overview

ArithMatrix is a multi-modal calculator built for users who want
more than basic arithmetic.

Most calculators are just buttons. ArithMatrix supports four
distinct input modes — typed expressions, voice commands,
camera-based OCR, and currency conversion — all in one clean,
production-grade Android app with persistent history across
every mode.

Minimal on the surface. Powerful under the hood.

---

## ✨ Features

### 🧮 Basic Calculator
- Full expression input — `50 + 20%`, `12 × (5 + 3)`
- Proper operator precedence handling
- Intelligent percentage evaluation
- Clear separation between expression and result
- Fast and accurate custom evaluation engine

### 📷 Camera Calculator
- ML Kit OCR captures printed and typed math expressions
- Editable recognized expressions before evaluation
- One-tap evaluation after recognition
- Separate history tracking for camera input

### 🎙 Voice Calculator
- Speech-to-text expression input via Android SpeechRecognizer
- Live transcription visible before evaluation
- Optional TTS output — hear the result spoken back
- Mute / unmute control
- Dedicated voice calculation history

### 💱 Currency Converter
- Multi-currency conversion
- Clean numeric formatting
- Conversion history
- Architecture ready for live rate API integration

### 🗃 Smart History System
- Persistent local storage via Room
- Auto-updating history via Flow
- Mode-aware — Basic / Voice / Camera / Currency tracked separately
- Delete individual entries or clear all
- Reuse any past expression directly from history

---

## 🏗️ Architecture
UI Layer (Jetpack Compose)
↕ StateFlow / Flow
ViewModel Layer
↕
Repository Layer (Single Source of Truth)
↕                    ↕
Room Database        ML Kit + SpeechRecognizer
(History)            (Input processing)

ArithMatrix uses MVVM with Repository pattern:

- **UI** — Compose screens, observe state, emit events only
- **ViewModel** — holds UI state via StateFlow, all business logic
- **Repository** — single source of truth, mediates data sources
- **Room** — persistent history storage, reactive via Flow

**Result:** clear separation of concerns, testable, scalable,
feature addition time reduced by ~30%.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository Pattern |
| Dependency Injection | Hilt |
| Database | Room (SQLite) |
| State Management | StateFlow + Flow |
| Concurrency | Kotlin Coroutines |
| Camera + OCR | ML Kit Text Recognition |
| Voice | Android SpeechRecognizer + TTS |

---

## 🎨 Design Philosophy

- Minimalist, distraction-free interface
- Professional color palette — not flashy
- Adaptive light / dark theme
- Designed for one-hand usage
- No unnecessary visual noise

---

## 🚀 Getting Started

```bash
git clone https://github.com/ParthCh300x/ArithMatrix.git
```

Open in Android Studio. No API keys required.
Camera and microphone permissions requested at runtime.

Minimum SDK: 26

---

## 🔮 Roadmap

- [ ] Live currency exchange rates via Frankfurter API
- [ ] Advanced OCR for handwritten expressions
- [ ] Natural language expression parsing
- [ ] Cloud sync for calculation history
- [ ] Tablet-optimized layouts
- [ ] Accessibility improvements — voice-first mode

---

## 🤝 Contributors

**Parth Chaudhary** — Architecture, core logic, calculator engine,
Room database + history system, overall system design
→ [github.com/ParthCh300x](https://github.com/ParthCh300x)

**Shravan Bire** — UI refinement, feature contributions,
interaction design, architectural discussions
→ [github.com/shravanBire](https://github.com/shravanBire)

---

<div align="center">
<i>Type it. Say it. Point your camera at it.</i>
</div>
