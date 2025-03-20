package com.example.futbolitopocket

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    //Obtener la configuracion actual
    val configuration = LocalConfiguration.current
    //Obtener la altura de la pantalla en dp
    val screenHeightDp = configuration.screenHeightDp.dp
    //Definir el alto del encabezado (10% de la pantalla)
    val headerHeight = screenHeightDp * 0.1f
    //Definir el alto del campo (90% de la pantalla)
    val fieldHeight = screenHeightDp * 0.9f

    //Estados para llevar el marcador de goles
    var topGoalCount by remember { mutableStateOf(0) }
    var bottomGoalCount by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        //Encabezado que muestra el marcador
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight),
            contentAlignment = Alignment.Center
        ) {
            Row {
                Text(
                    text = "Canitas: $topGoalCount",
                    color = Color.Blue
                )
                Text(
                    text = "   |   ",
                    color = Color.Black
                )
                Text(
                    text = "Camposable: $bottomGoalCount",
                    color = Color.Red
                )
            }
        }
        //Zona del campo de futbol
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
        ) {
            //Se llama al composable que maneja la logica del campo y la pelota
            SoccerFieldCanvas(
                onTopGoalScored = { topGoalCount++ },
                onBottomGoalScored = { bottomGoalCount++ }
            )
        }
    }
}
