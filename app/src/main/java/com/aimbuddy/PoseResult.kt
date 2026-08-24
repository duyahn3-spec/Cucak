package com.example.poseresearch

data class PoseResult(
    val timestampMs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
    val centerX: Float,
    val centerY: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val keypoints: List<PoseKeypoint>,
    val latencyMs: Long
)
