# Markdown & PDF Editor with TTS

A premium Android application built using **Jetpack Compose** and **Material 3** that serves as a highly functional Markdown Editor, a native PDF page renderer, and a paragraph-by-paragraph Text-to-Speech (TTS) reader.

This project was built utilizing the operationalized **Android SDLC Dev Suite** workflow (MVVM, Clean Architecture, Hilt DI, and automated scaffolding/compliance auditing).

---

## 🌟 Key Features

1. **Markdown Editor**:
   - Live Markdown editing with monospace typography.
   - Text editing stats (real-time Word, Character, and Line counters).
   - Formatting bar to instantly inject markdown tags (Bold, Italic, Header, Code, Bullet List, Link) at the cursor selection.
   - Full Undo & Redo buffer stacks.
   - Native file saving and loading capabilities (scoped Storage Access Framework).

2. **Markdown Preview**:
   - Custom block parser converting raw Markdown into stylized Jetpack Compose components.
   - Styled Headings (Display, Headline, and Title tiers).
   - Inline formatting rendering: **bold**, *italic*, `inline code`, and custom links.
   - Code Blocks: syntax-focused backgrounds with scroll and a one-tap "Copy" button.
   - Blockquotes: styled with secondary accent left border.
   - Bullet lists and dividers.

3. **PDF Viewer**:
   - Native offline rendering utilizing Android's `PdfRenderer` to paint pages as high-performance Bitmaps.
   - Detailed document banners with page count stats.
   - Scrollable page listing with spacing, custom cards, and page counters.
   - Dynamic memory management preventing OOM errors on large files.

4. **Paragraph-by-Paragraph TTS (Text-to-Speech)**:
   - Segment-by-segment reading: plays, pauses, resumes, and stops.
   - Real-time visual paragraph highlighting: highlights the currently spoken paragraph in the preview pane.
   - TTS Control Sheet: float-control panel containing Speed rate slider (0.5x to 2.0x) and Voice Pitch slider (0.5x to 2.0x).
   - Reflection-safe Android 15 (API 35) PDF text extraction hook to enable TTS on selected PDF files.

---

## 🏗️ Clean Architecture & Project Structure

The project strictly follows the 3-layer Clean Architecture system:

```
MarkdownPdfEditor/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/markdownpdfeditor/
│   │   │   │   ├── di/                 # Dependency injection modules
│   │   │   │   ├── data/               # Data layer (TTS Manager, Pdf Manager, Markdown Parser)
│   │   │   │   ├── ui/                 # UI layer (ViewModels & Themes)
│   │   │   │   │   ├── theme/          # Custom Material 3 dynamic color tokens & Type scales
│   │   │   │   │   ├── screens/        # MainScreen, EditorScreen, PdfViewerScreen
│   │   │   │   │   └── EditorViewModel.kt
│   │   │   │   ├── MainActivity.kt     # App entry point
│   │   │   │   └── MarkdownPdfApplication.kt
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

---

## 🛠️ Build & Setup Guide

### Prerequisites
- **JDK 17** (Temurin/OpenJDK 17 recommended)
- **Android SDK 35** (compileSdk/targetSdk = 35)

### Running Commands

1. **Compile Kotlin Source Code**:
   ```bash
   ./gradlew compileDebugKotlin
   ```

2. **Assemble Debug Build**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Pre-flight Compliance Audit**:
   You can audit the project for release suitability (obfuscation, target SDK, permissions) using the dev suite auditor:
   ```bash
   python3 /home/user/.agents/skills/android-dev-suite/shared/android_suite_tool.py audit --path .
   ```

---

## 📜 Dev Suite Workflow & Scaffolding Log

1. **Scaffolding**: Initialized using the `android_suite_tool.py` scaffolding utility.
2. **Setup**: Configured custom `AppTheme` with calibrated dark/light surface colors and typography mapping.
3. **Core APIs**:
   - Implemented `TtsManager` wrapping `android.speech.tts.TextToSpeech` with progress listeners.
   - Implemented `PdfManager` wrapping `android.graphics.pdf.PdfRenderer` and reflecting API 35 content extraction.
   - Implemented local Markdown text tokenizing and renderer.
4. **Validation**: Built using Gradle and tested compliance.
