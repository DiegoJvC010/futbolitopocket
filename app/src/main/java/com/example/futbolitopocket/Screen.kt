package com.example.futbolitopocket

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val headerHeight = screenHeightDp * 0.1f
    val fieldHeight = screenHeightDp * 0.9f

    var topGoalCount by remember { mutableStateOf(0) }
    var bottomGoalCount by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Encabezado con el marcador
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Canitas: $topGoalCount   |   Camposable: $bottomGoalCount"
            )
        }
        // Campo de fútbol
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
        ) {
            SoccerFieldCanvas(
                onTopGoalScored = { topGoalCount++ },
                onBottomGoalScored = { bottomGoalCount++ }
            )
        }
    }
}
