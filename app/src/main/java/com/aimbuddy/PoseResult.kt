package com.aimbuddy

data class PosePoint(
    val x: Float,
    val y: Float,
    val score: Float
)

data class PoseResult(
    val centerX: Float,
    val centerY: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val keypoints: List<PosePoint>,
    val inferenceMs: Double
)
