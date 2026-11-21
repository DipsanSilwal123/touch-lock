# touch-lock

An Android app that overlays a transparent, full-screen view to temporarily block accidental touches. Double-tap anywhere on the overlay or use the notification's **Unlock** action to release the touch lock.

## Permissions

- **Display over other apps**: Required to show the touch-lock overlay.
- **Notifications**: Required on Android 13+ for the foreground-service notification.

## Build

```bash
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
