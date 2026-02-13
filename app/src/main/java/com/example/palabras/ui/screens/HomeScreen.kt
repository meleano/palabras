package com.example.palabras.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onViewStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PALABRAS",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Nueva partida", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onContinueGame,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Continuar partida", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onViewStats,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Estadísticas", fontSize = 20.sp)
        }
    }
}
