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
    //Obtener el density actual para convertir dp a pixeles
    val density = LocalDensity.current

    //Comprobar si el acelerometro esta disponible
    if (!isAccelerometerSensorAvailable()) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Acelerómetro no disponible", color = Color.Red)
        }
        return
    }
    //Lectura del sensor
    val sensorValue by rememberAccelerometerSensorValueAsState()

    //Estados para la posicion (ballX, ballY) y la velocidad (velocityX, velocityY) de la pelota
    var ballX by remember { mutableStateOf(0f) }
    var ballY by remember { mutableStateOf(0f) }
    var velocityX by remember { mutableStateOf(0f) }
    var velocityY by remember { mutableStateOf(0f) }

    //Flags para evitar contar multiples goles cuando se permanece en el area de goleo
    var ballInTopGoal by remember { mutableStateOf(false) }
    var ballInBottomGoal by remember { mutableStateOf(false) }


    //Parametros:
    //friction: factor que reduce la velocidad (simula la friccion e inercia)
    //rebound: factor de rebote (al chocar, la velocidad se invierte y se multiplica por este valor)
    //timeStep: delta de tiempo en segundos

    //sensorFactor: escala los valores del sensor para ajustar la sensibilidad. Este valor se
    //multiplica por la aceleracion que se lee del sensor, de modo que si el sensorFactor es mayor,
    //incluso cambios pequeños en la inclinacion produciran cambios mas significativos en la
    //velocidad de la pelota. Si es menor, la respuesta de la pelota será más suave y menos
    //sensible a los movimientos del dispositivo.
    val friction = 0.92f
    val rebound = 1.2f
    val timeStep = 0.016f
    val sensorFactor = 8f

    BoxWithConstraints(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        //Obtener el ancho (w) y alto (h) del canvas a partir de las constraints
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        //Convertir 10.dp a pixeles para obtener el radio de la pelota
        val radius = with(density) { 10.dp.toPx() }

        //Si la posicion de la pelota aun es (0,0), se inicializa en el centro del canvas
        if (ballX == 0f && ballY == 0f) {
            ballX = w / 2
            ballY = h / 2
        }

        //Las dimensiones de las porterias
        //-La portería ocupa el 15% del ancho y el 3% del alto del canvas
        val goalWidth = w * 0.15f
        val goalHeight = h * 0.03f
        //Se calcula la posicion horizontal para centrar la porteria
        val goalTopLeftX = (w - goalWidth) / 2
        //Se crea un rectangulo para la porteria superior
        val topGoalRect = Rect(
            offset = Offset(goalTopLeftX, 0f),
            size = Size(goalWidth, goalHeight)
        )
        //Se crea un rectangulo para la porteria inferior
        val bottomGoalRect = Rect(
            offset = Offset(goalTopLeftX, h - goalHeight),
            size = Size(goalWidth, goalHeight)
        )

        LaunchedEffect(sensorValue) {
            //Se inicia un bucle infinito que se ejecuta mientras sensorValue cambia
            while (true) {

                //Se obtienen los valores actuales del sensor (ax, ay, az). Si el sensorValue es nulo se usan
                //ceros para todas las componentes (Triple(0f, 0f, 0f))
                val (ax, ay, _) = sensorValue?.value ?: Triple(0f, 0f, 0f)

                //Se calcula la aceleracion en el eje X
                //Se invierte el valor de 'ax' (por ejemplo, para que una inclinacion a la izquierda produzca
                //movimiento a la izquierda por que el cenzor da valores positivos hacia la izquierda y para
                //posicionar la pelota correctamente se necesita eso) y se multiplica por 'sensorFactor'
                //para ajustar la sensibilidad y por 'timeStep' para escalar la integracion en funcion
                //del tiempo transcurrido
                val accelerationX = -ax * sensorFactor * timeStep

                //Se calcula la aceleracion en el eje Y
                //Se multiplica 'ay' por 'sensorFactor' y 'timeStep' para integrar correctamente la aceleración
                //lo mismo que en el eje X
                val accelerationY = ay * sensorFactor * timeStep

                //Se actualiza la velocidad en X
                //Se suma la aceleracion calculada en X a la velocidad actual y luego se multiplica
                //por 'friction' para simular la perdida de energia reduciendo la velocidad
                velocityX = (velocityX + accelerationX) * friction

                //Se actualiza la velocidad en Y de forma similar
                velocityY = (velocityY + accelerationY) * friction

                //Se actualiza la posicion de la pelota en X
                //Se suma a la posicion actual 'ballX' la velocidad en X multiplicada por 'timeStep'
                //y por un factor de 1000
                //Este factor de 1000 se utiliza para que el movimiento sea perceptible en el canvas
                //por que sino seria un valor muy pequeño que no seria perceptible en pantalla
                ballX += velocityX * timeStep * 1000

                //Se actualiza la posicion de la pelota en Y de la misma forma
                ballY += velocityY * timeStep * 1000

                //Verificar colisiones horizontales
                //Si la posición X de la pelota menos el radio es menor que 0
                //(se sale por el lado izquierdo)
                if (ballX - radius < 0f) {
                    //Se posiciona la pelota justo en el borde izquierdo (a la distancia del radio)
                    ballX = radius
                    //Se invierte la velocidad horizontal y se multiplica por 'rebound' para
                    //aplicar el factor de rebote lo que simula la perdida de energía en el choque
                    velocityX = -velocityX * rebound
                }
                //Si la posicion X de la pelota mas el radio es mayor que el ancho del canvas
                //(se sale por el lado derecho)
                else if (ballX + radius > w) {
                    //Se posiciona la pelota justo en el borde derecho
                    ballX = w - radius
                    //Se invierte la velocidad horizontal aplicando el factor de rebote
                    velocityX = -velocityX * rebound
                }

                //Verificar colisiones verticales
                //Si la pelota se sale por la parte superior (ballY - radius < 0) y ademas no esta
                //dentro del area de la porteria superior
                if (ballY - radius < 0f && !topGoalRect.contains(Offset(ballX, ballY))) {
                    //Se posiciona la pelota justo en el borde superior
                    ballY = radius
                    //Se invierte la velocidad vertical y se aplica el factor de rebote
                    velocityY = -velocityY * rebound
                }
                //Si la pelota se sale por la parte inferior (ballY + radius > h) y no esta dentro
                //del area de la porteria inferior
                else if (ballY + radius > h && !bottomGoalRect.contains(Offset(ballX, ballY))) {
                    //Se posiciona la pelota en el borde inferior
                    ballY = h - radius
                    //Se invierte la velocidad vertical con el factor de rebote
                    velocityY = -velocityY * rebound
                }

                //Deteccion de goles
                //Si la pelota esta dentro del area de la porteria superior
                //y aun no se ha marcado ese gol
                if (topGoalRect.contains(Offset(ballX, ballY)) && !ballInTopGoal) {
                    //Se llama a la funcion para anotar gol en la porteria superior
                    onTopGoalScored()
                    //Se marca el flag para evitar contar el mismo gol varias veces
                    ballInTopGoal = true
                    //Se reinicia la pelota en el centro del canvas
                    ballX = w / 2
                    ballY = h / 2
                    ///Se reinician las velocidades para detener el movimiento
                    velocityX = 0f
                    velocityY = 0f
                } else if (!topGoalRect.contains(Offset(ballX, ballY))) {
                    //Si la pelota sale del area de gol se desactiva el flag
                    ballInTopGoal = false
                }
                //Se realiza lo mismo para la portería inferior
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

                //Se espera 16 ms antes de la siguiente iteracin
                delay(16)
            }
        }


        Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            //Obtener el ancho y alto del canvas actual
            val w = size.width
            val h = size.height

            /*-----------------------------------------------
            Dibujar el campo con franjas alternadas
            -----------------------------------------------*/
            //Se define el numero total de franjas
            val stripeCount = 10
            // Se calcula la altura de cada franja dividiendo el alto total entre el numero de franjas
            val stripeHeight = h / stripeCount
            //Se dibuja cada franja en un bucle for
            for (i in 0 until stripeCount) {
                drawRect(
                    //Alternar colores si el indice es par para usar un tono de verde oscuro
                    //y si es impar un tono de verde mas claro
                    color = if (i % 2 == 0) Color(0xFF4A8438) else Color(0xFF639539),
                    //La esquina superior izquierda del rectangulo se ubica en (0, i * stripeHeight)
                    topLeft = Offset(0f, i * stripeHeight),
                    //El tamaño del rectangulo es igual al ancho completo del canvas y la altura de la franja
                    size = Size(w, stripeHeight)
                )
            }

            /*-----------------------------------------------
            Dibujar el borde exterior, la linea del mediao y el círculo central
            -----------------------------------------------*/
            //Definir el grosor de las lineas
            val lineWidth = 4f

            //Dibuja el borde exterior del campo
            //Se dibuja un rectangulo con trazo blanco que abarca la totalidad del canvas
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                style = Stroke(width = lineWidth)
            )

            //Dibuja la linea del medio horizontal
            //Se traza una linea blanca a la mitad de la altura del canvas
            drawLine(
                color = Color.White,
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = lineWidth
            )

            //Dibujar el circulo central
            //Se calcula el radio del circulo central como el 15% del alto total del canvas
            val centerCircleRadius = h * 0.15f
            //Se dibuja un circulo blanco con centro en la posicion central del canvas
            drawCircle(
                color = Color.White,
                center = Offset(w / 2, h / 2),
                radius = centerCircleRadius,
                style = Stroke(width = lineWidth)
            )

            /*-----------------------------------------------
            Dibujar las áreas de penal y las porterías
            -----------------------------------------------*/
            //Se definen las fracciones del canvas que usaran las areas de penal y las porterias
            val penaltyAreaHeightFraction = 0.18f
            val penaltyAreaWidthFraction = 0.44f
            val goalAreaHeightFraction = 0.065f
            val goalAreaWidthFraction = 0.20f

            //Se calcula la altura y el ancho del area penal superior
            val penAreaTopHeight = h * penaltyAreaHeightFraction
            val penAreaTopWidth = w * penaltyAreaWidthFraction
            //Se calcula la posicion horizontal para centrar el area penal superior
            val penAreaTopLeftX = (w - penAreaTopWidth) / 2
            //Se dibuja el rectangulo del area penal superior
            drawRect(
                color = Color.White,
                topLeft = Offset(penAreaTopLeftX, 0f),
                size = Size(penAreaTopWidth, penAreaTopHeight),
                style = Stroke(width = lineWidth)
            )

            //Se calcula la altura y el ancho del area de la porteria superior (area chica)
            val goalAreaTopHeight = h * goalAreaHeightFraction
            val goalAreaTopWidth = w * goalAreaWidthFraction
            //Se calcula la posicion horizontal para centrar el area de la porteria superior
            val goalAreaTopLeftX = (w - goalAreaTopWidth) / 2
            //Se dibuja el rectangulo del área de la porteria superior
            drawRect(
                color = Color.White,
                topLeft = Offset(goalAreaTopLeftX, 0f),
                size = Size(goalAreaTopWidth, goalAreaTopHeight),
                style = Stroke(width = lineWidth)
            )

            //Para el area penal inferior se usa la misma altura que el area superior
            val penAreaBottomHeight = penAreaTopHeight
            //Se calcula la posición vertical en la que comienza el area penal inferior
            val penAreaBottomLeftY = h - penAreaBottomHeight
            //Se dibuja el rectangulo del area penal inferior
            drawRect(
                color = Color.White,
                topLeft = Offset(penAreaTopLeftX, penAreaBottomLeftY),
                size = Size(penAreaTopWidth, penAreaBottomHeight),
                style = Stroke(width = lineWidth)
            )

            //Para el area de la porteria inferior se usa la misma altura que el area superior
            val goalAreaBottomHeight = goalAreaTopHeight
            //Se calcula la posicion vertical en la que comienza el area de la porteria inferior
            val goalAreaBottomLeftY = h - goalAreaBottomHeight
            //Se dibuja el rectangulo del area de la porteria inferior
            drawRect(
                color = Color.White,
                topLeft = Offset(goalAreaTopLeftX, goalAreaBottomLeftY),
                size = Size(goalAreaTopWidth, goalAreaBottomHeight),
                style = Stroke(width = lineWidth)
            )

            //Dibujar las porterias
            //Se dibuja un rectangulo para la porteria superior
            drawRect(
                color = Color.Red,
                topLeft = Offset(goalTopLeftX, 0f),
                size = Size(goalWidth, goalHeight),
                style = Stroke(width = lineWidth)
            )
            //Se dibuja un rectangulo para la porteria inferior
            drawRect(
                color = Color.Blue,
                topLeft = Offset(goalTopLeftX, h - goalHeight),
                size = Size(goalWidth, goalHeight),
                style = Stroke(width = lineWidth)
            )

            /*-----------------------------------------------
            Dibujar la pelota
            -----------------------------------------------*/
            //Se dibuja un circulo rojo que representa la pelota, en la
            //posicion actual (ballX, ballY) con el radio calculado
            drawCircle(
                color = Color.Red,
                radius = radius,
                center = Offset(ballX, ballY)
            )
        }

    }
}
