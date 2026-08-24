package com.aimbuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "START_CAPTURE"
        const val ACTION_STOP = "STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val CHANNEL_ID = "capture_channel"
        private const val NOTIFICATION_ID = 1001

        private const val DELAY_MS = 3000L
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var frameCount = 0L

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {

                val resultCode =
                    intent.getIntExtra(
                        EXTRA_RESULT_CODE,
                        -1
                    )

                val resultData =
                    if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA,
                            Intent::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA
                        )
                    }

                if (
                    resultCode != -1 &&
                    resultData != null
                ) {
                    startCaptureWithDelay(
                        resultCode,
                        resultData
                    )
                }
            }

            ACTION_STOP -> {
                stopCapture()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startCaptureWithDelay(
        resultCode: Int,
        resultData: Intent
    ) {

        stopCapture()

        /*
         * Quyền đã được cấp.
         * Chờ đúng 3 giây trước khi tạo
         * VirtualDisplay/ImageReader.
         */
        handler.postDelayed({

            if (isDestroyed()) {
                return@postDelayed
            }

            startCapture(
                resultCode,
                resultData
            )

        }, DELAY_MS)
    }

    private fun startCapture(
        resultCode: Int,
        resultData: Intent
    ) {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        mediaProjection =
            manager.getMediaProjection(
                resultCode,
                resultData
            )

        if (mediaProjection == null) {
            stopSelf()
            return
        }

        val metrics =
            resources.displayMetrics

        val width =
            metrics.widthPixels

        val height =
            metrics.heightPixels

        val density =
            metrics.densityDpi

        imageReader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

        imageReader?.setOnImageAvailableListener(
            { reader ->

                val image =
                    reader.acquireLatestImage()
                        ?: return@setOnImageAvailableListener

                try {

                    /*
                     * Chỉ kiểm tra pipeline.
                     * Không lưu frame.
                     */
                    frameCount++

                } finally {

                    /*
                     * BẮT BUỘC đóng image
                     * để tránh đầy ImageReader.
                     */
                    image.close()
                }

            },
            handler
        )

        virtualDisplay =
            mediaProjection?.createVirtualDisplay(
                "CucakCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
    }

    private fun stopCapture() {

        handler.removeCallbacksAndMessages(null)

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.setOnImageAvailableListener(
            null,
            null
        )

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        frameCount = 0L
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Screen Capture",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setContentTitle(
                "Cucak"
            )
            .setContentText(
                "Screen capture is running"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_camera
            )
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {

        stopCapture()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
