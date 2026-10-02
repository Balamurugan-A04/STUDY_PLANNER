package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.model.SubjectData
import com.example.model.SyllabusData
import com.example.ui.components.AddSubjectDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusScreen(
    mode: PreparationMode,
    syllabusList: List<SyllabusData>,
    subjectDataList: List<SubjectData> = emptyList(),
    onToggleTopic: (String, Boolean) -> Unit,
    onAddSubject: (name: String, importance: String, difficulty: String, availableMinutes: Int, examDate: String) -> Unit,
    onDeleteSubject: (id: String) -> Unit,
    onDeleteSubtopic: (id: String) -> Unit = {},
    onNavigateSubjectDetail: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedSubjectTab by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<SubjectData?>(null) }
    var topicToDelete by remember { mutableStateOf<SyllabusData?>(null) }

    val allSubjectNames = remember(syllabusList, subjectDataList) {
        val namesFromSubjects = subjectDataList.map { it.name }
        listOf("All") + namesFromSubjects.distinct()
    }

    LaunchedEffect(allSubjectNames) {
        if (selectedSubjectTab != "All" && !allSubjectNames.contains(selectedSubjectTab)) {
            selectedSubjectTab = "All"
        }
    }

    val filteredSubjects = remember(subjectDataList, selectedSubjectTab) {
        if (selectedSubjectTab == "All") subjectDataList
        else subjectDataList.filter { it.name.equals(selectedSubjectTab, ignoreCase = true) }
    }

    val filteredTopics = remember(syllabusList, selectedSubjectTab) {
        if (selectedSubjectTab == "All") syllabusList
        else syllabusList.filter { it.subject.equals(selectedSubjectTab, ignoreCase = true) }
    }

    val completedTopicCount = remember(filteredTopics) { filteredTopics.count { it.isCompleted } }
    val totalTopicCount = remember(filteredTopics) { filteredTopics.size }
    val progressPercent = if (totalTopicCount > 0) (completedTopicCount.toFloat() / totalTopicCount.toFloat()) else 0f

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (mode == PreparationMode.PROFESSIONAL_GATE) "GATE Syllabus" else "Academic Subjects",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (mode == PreparationMode.ACADEMIC) {
                            Button(
                                onClick = { showAddDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("btn_open_add_subject")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Subject", fontSize = 13.sp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_syllabus")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Completion Progress Header
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Completion Progress",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$completedTopicCount / $totalTopicCount Topics",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }

                // Subject Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = allSubjectNames.indexOf(selectedSubjectTab).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    allSubjectNames.forEach { subjectName ->
                        Tab(
                            selected = selectedSubjectTab == subjectName,
                            onClick = { selectedSubjectTab = subjectName },
                            text = { Text(subjectName, fontWeight = if (selectedSubjectTab == subjectName) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Subjects Section
                    if (filteredSubjects.isNotEmpty()) {
                        item {
                            Text(
                                text = "Subjects (${filteredSubjects.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(filteredSubjects, key = { "subj_${it.id}" }) { subject ->
                            val subjectTopics = syllabusList.filter { it.subject == subject.name && it.mode == subject.mode }
                            val completed = subjectTopics.count { it.isCompleted }
                            val total = subjectTopics.size
                            val subjectProgress = if (total > 0) completed.toFloat() / total.toFloat() else subject.progress

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateSubjectDetail(subject.id) }
                                    .testTag("card_subject_${subject.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subject.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        IconButton(
                                            onClick = { subjectToDelete = subject },
                                            modifier = Modifier.testTag("btn_delete_subject_${subject.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Subject",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    if (total > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$completed / $total subtopics completed",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${(subjectProgress * 100).toInt()}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { subjectProgress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Badges / Metadata Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Imp: ${subject.importance}", fontSize = 11.sp) }
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Diff: ${subject.difficulty}", fontSize = 11.sp) }
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("⏱ ${subject.availableStudyMinutesPerDay}m/day", fontSize = 11.sp) }
                                        )
                                    }

                                    if (subject.examDate.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📅 Exam Date: ${subject.examDate}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    // 2. Syllabus Topics Section
                    if (filteredTopics.isNotEmpty()) {
                        item {
                            Text(
                                text = "Syllabus Topics (${filteredTopics.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(filteredTopics, key = { "top_${it.id}" }) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTopic(item.id, !item.isCompleted) }
                                    .testTag("topic_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (item.isCompleted) "Completed" else "Incomplete",
                                        tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.topic,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.subject} • ${item.subtopic}",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { topicToDelete = item },
                                        modifier = Modifier.testTag("btn_delete_topic_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Subtopic",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    } else if (filteredSubjects.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (mode == PreparationMode.ACADEMIC) "No subjects added yet." else "No syllabus topics available.",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (mode == PreparationMode.ACADEMIC) "Add your subjects to start building your academic study plan." else "Please check your syllabus configuration.",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    if (mode == PreparationMode.ACADEMIC) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = { showAddDialog = true },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Subject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Add Subject Dialog
    if (showAddDialog) {
        AddSubjectDialog(
            onDismiss = { showAddDialog = false },
            onSaveSubject = { name, importance, difficulty, availableMinutes, examDate ->
                onAddSubject(name, importance, difficulty, availableMinutes, examDate)
                showAddDialog = false
            }
        )
    }

    // Delete Subject Confirmation Dialog
    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = {
                Text("Delete this subject?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Deleting this subject will also remove its related subtopics, tasks, schedule data, and other subject-specific data.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        subjectToDelete?.let { onDeleteSubject(it.id) }
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Subtopic Confirmation Dialog
    if (topicToDelete != null) {
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = {
                Text("Delete this subtopic?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete '${topicToDelete?.topic}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        topicToDelete?.let { onDeleteSubtopic(it.id) }
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_subtopic")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
