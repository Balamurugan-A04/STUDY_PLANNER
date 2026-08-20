package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_data")
data class SubjectData(
    @PrimaryKey val id: String,
    val name: String,
    val importance: String = "Medium", // High, Medium, Low
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val availableStudyMinutesPerDay: Int = 60, // 30, 60, 120, 180, 240
    val examDate: String = "",
    val progress: Float = 0f,
    val isUserCreated: Boolean = false,
    val mode: PreparationMode = PreparationMode.ACADEMIC,
    val userId: String = ""
)
