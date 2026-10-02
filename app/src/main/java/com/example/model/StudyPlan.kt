package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_plans")
data class StudyPlan(
    @PrimaryKey val mode: PreparationMode,
    val dailyHours: Int = 4,
    val targetExam: String = "GATE CS 2026",
    val dailyGoals: List<String> = listOf("Complete Algorithms - Dynamic Programming", "Practice 10 PYQs on OS", "Review Digital Logic notes"),
    val weeklySchedule: List<String> = listOf("Mon: Algorithms", "Tue: OS", "Wed: Computer Networks", "Thu: DBMS", "Fri: Theory of Comp", "Sat: Mock Test", "Sun: Revision"),
    val completedGoals: List<String> = emptyList(),
    val missedGoals: List<String> = emptyList()
)
