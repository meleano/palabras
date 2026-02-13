package com.example.palabras.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palabras.data.database.UserStats
import com.example.palabras.data.repository.UserStatsRepository
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    repository: UserStatsRepository,
    onBack: () -> Unit
) {
    val stats by repository.getUserStats().collectAsState(initial = UserStats())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("PARTIDA ACTUAL", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            StatItem("Puntuación", stats?.currentScore?.toString() ?: "0")
            StatItem("Nivel", stats?.currentLevel?.toString() ?: "1")
            StatItem("Palabras Encontradas", stats?.currentWordsFound?.toString() ?: "0")
            StatItem("Palabras Extra", stats?.currentExtraWordsFound?.toString() ?: "0")

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("HISTÓRICO ACUMULADO", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            StatItem("Puntuación Total", stats?.totalScore?.toString() ?: "0")
            StatItem("Niveles Completados", stats?.totalLevelsCompleted?.toString() ?: "0")
            StatItem("Palabras Encontradas", stats?.totalWordsFound?.toString() ?: "0")
            StatItem("Palabras Extra", stats?.totalExtraWordsFound?.toString() ?: "0")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
