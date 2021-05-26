package com.gamapp.animateutils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.gamapp.animationutils.AnimateUtils
import kotlin.math.abs
import kotlin.math.min

class SwitchStateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var weight: Float = 2f
    var animationUtils = AnimateUtils {
        animation(0, 8000) {
            weight = abs(it)*2f
            postInvalidate()
        }
            .domain(0f, 2f)
            .mode(AnimateUtils.CurveModel.COS, 1f)
            .onStart { }
            .onEnd { }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawCircle(width / 2f, height / 2f, min(width, height) / 4f * weight, Paint().apply {
                color = Color.rgb(0, 20, 180)
                style = Paint.Style.FILL
                isAntiAlias = true
            })
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    animationUtils.start()
                }
            }
        }
        return true
    }


}