package com.aimbuddy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var overlayView: PoseOverlayView

    companion object {
        private const val REQUEST_CODE_OVERLAY = 1001
        private const val REQUEST_CODE_MEDIA_PROJECTION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        overlayView = findViewById(R.id.overlayView)

        btnStart.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName"))
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY)
                    return@setOnClickListener
                }
            }
            requestMediaProjection()
        }

        btnStop.setOnClickListener {
            stopCapture()
        }

        btnStop.isEnabled = false
    }

    private fun requestMediaProjection() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    requestMediaProjection()
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK && data != null) {
                    startCapture(resultCode, data)
                } else {
                    Toast.makeText(this, "MediaProjection permission required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, CaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        btnStart.isEnabled = false
        btnStop.isEnabled = true
        Toast.makeText(this, "Capture started", Toast.LENGTH_SHORT).show()
    }

    private fun stopCapture() {
        stopService(Intent(this, CaptureService::class.java))
        overlayView.clearPose()
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        Toast.makeText(this, "Capture stopped", Toast.LENGTH_SHORT).show()
    }
}
