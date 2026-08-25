package com.aimbuddy

object PoseDecoder {

    private val KEYPOINT_NAMES = arrayOf(
        "Nose", "Neck", "RShoulder", "RElbow", "RWrist",
        "LShoulder", "LElbow", "LWrist", "RHip", "RKnee",
        "RAnkle", "LHip", "LKnee", "LAnkle", "REye",
        "LEye", "REar", "LEar"
    )

    fun decode(output: FloatArray): PoseResult {
        val points = mutableListOf<PoseKeypoint>()
        for (k in 0 until 18) {
            var bestScore = Float.NEGATIVE_INFINITY
            var bestX = 0
            var bestY = 0
            for (y in 0 until 32) {
                for (x in 0 until 32) {
                    val score = output[(y * 32 + x) * 19 + k]
                    if (score > bestScore) {
                        bestScore = score
                        bestX = x
                        bestY = y
                    }
                }
            }
            points.add(PoseKeypoint(
                name = KEYPOINT_NAMES[k],
                x = bestX / 31f,
                y = bestY / 31f,
                confidence = bestScore
            ))
        }
        return PoseResult(points)
    }
}
