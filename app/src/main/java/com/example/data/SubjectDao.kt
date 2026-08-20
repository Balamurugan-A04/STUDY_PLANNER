package com.example.data

import androidx.room.*
import com.example.model.PreparationMode
import com.example.model.SubjectData
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subject_data WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    fun getSubjectsByModeFlow(mode: PreparationMode, userId: String): Flow<List<SubjectData>>

    @Query("SELECT * FROM subject_data WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    suspend fun getSubjectsByMode(mode: PreparationMode, userId: String): List<SubjectData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectData>)

    @Query("DELETE FROM subject_data WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    @Query("DELETE FROM subject_data WHERE mode = 'ACADEMIC' AND userId = ''")
    suspend fun deleteLegacyAcademicSeedSubjects()

    @Query("SELECT COUNT(*) FROM subject_data WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    suspend fun getTotalCount(mode: PreparationMode, userId: String): Int

    @Query("SELECT COUNT(*) FROM subject_data WHERE mode = :mode AND (userId = :userId OR (mode = 'PROFESSIONAL_GATE' AND userId = ''))")
    suspend fun getUserSpecificCount(mode: PreparationMode, userId: String): Int

    @Query("UPDATE subject_data SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float)
}
