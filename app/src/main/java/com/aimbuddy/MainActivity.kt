package com.example.poseresearch

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity :
    AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var overlay: PoseOverlayView
    private lateinit var status: TextView
    private lateinit var detector: PoseDetector

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                loadAndRun(uri)
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        imageView =
            findViewById(R.id.imageView)

        overlay =
            findViewById(R.id.poseOverlay)

        status =
            findViewById(R.id.status)

        detector =
            PoseDetector(this)

        findViewById<Button>(
            R.id.selectButton
        ).setOnClickListener {

            imagePicker.launch(
                "image/*"
            )
        }
    }

    private fun loadAndRun(
        uri: Uri
    ) {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                val bitmap =
                    loadBitmap(uri)

                if (bitmap == null) {

                    withContext(
                        Dispatchers.Main
                    ) {
                        status.text =
                            "Cannot load image"
                    }

                    return@launch
                }

                val start =
                    System.nanoTime()

                val result =
                    detector.detect(bitmap)

                val elapsed =
                    (
                        System.nanoTime() -
                            start
                        ) / 1_000_000.0

                withContext(
                    Dispatchers.Main
                ) {

                    imageView.setImageBitmap(
                        bitmap
                    )

                    overlay.setPose(
                        result
                    )

                    status.text =
                        "18 keypoints | " +
                        "%.1f ms".format(
                            elapsed
                        )
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    status.text =
                        "Error: ${e.message}"
                }
            }
        }
    }

    private fun loadBitmap(
        uri: Uri
    ): Bitmap? {

        return try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val source =
                    ImageDecoder.createSource(
                        contentResolver,
                        uri
                    )

                ImageDecoder.decodeBitmap(
                    source
                )

            } else {

                @Suppress("DEPRECATION")

                MediaStore.Images.Media
                    .getBitmap(
                        contentResolver,
                        uri
                    )
            }

        } catch (
            _: Exception
        ) {

            null
        }
    }

    override fun onDestroy() {

        detector.close()

        super.onDestroy()
    }
    }
