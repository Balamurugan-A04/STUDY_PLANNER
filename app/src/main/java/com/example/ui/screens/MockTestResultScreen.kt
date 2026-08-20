package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.ui.components.PrimaryButton
import com.example.ui.components.QuestionCardView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestResultScreen(
    mode: PreparationMode,
    questions: List<QuestionData>,
    userAnswers: Map<Int, Int>,
    timeTakenSeconds: Int,
    userNatAnswers: Map<Int, String> = emptyMap(),
    onRetakeTest: () -> Unit,
    onNavigateDashboard: () -> Unit
) {
    var totalMarks = 0.0
    var obtainedMarks = 0.0
    var correctCount = 0
    var incorrectCount = 0
    var unattemptedCount = 0

    questions.forEachIndexed { idx, q ->
        totalMarks += q.marks
        if (q.options.isEmpty()) {
            val userNat = userNatAnswers[idx]
            if (userNat.isNullOrBlank()) {
                unattemptedCount++
            } else if (com.example.ui.components.isNatAnswerCorrect(userNat, q.explanation)) {
                correctCount++
                obtainedMarks += q.marks
            } else {
                incorrectCount++
                // No negative marks for NAT in GATE
            }
        } else {
            val userChoice = userAnswers[idx]
            when {
                userChoice == null -> unattemptedCount++
                userChoice == q.correctAnswer -> {
                    correctCount++
                    obtainedMarks += q.marks
                }
                else -> {
                    incorrectCount++
                    if (mode == PreparationMode.PROFESSIONAL_GATE) {
                        obtainedMarks -= (q.marks * 0.33) // Apply GATE 1/3rd negative marking
                    }
                }
            }
        }
    }

    if (obtainedMarks < 0) obtainedMarks = 0.0
    val percentage = if (totalMarks > 0) ((obtainedMarks / totalMarks) * 100).toInt() else 0
    val minutesSpent = timeTakenSeconds / 60
    val secondsSpent = timeTakenSeconds % 60

    var reviewIndex by remember { mutableIntStateOf(0) }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mock Test Results", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateDashboard, modifier = Modifier.testTag("btn_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_mock_test_result")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Result Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Score: ${String.format("%.2f", obtainedMarks)} / ${String.format("%.2f", totalMarks)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "$percentage% Accuracy • Time: ${minutesSpent}m ${secondsSpent}s",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$correctCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StatusSuccess)
                                }
                                Text("Correct", fontSize = 12.sp, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$incorrectCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StatusError)
                                }
                                Text("Incorrect", fontSize = 12.sp, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$unattemptedCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextSecondary)
                                Text("Skipped", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Review Section Header
                Text(
                    text = "Question Solutions & Analysis",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (questions.isNotEmpty() && reviewIndex in questions.indices) {
                    val revQ = questions[reviewIndex]
                    val userSel = userAnswers[reviewIndex]

                    QuestionCardView(
                        question = revQ,
                        selectedOption = userSel,
                        onOptionSelect = {},
                        natAnswer = userNatAnswers[reviewIndex],
                        onNatAnswerChange = null,
                        showAnswer = true,
                        questionNumber = reviewIndex + 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { if (reviewIndex > 0) reviewIndex-- },
                            enabled = reviewIndex > 0
                        ) {
                            Text("Previous Solution")
                        }

                        Button(
                            onClick = { if (reviewIndex < questions.size - 1) reviewIndex++ },
                            enabled = reviewIndex < questions.size - 1
                        ) {
                            Text("Next Solution")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Retake Exam",
                    icon = Icons.Default.Refresh,
                    onClick = onRetakeTest,
                    testTag = "btn_retake_test"
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
