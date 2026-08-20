package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val email: String,
    val fullName: String,
    val mode: PreparationMode,
    val age: Int? = null,
    val collegeName: String? = null,
    val gateDepartment: GateDepartment? = null,
    val isRemembered: Boolean = true
)
