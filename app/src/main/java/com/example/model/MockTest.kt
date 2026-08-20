package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mock_tests")
data class MockTest(
    @PrimaryKey val id: String,
    val testName: String,
    val mode: PreparationMode,
    val department: String,
    val selectedSubject: String,
    val totalQuestions: Int,
    val difficulty: String,
    val score: Double = 0.0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unansweredCount: Int = 0,
    val accuracy: Double = 0.0,
    val timeUsedSeconds: Long = 0,
    val completedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
