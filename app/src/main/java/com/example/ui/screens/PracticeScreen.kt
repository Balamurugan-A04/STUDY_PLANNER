package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.ui.components.QuestionCardView
import com.example.ui.theme.AppTheme
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    mode: PreparationMode,
    subjects: List<String>,
    onSubjectSelected: (String) -> Unit,
    questions: List<QuestionData>,
    onNavigateBack: () -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "All") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var userNatAnswer by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }

    var correctCount by remember { mutableIntStateOf(0) }
    var attemptedCount by remember { mutableIntStateOf(0) }

    val currentQuestion = remember(questions, currentIndex) {
        if (questions.isNotEmpty() && currentIndex in questions.indices) questions[currentIndex] else null
    }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GATE Practice", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back")) {
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
                .testTag("screen_practice")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Subject selector row
                ScrollableTabRow(
                    selectedTabIndex = subjects.indexOf(selectedSubject).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    subjects.forEach { subject ->
                        Tab(
                            selected = selectedSubject == subject,
                            onClick = {
                                selectedSubject = subject
                                currentIndex = 0
                                selectedOption = null
                                userNatAnswer = ""
                                showAnswer = false
                                onSubjectSelected(subject)
                            },
                            text = { Text(subject, fontWeight = if (selectedSubject == subject) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                if (currentQuestion == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        Text("No practice questions available.", color = TextSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Score Header Banner
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${currentIndex + 1} of ${if (mode == PreparationMode.PROFESSIONAL_GATE) 100 else questions.size}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Score: $correctCount / $attemptedCount",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess
                                )
                            }
                        }

                        QuestionCardView(
                            question = currentQuestion,
                            selectedOption = selectedOption,
                            onOptionSelect = { optionIndex ->
                                if (selectedOption == null) {
                                    selectedOption = optionIndex
                                    showAnswer = true
                                    attemptedCount++
                                    if (optionIndex == currentQuestion.correctAnswer) {
                                        correctCount++
                                    }
                                }
                            },
                            natAnswer = userNatAnswer,
                            onNatAnswerChange = { text -> userNatAnswer = text },
                            showAnswer = showAnswer,
                            questionNumber = currentIndex + 1
                        )

                        if (currentQuestion.options.isEmpty() && !showAnswer) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showAnswer = true
                                    attemptedCount++
                                    if (com.example.ui.components.isNatAnswerCorrect(userNatAnswer, currentQuestion.explanation)) {
                                        correctCount++
                                    }
                                },
                                enabled = userNatAnswer.isNotBlank(),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Check Answer")
                            }
                        }

                        // Navigation Control Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        selectedOption = null
                                        userNatAnswer = ""
                                        showAnswer = false
                                    }
                                },
                                enabled = currentIndex > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous")
                            }

                            Button(
                                onClick = {
                                    if (currentIndex < questions.size - 1) {
                                        currentIndex++
                                        selectedOption = null
                                        userNatAnswer = ""
                                        showAnswer = false
                                    }
                                },
                                enabled = currentIndex < questions.size - 1
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
