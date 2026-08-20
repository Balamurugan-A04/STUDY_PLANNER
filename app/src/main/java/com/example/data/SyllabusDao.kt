package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.PreparationMode
import com.example.model.SyllabusData
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {
    @Query("SELECT * FROM syllabus_topics WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    fun getSyllabusByModeFlow(mode: PreparationMode, userId: String): Flow<List<SyllabusData>>

    @Query("SELECT * FROM syllabus_topics WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    suspend fun getSyllabusByMode(mode: PreparationMode, userId: String): List<SyllabusData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<SyllabusData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: SyllabusData)

    @Query("DELETE FROM syllabus_topics WHERE id = :id")
    suspend fun deleteTopicById(id: String)

    @Query("DELETE FROM syllabus_topics WHERE subject = :subjectName AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = '')) AND mode = :mode")
    suspend fun deleteTopicsBySubjectName(subjectName: String, userId: String, mode: PreparationMode)

    @Query("DELETE FROM syllabus_topics WHERE mode = 'ACADEMIC' AND (userId = '' OR id LIKE 'as%')")
    suspend fun deleteLegacyAcademicSeedTopics()

    @Query("UPDATE syllabus_topics SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTopicStatus(id: String, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM syllabus_topics WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    suspend fun getTotalCount(mode: PreparationMode, userId: String): Int

    @Query("SELECT COUNT(*) FROM syllabus_topics WHERE mode = :mode AND isCompleted = 1 AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    suspend fun getCompletedCount(mode: PreparationMode, userId: String): Int
}
