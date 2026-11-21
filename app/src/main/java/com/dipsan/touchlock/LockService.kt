package com.dipsan.touchlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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
        startAsForeground()
        if (overlay == null) showOverlay()
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("lock", "Touch lock", NotificationManager.IMPORTANCE_LOW)
        )
        val unlockIntent = PendingIntent.getService(
            this, 0,
            Intent(this, LockService::class.java).setAction(ACTION_UNLOCK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "lock")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Touch is locked")
            .setContentText("Double tap the screen to unlock")
            .addAction(Notification.Action.Builder(null, "Unlock", unlockIntent).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun showOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                unlock()
                return true
            }
        })
        val view = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            keepScreenOn = true
            setOnTouchListener { _, e -> detector.onTouchEvent(e); true }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(view, params)
        overlay = view
    }

    private fun unlock() {
        overlay?.let {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it)
        }
        overlay = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        overlay?.let {
            try { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
        super.onDestroy()
    }

    companion object { const val ACTION_UNLOCK = "com.dipsan.touchlock.UNLOCK" }
}
