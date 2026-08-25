package com.aimbuddy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var keypoints: List<PoseKeypoint> = emptyList()
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
        textSize = 40f
    }
    private val dotPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        1 to 5, 5 to 6, 6 to 7,
        1 to 8, 8 to 9, 9 to 10,
        8 to 11, 11 to 12, 12 to 13,
        0 to 14, 0 to 15,
        14 to 16, 15 to 17
    )

    fun updatePose(points: List<PoseKeypoint>) {
        keypoints = points
        invalidate()
    }

    fun clearPose() {
        keypoints = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (keypoints.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        for ((i, j) in connections) {
            if (i < keypoints.size && j < keypoints.size) {
                val p1 = keypoints[i]
                val p2 = keypoints[j]
                if (p1.confidence > 0.3f && p2.confidence > 0.3f) {
                    canvas.drawLine(p1.x * w, p1.y * h, p2.x * w, p2.y * h, paint)
                }
            }
        }

        for (kp in keypoints) {
            if (kp.confidence > 0.3f) {
                val x = kp.x * w
                val y = kp.y * h
                canvas.drawCircle(x, y, 12f, dotPaint)
                canvas.drawText("${kp.name} (${"%.2f".format(kp.confidence)})", x + 20, y - 20, paint)
            }
        }
    }
}
