package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.ui.components.QuestionCardView
import com.example.ui.theme.AppTheme
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestActiveScreen(
    mode: PreparationMode,
    questions: List<QuestionData>,
    durationMinutes: Int,
    onSubmitTest: (userAnswers: Map<Int, Int>, timeTakenSeconds: Int, userNatAnswers: Map<Int, String>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> selectedOptionIndex
    val natAnswers = remember { mutableStateMapOf<Int, String>() } // questionIndex -> enteredNatText
    val markedForReview = remember { mutableStateMapOf<Int, Boolean>() }

    var remainingSeconds by remember { mutableIntStateOf(durationMinutes * 60) }
    var totalElapsedSeconds by remember { mutableIntStateOf(0) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }

    // Countdown Timer
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            totalElapsedSeconds++
        }
        if (remainingSeconds == 0) {
            onSubmitTest(userAnswers.toMap(), totalElapsedSeconds, natAnswers.toMap())
        }
    }

    val minutesLeft = remainingSeconds / 60
    val secondsLeft = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutesLeft, secondsLeft)

    val currentQuestion = remember(questions, currentIndex) {
        if (questions.isNotEmpty() && currentIndex in questions.indices) questions[currentIndex] else null
    }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = if (remainingSeconds < 300) StatusError else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingSeconds < 300) StatusError else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showPaletteDialog = true }) {
                            Icon(Icons.Default.GridOn, contentDescription = "Question Palette")
                        }
                        Button(
                            onClick = { showSubmitConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("btn_submit_mock")
                        ) {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_mock_test_active")
        ) { paddingValues ->
            if (currentQuestion == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No test questions available.", color = TextSecondary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} / ${questions.size}",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        TextButton(
                            onClick = {
                                val currentMark = markedForReview[currentIndex] ?: false
                                markedForReview[currentIndex] = !currentMark
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (markedForReview[currentIndex] == true) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (markedForReview[currentIndex] == true) MaterialTheme.colorScheme.primary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (markedForReview[currentIndex] == true) "Marked" else "Mark for Review",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (markedForReview[currentIndex] == true) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        QuestionCardView(
                            question = currentQuestion,
                            selectedOption = userAnswers[currentIndex],
                            onOptionSelect = { selectedOptionIndex ->
                                userAnswers[currentIndex] = selectedOptionIndex
                            },
                            natAnswer = natAnswers[currentIndex],
                            onNatAnswerChange = { text ->
                                if (text.isBlank()) {
                                    natAnswers.remove(currentIndex)
                                    userAnswers.remove(currentIndex)
                                } else {
                                    natAnswers[currentIndex] = text
                                    userAnswers[currentIndex] = 0
                                }
                            },
                            showAnswer = false, // Do not reveal correct answers during exam mode!
                            questionNumber = currentIndex + 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Navigation and Clear Option Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            enabled = currentIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        TextButton(
                            onClick = {
                                userAnswers.remove(currentIndex)
                                natAnswers.remove(currentIndex)
                            },
                            enabled = userAnswers.containsKey(currentIndex) || natAnswers.containsKey(currentIndex)
                        ) {
                            Text("Clear Selection", color = StatusError)
                        }

                        Button(
                            onClick = { if (currentIndex < questions.size - 1) currentIndex++ },
                            enabled = currentIndex < questions.size - 1
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            // Question Palette Dialog
            if (showPaletteDialog) {
                AlertDialog(
                    onDismissRequest = { showPaletteDialog = false },
                    title = { Text("Question Overview", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("Answered: ${userAnswers.size}", fontSize = 12.sp, color = StatusSuccess)
                                Text("Marked: ${markedForReview.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Unanswered: ${questions.size - userAnswers.size}", fontSize = 12.sp, color = TextSecondary)
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                itemsIndexed(questions) { idx, _ ->
                                    val isAnswered = userAnswers.containsKey(idx)
                                    val isMarked = markedForReview[idx] == true
                                    val isCurrent = idx == currentIndex

                                    val bg = when {
                                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                        isAnswered -> StatusSuccess.copy(alpha = 0.2f)
                                        isMarked -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .clickable {
                                                currentIndex = idx
                                                showPaletteDialog = false
                                            }
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPaletteDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Submit Confirmation Dialog
            if (showSubmitConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showSubmitConfirmDialog = false },
                    title = { Text("Submit Mock Test?") },
                    text = {
                        Text("You have answered ${userAnswers.size} out of ${questions.size} questions. Are you sure you want to finish the test?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSubmitConfirmDialog = false
                                onSubmitTest(userAnswers.toMap(), totalElapsedSeconds, natAnswers.toMap())
                            }
                        ) {
                            Text("Submit Now")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSubmitConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
