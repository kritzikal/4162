package com.example.lab42

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var samples = FloatArray(0)
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
        color = Color.rgb(103, 80, 164) // Material purple
    }
    private val path = Path()

    fun setSamples(newSamples: FloatArray) {
        samples = newSamples.copyOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f

        if (samples.isEmpty()) {
            paint.alpha = 77
            canvas.drawLine(0f, centerY, width.toFloat(), centerY, paint)
            paint.alpha = 255
            return
        }

        path.reset()
        val amplitudeScale = height / 2f * 0.9f

        samples.forEachIndexed { i, sample ->
            val x = i * width.toFloat() / samples.size
            val y = centerY - sample * amplitudeScale
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        canvas.drawPath(path, paint)
    }
}
