// In: /app/src/main/java/com/ultralytics/yoloapp/yolo/ObjectDetector.kt
package com.ultralytics.yoloapp.yolo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

class ObjectDetector(
    context: Context,
    modelPath: String,
    override var labels: List<String>,
    private val useGpu: Boolean = true
) : BasePredictor() {

    private var numItemsThreshold: Int = 300
    private lateinit var imageProcessorCamera: ImageProcessor
    private lateinit var imageProcessorSingleImage: ImageProcessor
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var rawOutput: Array<Array<FloatArray>>
    private var out1: Int = 0
    private var out2: Int = 0

    init {
        val interpreterOptions = Interpreter.Options().apply {
            if (useGpu) {
                try { addDelegate(GpuDelegate()) } catch (e: Exception) { Log.e(TAG, "GPU delegate error: ${e.message}") }
            }
            numThreads = Runtime.getRuntime().availableProcessors()
        }

        val modelBuffer = YoloUtils.loadModelFile(context, modelPath)
        
        // Simplified label loading for this example
        if (this.labels.isEmpty()) {
            try {
                this.labels = FileUtil.loadLabels(context, "labels.txt")
            } catch (e: IOException) {
                Log.e(TAG, "Could not load labels.txt from assets.", e)
            }
        }
        
        interpreter = Interpreter(modelBuffer, interpreterOptions)

        val inputShape = interpreter.getInputTensor(0).shape()
        val modelHeight = inputShape[1]
        val modelWidth = inputShape[2]
        inputSize = Size(modelWidth, modelHeight)

        val outputShape = interpreter.getOutputTensor(0).shape()
        out1 = outputShape[1]
        out2 = outputShape[2]

        // Pre-allocate buffers
        rawOutput = Array(1) { Array(out1) { FloatArray(out2) } }
        inputBuffer = ByteBuffer.allocateDirect(1 * modelWidth * modelHeight * 3 * 4).order(ByteOrder.nativeOrder())
        
        setupImageProcessors()
        Log.d(TAG, "ObjectDetector initialized.")
    }

    override fun predict(bitmap: Bitmap, origWidth: Int, origHeight: Int, rotateForCamera: Boolean): YOLOResult {
        t0 = System.nanoTime()

        val imageProcessor = if (rotateForCamera) imageProcessorCamera else imageProcessorSingleImage
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
        inputBuffer.rewind()
        inputBuffer.put(tensorImage.buffer)

        interpreter.run(inputBuffer, rawOutput)

        // Delegate to native post-processing
        val finalPredictions = postprocess(
            rawOutput[0],
            origWidth,
            origHeight,
            CONFIDENCE_THRESHOLD,
            IOU_THRESHOLD,
            labels.size,
            numItemsThreshold
        )

        val boxes = finalPredictions.mapNotNull {
            if (it.size < 6) return@mapNotNull null
            Box(
                index = it[5].toInt(),
                cls = labels.getOrElse(it[5].toInt()) { "Unknown" },
                conf = it[4],
                xywh = RectF(it[0], it[1], it[2], it[3]),
                xywhn = RectF(it[0] / origWidth, it[1] / origHeight, it[2] / origWidth, it[3] / origHeight)
            )
        }

        updateTiming()
        return YOLOResult(origImageSize = Size(origWidth, origHeight), boxes = boxes, inferenceTime = t2)
    }

    override fun setNumItemsThreshold(n: Int) {
        this.numItemsThreshold = n
    }

    private fun setupImageProcessors() {
        val normalizeOp = NormalizeOp(0f, 255f)
        val castOp = CastOp(DataType.FLOAT32)
        val resizeOp = ResizeOp(inputSize.height, inputSize.width, ResizeOp.ResizeMethod.BILINEAR)

        imageProcessorCamera = ImageProcessor.Builder()
            .add(Rot90Op(-1))
            .add(resizeOp)
            .add(normalizeOp)
            .add(castOp)
            .build()

        imageProcessorSingleImage = ImageProcessor.Builder()
            .add(resizeOp)
            .add(normalizeOp)
            .add(castOp)
            .build()
    }

    private external fun postprocess(
        predictions: Array<FloatArray>,
        w: Int,
        h: Int,
        confidenceThreshold: Float,
        iouThreshold: Float,
        numClasses: Int,
        numItemsThreshold: Int
    ): Array<FloatArray>

    companion object {
        private const val TAG = "ObjectDetector"
        init {
            System.loadLibrary("ultralytics")
        }
    }
}