package com.example.palabras.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.palabras.domain.models.Level
import com.example.palabras.ui.viewmodel.GameUiState
import com.example.palabras.ui.viewmodel.GameViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val foundWords by viewModel.foundWordsState.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val revealedHints by viewModel.revealedHints.collectAsState()

    val backgroundGradients = listOf(
        Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF0288D1))), // Azul intenso
        Brush.verticalGradient(listOf(Color(0xFFC8E6C9), Color(0xFF388E3C))), // Verde intenso
        Brush.verticalGradient(listOf(Color(0xFFFFE0B2), Color(0xFFF57C00))), // Naranja intenso
        Brush.verticalGradient(listOf(Color(0xFFE1BEE7), Color(0xFF7B1FA2))), // Púrpura intenso
        Brush.verticalGradient(listOf(Color(0xFFD7CCC8), Color(0xFF5D4037)))  // Tierra intenso
    )

    val currentGradient = backgroundGradients[(stats.currentLevel - 1) % backgroundGradients.size]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Palabras", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(
                            "Nivel ${stats.currentLevel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.Black)
                    }
                },
                actions = {
                    ScoreBadge(score = stats.currentScore)
                    IconButton(onClick = { viewModel.getHint() }) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Ayuda", tint = Color(0xFFFBC02D))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.5f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentGradient)
                .padding(padding)
        ) {
            when (val state = uiState) {
                is GameUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is GameUiState.Success -> {
                    GameContent(
                        level = state.level,
                        foundWords = foundWords,
                        revealedHints = revealedHints,
                        onWordSubmitted = { viewModel.onWordSubmitted(it) }
                    )
                }
                is GameUiState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(score: Int) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = score.toString(),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun GameContent(
    level: Level,
    foundWords: Set<String>,
    revealedHints: Map<String, Int>,
    onWordSubmitted: (String) -> Unit
) {
    val selectedIndicesState = remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val letterPositions = remember { mutableStateMapOf<Int, Offset>() }
    var showTargetWords by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(level.targetWords) { word ->
                val hintLength = revealedHints[word] ?: 0
                WordItem(
                    word = word,
                    isFound = foundWords.contains(word),
                    hintText = if (hintLength > 0) word.substring(0, hintLength) else ""
                )
            }
        }

        // Replace unsafe mapping with a filtered-safe version to avoid IndexOutOfBounds
        val currentWord = selectedIndicesState.value
            .filter { it >= 0 && it < level.letters.size }
            .map { level.letters[it] }
            .joinToString("")

        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (currentWord.isEmpty()) Color.Transparent else Color.Black.copy(alpha = 0.8f),
            tonalElevation = 8.dp
        ) {
            Text(
                text = currentWord.ifEmpty { " " }.uppercase(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .size(320.dp)
                .padding(20.dp)
                .pointerInput(level.letters) {
                    val hitRadiusPx = 35.dp.toPx()
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentTouchPosition = offset
                            letterPositions.forEach { (index, center) ->
                                if (isInsideCircle(offset, center, hitRadiusPx)) {
                                    selectedIndicesState.value = listOf(index)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            currentTouchPosition = change.position
                            letterPositions.forEach { (index, center) ->
                                if (isInsideCircle(change.position, center, hitRadiusPx)) {
                                    val currentList = selectedIndicesState.value
                                    if (!currentList.contains(index)) {
                                        selectedIndicesState.value = currentList + index
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            val finalWord = selectedIndicesState.value
                                .filter { it >= 0 && it < level.letters.size }
                                .map { level.letters[it] }
                                .joinToString("")
                            if (finalWord.isNotEmpty()) {
                                onWordSubmitted(finalWord)
                            }
                            selectedIndicesState.value = emptyList()
                            currentTouchPosition = null
                        },
                        onDragCancel = {
                            selectedIndicesState.value = emptyList()
                            currentTouchPosition = null
                        }
                    )
                },
            contentAlignment = Alignment.TopStart
        ) {
            // Limpiar índices inválidos ANTES de usarlos
            val validIndices = selectedIndicesState.value.filter { it < level.letters.size }
            if (validIndices.size != selectedIndicesState.value.size) {
                selectedIndicesState.value = validIndices
            }

            val lineColor = Color.White // Blanco sólido para máximo contraste

            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentIndices = validIndices
                if (currentIndices.isNotEmpty()) {
                    for (i in 0 until currentIndices.size - 1) {
                        val start = letterPositions[currentIndices[i]] ?: Offset.Zero
                        val end = letterPositions[currentIndices[i + 1]] ?: Offset.Zero
                        drawLine(
                            color = lineColor,
                            start = start,
                            end = end,
                            strokeWidth = 14.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    currentTouchPosition?.let { touch ->
                        val start = letterPositions[currentIndices.last()] ?: Offset.Zero
                        drawLine(
                            color = lineColor,
                            start = start,
                            end = touch,
                            strokeWidth = 14.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            val letters = level.letters
            val radiusPx = with(density) { 110.dp.toPx() }
            val centerPx = with(density) { 140.dp.toPx() }
            val letterRadiusPx = with(density) { 28.dp.toPx() }

            letters.forEachIndexed { index, char ->
                val angle = (2 * Math.PI * index / letters.size) - Math.PI / 2
                val x = (radiusPx * cos(angle)).toFloat() + centerPx
                val y = (radiusPx * sin(angle)).toFloat() + centerPx

                val isSelected = validIndices.contains(index)
                val center = Offset(x, y)
                letterPositions[index] = center

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (x - letterRadiusPx).toInt(),
                                (y - letterRadiusPx).toInt()
                            )
                        }
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White
                            else Color.Black.copy(alpha = 0.6f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        // Botón temporal de depuración "Rendición"
        if (showTargetWords) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFEBEE),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📋 PALABRAS OBJETIVO (DEBUG)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    level.targetWords.forEach { word ->
                        Text(
                            text = "• $word",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = { showTargetWords = !showTargetWords },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showTargetWords) Color(0xFFC62828) else Color(0xFFF57C00)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (showTargetWords) "Ocultar Palabras 👻" else "🏳️ Rendición (DEBUG)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun isInsideCircle(point: Offset, center: Offset, radius: Float): Boolean {
    val distance = sqrt((point.x - center.x).pow(2) + (point.y - center.y).pow(2))
    return distance <= radius
}

@Composable
fun WordItem(word: String, isFound: Boolean, hintText: String = "") {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .height(44.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFound) Color.White.copy(alpha = 0.9f) 
                            else Color.Black.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val textToShow = when {
                isFound -> word.uppercase()
                hintText.isNotEmpty() -> hintText.uppercase() + "_".repeat(word.length - hintText.length)
                else -> "?".repeat(word.length)
            }
            Text(
                text = textToShow,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = if (isFound) Color.Black else Color.White
            )
        }
    }
}
