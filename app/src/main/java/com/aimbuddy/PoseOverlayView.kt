package com.example.poseresearch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val skeletonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 32f
    }

    private var results: List<PoseResult> = emptyList()
    private var fps = 0f
    private var latencyMs = 0f

    fun update(
        newResults: List<PoseResult>,
        newFps: Float,
        newLatencyMs: Float
    ) {
        results = newResults
        fps = newFps
        latencyMs = newLatencyMs
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in results) {
            drawPerson(canvas, result)
        }

        textPaint.textSize = 28f
        canvas.drawText(
            "FPS: %.1f  Latency: %.1f ms".format(
                fps,
                latencyMs
            ),
            20f,
            40f,
            textPaint
        )
    }

    private fun drawPerson(
        canvas: Canvas,
        result: PoseResult
    ) {
        val box = RectF(
            result.left,
            result.top,
            result.right,
            result.bottom
        )

        canvas.drawRect(box, boxPaint)

        val cx = (result.left + result.right) / 2f
        val cy = (result.top + result.bottom) / 2f

        textPaint.textSize = 30f

        canvas.drawText(
            "ID ${result.id}",
            result.left,
            (result.top - 55f).coerceAtLeast(30f),
            textPaint
        )

        canvas.drawText(
            "X: ${cx.toInt()}  Y: ${cy.toInt()}",
            result.left,
            (result.top - 20f).coerceAtLeast(30f),
            textPaint
        )

        drawSkeleton(
            canvas,
            result.keypoints
        )
    }

    private fun drawSkeleton(
        canvas: Canvas,
        points: List<PoseKeypoint>
    ) {
        val connections = arrayOf(
            intArrayOf(0, 1),
            intArrayOf(1, 2),
            intArrayOf(2, 3),
            intArrayOf(3, 4),

            intArrayOf(5, 6),

            intArrayOf(5, 7),
            intArrayOf(7, 9),

            intArrayOf(6, 8),
            intArrayOf(8, 10),

            intArrayOf(5, 11),
            intArrayOf(6, 12),

            intArrayOf(11, 12),

            intArrayOf(11, 13),
            intArrayOf(13, 15),

            intArrayOf(12, 14),
            intArrayOf(14, 16)
        )

        for (connection in connections) {

            val a = connection[0]
            val b = connection[1]

            if (a >= points.size || b >= points.size) {
                continue
            }

            val p1 = points[a]
            val p2 = points[b]

            if (p1.score <= 0f || p2.score <= 0f) {
                continue
            }

            canvas.drawLine(
                p1.x,
                p1.y,
                p2.x,
                p2.y,
                skeletonPaint
            )
        }

        for (point in points) {

            if (point.score <= 0f) {
                continue
            }

            canvas.drawCircle(
                point.x,
                point.y,
                6f,
                pointPaint
            )
        }
    }
}
