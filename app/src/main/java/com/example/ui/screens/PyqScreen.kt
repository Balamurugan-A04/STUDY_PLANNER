package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
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
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyqScreen(
    mode: PreparationMode,
    years: List<String>,
    subjects: List<String>,
    onFilterChanged: (String, String) -> Unit,
    questions: List<QuestionData>,
    onNavigateBack: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(years.firstOrNull() ?: "All") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "All") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var userNatAnswer by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }

    var expandedYear by remember { mutableStateOf(false) }
    var expandedSubject by remember { mutableStateOf(false) }

    val currentQuestion = remember(questions, currentIndex) {
        if (questions.isNotEmpty() && currentIndex in questions.indices) questions[currentIndex] else null
    }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GATE PYQ Practice", fontWeight = FontWeight.Bold) },
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
                .testTag("screen_pyq")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subject Filter
                    ExposedDropdownMenuBox(
                        expanded = expandedSubject,
                        onExpandedChange = { expandedSubject = !expandedSubject },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSubject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubject) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubject,
                            onDismissRequest = { expandedSubject = false }
                        ) {
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject) },
                                    onClick = {
                                        selectedSubject = subject
                                        expandedSubject = false
                                        currentIndex = 0
                                        selectedOption = null
                                        userNatAnswer = ""
                                        showAnswer = false
                                        onFilterChanged(selectedSubject, selectedYear)
                                    }
                                )
                            }
                        }
                    }

                    // Year Filter
                    ExposedDropdownMenuBox(
                        expanded = expandedYear,
                        onExpandedChange = { expandedYear = !expandedYear },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedYear,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedYear,
                            onDismissRequest = { expandedYear = false }
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year) },
                                    onClick = {
                                        selectedYear = year
                                        expandedYear = false
                                        currentIndex = 0
                                        selectedOption = null
                                        userNatAnswer = ""
                                        showAnswer = false
                                        onFilterChanged(selectedSubject, selectedYear)
                                    }
                                )
                            }
                        }
                    }
                }

                if (currentQuestion == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        Text("No questions found for selected filters.", color = TextSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentIndex + 1} of ${if (mode == PreparationMode.PROFESSIONAL_GATE) 100 else questions.size}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(onClick = { showAnswer = !showAnswer }) {
                                Text(if (showAnswer) "Hide Answer" else "Reveal Answer")
                            }
                        }

                        QuestionCardView(
                            question = currentQuestion,
                            selectedOption = selectedOption,
                            onOptionSelect = { optionIndex ->
                                selectedOption = optionIndex
                                showAnswer = true
                            },
                            natAnswer = userNatAnswer,
                            onNatAnswerChange = { text -> userNatAnswer = text },
                            showAnswer = showAnswer,
                            questionNumber = currentIndex + 1
                        )

                        // Action Buttons Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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

                            IconButton(onClick = { /* Mark review */ }) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark")
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
