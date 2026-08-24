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
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

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
            "capture"

        private const val NOTIFICATION_ID =
            1001

        private const val START_DELAY =
            3000L
    }

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var projection:
            MediaProjection? = null

    private var display:
            VirtualDisplay? = null

    private var reader:
            ImageReader? = null

    private var detector:
            PoseDetector? = null

    private var bleSender:
            BleSender? = null

    private val processing =
        AtomicBoolean(false)

    private var frameCount = 0

    private var lastFpsTime =
        System.nanoTime()

    private var fps = 0

    override fun onCreate() {

        super.onCreate()

        createChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        detector =
            PoseDetector(this)

        bleSender =
            BleSender()
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
                    if (
                        Build.VERSION.SDK_INT >= 33
                    ) {

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
                     * Cấp quyền xong không capture ngay.
                     * Chờ đúng 3 giây.
                     */

                    handler.postDelayed({

                        startCapture(
                            resultCode,
                            resultData
                        )

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

    private fun startCapture(
        resultCode: Int,
        resultData: Intent
    ) {

        stopCapture()

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        projection =
            manager.getMediaProjection(
                resultCode,
                resultData
            )

        if (projection == null) {

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

        /*
         * 2 buffer để giảm khả năng bị nghẽn.
         */

        reader =
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

        reader?.setOnImageAvailableListener(
            { imageReader ->

                /*
                 * Nếu detector đang xử lý frame trước,
                 * bỏ frame mới này.
                 *
                 * Mục đích là không tạo backlog.
                 */

                if (
                    !processing.compareAndSet(
                        false,
                        true
                    )
                ) {

                    imageReader.acquireLatestImage()
                        ?.close()

                    return@setOnImageAvailableListener
                }

                val image =
                    imageReader.acquireLatestImage()

                if (image == null) {

                    processing.set(false)

                    return@setOnImageAvailableListener
                }

                try {

                    val bitmap =
                        imageToBitmap(image)

                    if (bitmap != null) {

                        processFrame(
                            bitmap
                        )

                        bitmap.recycle()
                    }

                } catch (
                    _: Exception
                ) {

                    /*
                     * Không để một frame lỗi
                     * làm chết capture service.
                     */

                } finally {

                    image.close()

                    processing.set(false)
                }

            },
            handler
        )

        display =
            projection?.createVirtualDisplay(
                "CucakCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface,
                null,
                handler
            )
    }

    private fun processFrame(
        bitmap: Bitmap
    ) {

        val detector =
            detector
                ?: return

        val start =
            System.nanoTime()

        val result =
            detector.detect(
                bitmap
            )

        val elapsed =
            (
                System.nanoTime() -
                    start
                ) / 1_000_000.0

        frameCount++

        val now =
            System.nanoTime()

        if (
            now - lastFpsTime >=
            1_000_000_000L
        ) {

            fps =
                frameCount

            frameCount = 0

            lastFpsTime = now
        }

        /*
         * Kết quả nghiên cứu:
         *
         * result.centerX
         * result.centerY
         * result.keypoints
         * result.inferenceMs
         * fps
         */

        /*
         * Chỉ gọi BLE ở đây nếu bạn đã cấu hình
         * kết nối BLE nghiên cứu của mình.
         *
         * bleSender?.sendPose(result)
         */

        @Suppress(
            "UNUSED_VARIABLE"
        )
        val ignored =
            elapsed + fps
    }

    private fun imageToBitmap(
        image: Image
    ): Bitmap? {

        val plane =
            image.planes.firstOrNull()
                ?: return null

        val buffer:
            ByteBuffer =
            plane.buffer

        val pixelStride =
            plane.pixelStride

        val rowStride =
            plane.rowStride

        val rowPadding =
            rowStride -
                pixelStride *
                image.width

        val bitmapWidth =
            image.width +
                rowPadding /
                pixelStride

        val bitmap =
            Bitmap.createBitmap(
                bitmapWidth,
                image.height,
                Bitmap.Config.ARGB_8888
            )

        bitmap.copyPixelsFromBuffer(
            buffer
        )

        /*
         * Cắt phần padding của ImageReader.
         */

        if (
            bitmapWidth != image.width
        ) {

            val cropped =
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    image.width,
                    image.height
                )

            bitmap.recycle()

            return cropped
        }

        return bitmap
    }

    private fun stopCapture() {

        handler.removeCallbacksAndMessages(
            null
        )

        processing.set(false)

        display?.release()

        display = null

        reader?.setOnImageAvailableListener(
            null,
            null
        )

        reader?.close()

        reader = null

        projection?.stop()

        projection = null
    }

    private fun createChannel() {

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

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
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
            .setContentTitle(
                "Cucak"
            )
            .setContentText(
                "AI capture is running"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_camera
            )
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {

        stopCapture()

        detector?.close()

        detector = null

        bleSender?.close()

        bleSender = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}
