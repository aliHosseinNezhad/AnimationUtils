package com.gamapp.animationutils

import android.os.CountDownTimer
import androidx.annotation.IntRange
import com.gamapp.animationutils.AnimateUtils.Direction.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

open class AnimateUtils(
    @IntRange(from = 1) var interval: Long = 1L,
    param: (AnimateUtils.() -> Unit)? = null
) {
    private var timeLine: TimeLine? = null
    private var framesArray = ArrayList<Frames>()
    private var minTime = 0L
    private var maxTime = 0L
    private var onStartParam: ((Direction) -> Unit)? = null
    private var onEndParam: ((Direction) -> Unit)? = null
    private var direction = STE
    private var currentTime: Long = 0L
        set(value) {
            field = value
        }
    private val duration: Long get() = maxTime

    init {
        param?.let {
            it(this)
        }
    }

    fun start(direction: Direction) {
        this.direction = direction
        timeLine?.stop()
        timeLine = if (direction == ETS) {
            if (currentTime == 0L) {
                initFrames()
                TimeLine(duration, interval, direction)
            } else {
                if (currentTime == duration) {
                    initFrames()
                }
                TimeLine(currentTime, interval, direction)
            }
        } else {
            if (currentTime == duration) {
                if (currentTime == 0L) {
                    initFrames()
                }
                TimeLine(duration, interval, direction)
            } else {
                if (currentTime == 0L) {
                    initFrames()
                }
                val timeLine = TimeLine(duration - currentTime, interval, direction)
                timeLine.startTime = currentTime
                timeLine
            }
        }
        timeLine?.let { it ->
            it.onRefresh {
                refresh(it)
            }
            it.onEnd { end() }
            it.start()
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


    private fun initFrames() {
        framesArray.forEach {
            it.lastDisplayed = false
        }
    }

    fun onStart(param: (Direction) -> Unit) {
        onStartParam = param
    }

    fun onEnd(param: (Direction) -> Unit) {
        onEndParam = param
    }


    private fun end() {
        if (direction == ETS) onStartParam?.let { it(direction) } else
            onEndParam?.let { it(direction) }
    }

    private fun refresh(time: Long) {
        currentTime = time
        framesArray.forEach {
            if (time in it.sTime..it.eTime) {
                if (!it.lastDisplayed) {
                    if (ETS == direction) it.end(direction) else it.start(direction)
                }
                it.lastDisplayed = true
                it.refresh(
                    calculate(
                        time,
                        it.sTime,
                        it.eTime,
                        it.curveModel,
                        it.sWeight,
                        it.eWeight
                    )
                )
            }
            if (if (ETS == direction) it.sTime >= time else it.eTime <= time) {
                if (it.lastDisplayed)
                    if (ETS == direction) it.start(direction) else it.end(direction)
                it.lastDisplayed = false
            }
        }
    }


    private fun calculate(
        time: Long,
        sTime: Long,
        eTime: Long,
        model: CurveModel,
        sWeight: Float = 0f,
        eWeight: Float = 1f
    ): Float {
        var t = (time - sTime) / (eTime - sTime).toFloat()
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

        fun mode(curveModel: CurveModel): Data {
            frame.curveModel = curveModel
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

        fun onStart(param: (Direction) -> Unit): Data {
            frame.onStartParam = param
            return this
        }

        fun onEnd(param: (Direction) -> Unit): Data {
            frame.onEndParam = param
            return this
        }
    }

    class Frames(private var param: ((Float) -> Unit)) {
        var curveModel: CurveModel = CurveModel.LINEAR
        var lastDisplayed = false
        var sTime: Long = 0
        var eTime: Long = 0
        var sWeight: Float = 0f
        var eWeight: Float = 1f
        var onStartParam: ((Direction) -> Unit)? = null
        var onEndParam: ((Direction) -> Unit)? = null
        fun start(direction: Direction) {
            onStartParam?.let { it(direction) }
        }

        fun end(direction: Direction) {
            onEndParam?.let { it(direction) }
        }

        fun refresh(it: Float) {
            param(it)
        }

    }

    enum class CurveModel {
        LINEAR, SIN, COS, X_SIN
    }

    enum class Direction {
        STE, //start to end time
        ETS //end to start time
    }
}

class TimeLine(val duration: Long, val interval: Long, val direction: AnimateUtils.Direction) {
    var countDownTimer = Timer(duration, interval)
    private var nextTime = 0L
    private var lastTime = 0L
    var startTime = 0L
    val currentTime get() = nextTime + lastTime
    private var finishParam: (() -> Unit)? = null
    private var onTickParam: ((Long) -> Unit)? = null
    private fun getTimeByDirection(time: Long): Long {
        return startTime + (if (direction == STE) {
            time
        } else duration - (time))
    }

    fun start() {
        nextTime = 0L
        lastTime = 0L
        countDownTimer.cancel()
        countDownTimer = Timer(duration, interval)
        synchronized(this) {
            onTickParam?.let {
                it(getTimeByDirection(0))
            }
        }
        countDownTimer.start()
    }

    fun stop() {
        countDownTimer.cancel()
    }

    fun resume(revert: Boolean = false) {
        countDownTimer.cancel()
        lastTime += nextTime
        nextTime = 0
        countDownTimer = Timer(duration - lastTime, interval)
        countDownTimer.start()
    }

    fun onEnd(param: () -> Unit) {
        finishParam = param
    }

    fun onRefresh(param: (Long) -> Unit) {
        onTickParam = param
    }

    inner class Timer(val duration: Long, val interval: Long) :
        CountDownTimer(duration, interval) {
        override fun onTick(millisUntilFinished: Long) {
            nextTime = duration - millisUntilFinished
            synchronized(this@TimeLine) {
                onTickParam?.let {
                    it(getTimeByDirection(nextTime + lastTime))
                }
            }
        }

        override fun onFinish() {
            synchronized(this@TimeLine) {
                onTickParam?.let {
                    it(getTimeByDirection(duration))
                }
                finishParam?.let {
                    it()
                }
            }

        }

    }

}