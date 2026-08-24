package com.example.poseresearch

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PoseDetector(
    context: Context
) {

    private val detector =
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(
                    PoseDetectorOptions.STREAM_MODE
                )
                .build()
        )

    fun detect(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): PoseResult? {

        val start =
            System.currentTimeMillis()

        val input =
            InputImage.fromBitmap(
                bitmap,
                0
            )

        var resultPose: Pose? = null

        val latch =
            CountDownLatch(1)

        detector.process(input)
            .addOnSuccessListener {
                resultPose = it
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        latch.await(
            150L,
            TimeUnit.MILLISECONDS
        )

        val pose =
            resultPose
                ?: return null

        val landmarks =
            pose.allPoseLandmarks

        if (landmarks.isEmpty()) {
            return null
        }

        var minX =
            Float.MAX_VALUE

        var minY =
            Float.MAX_VALUE

        var maxX =
            Float.MIN_VALUE

        var maxY =
            Float.MIN_VALUE

        val keypoints =
            ArrayList<PoseKeypoint>()

        for (landmark in landmarks) {

            val x =
                landmark.position.x

            val y =
                landmark.position.y

            minX =
                minOf(
                    minX,
                    x
                )

            minY =
                minOf(
                    minY,
                    y
                )

            maxX =
                maxOf(
                    maxX,
                    x
                )

            maxY =
                maxOf(
                    maxY,
                    y
                )

            keypoints.add(
                PoseKeypoint(
                    name =
                        landmarkName(
                            landmark.landmarkType
                        ),
                    x = x,
                    y = y,
                    confidence =
                        landmark.inFrameLikelihood
                )
            )
        }

        val centerX =
            (minX + maxX) / 2f

        val centerY =
            (minY + maxY) / 2f

        return PoseResult(
            timestampMs =
                System.currentTimeMillis(),
            frameWidth =
                width,
            frameHeight =
                height,
            centerX =
                centerX,
            centerY =
                centerY,
            left =
                minX,
            top =
                minY,
            right =
                maxX,
            bottom =
                maxY,
            keypoints =
                keypoints,
            latencyMs =
                System.currentTimeMillis() -
                    start
        )
    }

    private fun landmarkName(
        type: Int
    ): String {

        return when (type) {

            PoseLandmark.NOSE ->
                "nose"

            PoseLandmark.LEFT_EYE_INNER ->
                "left_eye_inner"

            PoseLandmark.LEFT_EYE ->
                "left_eye"

            PoseLandmark.LEFT_EYE_OUTER ->
                "left_eye_outer"

            PoseLandmark.RIGHT_EYE_INNER ->
                "right_eye_inner"

            PoseLandmark.RIGHT_EYE ->
                "right_eye"

            PoseLandmark.RIGHT_EYE_OUTER ->
                "right_eye_outer"

            PoseLandmark.LEFT_EAR ->
                "left_ear"

            PoseLandmark.RIGHT_EAR ->
                "right_ear"

            PoseLandmark.LEFT_SHOULDER ->
                "left_shoulder"

            PoseLandmark.RIGHT_SHOULDER ->
                "right_shoulder"

            PoseLandmark.LEFT_ELBOW ->
                "left_elbow"

            PoseLandmark.RIGHT_ELBOW ->
                "right_elbow"

            PoseLandmark.LEFT_WRIST ->
                "left_wrist"

            PoseLandmark.RIGHT_WRIST ->
                "right_wrist"

            PoseLandmark.LEFT_HIP ->
                "left_hip"

            PoseLandmark.RIGHT_HIP ->
                "right_hip"

            PoseLandmark.LEFT_KNEE ->
                "left_knee"

            PoseLandmark.RIGHT_KNEE ->
                "right_knee"

            PoseLandmark.LEFT_ANKLE ->
                "left_ankle"

            PoseLandmark.RIGHT_ANKLE ->
                "right_ankle"

            PoseLandmark.LEFT_HEEL ->
                "left_heel"

            PoseLandmark.RIGHT_HEEL ->
                "right_heel"

            PoseLandmark.LEFT_FOOT_INDEX ->
                "left_foot"

            PoseLandmark.RIGHT_FOOT_INDEX ->
                "right_foot"

            else ->
                "unknown"
        }
    }

    fun close() {
        detector.close()
    }
}
