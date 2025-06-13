// In: /app/src/main/java/com/ultralytics/yoloapp/yolo/Predictor.kt
package com.ultralytics.yoloapp.yolo

import android.graphics.Bitmap
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter

/**
 * Defines the contract for any YOLO predictor.
 * Specifies the properties and functions that all predictor types must implement.
 */
interface Predictor {
    var inputSize: Size
    var isUpdating: Boolean
    var labels: List<String>
    var transformationMatrix: Matrix?
    var pendingBitmapFrame: Bitmap?

    fun predict(bitmap: Bitmap, origWidth: Int, origHeight: Int, rotateForCamera: Boolean = true): YOLOResult
    fun setConfidenceThreshold(conf: Double)
    fun setIouThreshold(iou: Double)
    fun setNumItemsThreshold(progress: Int)
}

/**
 * An abstract base class for YOLO predictors.
 * It handles common functionalities like managing the TFLite interpreter,
 * timing, and threshold parameters, which are shared across all predictor types.
 */
abstract class BasePredictor : Predictor {
    override var isUpdating: Boolean = false
    override lateinit var labels: List<String>
    lateinit var interpreter: Interpreter // Changed to public for simplicity in direct access from concrete classes
    override lateinit var inputSize: Size
    lateinit var modelInputSize: Pair<Int, Int> // Changed to public
    override var transformationMatrix: Matrix? = null
    override var pendingBitmapFrame: Bitmap? = null

    protected val isInterpreterInitialized: Boolean
        get() = ::interpreter.isInitialized

    // Timing variables for performance monitoring
    var t0: Long = 0L // Changed to public
    var t2: Double = 0.0 // Changed to public
    private var t3: Long = System.nanoTime()
    var t4: Double = 0.0 // Changed to public

    // Thresholds
    var CONFIDENCE_THRESHOLD: Float = 0.45f // Changed to public with a reasonable default
    var IOU_THRESHOLD: Float = 0.65f // Changed to public with a reasonable default

    override fun setConfidenceThreshold(conf: Double) { this.CONFIDENCE_THRESHOLD = conf.toFloat() }
    override fun setIouThreshold(iou: Double) { this.IOU_THRESHOLD = iou.toFloat() }
    override fun setNumItemsThreshold(progress: Int) { /* Default is no-op */ }

    /** Updates timing metrics using a smoothed moving average. */
    fun updateTiming() {
        val currentTime = System.nanoTime()
        t2 = ((currentTime - t0).toDouble() / 1_000_000_000.0) * 0.05 + t2 * 0.95
        t4 = ((currentTime - t3).toDouble() / 1_000_000_000.0) * 0.05 + t4 * 0.95
        t3 = currentTime
    }
}