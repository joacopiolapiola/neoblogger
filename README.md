# Neoblogger

Neoblogger is a native Android editor for creating static websites. The product contract and engineering workflow are documented in [PRODUCT_SPEC.md](PRODUCT_SPEC.md), [AGENTS.md](AGENTS.md), and [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Current implementation

The first app slice provides a dark Compose shell, private local project creation and reopening, editing/saving of `index.html`, `style.css`, and `script.js`, and an interactive WebView Preview. The Preview serves only files inside the currently open project through `WebViewAssetLoader`; external top-level links require confirmation, non-GET top-level submissions are blocked, and no native JavaScript bridge is exposed.

The Design tab is intentionally not a visual editor yet. SAF folder import/export, ZIP import/export, Emmet, CodeMirror, syntax highlighting, visual/source synchronization, undo/redo, and ad behavior are not implemented in this milestone. The source editor is a basic multiline Compose field.

## Build

Prerequisites: Android Studio, JDK 17, Android SDK Platform 35, Build Tools, and Gradle 8.9. Dependency versions are pinned in the Gradle build files. The application ID `com.neoblogger.app` is provisional and should be confirmed before publishing. From this directory:

```powershell
gradle testDebugUnitTest
gradle assembleDebug
```

The Gradle wrapper scripts/JAR are not included yet because no Gradle installation was available to generate the wrapper. Once Gradle 8.9 is installed, run `gradle wrapper --gradle-version 8.9`, then use `./gradlew` (macOS/Linux) or `./gradlew.bat` (Windows). The Android build and device behavior have not yet been verified in the current environment.
