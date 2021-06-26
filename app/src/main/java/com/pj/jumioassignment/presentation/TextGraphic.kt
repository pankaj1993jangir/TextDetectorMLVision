package com.pj.jumioassignment.presentation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.text.Text

class TextGraphic internal constructor(
    overlay: GraphicOverlay,
    private val element: Text.Element?
) : GraphicOverlay.Graphic(overlay) {
    private val rectPaint: Paint = Paint()

    override fun draw(canvas: Canvas) {
        Log.d(TAG, "on draw text graphic")
        checkNotNull(element) { "Attempting to draw a null text." }
        val rect = RectF(element.boundingBox)
        canvas.drawRect(rect, rectPaint)
    }

    companion object {
        private const val TAG = "TextGraphic"
        private const val TEXT_COLOR = Color.RED
        private const val STROKE_WIDTH = 5.0f
    }

    init {
        rectPaint.color = TEXT_COLOR
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH
        postInvalidate()
    }
}
