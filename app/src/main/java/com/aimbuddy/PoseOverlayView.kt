package com.example.poseresearch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class PoseOverlayView(
    context: Context
) : View(context) {

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private var result:
        PoseResult? = null

    fun setResult(
        value: PoseResult?
    ) {

        result = value

        invalidate()
    }

    override fun onDraw(
        canvas: Canvas
    ) {

        super.onDraw(canvas)

        val r =
            result
                ?: return

        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            3f

        canvas.drawRect(
            r.left,
            r.top,
            r.right,
            r.bottom,
            paint
        )

        paint.style =
            Paint.Style.FILL

        for (point in r.keypoints) {

            canvas.drawCircle(
                point.x,
                point.y,
                5f,
                paint
            )
        }
    }
}
