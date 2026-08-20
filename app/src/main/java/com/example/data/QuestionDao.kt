package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.PreparationMode
import com.example.model.QuestionData
import com.example.model.SourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE mode = :mode AND (userId = :userId OR userId = '')")
    fun getQuestionsByModeFlow(mode: PreparationMode, userId: String): Flow<List<QuestionData>>

    @Query("SELECT * FROM questions WHERE mode = :mode AND sourceType = :sourceType AND (userId = :userId OR userId = '')")
    suspend fun getQuestionsBySourceType(mode: PreparationMode, sourceType: SourceType, userId: String): List<QuestionData>

    @Query("SELECT DISTINCT subject FROM questions WHERE mode = :mode AND (userId = :userId OR userId = '')")
    suspend fun getSubjects(mode: PreparationMode, userId: String): List<String>

    @Query("SELECT DISTINCT year FROM questions WHERE mode = :mode AND sourceType = 'PYQ' AND (userId = :userId OR userId = '')")
    suspend fun getPyqYears(mode: PreparationMode, userId: String): List<String>

    @Query("SELECT * FROM questions WHERE mode = :mode AND (:subject = 'All' OR :subject = 'All Subjects' OR :subject = 'Full Syllabus' OR LOWER(subject) = LOWER(:subject)) AND (:year = 'All' OR year = :year) AND (userId = :userId OR userId = '')")
    suspend fun getFilteredPyqs(mode: PreparationMode, subject: String, year: String, userId: String): List<QuestionData>

    @Query("SELECT * FROM questions WHERE mode = :mode AND (:subject = 'All' OR :subject = 'All Subjects' OR :subject = 'Full Syllabus' OR LOWER(subject) = LOWER(:subject)) AND (userId = :userId OR userId = '')")
    suspend fun getPracticeQuestions(mode: PreparationMode, subject: String, userId: String): List<QuestionData>

    @Query("DELETE FROM questions WHERE mode = :mode AND id IN ('q1', 'q2', 'q3', 'q4', 'q5', 'q6')")
    suspend fun deleteLegacySampleQuestions(mode: PreparationMode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionData>)
}
