package com.gamapp.animationutils

import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.IntRange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AnimateUtils(
    @IntRange(from = 1) var interval: Long = 1L,
    param: (AnimateUtils.() -> Unit)? = null
) {


    private var isAnimating: Boolean = false
    private var timeLine = TimeLine(1, interval)
    private var framesArray = ArrayList<Frames>()
    private var minTime = 0L
    private var maxTime = 0L
    private var onStartParam: (() -> Unit)? = null
    private var onEndParam: (() -> Unit)? = null
    var revert: Boolean = false

    init {
        param?.let {
            it(this)
        }
    }

    fun start(revert: Boolean = false) {
        if (!isAnimating) {
            isAnimating = true
            this.revert = revert
            timeLine.stop()
            timeLine = TimeLine(maxTime, interval)
            framesArray.forEach {
                it.lastDisplayed = false
            }
            timeLine.start()
            if (revert) onEndParam?.let { it() } else
                onStartParam?.let { it() }
            timeLine.onRefresh {
                refresh(
                    if (revert) maxTime - it else it,
                    if (revert) it == 0L else it == maxTime
                )
            }
            timeLine.onEnd { end(if (revert) maxTime - it else it) }
        }
    }

    fun animation(from: Long, to: Long, param: (Float) -> Unit): Data {
        val frame = Frames(param)
        val data = Data(frame, from, to)
        framesArray.add(frame)
        if (frame.sTime < minTime) {
            minTime = frame.sTime
        }
        if (frame.eTime > maxTime) {
            maxTime = frame.eTime
        }
        return data
    }

    fun onStart(param: () -> Unit) {
        onStartParam = param
    }

    fun onEnd(param: () -> Unit) {
        onEndParam = param
    }


    private fun end(time: Long) {
        framesArray.forEach {
            if (it.lastDisplayed)
                if (revert) it.start() else it.end()
            it.lastDisplayed = false
        }
        if (revert) onStartParam?.let { it() } else
            onEndParam?.let { it() }
        isAnimating = false
    }

    private fun refresh(time: Long, last: Boolean) {
        framesArray.forEach {
            if (time in it.sTime..it.eTime) {
                if (!it.lastDisplayed) {
                    if (revert) it.end() else it.start()
                }
                it.lastDisplayed = true
                it.refresh(
                    calculate(
                        time,
                        it.sTime,
                        it.eTime,
                        it.curveModel,
                        it.cofficient,
                        it.sWeight,
                        it.eWeight
                    )
                )
            } else if (if (revert) it.sTime > time else it.eTime < time) {
                if (it.lastDisplayed)
                    if (revert) it.start() else it.end()
                it.lastDisplayed = false
            }
        }

    }


    fun calculate(
        time: Long,
        sTime: Long,
        eTime: Long,
        model: CurveModel, coefficient: Float = 1f,
        sWeight: Float = 0f,
        eWeight: Float = 1f
    ): Float {
        var t = (time - sTime) / (eTime - sTime).toFloat()
        t *= coefficient
        t = sWeight + (eWeight - sWeight) * t
        return when (model) {
            CurveModel.LINEAR -> {
                t
            }
            CurveModel.COS -> {
                cos(t * PI * 2)
            }
            CurveModel.SIN -> {
                sin(t * PI * 2)
            }
            CurveModel.X_SIN -> {
                (t * PI * 2 - sin(t * PI * 2)) / (2 * PI)
            }
        }.toFloat()
    }

    class Data(
        private val frame: Frames,
        //startTime
        val sTime: Long,
        //endTime
        val eTime: Long
    ) {

        fun mode(curveModel: CurveModel, coefficient: Float = 1f): Data {
            frame.curveModel = curveModel
            frame.cofficient = coefficient
            return this
        }

        init {
            frame.sTime = sTime
            frame.eTime = eTime
            frame.sWeight = 0f
            frame.eWeight = 1f
        }

        fun domain(sWeight: Float, eWeight: Float): Data {
            frame.sWeight = sWeight
            frame.eWeight = eWeight
            return this
        }

        fun onStart(param: () -> Unit): Data {
            frame.onStartParam = param
            return this
        }

        fun onEnd(param: () -> Unit): Data {
            frame.onEndParam = param
            return this
        }
    }

    class Frames(private var param: ((Float) -> Unit)) {
        var cofficient: Float = 1f
        var curveModel: CurveModel = CurveModel.LINEAR
        var lastDisplayed = false
        var sTime: Long = 0
        var eTime: Long = 0
        var sWeight: Float = 0f
        var eWeight: Float = 1f
        var onStartParam: (() -> Unit)? = null
        var onEndParam: (() -> Unit)? = null
        fun start() {
            onStartParam?.let { it() }
        }

        fun end() {
            onEndParam?.let { it() }
        }

        fun refresh(it: Float) {
            param(it)
        }

    }

    enum class CurveModel {
        LINEAR, SIN, COS, X_SIN
    }
}

class TimeLine(val duration: Long, val interval: Long) {
    var countDownTimer = Timer(duration, interval)
    var currentTime = 0L
    var lastTime = 0L
    var finishParam: ((Long) -> Unit)? = null
    var onTickParam: ((Long) -> Unit)? = null
    fun start() {
        currentTime = 0L
        lastTime = 0L
        countDownTimer.cancel()
        countDownTimer = Timer(duration, interval)
        countDownTimer.start()

    }

    fun stop() {
        countDownTimer.cancel()
    }

    fun resume(revert: Boolean = false) {
        countDownTimer.cancel()
        lastTime += currentTime
        currentTime = 0
        countDownTimer = Timer(duration - lastTime, interval)
        countDownTimer.start()
    }

    fun onEnd(param: (Long) -> Unit) {
        finishParam = param
    }

    fun onRefresh(param: (Long) -> Unit) {
        onTickParam = param
    }


    inner class Timer(val duration: Long, val interval: Long) :
        CountDownTimer(duration, interval) {
        override fun onTick(millisUntilFinished: Long) {
            currentTime = duration - millisUntilFinished
            synchronized(this@TimeLine) {
                onTickParam?.let {
                    it(currentTime + lastTime)
                }
            }
            Log.i("TAG011", "onTick: ${currentTime + lastTime}")
        }

        override fun onFinish() {
            onTickParam?.let {
                it(currentTime + lastTime)
            }
            finishParam?.let {
                it(currentTime + lastTime)
            }
        }

    }

}