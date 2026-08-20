package com.example.data

import androidx.room.TypeConverter
import com.example.model.GateDepartment
import com.example.model.PreparationMode
import com.example.model.SourceType

class Converters {
    @TypeConverter
    fun fromPreparationMode(mode: PreparationMode?): String? = mode?.name

    @TypeConverter
    fun toPreparationMode(value: String?): PreparationMode? = value?.let { PreparationMode.valueOf(it) }

    @TypeConverter
    fun fromGateDepartment(dept: GateDepartment?): String? = dept?.name

    @TypeConverter
    fun toGateDepartment(value: String?): GateDepartment? = value?.let { GateDepartment.valueOf(it) }

    @TypeConverter
    fun fromSourceType(type: SourceType?): String? = type?.name

    @TypeConverter
    fun toSourceType(value: String?): SourceType? = value?.let { SourceType.valueOf(it) }

    @TypeConverter
    fun fromListString(list: List<String>?): String? = list?.joinToString("||")

    @TypeConverter
    fun toListString(value: String?): List<String> = if (value.isNullOrEmpty()) emptyList() else value.split("||")
}
