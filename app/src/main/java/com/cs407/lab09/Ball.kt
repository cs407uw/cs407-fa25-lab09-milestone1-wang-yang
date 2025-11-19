package com.cs407.lab09

class Ball(
    private val backgroundWidth: Float,
    private val backgroundHeight: Float,
    private val ballSize: Float
) {
    var posX = 0f
    var posY = 0f
    var velocityX = 0f
    var velocityY = 0f
    private var accX = 0f
    private var accY = 0f

    private var isFirstUpdate = true

    init {
        reset()
    }

    fun updatePositionAndVelocity(xAcc: Float, yAcc: Float, dT: Float) {
        if (dT <= 0f) return

        if (isFirstUpdate) {
            isFirstUpdate = false
            accX = xAcc
            accY = yAcc
            return
        }

        val ax0 = accX
        val ay0 = accY
        val vx0 = velocityX
        val vy0 = velocityY

        val vx1 = vx0 + 0.5f * (ax0 + xAcc) * dT
        val vy1 = vy0 + 0.5f * (ay0 + yAcc) * dT

        val dt2 = dT * dT
        val dx = vx0 * dT + (1f / 6f) * (3f * ax0 + xAcc) * dt2
        val dy = vy0 * dT + (1f / 6f) * (3f * ay0 + yAcc) * dt2

        posX += dx
        posY += dy

        velocityX = vx1
        velocityY = vy1

        accX = xAcc
        accY = yAcc
    }

    fun checkBoundaries() {
        val radius = ballSize / 2f

        if (posX < radius) {
            posX = radius
            velocityX = 0f
            accX = 0f
        } else if (posX > backgroundWidth - radius) {
            posX = backgroundWidth - radius
            velocityX = 0f
            accX = 0f
        }

        if (posY < radius) {
            posY = radius
            velocityY = 0f
            accY = 0f
        } else if (posY > backgroundHeight - radius) {
            posY = backgroundHeight - radius
            velocityY = 0f
            accY = 0f
        }
    }

    fun reset() {
        posX = backgroundWidth / 2f
        posY = backgroundHeight / 2f
        velocityX = 0f
        velocityY = 0f
        accX = 0f
        accY = 0f
        isFirstUpdate = true
    }
}