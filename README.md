# Bluetooth Shark

Android test app for selecting a paired Bluetooth device and watching its connection state.

## Important Android limitation

A normal third-party Android app cannot globally force every other app's media audio to stay routed to one Bluetooth A2DP device. This prototype can monitor the chosen paired device and alert when it disconnects.

## Build

GitHub Actions automatically builds a debug APK on pushes to `main`.

Open:
Actions → latest "Build Android APK" run → Artifacts → Bluetooth-Shark-debug
