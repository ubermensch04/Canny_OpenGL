package com.example.myapplication

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.opengl.GLSurfaceView
import android.view.View

class MainActivity : ComponentActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var renderer: MyGLRenderer
    external fun preprocessFrame(data: ByteArray, width: Int, height: Int): ByteArray

    private fun setupGLSurfaceView() {
        // Set OpenGL ES version
        viewBinding.glSurfaceView.setEGLContextClientVersion(2)

        // Create and set the renderer
        renderer = MyGLRenderer()
        renderer.setSurfaceReadyCallback(object : MyGLRenderer.SurfaceReadyCallback {
            override fun onSurfaceReady() {
                // Start camera only after GL surface is ready
                runOnUiThread {
                    if (allPermissionsGranted()) {
                        startCamera()
                    } else {
                        requestPermissions()
                    }
                }
            }
        })
        viewBinding.glSurfaceView.setRenderer(renderer)

        // Only render when there's a change
        viewBinding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Setup GLSurfaceView
        setupGLSurfaceView()

        // Don't start camera yet - wait for surface to be ready
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set up the preview use case
            val preview = Preview.Builder().build()

            // Set up the image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        // Process the image with your native code
                        val planes = image.planes
                        val yBuffer = planes[0].buffer
                        val uBuffer = planes[1].buffer
                        val vBuffer = planes[2].buffer

                        val ySize = yBuffer.remaining()
                        val uSize = uBuffer.remaining()
                        val vSize = vBuffer.remaining()

                        val nv21 = ByteArray(ySize + uSize + vSize)

                        yBuffer.get(nv21, 0, ySize)
                        vBuffer.get(nv21, ySize, vSize)
                        uBuffer.get(nv21, ySize + vSize, uSize)

                        // Process the frame
                        val result = preprocessFrame(nv21, image.width, image.height)

                        // Update the GL renderer with the edge detection result
                        renderer.updateFrame(result, image.width, image.height)

                        image.close()
                    }
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any bound use cases
                cameraProvider.unbindAll()

                // IMPORTANT: Set view visibility BEFORE binding camera
                viewBinding.viewFinder.visibility = View.GONE
                viewBinding.glSurfaceView.visibility = View.VISIBLE

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalyzer)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                // Handle permission denial
            } else {
                startCamera()
            }
        }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).toTypedArray()

        init {
            System.loadLibrary("myapplication")
        }

    }

}
