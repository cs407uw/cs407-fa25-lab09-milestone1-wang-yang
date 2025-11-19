package com.cs407.lab09

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cs407.lab09.ui.theme.Lab09Theme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: BallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab09Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: BallViewModel) {
    val context = LocalContext.current

    // 1️⃣ SensorManager
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // 2️⃣ 重力传感器
    val gravitySensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    // 3️⃣ 注册 / 注销监听器
    DisposableEffect(sensorManager, gravitySensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { viewModel.onSensorDataChanged(it) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // 不需要处理
            }
        }

        if (gravitySensor != null) {
            sensorManager.registerListener(
                listener,
                gravitySensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        onDispose {
            if (gravitySensor != null) {
                sensorManager.unregisterListener(listener, gravitySensor)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Reset 按钮
        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 40.dp, bottom = 16.dp)
        ) {
            Text(text = "Reset")
        }

        // 小球尺寸
        val ballSize = 50.dp
        val density = LocalDensity.current
        val ballSizePx = with(density) { ballSize.toPx() }
        val ballRadiusPx = ballSizePx / 2f

        // 订阅小球位置（注意：Ball 里 posX/posY 是“中心点”）
        val ballPosition by viewModel.ballPosition.collectAsStateWithLifecycle()

        // 球场背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .paint(
                    painter = painterResource(id = R.drawable.field),
                    contentScale = ContentScale.FillBounds
                )
                .onSizeChanged { size ->
                    // 通知 ViewModel 场地尺寸（像素）
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    viewModel.initBall(width, height, ballSizePx)
                }
        ) {
            // 小球（用中心点减半径来偏移，让它画在正确位置）
            Image(
                painter = painterResource(id = R.drawable.soccer),
                contentDescription = "Soccer Ball",
                modifier = Modifier
                    .size(ballSize)
                    .offset {
                        IntOffset(
                            x = (ballPosition.x - ballRadiusPx).roundToInt(),
                            y = (ballPosition.y - ballRadiusPx).roundToInt()
                        )
                    }
            )
        }
    }
}