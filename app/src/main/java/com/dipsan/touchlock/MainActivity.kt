package com.dipsan.touchlock

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var permissionStatus: TextView
    private var pendingLock = false
    private var requestedNotificationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val info = TextView(this).apply {
            text = "Temporarily block screen touches. Double-tap anywhere or use the notification to unlock."
            gravity = Gravity.CENTER
            textSize = 16f
        }

        permissionStatus = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 14f
        }

        val button = Button(this).apply {
            text = "Lock touch"
            setOnClickListener { startLocking() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(info)
            addView(permissionStatus)
            addView(button)
        })

        updatePermissionStatus()
    }

    private fun startLocking() {
        pendingLock = true
        continueLocking()
    }

    private fun continueLocking() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ),
                REQUEST_OVERLAY_PERMISSION
            )
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED &&
            !requestedNotificationPermission
        ) {
            requestedNotificationPermission = true
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
            return
        }

        pendingLock = false
        try {
            startForegroundService(Intent(this, LockService::class.java))
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "Android could not start the touch lock. Try again.", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Touch Lock is missing a required permission.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION && pendingLock) {
            if (Settings.canDrawOverlays(this)) {
                continueLocking()
            } else {
                pendingLock = false
                updatePermissionStatus()
                Toast.makeText(this, "Allow display over other apps to use Touch Lock.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION && pendingLock) {
            continueLocking()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        if (pendingLock && Settings.canDrawOverlays(this)) {
            continueLocking()
        }
    }

    private fun updatePermissionStatus() {
        if (!::permissionStatus.isInitialized) return
        val overlayStatus = if (Settings.canDrawOverlays(this)) "Allowed" else "Required"
        val notificationStatus = if (Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            "Allowed"
        } else {
            "Optional"
        }
        permissionStatus.text =
            "Display over other apps: $overlayStatus\nNotifications: $notificationStatus"
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 100
        private const val REQUEST_NOTIFICATION_PERMISSION = 101
    }
}
