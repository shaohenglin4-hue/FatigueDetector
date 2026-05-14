package com.example.fatiguedetector

import kotlin.math.pow
import kotlin.math.sqrt

// 定义一个简单的数据类，用来存放特征点的 X 和 Y 坐标 (就是报错里提示找不到的那个 Point2D)
data class Point2D(val x: Float, val y: Float)

class FatigueAnalyzer(
    private val earThreshold: Double = 0.22,
    private val maxClosedFrames: Int = 15
) {
    var closedFramesCounter = 0
        private set
    var isAlarmOn = false
        private set

    // 左眼和右眼的特征点索引
    private val leftEyeIndices = intArrayOf(362, 385, 387, 263, 373, 380)
    private val rightEyeIndices = intArrayOf(33, 160, 158, 133, 153, 144)

    // 欧氏距离计算
    private fun euclidean(p1: Point2D, p2: Point2D): Double {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2)).toDouble()
    }

    // 计算 EAR
    private fun calculateEar(eyePoints: List<Point2D>): Double {
        val a = euclidean(eyePoints[1], eyePoints[5])
        val b = euclidean(eyePoints[2], eyePoints[4])
        val c = euclidean(eyePoints[0], eyePoints[3])
        return (a + b) / (2.0 * c)
    }

    // 核心判定方法
    fun analyze(landmarks: List<Point2D>): Double {
        val leftEyePoints = leftEyeIndices.map { landmarks[it] }
        val rightEyePoints = rightEyeIndices.map { landmarks[it] }

        val leftEar = calculateEar(leftEyePoints)
        val rightEar = calculateEar(rightEyePoints)
        val avgEar = (leftEar + rightEar) / 2.0

        if (avgEar < earThreshold) {
            closedFramesCounter++
            if (closedFramesCounter >= maxClosedFrames) {
                isAlarmOn = true
            }
        } else {
            closedFramesCounter = 0
            isAlarmOn = false
        }

        return avgEar
    }
}