package com.example.fatiguedetector

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class FaceOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 半透明的黑色遮罩背景
        color = Color.parseColor("#99000000") // 60% 不透明度
        style = Paint.Style.FILL
    }

    private val transparentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 这支笔是用来“挖洞”的
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 这支笔用来画绿色的四个角
        color = Color.parseColor("#00FF00") // 科技绿
        style = Paint.Style.STROKE
        strokeWidth = 10f // 线条粗细
        strokeCap = Paint.Cap.ROUND
    }

    // 设置硬件加速支持透明度抠图
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. 画全屏的黑色半透明遮罩
        canvas.drawRect(0f, 0f, w, h, maskPaint)

        // 2. 决定框的大小和位置（居中，占据屏幕宽度的一部分）
        val boxWidth = w * 0.65f
        // 面部框通常是一个稍长的矩形
        val boxHeight = boxWidth * 1.2f

        val left = (w - boxWidth) / 2f
        val top = (h - boxHeight) / 2f // 稍微偏上一点，因为仪表盘在下面
        val right = left + boxWidth
        val bottom = top + boxHeight

        val rect = RectF(left, top, right, bottom)
        val cornerRadius = 40f // 框的圆角

        // 3. 在中间“挖”出一个透明的圆角矩形
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, transparentPaint)

        // 4. 画四个科技感边角
        val lineLength = 60f // 边角的长度

        // 左上角
        canvas.drawLine(left, top + cornerRadius, left, top + lineLength, borderPaint)
        canvas.drawLine(left + cornerRadius, top, left + lineLength, top, borderPaint)

        // 右上角
        canvas.drawLine(right, top + cornerRadius, right, top + lineLength, borderPaint)
        canvas.drawLine(right - cornerRadius, top, right - lineLength, top, borderPaint)

        // 左下角
        canvas.drawLine(left, bottom - cornerRadius, left, bottom - lineLength, borderPaint)
        canvas.drawLine(left + cornerRadius, bottom, left + lineLength, bottom, borderPaint)

        // 右下角
        canvas.drawLine(right, bottom - cornerRadius, right, bottom - lineLength, borderPaint)
        canvas.drawLine(right - cornerRadius, bottom, right - lineLength, bottom, borderPaint)
    }
}