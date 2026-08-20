package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSaveSubject: (name: String, importance: String, difficulty: String, availableMinutes: Int, examDate: String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var name by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf("Medium") }
    var difficulty by remember { mutableStateOf("Medium") }
    var availableMinutes by remember { mutableStateOf(60) } // Default 1 hour/day
    
    // Default exam date set to 30 days in the future formatted YYYY-MM-DD
    val futureCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
    var examDate by remember {
        mutableStateOf(
            String.format(
                Locale.US,
                "%04d-%02d-%02d",
                futureCal.get(Calendar.YEAR),
                futureCal.get(Calendar.MONTH) + 1,
                futureCal.get(Calendar.DAY_OF_MONTH)
            )
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val importanceOptions = listOf("High", "Medium", "Low")
    val difficultyOptions = listOf("Easy", "Medium", "Hard")
    val timeOptions = listOf(
        30 to "30 minutes/day",
        60 to "1 hour/day",
        120 to "2 hours/day",
        180 to "3 hours/day",
        240 to "4+ hours/day"
    )

    fun openDatePicker() {
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                examDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    fun handleSave() {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            errorMessage = "Please enter a subject name."
            return
        }
        errorMessage = null
        onSaveSubject(trimmedName, importance, difficulty, availableMinutes, examDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Subject",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject Name Field
                Column {
                    Text("Subject Name", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) errorMessage = null
                        },
                        placeholder = { Text("Enter subject name") },
                        isError = errorMessage != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_subject_name")
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Importance Level
                Column {
                    Text("Importance Level", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        importanceOptions.forEach { opt ->
                            FilterChip(
                                selected = importance == opt,
                                onClick = { importance = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Difficulty Level
                Column {
                    Text("Difficulty Level", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        difficultyOptions.forEach { opt ->
                            FilterChip(
                                selected = difficulty == opt,
                                onClick = { difficulty = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Available Study Time
                Column {
                    Text("Available Study Time", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        timeOptions.forEach { (mins, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { availableMinutes = mins }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = availableMinutes == mins,
                                    onClick = { availableMinutes = mins }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Exam Date
                Column {
                    Text("Exam Date", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        onClick = { openDatePicker() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_select_exam_date")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (examDate.isNotEmpty()) examDate else "Select Exam Date",
                                fontSize = 14.sp
                            )
                            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { handleSave() },
                modifier = Modifier.testTag("btn_save_subject")
            ) {
                Text("Save Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
