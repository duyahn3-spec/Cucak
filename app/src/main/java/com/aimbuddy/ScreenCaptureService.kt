package com.aimbuddy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {

        const val ACTION_START =
            "com.example.poseresearch.START"

        const val ACTION_STOP =
            "com.example.poseresearch.STOP"

        const val EXTRA_RESULT_CODE =
            "result_code"

        const val EXTRA_RESULT_DATA =
            "result_data"

        private const val CHANNEL_ID =
            "pose_capture"

        private const val NOTIFICATION_ID =
            1001
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var detector: PoseDetector? = null
    private var ble: BleTransport? = null

    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler

    private val mainHandler =
        Handler.createAsync(
            android.os.Looper.getMainLooper()
        )

    private var started = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        workerThread =
            HandlerThread("PoseWorker")

        workerThread.start()

        workerHandler =
            Handler(workerThread.looper)

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

                val data =
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
                    data != null &&
                    !started
                ) {

                    mainHandler.postDelayed(
                        {

                            startCapture(
                                resultCode,
                                data
                            )

                        },
                        3000L
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

    private fun startCapture(
        resultCode: Int,
        resultData: Intent
    ) {

        if (started) {
            return
        }

        started = true

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        try {

            projection =
                manager.getMediaProjection(
                    resultCode,
                    resultData
                )

        } catch (e: Exception) {

            started = false
            stopSelf()
            return
        }

        if (projection == null) {

            started = false
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

        detector =
            PoseDetector(
                applicationContext
            )

        ble =
            BleTransport(
                applicationContext
            )

        imageReader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

        imageReader?.setOnImageAvailableListener(
            { reader ->

                processLatestFrame(
                    reader
                )

            },
            workerHandler
        )

        virtualDisplay =
            projection?.createVirtualDisplay(
                "PoseResearch",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                workerHandler
            )
    }

    private fun processLatestFrame(
        reader: ImageReader
    ) {

        val image =
            reader.acquireLatestImage()
                ?: return

        try {

            val width =
                image.width

            val height =
                image.height

            val plane =
                image.planes[0]

            val buffer =
                plane.buffer

            val pixelStride =
                plane.pixelStride

            val rowStride =
                plane.rowStride

            val rowPadding =
                rowStride -
                    pixelStride * width

            val bitmapWidth =
                width +
                    rowPadding / pixelStride

            val bitmap =
                Bitmap.createBitmap(
                    bitmapWidth,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            buffer.rewind()

            bitmap.copyPixelsFromBuffer(
                buffer
            )

            val result =
                detector?.detect(
                    bitmap,
                    width,
                    height
                )

            bitmap.recycle()

            if (result != null) {

                ble?.send(
                    result
                )
            }

        } catch (_: Throwable) {

            // Bỏ frame lỗi, không lưu frame.

        } finally {

            image.close()
        }
    }

    private fun stopCapture() {

        started = false

        mainHandler.removeCallbacksAndMessages(
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

        detector?.close()
        detector = null

        ble?.close()
        ble = null
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Pose Research",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "Pose Research"
            )
            .setContentText(
                "Đang xử lý frame realtime"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_camera
            )
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {

        stopCapture()

        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
