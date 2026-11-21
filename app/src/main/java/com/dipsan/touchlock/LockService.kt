package com.dipsan.touchlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class LockService : Service() {
    private var overlay: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UNLOCK) {
            unlock()
            return START_NOT_STICKY
        }

        try {
            startAsForeground()
        } catch (error: SecurityException) {
            Log.e(TAG, "Unable to start the foreground service", error)
            reportLockFailure("Android could not start Touch Lock. Check its permissions and try again.")
            stopSelfResult(startId)
            return START_NOT_STICKY
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Unable to start the foreground service", error)
            reportLockFailure("Android could not start Touch Lock. Check its permissions and try again.")
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        if (overlay == null) showOverlay()
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Touch lock",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val unlockIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LockService::class.java).setAction(ACTION_UNLOCK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Touch is locked")
            .setContentText("Double tap the screen to unlock")
            .setContentIntent(contentIntent)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_lock_lock,
                    "Unlock",
                    unlockIntent
                ).build()
            )
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            reportLockFailure("Allow display over other apps, then try again.")
            stopSelf()
            return
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                unlock()
                return true
            }
        })

        val view = View(this).apply {
            keepScreenOn = true
            setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT
        )

        try {
            windowManager.addView(view, params)
            overlay = view
        } catch (error: SecurityException) {
            reportLockFailure("Touch Lock could not display its overlay. Check app permissions.")
            Log.e(TAG, "Overlay permission was rejected while adding the lock view", error)
            stopSelf()
        } catch (error: WindowManager.BadTokenException) {
            reportLockFailure("Touch Lock could not display its overlay. Please try again.")
            Log.e(TAG, "Android rejected the touch-lock overlay", error)
            stopSelf()
        } catch (error: IllegalStateException) {
            reportLockFailure("Touch Lock could not display its overlay. Please try again.")
            Log.e(TAG, "Unable to add the touch-lock overlay", error)
            stopSelf()
        }
    }

    private fun reportLockFailure(message: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Touch lock did not start")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun unlock() {
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        overlay = null
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Touch-lock overlay was already removed", error)
        }
    }

    companion object {
        const val ACTION_UNLOCK = "com.dipsan.touchlock.UNLOCK"
        private const val NOTIFICATION_CHANNEL_ID = "touch_lock"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "TouchLock"
    }
}
