package com.example.poseresearch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAPTURE = 5001
    }

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            requestProjection()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            requestBluetoothPermissions()
        }

        stopButton.setOnClickListener {
            stopCapture()
        }

        statusText.text = "Sẵn sàng"
    }

    private fun requestBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

            if (missing.isNotEmpty()) {
                permissionLauncher.launch(
                    missing.toTypedArray()
                )
                return
            }
        }

        requestProjection()
    }

    private fun requestProjection() {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val intent =
            manager.createScreenCaptureIntent()

        startActivityForResult(
            intent,
            REQUEST_CAPTURE
        )
    }

    @Deprecated("Deprecated API retained for Android compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode != REQUEST_CAPTURE) {
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null) {

            statusText.text =
                "MediaProjection bị từ chối"

            return
        }

        val serviceIntent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {

                action =
                    ScreenCaptureService.ACTION_START

                putExtra(
                    ScreenCaptureService.EXTRA_RESULT_CODE,
                    resultCode
                )

                putExtra(
                    ScreenCaptureService.EXTRA_RESULT_DATA,
                    data
                )
            }

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )

        statusText.text =
            "Đã cấp quyền — chờ 3 giây..."
    }

    private fun stopCapture() {

        val intent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {
                action =
                    ScreenCaptureService.ACTION_STOP
            }

        startService(intent)

        statusText.text =
            "Đã dừng"
    }
}
