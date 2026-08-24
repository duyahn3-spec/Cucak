package com.example.poseresearch

object PoseDecoder {

    private const val KEYPOINT_COUNT = 18

    fun decode(
        output: FloatArray
    ): PoseResult {

        require(output.size == 32 * 32 * 19) {
            "Unexpected output size: ${output.size}"
        }

        val points = ArrayList<PoseKeypoint>(
            KEYPOINT_COUNT
        )

        for (keypoint in 0 until KEYPOINT_COUNT) {

            var bestScore = Float.NEGATIVE_INFINITY
            var bestX = 0
            var bestY = 0

            for (y in 0 until 32) {

                for (x in 0 until 32) {

                    val index =
                        (y * 32 + x) * 19 + keypoint

                    val score = output[index]

                    if (score > bestScore) {
                        bestScore = score
                        bestX = x
                        bestY = y
                    }
                }
            }

            points.add(
                PoseKeypoint(
                    x = bestX / 31f,
                    y = bestY / 31f,
                    score = bestScore
                )
            )
        }

        return PoseResult(points)
    }
}
