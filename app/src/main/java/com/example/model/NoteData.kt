package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteData(
    @PrimaryKey val id: String,
    val subject: String,
    val topic: String,
    val subtopic: String,
    val content: String,
    val mode: PreparationMode,
    val isCompleted: Boolean = false,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
