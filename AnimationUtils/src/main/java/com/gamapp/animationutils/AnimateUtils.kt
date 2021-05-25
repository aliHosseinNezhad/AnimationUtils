package com.gamapp.animationutils

import android.os.CountDownTimer
import android.util.Log
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AnimateUtils(var interval: Long) {
    private var isAnimating: Boolean = false
    private var timeLine = TimeLine(1, interval)
    private var frames = ArrayList<Frame>()
    private var minTime = 0L
    private var maxTime = 0L
    private var onStartParam: (() -> Unit)? = null
    private var onEndParam: (() -> Unit)? = null
    var revert: Boolean = false
    fun start(revert: Boolean = false) {
        if (!isAnimating) {
            isAnimating = true
            this.revert = revert
            timeLine.stop()
            timeLine = TimeLine(maxTime, interval)
            frames.forEach {
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

    fun frame(from: Long, to: Long, param: (Double) -> Unit): Data {
        val frame = Frame(param)
        val data = Data(frame, from, to)
        frames.add(frame)
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
        frames.forEach {
            if (it.lastDisplayed)
                if (revert) it.start() else it.end()
            it.lastDisplayed = false
        }
        if (revert) onStartParam?.let { it() } else
            onEndParam?.let { it() }
        isAnimating = false
    }

    private fun refresh(time: Long, last: Boolean) {
        frames.forEach {
            if (time in it.sTime..it.eTime) {
                if (!it.lastDisplayed) {
                    if (revert) it.end() else it.start()
                }
                it.lastDisplayed = true
                it.refresh(calculate(time, it.sTime, it.eTime, it.sWeight, it.eWeight, it.curveModel))
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
        sWeight: Float=0f,
        eWeight: Float=1f,
        model: TimeLine.CurveModel
    ): Double {
        val t = sWeight + (eWeight-sWeight)*(time - sTime) / (eTime - sTime).toFloat()
        return when(model){
            TimeLine.CurveModel.LINEAR ->{
                t.toDouble()
            }
            TimeLine.CurveModel.COS ->{
                cos(t * PI*2)
            }
            TimeLine.CurveModel.SIN ->{
                sin(t * PI *2)
            }
            TimeLine.CurveModel.X_SIN ->{
                (t*PI*2 - sin(t * PI *2))/(2*PI)
            }
        }
    }

    class Data(
        private val frame: Frame,
        //startTime
        val sTime: Long,
        //endTime
        val eTime: Long
    ) {

        fun mode(curveModel: TimeLine.CurveModel): Data {
            frame.curveModel = curveModel
            return this
        }

        init {
            frame.sTime = sTime
            frame.eTime = eTime
            frame.sWeight = 0f
            frame.eWeight = 1f
        }

        fun out(sWeight: Float, eWeight: Float): Data {
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

    class Frame(private var param: ((Double) -> Unit)) {
        var curveModel: TimeLine.CurveModel = TimeLine.CurveModel.LINEAR
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

        fun refresh(it: Double) {
            param(it)
        }

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
            synchronized(this@TimeLine){
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
    enum class CurveModel{
        LINEAR,SIN,COS,X_SIN
    }
}