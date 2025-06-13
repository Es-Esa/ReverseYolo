// In: /app/src/main/java/com/ultralytics/yoloapp/yolo/YoloView.kt
package com.ultralytics.yoloapp.yolo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class YoloView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), DefaultLifecycleObserver {

    private val previewView: PreviewView
    private val overlayView: OverlayView
    private var lifecycleOwner: LifecycleOwner? = null
    private var predictor: Predictor? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var inferenceResult: YOLOResult? = null

    init {
        previewView = PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        overlayView = OverlayView(context)
        addView(previewView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(overlayView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    fun setLifecycleOwner(owner: LifecycleOwner) {
        this.lifecycleOwner = owner
        owner.lifecycle.addObserver(this)
    }

    fun setModel(modelPath: String) {
        Executors.newSingleThreadExecutor().execute {
            try {
                // For simplicity, we hardcode the DETECT task
                val newPredictor = ObjectDetector(context, modelPath, emptyList(), useGpu = true)
                post {
                    predictor = newPredictor
                    Log.d(TAG, "Model loaded successfully: $modelPath")
                    startCamera()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model: $modelPath", e)
            }
        }
    }

    private fun onFrame(imageProxy: ImageProxy) {
        if (predictor == null) {
            imageProxy.close()
            return
        }

        val bitmap = ImageUtils.toBitmap(imageProxy)
        if (bitmap == null) {
            Log.e(TAG, "Failed to convert ImageProxy to Bitmap")
            imageProxy.close()
            return
        }

        try {
            inferenceResult = predictor?.predict(bitmap, imageProxy.height, imageProxy.width, true)
            post { overlayView.invalidate() }
        } catch (e: Exception) {
            Log.e(TAG, "Error during prediction", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun startCamera() {
        val owner = lifecycleOwner ?: run {
            Log.e(TAG, "Cannot start camera without a LifecycleOwner.")
            return
        }
        if (!allPermissionsGranted()) {
            Log.e(TAG, "Camera permission not granted.")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { ip -> onFrame(ip) }
                    }
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(owner, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private inner class OverlayView(context: Context) : View(context) {
        private val paint = Paint()
        private val textPaint = Paint()

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8.0f
            textPaint.color = Color.WHITE
            textPaint.textSize = 40.0f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val result = inferenceResult ?: return
            
            val scaleFactorX = width.toFloat() / result.origImageSize.height
            val scaleFactorY = height.toFloat() / result.origImageSize.width

            result.boxes.forEach { box ->
                paint.color = ultralyticsColors[box.index % ultralyticsColors.size]
                
                val rect = RectF(
                    box.xywh.left * scaleFactorX,
                    box.xywh.top * scaleFactorY,
                    box.xywh.right * scaleFactorX,
                    box.xywh.bottom * scaleFactorY
                )
                
                canvas.drawRect(rect, paint)
                
                val text = "${box.cls} ${(box.conf * 100).toInt()}%"
                val textBounds = Rect()
                textPaint.getTextBounds(text, 0, text.length, textBounds)
                
                canvas.drawRect(rect.left, rect.top - textBounds.height() - 8, rect.left + textBounds.width() + 8, rect.top, paint)
                canvas.drawText(text, rect.left + 4, rect.top - 4, textPaint)
            }
        }
    }

    companion object {
        private const val TAG = "YoloView"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val ultralyticsColors = arrayOf(
            Color.rgb(0, 70, 255), Color.rgb(255, 116, 69) // Add more as needed
        )
    }
}