package com.cs407.lab09

import android.hardware.Sensor
import android.hardware.SensorEvent
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BallViewModel : ViewModel() {

    private var ball: Ball? = null

    // 上一次传感器事件时间戳（纳秒）
    private var lastTimestamp: Long = 0L

    // 记录第一次的重力作为基准，这样“当前姿态 = 0 加速度”
    private var baseX = 0f
    private var baseY = 0f
    private var hasBase = false

    // 对 UI 暴露的球的中心位置
    private val _ballPosition = MutableStateFlow(Offset.Zero)
    val ballPosition: StateFlow<Offset> = _ballPosition.asStateFlow()

    fun initBall(fieldWidth: Float, fieldHeight: Float, ballSizePx: Float) {
        if (ball == null) {
            ball = Ball(
                backgroundWidth = fieldWidth,
                backgroundHeight = fieldHeight,
                ballSize = ballSizePx
            )
            ball?.let {
                _ballPosition.value = Offset(it.posX, it.posY)
            }
        }
    }

    fun onSensorDataChanged(event: SensorEvent) {
        val currentBall = ball ?: return
        if (event.sensor.type != Sensor.TYPE_GRAVITY) return

        val NS2S = 1.0f / 1_000_000_000f
        val SENSITIVITY = 50f   // 灵敏度：数值越大球越“活”，可以自己微调

        // 第一次：只记录基准重力，不移动球
        if (!hasBase) {
            baseX = event.values[0]
            baseY = event.values[1]
            hasBase = true
            lastTimestamp = event.timestamp
            return
        }

        if (lastTimestamp != 0L) {
            // 1️⃣ 计算 dT（秒），并限制在 [0.01, 0.05] 范围内
            val rawDt = (event.timestamp - lastTimestamp) * NS2S
            val dT = rawDt.coerceIn(0.01f, 0.05f)

            // 2️⃣ 相对重力（当前 - 基准）
            val sensorX = event.values[0]
            val sensorY = event.values[1]
            val gX = sensorX - baseX
            val gY = sensorY - baseY

            // 3️⃣ 映射到屏幕坐标，并放大
            //  X轴：右倾时sensor减小(gX<0)，球应向右(xAcc>0)，故取反
            val xAcc = -gX * SENSITIVITY
            //  Y轴：后倾时sensor减小(gY<0)，球应向下(yAcc>0)，故取反
            val yAcc = -gY * SENSITIVITY

            // 4️⃣ 更新物理状态
            currentBall.updatePositionAndVelocity(xAcc, yAcc, dT)
            currentBall.checkBoundaries()

            // 5️⃣ 通知 UI
            _ballPosition.update { Offset(currentBall.posX, currentBall.posY) }
        }

        lastTimestamp = event.timestamp
    }

    fun reset() {
        ball?.reset()
        ball?.let {
            _ballPosition.value = Offset(it.posX, it.posY)
        }
        // 重置时间和基准，下次重新校准
        lastTimestamp = 0L
        hasBase = false
    }
}