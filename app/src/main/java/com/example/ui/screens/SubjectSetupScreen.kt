package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
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
import com.example.ui.components.AddSubjectDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSetupScreen(
    mode: PreparationMode,
    subjects: List<SubjectData>,
    onAddSubject: (name: String, importance: String, difficulty: String, availableMinutes: Int, examDate: String) -> Unit,
    onDeleteSubject: (id: String) -> Unit,
    onContinue: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<SubjectData?>(null) }

    AppTheme(mode = mode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Subject Setup", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Let's set up your subjects",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Add the subjects you want to prepare for.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (subjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (mode == PreparationMode.PROFESSIONAL_GATE) "Preparing your GATE subjects..." 
                                else "No subjects added yet.", 
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(subjects) { subject ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subject.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { subjectToDelete = subject }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (mode == PreparationMode.ACADEMIC) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Subject")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onContinue,
                    enabled = subjects.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Continue to Dashboard")
                }
            }
        }

        if (showAddDialog) {
            AddSubjectDialog(
                onDismiss = { showAddDialog = false },
                onSaveSubject = { name, imp, diff, mins, date ->
                    onAddSubject(name, imp, diff, mins, date)
                    showAddDialog = false
                }
            )
        }

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
    }
}
