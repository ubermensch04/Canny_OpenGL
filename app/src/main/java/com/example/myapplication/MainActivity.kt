package com.example.myapplication

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
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
import gl.MyGLRenderer

class MainActivity : ComponentActivity() {

    // Flag to toggle between raw feed and edge detection view
    private var showingRawFeed = false

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private val fpsUpdateInterval = 1000
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var renderer: MyGLRenderer
    external fun preprocessFrame(data: ByteArray, width: Int, height: Int): ByteArray

    private var currentEffect = MyGLRenderer.ShaderEffect.NORMAL

    private fun setupGLSurfaceView() {
        // Set OpenGL ES version
        viewBinding.glSurfaceView.setEGLContextClientVersion(2)

        // Create and set the renderer
        renderer = MyGLRenderer()
        renderer.setGLSurfaceView(viewBinding.glSurfaceView)
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

        viewBinding.toggleButton.setOnClickListener {
            toggleView()
        }

        // Effect button to cycle through shader effects
        viewBinding.effectButton.setOnClickListener {
            cycleEffect()
        }

        // Don't start camera yet - wait for surface to be ready
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun cycleEffect() {
        val oldEffect = currentEffect
        currentEffect = when (currentEffect) {
            MyGLRenderer.ShaderEffect.NORMAL -> MyGLRenderer.ShaderEffect.INVERT
            MyGLRenderer.ShaderEffect.INVERT -> MyGLRenderer.ShaderEffect.NORMAL
        }

        Log.d("MainActivity", "Switching effect from $oldEffect to $currentEffect")

        // Update button text to show current effect
        viewBinding.effectButton.text = "Effect: ${currentEffect.name}"

        // Apply the effect
        renderer.setEffect(currentEffect)
    }

    private fun rebindCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        }
    }
    private fun toggleView() {
        showingRawFeed = !showingRawFeed

        if (showingRawFeed) {
            // Show raw camera feed
            viewBinding.viewFinder.visibility = View.VISIBLE
            viewBinding.glSurfaceView.visibility = View.GONE
        } else {
            // Show edge detection view
            viewBinding.viewFinder.visibility = View.GONE
            viewBinding.glSurfaceView.visibility = View.VISIBLE
        }

        // Reset FPS counter when switching views
        frameCount = 0
        lastFpsTimestamp = System.currentTimeMillis()
        viewBinding.fpsText.text = "FPS: 0"
        rebindCamera()
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)

            // Set up the image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)

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

                        // Update the FPS counter
                        updateFps()

                        image.close()
                    }
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any bound use cases
                cameraProvider.unbindAll()

                // Set initial visibility - modified to use the current toggle state
                viewBinding.viewFinder.visibility = if (showingRawFeed) View.VISIBLE else View.GONE
                viewBinding.glSurfaceView.visibility = if (showingRawFeed) View.GONE else View.VISIBLE

                // Bind use cases to camera
                if (showingRawFeed) {
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer)
                } else {
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, imageAnalyzer)
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
//            val camera = cameraProvider.bindToLifecycle(
//                this, cameraSelector, imageAnalyzer)
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
    private fun updateFps() {
        frameCount++
        val now = System.currentTimeMillis()
        val elapsedMs = now - lastFpsTimestamp

        if (elapsedMs >= fpsUpdateInterval) {
            val fps = frameCount * 1000 / elapsedMs
            runOnUiThread {
                viewBinding.fpsText.text = "FPS: $fps"
            }
            frameCount = 0
            lastFpsTimestamp = now
        }
    }

}
