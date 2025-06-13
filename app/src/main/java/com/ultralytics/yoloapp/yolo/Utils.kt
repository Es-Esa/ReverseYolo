// In: /app/src/main/java/com/ultralytics/yoloapp/yolo/Utils.kt
package com.ultralytics.yoloapp.yolo

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.common.FileUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.math.abs

/**
 * Singleton object for generic file and model loading utilities.
 */
object YoloUtils {
    private const val TAG = "YoloUtils"

    fun isAbsolutePath(path: String): Boolean = path.startsWith("/")
    fun fileExistsAtPath(path: String): Boolean = File(path).exists() && File(path).isFile
    fun ensureTFLiteExtension(modelPath: String): String = if (!modelPath.lowercase(Locale.ROOT).endsWith(".tflite")) "$modelPath.tflite" else modelPath

    fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val finalModelPath = ensureTFLiteExtension(modelPath)
        try {
            return if (isAbsolutePath(finalModelPath) && fileExistsAtPath(finalModelPath)) {
                loadModelFromFilesystem(finalModelPath)
            } else {
                FileUtil.loadMappedFile(context, finalModelPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: $finalModelPath", e)
            throw IOException("Failed to load model file.", e)
        }
    }

    private fun loadModelFromFilesystem(filePath: String): MappedByteBuffer {
        val file = File(filePath)
        return RandomAccessFile(file, "r").channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    }
}

/**
 * Singleton object for low-level image format and matrix transformations.
 */
object ImageUtils {
    @JvmStatic
    fun toBitmap(imageProxy: ImageProxy): Bitmap? {
        val nv21Bytes = yuv420888ToNv21(imageProxy)
        val yuvImage = YuvImage(nv21Bytes, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        if (!yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)) {
            return null
        }
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420888ToNv21(imageProxy: ImageProxy): ByteArray {
        val pixelCount = imageProxy.cropRect.width() * imageProxy.cropRect.height()
        val bufferSize = pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
        val nv21Buffer = ByteArray(bufferSize)
        val planes = imageProxy.planes
        val yPlane = planes[0].buffer
        val uPlane = planes[1].buffer
        val vPlane = planes[2].buffer
        yPlane.get(nv21Buffer, 0, pixelCount)
        vPlane.get(nv21Buffer, pixelCount, vPlane.remaining())
        return nv21Buffer
    }
}

/**
 * Singleton object for geometric calculations like IoU and NMS.
 */
object GeometryUtils {
    fun nonMaxSuppression(boxes: List<RectF>, scores: List<Float>, iouThreshold: Float): List<Int> {
        val sortedIndices = scores.indices.sortedByDescending { scores[it] }
        val selectedIndices = mutableListOf<Int>()
        val suppressed = BooleanArray(boxes.size) { false }

        for (i in sortedIndices) {
            if (!suppressed[i]) {
                selectedIndices.add(i)
                for (j in sortedIndices) {
                    if (!suppressed[j] && i != j) {
                        if (computeIoU(boxes[i], boxes[j]) > iouThreshold) {
                            suppressed[j] = true
                        }
                    }
                }
            }
        }
        return selectedIndices
    }

    fun computeIoU(a: RectF, b: RectF): Float {
        val intersectionLeft = maxOf(a.left, b.left)
        val intersectionTop = maxOf(a.top, b.top)
        val intersectionRight = minOf(a.right, b.right)
        val intersectionBottom = minOf(a.bottom, b.bottom)
        val intersectionWidth = maxOf(0f, intersectionRight - intersectionLeft)
        val intersectionHeight = maxOf(0f, intersectionBottom - intersectionTop)
        val intersectionArea = intersectionWidth * intersectionHeight
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = areaA + areaB - intersectionArea
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }
}