# touch-lock

An Android app that overlays a transparent, full-screen view to temporarily block accidental touches. Double-tap anywhere on the overlay or use the notification's **Unlock** action to release the touch lock.

## Permissions

- **Display over other apps**: Required to show the touch-lock overlay.
- **Notifications**: Recommended on Android 13+ so the persistent lock notification and its unlock action are visible.

## Install on a POCO X8 Pro

1. Build the APK using the command below and copy `app/build/outputs/apk/debug/app-debug.apk` to the phone.
2. Open the APK and allow your file manager or browser to install unknown apps if prompted.
3. Open Touch Lock and tap **Lock touch**. Allow **Display over other apps** when Android opens its permission screen. Notification permission is recommended but optional.
4. If HyperOS stops the lock service in the background, open the app's battery settings and select **No restrictions**. If available, enable **Autostart** for Touch Lock. Menu names can vary by HyperOS version.

Double-tap the screen or use **Unlock** in the persistent notification to end the lock.

## Build

```bash
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
