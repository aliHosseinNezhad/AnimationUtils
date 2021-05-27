package com.gamapp.animateutils

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.gamapp.animationutils.AnimateUtils
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class SwitchStateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var weight: Float = 2f
    var paint = Paint().apply {
        color = Color.rgb(0, 20, 180)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    var animationUtils = AnimateUtils {
        animation(0, 3000) {
            Log.i("TAG02222", "current time : $it")
            weight = (it+1)
            paint.isAntiAlias = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                paint.blendMode = BlendMode.HUE
            }
            postInvalidate()
        }
            .domain(0.25f, -0.25f)
            .mode(AnimateUtils.CurveModel.SIN)
            .onStart { Log.i("TAG02222", " onStart $it")}
            .onEnd {Log.i("TAG02222", " onEnd $it") }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawCircle(width / 2f, height / 2f, min(width, height) / 4f * weight, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    animationUtils.start(AnimateUtils.Direction.STE)
                }
            }
        }
        return true
    }


}