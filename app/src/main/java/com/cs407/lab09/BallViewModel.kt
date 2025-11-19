package com.cs407.lab09

import android.hardware.Sensor
import android.hardware.SensorEvent
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the Tilt 'n Roll game (Milestone 1).
 *
 * 职责：
 *  - 持有 Ball 实例
 *  - 处理 gravity sensor 的 SensorEvent
 *  - 计算时间差 dT
 *  - 把传感器坐标系映射到屏幕坐标系
 *  - 调用 Ball.updatePositionAndVelocity 和 Ball.checkBoundaries
 *  - 通过 StateFlow 暴露球的位置给 UI
 */
class BallViewModel : ViewModel() {

    private var ball: Ball? = null

    // 上一次传感器事件的时间戳（纳秒）
    private var lastTimestamp: Long = 0L

    // 对 UI 暴露的球的位置（屏幕坐标系）
    private val _ballPosition = MutableStateFlow(Offset.Zero)
    val ballPosition: StateFlow<Offset> = _ballPosition.asStateFlow()

    /**
     * 在游戏场地大小已知时由 UI 调用，只初始化一次 Ball。
     */
    fun initBall(fieldWidth: Float, fieldHeight: Float, ballSizePx: Float) {
        if (ball == null) {
            ball = Ball(
                backgroundWidth = fieldWidth,
                backgroundHeight = fieldHeight,
                ballSize = ballSizePx
            )
            // Ball.reset() 会在构造里被调用，直接拿当前坐标作为初始 UI 位置
            ball?.let {
                _ballPosition.value = Offset(it.posX, it.posY)
            }
        }
    }

    /**
     * 由 GameScreen 的 SensorEventListener 调用。
     * 处理 gravity sensor 的数据并更新球的状态。
     */
    fun onSensorDataChanged(event: SensorEvent) {
        val currentBall = ball ?: return

        // 只处理 GRAVITY 传感器（Milestone 1 要求）
        if (event.sensor.type != Sensor.TYPE_GRAVITY) return

        if (lastTimestamp != 0L) {
            // 1️⃣ 计算时间差（秒）
            // event.timestamp 单位是纳秒
            val NS2S = 1.0f / 1_000_000_000.0f
            val dT = (event.timestamp - lastTimestamp) * NS2S

            // 2️⃣ 从传感器坐标系映射到屏幕坐标系
            //  - 传感器坐标：+x 向右，+y 向上（出屏幕）
            //  - 屏幕坐标：+x 向右，+y 向下
            //  handout 里也提到 y 轴方向相反，需要翻转。:contentReference[oaicite:0]{index=0}
            val sensorX = event.values[0]
            val sensorY = event.values[1]

            // 让“向右倾斜 → 球向右滚”，“向下倾斜 → 球向下滚”
            // 如果方向感觉相反，可以把某一项再乘以 -1 调整手感。
            val xAcc = -sensorX          // x 轴取反：方向与重力一致
            val yAcc = sensorY           // y 轴本来与屏幕 y 相反，取反已经在物理模型里体现

            // 3️⃣ 更新小球物理状态
            currentBall.updatePositionAndVelocity(
                xAcc = xAcc,
                yAcc = yAcc,
                dT = dT
            )
            currentBall.checkBoundaries()

            // 4️⃣ 通知 UI 位置变化
            _ballPosition.update { Offset(currentBall.posX, currentBall.posY) }
        }

        // 5️⃣ 记录这次事件时间戳，供下一次算 dT
        lastTimestamp = event.timestamp
    }

    /**
     * 重置球到初始位置和速度。
     * 由 UI 的 Reset 按钮调用。
     */
    fun reset() {
        ball?.reset()
        ball?.let {
            _ballPosition.value = Offset(it.posX, it.posY)
        }
        lastTimestamp = 0L
    }
}