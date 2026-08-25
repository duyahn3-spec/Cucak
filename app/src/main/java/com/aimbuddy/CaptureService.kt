package com.example.poseresearch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class CaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "pose_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CAPTURE_WIDTH = 320
        private const val CAPTURE_HEIGHT = 240
        private const val TAG = "CaptureService"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var poseDetector: PoseDetector
    private lateinit var bleManager: BleManager
    private lateinit var overlayView: PoseOverlayView

    private val handlerThread = HandlerThread("CaptureThread")
    private lateinit var handler: Handler
    private var frameCount = 0
    private var startTime = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        poseDetector = PoseDetector(this)
        bleManager = BleManager(this)
        bleManager.connect()
        overlayView = PoseOverlayView(this, null) // sẽ được tham chiếu từ MainActivity
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra("resultCode", -1)
        val data = intent.getParcelableExtra<Intent>("data")

        if (resultCode == -1 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startMediaProjection(resultCode, data)
        return START_NOT_STICKY
    }

    private fun startMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        imageReader = ImageReader.newInstance(CAPTURE_WIDTH, CAPTURE_HEIGHT, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                handler.post { processImage(it) }
                it.close()
            }
        }, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "PoseResearch",
            CAPTURE_WIDTH, CAPTURE_HEIGHT, 160,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )
        startTime = System.currentTimeMillis()
    }

    private fun processImage(image: Image) {
        val bitmap = imageToBitmap(image) ?: return
        val keypoints = poseDetector.detect(bitmap)
        bitmap.recycle()

        // Lấy Nose (index 0)
        val nose = keypoints.firstOrNull { it.name == "Nose" }
        if (nose != null && nose.confidence > 0.4f) {
            val screenWidth = 1080  // thay bằng độ phân giải màn hình thực tế
            val screenHeight = 2400
            val x = nose.x * screenWidth
            val y = nose.y * screenHeight
            val dx = (x - screenWidth / 2).toInt()
            val dy = (y - screenHeight / 2).toInt()
            bleManager.sendDelta(dx, dy)
        }

        // Cập nhật overlay
        val intent = Intent("POSE_UPDATE").apply {
            putExtra("keypoints", keypoints.toTypedArray())
        }
        sendBroadcast(intent)

        // Tính FPS
        frameCount++
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed >= 1000) {
            Log.d(TAG, "FPS: $frameCount")
            frameCount = 0
            startTime = System.currentTimeMillis()
        }
    }

    private fun imageToBitmap(image: Image): android.graphics.Bitmap? {
        val buffer = image.planes[0].buffer
        val bitmap = android.graphics.Bitmap.createBitmap(image.width, image.height, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        bleManager.close()
        handlerThread.quitSafely()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pose Research",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pose Research")
            .setContentText("Capturing screen for pose estimation")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }
}
