package com.example.lab4

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class SignalPlotView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val sinePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val cosinePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val axisPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val sinePath = Path()
    private val cosinePath = Path()

    private val frequency = 500f // Hz
    private val sampleRate = 50000f // Samples per second to make it smooth
    private val numPoints = 1000

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val midY = height / 2f
        val amplitude = height / 4f
        val widthPerPoint = width.toFloat() / numPoints

        // Draw axes
        canvas.drawLine(0f, midY, width.toFloat(), midY, axisPaint)

        sinePath.reset()
        cosinePath.reset()

        for (i in 0 until numPoints) {
            val t = i / sampleRate
            val x = i * widthPerPoint
            
            // Sine: y = A * sin(2 * PI * f * t)
            val ySine = midY - amplitude * sin(2 * Math.PI * frequency * t).toFloat()
            if (i == 0) sinePath.moveTo(x, ySine) else sinePath.lineTo(x, ySine)

            // Cosine: y = A * cos(2 * PI * f * t)
            val yCosine = midY - amplitude * cos(2 * Math.PI * frequency * t).toFloat()
            if (i == 0) cosinePath.moveTo(x, yCosine) else cosinePath.lineTo(x, yCosine)
        }

        canvas.drawPath(sinePath, sinePaint)
        canvas.drawPath(cosinePath, cosinePaint)
    }
}
