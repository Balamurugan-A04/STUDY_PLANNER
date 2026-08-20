package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "syllabus_topics")
data class SyllabusData(
    @PrimaryKey val id: String,
    val subject: String,
    val topic: String,
    val subtopic: String,
    val mode: PreparationMode,
    val isCompleted: Boolean = false,
    val userId: String = ""
)
