// In: /app/src/main/java/com/ultralytics/yoloapp/yolo/YoloData.kt
package com.ultralytics.yoloapp.yolo

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

data class Box(var index: Int, var cls: String, var conf: Float, val xywh: RectF, val xywhn: RectF)
data class Keypoints(val xyn: List<Pair<Float, Float>>, val xy: List<Pair<Float, Float>>, val conf: List<Float>)
data class Masks(val masks: List<List<List<Float>>>, val combinedMask: Bitmap?)
data class OBB(val cx: Float, val cy: Float, val w: Float, val h: Float, val angle: Float) {
    val area: Float get() = w * h
    fun toPolygon(): List<PointF> {
        val halfW = w / 2; val halfH = h / 2
        val corners = listOf(PointF(-halfW, -halfH), PointF(halfW, -halfH), PointF(halfW, halfH), PointF(-halfW, halfH))
        val cosAngle = cos(angle.toDouble()).toFloat(); val sinAngle = sin(angle.toDouble()).toFloat()
        return corners.map { PointF(it.x * cosAngle - it.y * sinAngle + cx, it.x * sinAngle + it.y * cosAngle + cy) }
    }
}
data class OBBResult(val box: OBB, val confidence: Float, val cls: String, val index: Int)
data class Probs(val top1: String, val top5: List<String>, val top1Conf: Float, val top5Confs: List<Float>, val top1Index: Int)
data class Size(val width: Int, val height: Int)
data class YOLOResult(
    val origImageSize: Size,
    var boxes: List<Box> = emptyList(),
    var masks: Masks? = null,
    var probs: Probs? = null,
    var keypointsList: List<Keypoints> = emptyList(),
    var obb: List<OBBResult> = emptyList(),
    var annotatedImage: Bitmap? = null,
    val inferenceTime: Double = 0.0,
    val fps: Double? = null,
    var originalImage: Bitmap? = null,
    var labels: List<String> = emptyList()
)
enum class YOLOTask { DETECT, SEGMENT, CLASSIFY, POSE, OBB }