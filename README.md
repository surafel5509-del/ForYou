# 3D Studio Pro Max

A touch-first Android 3D creation workspace inspired by professional desktop 3D tools. The app opens directly into a landscape modeling workspace with a sci-fi motorcycle viewport, topology editing overlay, transform gizmo, outliner, materials inspector, tool rail, workspace tabs, and animation timeline.

## Build locally

The project uses Android Gradle Plugin 9.1.1 and Java 17. With Android SDK 36 installed:

```bash
gradle :app:assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

`.github/workflows/android-build.yml` builds every push, pull request, and manual workflow dispatch. It installs Java 17, Android SDK 36, and Gradle 9.3.1, then publishes the debug APK as the `3d-studio-pro-max-debug-apk` workflow artifact.
