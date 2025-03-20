package com.example.futbolitopocket

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState
import kotlinx.coroutines.delay

@Composable
fun SoccerFieldCanvas(
    onTopGoalScored: () -> Unit,
    onBottomGoalScored: () -> Unit
) {
    val density = LocalDensity.current

    if (!isAccelerometerSensorAvailable()) {
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Acelerómetro no disponible", color = Color.Red)
        }
        return
    }

    // Lectura del sensor
    val sensorValue by rememberAccelerometerSensorValueAsState()

    // Estados para posición y velocidad de la pelota
    var ballX by remember { mutableStateOf(0f) }
    var ballY by remember { mutableStateOf(0f) }
    var velocityX by remember { mutableStateOf(0f) }
    var velocityY by remember { mutableStateOf(0f) }

    // Flags para evitar gol repetido
    var ballInTopGoal by remember { mutableStateOf(false) }
    var ballInBottomGoal by remember { mutableStateOf(false) }

    // Parámetros de física
    val friction = 0.92f
    val restitution = 1.2f
    val dt = 0.016f
    val sensorFactor = 8f

    BoxWithConstraints(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // Radio de la pelota
        val radius = with(density) { 10.dp.toPx() }

        // Inicializamos la pelota en el centro
        if (ballX == 0f && ballY == 0f) {
            ballX = w / 2
            ballY = h / 2
        }

        // Definición de las porterías
        val goalWidth = w * 0.15f
        val goalHeight = h * 0.03f
        val goalTopLeftX = (w - goalWidth) / 2
        val topGoalRect = Rect(
            offset = Offset(goalTopLeftX, 0f),
            size = Size(goalWidth, goalHeight)
        )
        val bottomGoalRect = Rect(
            offset = Offset(goalTopLeftX, h - goalHeight),
            size = Size(goalWidth, goalHeight)
        )

        LaunchedEffect(sensorValue) {
            while (true) {
                val (ax, ay, _) = sensorValue?.value ?: Triple(0f, 0f, 0f)
                val accelerationX = -ax * sensorFactor * dt
                val accelerationY = ay * sensorFactor * dt

                velocityX = (velocityX + accelerationX) * friction
                velocityY = (velocityY + accelerationY) * friction

                ballX += velocityX * dt * 1000
                ballY += velocityY * dt * 1000

                // Colisiones horizontales
                if (ballX - radius < 0f) {
                    ballX = radius
                    velocityX = -velocityX * restitution
                } else if (ballX + radius > w) {
                    ballX = w - radius
                    velocityX = -velocityX * restitution
                }

                // Colisiones verticales
                if (ballY - radius < 0f && !topGoalRect.contains(Offset(ballX, ballY))) {
                    ballY = radius
                    velocityY = -velocityY * restitution
                } else if (ballY + radius > h && !bottomGoalRect.contains(Offset(ballX, ballY))) {
                    ballY = h - radius
                    velocityY = -velocityY * restitution
                }

                // Detección de goles
                if (topGoalRect.contains(Offset(ballX, ballY)) && !ballInTopGoal) {
                    onTopGoalScored()
                    ballInTopGoal = true
                    ballX = w / 2
                    ballY = h / 2
                    velocityX = 0f
                    velocityY = 0f
                } else if (!topGoalRect.contains(Offset(ballX, ballY))) {
                    ballInTopGoal = false
                }
                if (bottomGoalRect.contains(Offset(ballX, ballY)) && !ballInBottomGoal) {
                    onBottomGoalScored()
                    ballInBottomGoal = true
                    ballX = w / 2
                    ballY = h / 2
                    velocityX = 0f
                    velocityY = 0f
                } else if (!bottomGoalRect.contains(Offset(ballX, ballY))) {
                    ballInBottomGoal = false
                }

                delay((dt * 1000).toLong())
            }
        }

        Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Dibujar el campo con franjas
            val stripeCount = 10
            val stripeHeight = h / stripeCount
            for (i in 0 until stripeCount) {
                drawRect(
                    color = if (i % 2 == 0) Color(0xFF4A8438) else Color(0xFF639539),
                    topLeft = Offset(0f, i * stripeHeight),
                    size = Size(w, stripeHeight)
                )
            }

            // Dibujar el borde exterior, línea media y círculo central
            val lineWidth = 4f
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                style = Stroke(width = lineWidth)
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = lineWidth
            )
            val centerCircleRadius = h * 0.15f
            drawCircle(
                color = Color.White,
                center = Offset(w / 2, h / 2),
                radius = centerCircleRadius,
                style = Stroke(width = lineWidth)
            )

            // Dibujar áreas de penal y porterías
            val penaltyAreaHeightFraction = 0.18f
            val penaltyAreaWidthFraction = 0.44f
            val goalAreaHeightFraction = 0.065f
            val goalAreaWidthFraction = 0.20f

            val penAreaTopHeight = h * penaltyAreaHeightFraction
            val penAreaTopWidth = w * penaltyAreaWidthFraction
            val penAreaTopLeftX = (w - penAreaTopWidth) / 2
            drawRect(
                color = Color.White,
                topLeft = Offset(penAreaTopLeftX, 0f),
                size = Size(penAreaTopWidth, penAreaTopHeight),
                style = Stroke(width = lineWidth)
            )

            val goalAreaTopHeight = h * goalAreaHeightFraction
            val goalAreaTopWidth = w * goalAreaWidthFraction
            val goalAreaTopLeftX = (w - goalAreaTopWidth) / 2
            drawRect(
                color = Color.White,
                topLeft = Offset(goalAreaTopLeftX, 0f),
                size = Size(goalAreaTopWidth, goalAreaTopHeight),
                style = Stroke(width = lineWidth)
            )

            val penAreaBottomHeight = penAreaTopHeight
            val penAreaBottomLeftY = h - penAreaBottomHeight
            drawRect(
                color = Color.White,
                topLeft = Offset(penAreaTopLeftX, penAreaBottomLeftY),
                size = Size(penAreaTopWidth, penAreaBottomHeight),
                style = Stroke(width = lineWidth)
            )

            val goalAreaBottomHeight = goalAreaTopHeight
            val goalAreaBottomLeftY = h - goalAreaBottomHeight
            drawRect(
                color = Color.White,
                topLeft = Offset(goalAreaTopLeftX, goalAreaBottomLeftY),
                size = Size(goalAreaTopWidth, goalAreaBottomHeight),
                style = Stroke(width = lineWidth)
            )

            // Dibujar las porterías
            drawRect(
                color = Color.White,
                topLeft = Offset(goalTopLeftX, 0f),
                size = Size(goalWidth, goalHeight),
                style = Stroke(width = lineWidth)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(goalTopLeftX, h - goalHeight),
                size = Size(goalWidth, goalHeight),
                style = Stroke(width = lineWidth)
            )

            // Dibujar la pelota
            drawCircle(
                color = Color.Red,
                radius = radius,
                center = Offset(ballX, ballY)
            )
        }
    }
}

