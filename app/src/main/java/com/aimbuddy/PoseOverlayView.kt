package com.example.poseresearch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class PoseOverlayView(
    context: Context
) : View(context) {

    private val pointPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

    private var pose: PoseResult? = null

    private val skeleton =
        arrayOf(
            0 to 1,

            1 to 2,
            2 to 3,
            3 to 4,

            1 to 5,
            5 to 6,
            6 to 7,

            1 to 8,
            8 to 9,
            9 to 10,

            8 to 11,
            11 to 12,
            12 to 13,

            0 to 14,
            0 to 15,

            14 to 16,
            15 to 17
        )

    fun setPose(
        result: PoseResult?
    ) {
        pose = result
        invalidate()
    }

    override fun onDraw(
        canvas: Canvas
    ) {
        super.onDraw(canvas)

        val result =
            pose ?: return

        for ((a, b) in skeleton) {

            val p1 =
                result.keypoints[a]

            val p2 =
                result.keypoints[b]

            if (p1.score < 0.20f ||
                p2.score < 0.20f
            ) {
                continue
            }

            canvas.drawLine(
                p1.x * width,
                p1.y * height,
                p2.x * width,
                p2.y * height,
                linePaint
            )
        }

        for (point in result.keypoints) {

            if (point.score < 0.20f) {
                continue
            }

            canvas.drawCircle(
                point.x * width,
                point.y * height,
                7f,
                pointPaint
            )
        }
    }
}
