package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import com.example.model.SyllabusData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestConfigScreen(
    mode: PreparationMode,
    subjects: List<String>,
    syllabusList: List<SyllabusData>,
    availableQuestionCount: Int,
    onUpdateConfig: (String, List<String>) -> Unit,
    onStartTest: (String, List<String>, Int, Int, (String) -> Unit) -> Unit, // subject, subtopics, questionCount, durationMinutes, onError
    onNavigateBack: () -> Unit
) {
    val availableSubjectOptions = remember(subjects, mode) {
        val cleanSubjects = subjects.filter { it != "All" && it != "All Subjects" && it != "Full Syllabus" }.distinct()
        if (mode == PreparationMode.ACADEMIC) {
            if (cleanSubjects.isEmpty()) {
                emptyList()
            } else {
                listOf("All Subjects") + cleanSubjects
            }
        } else {
            listOf("All Subjects") + cleanSubjects
        }
    }

    var selectedSubject by remember {
        mutableStateOf(if (availableSubjectOptions.isNotEmpty()) availableSubjectOptions.first() else "")
    }
    val selectedSubtopics = remember { mutableStateListOf<String>() }
    
    val availableSubtopics = remember(selectedSubject, syllabusList) {
        if (selectedSubject.isBlank() || selectedSubject == "All Subjects") emptyList()
        else syllabusList.filter { it.subject.trim().equals(selectedSubject.trim(), ignoreCase = true) }.map { it.topic }.distinct()
    }

    var selectedQuestionCount by remember { mutableIntStateOf(10) }
    var selectedDuration by remember { mutableIntStateOf(15) } // minutes
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubject, selectedSubtopics.toList()) {
        onUpdateConfig(selectedSubject, selectedSubtopics.toList())
    }

    LaunchedEffect(selectedSubject) {
        selectedSubtopics.clear()
    }

    LaunchedEffect(availableSubjectOptions) {
        if (availableSubjectOptions.isEmpty()) {
            selectedSubject = ""
        } else if (!availableSubjectOptions.contains(selectedSubject)) {
            selectedSubject = availableSubjectOptions.first()
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Mock Test Setup", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (mode == PreparationMode.PROFESSIONAL_GATE) "GATE Mock Test" else "Academic Quiz Setup",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_mock_test_config")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Configure Mock Exam",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Simulate actual exam conditions with timed practice",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Subject Selector
                Text(
                    text = "Select Subject / Pattern",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                var subjectExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        availableSubjectOptions.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    selectedSubject = sub
                                    subjectExpanded = false
                                },
                                modifier = Modifier.testTag("dropdown_item_$sub")
                            )
                        }
                    }
                }

                if (availableSubjectOptions.size <= 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No subjects available. Add a subject to create a subject-specific mock test.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                if (availableSubtopics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Select Subtopics (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Clean Column with checkboxes for subtopics without inner nested vertical scroll
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableSubtopics.forEach { st ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedSubtopics.contains(st)) selectedSubtopics.remove(st)
                                        else selectedSubtopics.add(st)
                                    }
                            ) {
                                Checkbox(
                                    checked = selectedSubtopics.contains(st),
                                    onCheckedChange = {
                                        if (it) selectedSubtopics.add(st)
                                        else selectedSubtopics.remove(st)
                                    }
                                )
                                Text(text = st, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Question Count
                Text(
                    text = "Number of Questions: $selectedQuestionCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15, 20, 25, 50).forEach { count ->
                        FilterChip(
                            selected = selectedQuestionCount == count,
                            onClick = { selectedQuestionCount = count },
                            label = { Text("$count Qs", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Duration Selector
                Text(
                    text = "Exam Duration: $selectedDuration Minutes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = selectedDuration == mins,
                            onClick = { selectedDuration = mins },
                            label = { Text("$mins Mins") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (mode == PreparationMode.ACADEMIC) {
                    if (availableSubjectOptions.isEmpty()) {
                        Text(
                            text = "No Academic subjects available. Please add a subject first.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        val subjectDisplay = if (selectedSubject == "All Subjects") "all subjects" else "'$selectedSubject'"
                        val subtopicDisplay = if (selectedSubtopics.isNotEmpty()) " (${selectedSubtopics.size} subtopic(s) selected)" else ""
                        Text(
                            text = "AI will generate $selectedQuestionCount high-quality questions for $subjectDisplay$subtopicDisplay.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    if (availableQuestionCount == 0) {
                        Text(
                            text = "No GATE questions are available for this subject/subtopic.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (availableQuestionCount < selectedQuestionCount) {
                        val subjectLabel = if (selectedSubject == "All Subjects") "all subjects" else "'$selectedSubject'"
                        Text(
                            text = "Only $availableQuestionCount questions are available for $subjectLabel. Please select a lower question count or another subject.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        val subjectLabel = if (selectedSubject == "All Subjects") "all subjects" else "'$selectedSubject'"
                        Text(
                            text = "$availableQuestionCount questions available for $subjectLabel.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isBtnEnabled = remember(mode, isGenerating, availableQuestionCount, selectedQuestionCount, selectedSubject, availableSubjectOptions) {
                    if (isGenerating) return@remember false
                    if (mode == PreparationMode.ACADEMIC) {
                        availableSubjectOptions.isNotEmpty() && selectedSubject.isNotBlank() && selectedQuestionCount > 0
                    } else {
                        availableQuestionCount > 0
                    }
                }

                PrimaryButton(
                    text = if (isGenerating) "Generating Mock Test..." else "Start Mock Exam Now",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        isGenerating = true
                        onStartTest(selectedSubject, selectedSubtopics.toList(), selectedQuestionCount, selectedDuration) { err ->
                            isGenerating = false
                            errorMessage = err
                        }
                    },
                    enabled = isBtnEnabled,
                    testTag = "btn_start_mock_test"
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
