package com.aimbuddy

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager:
        MediaProjectionManager

    private val projectionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode != RESULT_OK ||
                result.data == null
            ) {
                return@registerForActivityResult
            }

            val serviceIntent =
                Intent(
                    this,
                    ScreenCaptureService::class.java
                ).apply {

                    action =
                        ScreenCaptureService.ACTION_START

                    putExtra(
                        ScreenCaptureService.EXTRA_RESULT_CODE,
                        result.resultCode
                    )

                    putExtra(
                        ScreenCaptureService.EXTRA_RESULT_DATA,
                        result.data
                    )
                }

            startForegroundService(
                serviceIntent
            )
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

        projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        /*
         * Nếu layout hiện tại của bạn có Button
         * id = selectButton thì có thể dùng nó
         * để gọi requestCapture().
         */

        findViewById<android.view.View>(
            R.id.selectButton
        ).setOnClickListener {

            requestCapture()
        }
    }

    private fun requestCapture() {

        val intent =
            projectionManager.createScreenCaptureIntent()

        projectionLauncher.launch(
            intent
        )
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}
