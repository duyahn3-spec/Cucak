package com.aimbuddy

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel

class PoseDetector(context: Context) {

    companion object {
        private const val INPUT_SIZE = 256
    }

    private val model: CompiledModel = CompiledModel.create(
        context.assets,
        "models/pose_256_fp16.tflite",
        CompiledModel.Options(Accelerator.GPU),
        null
    )

    private val inputs = model.createInputBuffers()
    private val outputs = model.createOutputBuffers()

    fun detect(source: Bitmap): PoseResult {
        val bitmap = Bitmap.createScaledBitmap(source, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val input = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)
        var index = 0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            input[index++] = (r - 128f) / 256f
            input[index++] = (g - 128f) / 256f
            input[index++] = (b - 128f) / 256f
        }
        inputs[0].writeFloat(input)
        model.run(inputs, outputs)
        val output = outputs[0].readFloat()
        bitmap.recycle()
        return PoseDecoder.decode(output)
    }

    fun close() {
        model.destroy()
    }
}
