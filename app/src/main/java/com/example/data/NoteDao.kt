package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.NoteData
import com.example.model.PreparationMode
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE mode = :mode")
    fun getNotesByModeFlow(mode: PreparationMode): Flow<List<NoteData>>

    @Query("SELECT * FROM notes WHERE mode = :mode AND subject = :subject")
    suspend fun getNotesBySubject(mode: PreparationMode, subject: String): List<NoteData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteData)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes WHERE subject = :subject AND mode = :mode")
    suspend fun deleteNotesBySubject(subject: String, mode: PreparationMode)

    @Query("DELETE FROM notes WHERE topic = :topic AND mode = :mode")
    suspend fun deleteNotesByTopic(topic: String, mode: PreparationMode)

    @Query("DELETE FROM notes WHERE mode = 'ACADEMIC' AND (id = 'an1' OR (subject = 'Data Structures' AND topic = 'Stack and Queue'))")
    suspend fun deleteLegacyAcademicSeedNotes()

    @Query("UPDATE notes SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateNoteStatus(id: String, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM notes WHERE mode = :mode")
    suspend fun getTotalNotesCount(mode: PreparationMode): Int

    @Query("SELECT COUNT(*) FROM notes WHERE mode = :mode AND isCompleted = 1")
    suspend fun getCompletedNotesCount(mode: PreparationMode): Int
}
