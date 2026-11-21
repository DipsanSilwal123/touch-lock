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

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        val info = TextView(this).apply {
            text = "Tap the button, then use your phone normally.\nDouble tap anywhere to unlock."
            gravity = Gravity.CENTER
            textSize = 16f
        }

        val button = Button(this).apply {
            text = "Lock touch"
            setOnClickListener { startLocking() }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(info)
            addView(button)
        })
    }

    private fun startLocking() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        startForegroundService(Intent(this, LockService::class.java))
        finish()
    }
}
