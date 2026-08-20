package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType {
    PYQ,
    AI_PRACTICE
}

@Entity(tableName = "questions")
data class QuestionData(
    @PrimaryKey val id: String,
    val year: String = "2024",
    val subject: String,
    val topic: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int, // 0-based index
    val explanation: String = "",
    val marks: Int = 1,
    val difficulty: String = "Medium",
    val sourceType: SourceType = SourceType.PYQ,
    val isVerified: Boolean = true,
    val mode: PreparationMode = PreparationMode.PROFESSIONAL_GATE,
    val userId: String = ""
)
