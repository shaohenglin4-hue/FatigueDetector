package com.example.fatiguedetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

class FaceDetectorHelper(context: Context) {

    private var faceLandmarker: FaceLandmarker? = null
    // 引入上一节成员 A 写的核心算法
    private val fatigueAnalyzer = FatigueAnalyzer(earThreshold = 0.22, maxClosedFrames = 15)

    init {
        // 初始化 MediaPipe 的 FaceLandmarker
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task") // 读取刚才放入 assets 的模型
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumFaces(1) // 驾驶场景只检测一张脸，提升速度
            .setRunningMode(RunningMode.IMAGE) // 使用单帧图像模式，最稳定
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    // 处理每一帧图像，返回当前的状态信息和 EAR 值
    fun processFrame(imageProxy: ImageProxy): FaceAnalysisResult {
        // 1. 将 CameraX 的 ImageProxy 转换为 MediaPipe 需要的格式
        val bitmap = imageProxy.toBitmap()

        // 旋转图像以匹配屏幕方向 (前置摄像头通常需要旋转和镜像)
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        matrix.postScale(-1f, 1f) // 前置摄像头需要水平翻转防镜像
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        // 2. 喂给模型进行推理
        val result = faceLandmarker?.detect(mpImage)

        // 3. 解析结果并交给 FatigueAnalyzer 计算
        if (result != null && result.faceLandmarks().isNotEmpty()) {
            val landmarks = result.faceLandmarks()[0] // 获取第一张脸的数据

            // 将 MediaPipe 的 0~1 的归一化坐标转化为实际像素坐标 (和 Python 逻辑完全一致)
            val width = rotatedBitmap.width.toFloat()
            val height = rotatedBitmap.height.toFloat()

            val point2Ds = landmarks.map {
                Point2D(it.x() * width, it.y() * height)
            }

            // 4. 调用成员 A 的核心算法
            val ear = fatigueAnalyzer.analyze(point2Ds)

            return FaceAnalysisResult(
                isFaceDetected = true,
                ear = ear,
                isAlarmOn = fatigueAnalyzer.isAlarmOn
            )
        }

        // 如果没检测到脸
        return FaceAnalysisResult(isFaceDetected = false, ear = 0.0, isAlarmOn = false)
    }

    // 定义一个数据类用来打包返回结果
    data class FaceAnalysisResult(
        val isFaceDetected: Boolean,
        val ear: Double,
        val isAlarmOn: Boolean
    )
}