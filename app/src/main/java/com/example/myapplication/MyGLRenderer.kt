package com.example.myapplication

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {
    private var textureId = 0
    private var programHandle = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureUniformHandle = 0

    // Frame data
    @Volatile
    var frameData: ByteArray? = null
    var frameWidth = 0
    var frameHeight = 0

    // Vertex and texture coordinates
    private val vertexData = floatArrayOf(
        // X, Y, Z
        -1.0f, -1.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f,
        1.0f, 1.0f, 0.0f
    )

    private val texCoordData = floatArrayOf(
        // U, V
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    // Shader code
    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec2 vTexCoordinate;
        varying vec2 texCoord;
        void main() {
            gl_Position = vPosition;
            texCoord = vTexCoordinate;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D texSampler;
        varying vec2 texCoord;
        void main() {
            gl_FragColor = texture2D(texSampler, texCoord);
        }
    """

    // Callback interface for surface creation
    interface SurfaceReadyCallback {
        fun onSurfaceReady()
    }

    private var surfaceReadyCallback: SurfaceReadyCallback? = null
    private var glSurfaceView: GLSurfaceView? = null

    fun setSurfaceReadyCallback(callback: SurfaceReadyCallback) {
        this.surfaceReadyCallback = callback
    }
    fun setGLSurfaceView(view: GLSurfaceView) {
        glSurfaceView = view
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize buffers
        initBuffers()

        // Create texture
        textureId = createTexture()

        // Create shader program
        programHandle = createProgram()

        // Get handle to shader attributes/uniforms
        positionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(programHandle, "vTexCoordinate")
        textureUniformHandle = GLES20.glGetUniformLocation(programHandle, "texSampler")

        // Notify that surface is ready
        surfaceReadyCallback?.onSurfaceReady()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        synchronized(this) {
            frameData?.let {
                try {
                    Log.d("GLRenderer", "Drawing frame with dimensions: $frameWidth x $frameHeight")

                    // Create bitmap from edge detection data
                    val bitmap = createBitmap(it, frameWidth, frameHeight)

                    // Bind and update texture
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

                    // Draw the texture
                    drawTexture()

                    // Clean up
                    bitmap.recycle()

                } catch (e: Exception) {
                    Log.e("GLRenderer", "Error drawing frame", e)
                }
            }
        }
    }

    private fun initBuffers() {
        // Initialize vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
                position(0)
            }

        // Initialize texture coordinate buffer
        texCoordBuffer = ByteBuffer.allocateDirect(texCoordData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(texCoordData)
                position(0)
            }
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Set texture filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // Set texture wrapping
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        return textureId
    }

    private fun createShader(type: Int, shaderCode: String): Int {
        val shaderId = GLES20.glCreateShader(type)

        if (shaderId == 0) {
            Log.e("GLRenderer", "Failed to create shader")
            return 0
        }

        GLES20.glShaderSource(shaderId, shaderCode)
        GLES20.glCompileShader(shaderId)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == 0) {
            Log.e("GLRenderer", "Could not compile shader: ${GLES20.glGetShaderInfoLog(shaderId)}")
            GLES20.glDeleteShader(shaderId)
            return 0
        }

        return shaderId
    }

    private fun createProgram(): Int {
        val vertexShader = createShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = createShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        if (fragmentShader == 0) {
            return 0
        }

        val programHandle = GLES20.glCreateProgram()

        if (programHandle == 0) {
            Log.e("GLRenderer", "Failed to create program")
            return 0
        }

        GLES20.glAttachShader(programHandle, vertexShader)
        GLES20.glAttachShader(programHandle, fragmentShader)
        GLES20.glLinkProgram(programHandle)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linked, 0)

        if (linked[0] == 0) {
            Log.e("GLRenderer", "Could not link program: ${GLES20.glGetProgramInfoLog(programHandle)}")
            GLES20.glDeleteProgram(programHandle)
            return 0
        }

        return programHandle
    }

    private fun drawTexture() {
        // Use the shader program
        GLES20.glUseProgram(programHandle)

        // Set the vertex position data
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Set the texture coordinate data
        GLES20.glVertexAttribPointer(
            texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // Set the active texture unit to texture unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        // Bind the texture to this unit
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Tell the texture uniform sampler to use this texture
        GLES20.glUniform1i(textureUniformHandle, 0)

        // Draw the quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun createBitmap(data: ByteArray, width: Int, height: Int): Bitmap {

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Convert binary edge detection results to ARGB
        val buffer = ByteBuffer.allocate(width * height * 4)
        for (i in data.indices) {
            val value = data[i].toInt() and 0xFF
            buffer.put(value.toByte())      // R
            buffer.put(value.toByte())      // G
            buffer.put(value.toByte())      // B
            buffer.put(0xFF.toByte())       // A (fully opaque)
        }

        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)

        // Create a rotated bitmap
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f)
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Clean up the original bitmap
        bitmap.recycle()

        return rotatedBitmap
    }

    fun updateFrame(data: ByteArray, width: Int, height: Int) {
        synchronized(this) {
            this.frameData = data.copyOf()
            this.frameWidth = width
            this.frameHeight = height
            // Request render on the UI thread
            glSurfaceView?.queueEvent {
                glSurfaceView?.requestRender()
            }
        }
    }

}