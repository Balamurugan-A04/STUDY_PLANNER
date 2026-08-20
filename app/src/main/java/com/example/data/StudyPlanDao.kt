package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.PreparationMode
import com.example.model.StudyPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans WHERE mode = :mode LIMIT 1")
    fun getStudyPlanFlow(mode: PreparationMode): Flow<StudyPlan?>

    @Query("SELECT * FROM study_plans WHERE mode = :mode LIMIT 1")
    suspend fun getStudyPlan(mode: PreparationMode): StudyPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(studyPlan: StudyPlan)

    @Query("DELETE FROM study_plans WHERE mode = :mode")
    suspend fun deleteStudyPlan(mode: PreparationMode)
}
