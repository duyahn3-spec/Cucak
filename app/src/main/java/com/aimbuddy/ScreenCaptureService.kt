package com.aimbuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
        const val ACTION_START =
            "com.aimbuddy.START"

        const val ACTION_STOP =
            "com.aimbuddy.STOP"

        const val EXTRA_RESULT_CODE =
            "result_code"

        const val EXTRA_RESULT_DATA =
            "result_data"

        private const val CHANNEL_ID =
            "cucak_capture"

        private const val NOTIFICATION_ID =
            1001

        private const val START_DELAY =
            3000L
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private var projection:
        MediaProjection? = null

    private var virtualDisplay:
        VirtualDisplay? = null

    private var imageReader:
        ImageReader? = null

    private var captureStarted =
        false

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

                val resultData: Intent? =
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

                    handler.removeCallbacksAndMessages(
                        null
                    )

                    /*
                     * Sau khi người dùng cấp quyền,
                     * đợi đúng 3 giây rồi mới tạo
                     * MediaProjection.
                     */
                    handler.postDelayed({

                        if (!isDestroyed) {
                            startCapture(
                                resultCode,
                                resultData
                            )
                        }

                    }, START_DELAY)
                }
            }

            ACTION_STOP -> {
                stopCapture()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private var isDestroyed = false

    private fun startCapture(
        resultCode: Int,
        resultData: Intent
    ) {

        stopCapture()

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val newProjection =
            manager.getMediaProjection(
                resultCode,
                resultData
            )

        if (newProjection == null) {
            stopSelf()
            return
        }

        projection = newProjection

        val metrics =
            resources.displayMetrics

        val width =
            metrics.widthPixels

        val height =
            metrics.heightPixels

        val density =
            metrics.densityDpi

        /*
         * ImageReader chỉ giữ tối đa 2 frame.
         * Không tạo file ảnh/video.
         */
        val newReader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

        imageReader = newReader

        newReader.setOnImageAvailableListener(
            { reader ->

                /*
                 * acquireLatestImage() bỏ qua các
                 * frame cũ và lấy frame mới nhất.
                 */
                val image =
                    reader.acquireLatestImage()
                        ?: return@setOnImageAvailableListener

                try {

                    /*
                     * FRAME ĐƯỢC NHẬN TẠI ĐÂY.
                     *
                     * Nghiên cứu xử lý ảnh có thể được
                     * đặt ở đây.
                     *
                     * Không lưu frame xuống bộ nhớ.
                     */

                } finally {

                    image.close()
                }
            },
            handler
        )

        virtualDisplay =
            newProjection.createVirtualDisplay(
                "CucakCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                newReader.surface,
                null,
                handler
            )

        captureStarted = true
    }

    private fun stopCapture() {

        handler.removeCallbacksAndMessages(
            null
        )

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.setOnImageAvailableListener(
            null,
            null
        )

        imageReader?.close()
        imageReader = null

        projection?.stop()
        projection = null

        captureStarted = false
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Cucak Screen Capture",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "Screen capture service"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification():
        Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Cucak")
            .setContentText(
                if (captureStarted) {
                    "Screen capture is running"
                } else {
                    "Preparing screen capture..."
                }
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_camera
            )
            .setOngoing(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .build()
    }

    override fun onDestroy() {

        isDestroyed = true

        stopCapture()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
