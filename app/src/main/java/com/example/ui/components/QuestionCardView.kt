package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuestionData
import com.example.model.SourceType
import com.example.ui.theme.*

fun isNatAnswerCorrect(userAnswer: String?, explanation: String): Boolean {
    if (userAnswer.isNullOrBlank()) return false
    val trimmedUser = userAnswer.trim().lowercase()
    
    val correctPart = if (explanation.contains("Verified Correct Answer:")) {
        explanation.substringAfter("Verified Correct Answer:").substringBefore("\n").trim()
    } else {
        ""
    }
    
    if (correctPart.isNotBlank()) {
        val lowerCorrect = correctPart.lowercase().trim()
        if (trimmedUser == lowerCorrect) return true
        
        if (lowerCorrect.contains("to")) {
            val parts = lowerCorrect.split("to").mapNotNull { it.trim().toDoubleOrNull() }
            val userVal = trimmedUser.toDoubleOrNull()
            if (parts.size == 2 && userVal != null) {
                val minVal = minOf(parts[0], parts[1])
                val maxVal = maxOf(parts[0], parts[1])
                return userVal in minVal..maxVal
            }
        }
        
        val correctVal = lowerCorrect.toDoubleOrNull()
        val userVal = trimmedUser.toDoubleOrNull()
        if (correctVal != null && userVal != null) {
            return kotlin.math.abs(correctVal - userVal) < 0.001
        }
    }
    
    return false
}

fun getCorrectAnswerDisplay(question: QuestionData): String {
    if (question.options.isNotEmpty() && question.correctAnswer in question.options.indices) {
        val correctLetter = ('A' + question.correctAnswer).toString()
        return "Option $correctLetter"
    }
    if (question.explanation.contains("Verified Correct Answer:")) {
        return question.explanation.substringAfter("Verified Correct Answer:").substringBefore("\n").trim()
    }
    return if (question.explanation.isNotBlank()) question.explanation.take(60) else "N/A"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionCardView(
    question: QuestionData,
    selectedOption: Int? = null,
    onOptionSelect: ((Int) -> Unit)? = null,
    natAnswer: String? = null,
    onNatAnswerChange: ((String) -> Unit)? = null,
    showAnswer: Boolean = false,
    questionNumber: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("card_question")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badges & Marks row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Source Type Badge
                    val sourceBadgeBg = if (question.sourceType == SourceType.PYQ) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        StatusInfo.copy(alpha = 0.15f)
                    }
                    val sourceBadgeText = if (question.sourceType == SourceType.PYQ) {
                        "GATE ${question.year} PYQ"
                    } else {
                        "AI GENERATED PRACTICE"
                    }
                    val sourceBadgeColor = if (question.sourceType == SourceType.PYQ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        StatusInfo
                    }

                    Box(
                        modifier = Modifier
                            .background(sourceBadgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = sourceBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = sourceBadgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Verification Badge
                    if (!question.isVerified && question.options.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(StatusWarning.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Answer verification required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusWarning,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (question.options.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NAT Problem",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Text(
                    text = "${question.marks} Mark${if (question.marks > 1) "s" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject & Topic info
            Text(
                text = "${question.subject} • ${question.topic}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Question text with copy support
            SelectionContainer {
                Text(
                    text = if (questionNumber != null) "Q$questionNumber. ${question.question}" else question.question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (question.options.isNotEmpty()) {
                // Options list (MCQ / MSQ)
                question.options.forEachIndexed { index, optionText ->
                    val optionLetter = ('A' + index).toString()
                    val isSelected = selectedOption == index
                    val isCorrect = question.correctAnswer == index

                    val optionBg = when {
                        showAnswer && isCorrect -> StatusSuccess.copy(alpha = 0.15f)
                        showAnswer && isSelected && !isCorrect -> StatusError.copy(alpha = 0.15f)
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val optionBorder = when {
                        showAnswer && isCorrect -> StatusSuccess
                        showAnswer && isSelected && !isCorrect -> StatusError
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> BorderNeutral
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, optionBorder),
                        colors = CardDefaults.cardColors(containerColor = optionBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = onOptionSelect != null) {
                                onOptionSelect?.invoke(index)
                            }
                            .testTag("option_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (isSelected || (showAnswer && isCorrect)) optionBorder else BorderNeutral,
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLetter,
                                    color = if (isSelected || (showAnswer && isCorrect)) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = optionText,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }

                            if (showAnswer && isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (showAnswer && question.correctAnswer in question.options.indices) {
                    val correctLetter = ('A' + question.correctAnswer).toString()
                    val isUserCorrect = selectedOption == question.correctAnswer

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUserCorrect) StatusSuccess.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUserCorrect) "✓ Correct Answer: Option $correctLetter" else "✗ Correct Answer: Option $correctLetter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isUserCorrect) StatusSuccess else StatusError
                            )
                        }
                    }
                }
            } else {
                // NAT / Numerical Answer Type Input UI
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Numerical Answer Type (NAT)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your numerical answer below:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = natAnswer ?: "",
                            onValueChange = { onNatAnswerChange?.invoke(it) },
                            enabled = !showAnswer && onNatAnswerChange != null,
                            label = { Text("Enter Answer") },
                            placeholder = { Text("e.g. 21, 0.5, -2.0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_nat_answer")
                        )

                        if (showAnswer) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isCorrect = isNatAnswerCorrect(natAnswer, question.explanation)
                            val correctDisplay = getCorrectAnswerDisplay(question)
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCorrect) StatusSuccess.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isCorrect) "✓ Correct Answer: $correctDisplay"
                                               else "✗ Correct Answer: $correctDisplay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isCorrect) StatusSuccess else StatusError
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explanation block if requested
            if (showAnswer && question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Explanation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Explanation:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            SelectionContainer {
                                Text(
                                    text = question.explanation,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
