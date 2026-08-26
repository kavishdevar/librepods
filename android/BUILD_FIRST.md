# First Wear OS build

The current branch is prepared for the first debug APK build.

## Local build

From the repository root:

```text
cd android
./gradlew :app:assembleDebug
```

On Windows:

```text
cd android
gradlew.bat :app:assembleDebug
```

Expected APK:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## What the first build currently contains

- Wear OS-only application entry point.
- Minimal Compose home screen.
- Bluetooth permissions and connected-device foreground service declaration.
- Wear Bluetooth scanner boundary.
- Wear Bluetooth connection facade.
- Owned AirPods L2CAP connection session.
- AACP/ATT transport boundary and migration scaffolding.
- AirPods controller state/command/event model.
- Background Wear service lifecycle.
- Existing LibrePods AACP/BLE protocol code retained for incremental migration.

## Important

This first APK is an architecture/integration checkpoint, not the finished AirPods controller. The Connect button is intentionally not wired to a production handshake yet. The next phase connects discovery, protocol parameters, L2CAP session, ATT/AACP readers, and state updates end-to-end.

## Device target

The app declares `android.hardware.type.watch` and targets Wear OS. It is intentionally not a phone companion application.
