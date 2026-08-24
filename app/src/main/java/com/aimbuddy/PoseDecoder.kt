package com.example.poseresearch

object PoseDecoder {

    fun encode(
        result: PoseResult
    ): ByteArray {

        val builder =
            StringBuilder()

        builder.append(
            "POSE,"
        )

        builder.append(
            result.centerX
        )

        builder.append(',')

        builder.append(
            result.centerY
        )

        builder.append(',')

        builder.append(
            result.latencyMs
        )

        builder.append('\n')

        return builder
            .toString()
            .toByteArray(
                Charsets.UTF_8
            )
    }
}
