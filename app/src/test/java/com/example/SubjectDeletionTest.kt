package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.StudyRepository
import com.example.model.NoteData
import com.example.model.PreparationMode
import com.example.model.StudyPlan
import com.example.model.SubjectData
import com.example.model.SyllabusData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubjectDeletionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: StudyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudyRepository(
            syllabusDao = db.syllabusDao(),
            noteDao = db.noteDao(),
            questionDao = db.questionDao(),
            mockTestDao = db.mockTestDao(),
            studyPlanDao = db.studyPlanDao(),
            subjectDao = db.subjectDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun test1_deleteSubtopic_onlySubtopicRemoved_subjectAndOthersRemain() = runBlocking {
        val mode = PreparationMode.ACADEMIC
        val userId = "test_user@example.com"
        val subject = SubjectData(
            id = "sub_os",
            name = "Operating Systems",
            importance = "High",
            difficulty = "Medium",
            availableStudyMinutesPerDay = 60,
            mode = mode,
            userId = userId
        )
        repository.saveSubject(subject)

        val topic1 = SyllabusData("top_1", "Operating Systems", "Processes", "Core", mode, true, userId)
        val topic2 = SyllabusData("top_2", "Operating Systems", "Threads", "Core", mode, false, userId)
        val topic3 = SyllabusData("top_3", "Operating Systems", "Deadlock", "Core", mode, false, userId)
        repository.addSyllabusTopic(topic1)
        repository.addSyllabusTopic(topic2)
        repository.addSyllabusTopic(topic3)

        // Delete only Threads
        repository.deleteSyllabusTopic("top_2")

        val remainingTopics = db.syllabusDao().getSyllabusByMode(mode, userId)
        assertEquals(2, remainingTopics.size)
        assertTrue(remainingTopics.any { it.topic == "Processes" })
        assertTrue(remainingTopics.any { it.topic == "Deadlock" })
        assertFalse("Threads must be removed", remainingTopics.any { it.topic == "Threads" })

        // Subject itself must remain
        val subjects = db.subjectDao().getSubjectsByMode(mode, userId)
        assertEquals(1, subjects.size)
        assertEquals("Operating Systems", subjects[0].name)

        // Progress recalculation: 1 completed out of 2 remaining = 50%
        val completed = remainingTopics.count { it.isCompleted }
        val total = remainingTopics.size
        val progress = completed.toFloat() / total.toFloat()
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun test2_deleteSubject_removesSubjectAndAllSubtopicsAndNotes() = runBlocking {
        val mode = PreparationMode.ACADEMIC
        val userId = "test_user@example.com"
        val subject = SubjectData(
            id = "sub_cn",
            name = "Computer Networks",
            importance = "High",
            difficulty = "Hard",
            availableStudyMinutesPerDay = 90,
            mode = mode,
            userId = userId
        )
        repository.saveSubject(subject)

        val topic1 = SyllabusData("top_cn1", "Computer Networks", "Routing", "Network Layer", mode, false, userId)
        val topic2 = SyllabusData("top_cn2", "Computer Networks", "TCP/IP", "Transport Layer", mode, true, userId)
        repository.addSyllabusTopic(topic1)
        repository.addSyllabusTopic(topic2)

        val note = NoteData("n_cn1", "Computer Networks", "Routing", "Subnetting", "Note text", mode)
        repository.addNote(note)

        // Cascade delete Computer Networks
        repository.deleteSubject("sub_cn")
        repository.deleteSyllabusTopicsBySubjectName("Computer Networks", userId, mode)
        repository.deleteNotesBySubject("Computer Networks", mode)

        val subjects = db.subjectDao().getSubjectsByMode(mode, userId)
        assertTrue("Subject list must be empty", subjects.isEmpty())

        val topics = db.syllabusDao().getSyllabusByMode(mode, userId)
        assertTrue("Subtopics must be deleted", topics.isEmpty())

        val notes = db.noteDao().getNotesBySubject(mode, "Computer Networks")
        assertTrue("Notes must be deleted", notes.isEmpty())
    }

    @Test
    fun test3_plannerCleanup_removesSubjectTasksWithoutOrphans() = runBlocking {
        val mode = PreparationMode.ACADEMIC
        val userId = "test_user@example.com"

        val initialPlan = StudyPlan(
            mode = mode,
            dailyHours = 4,
            targetExam = "Academic 2026",
            dailyGoals = listOf(
                "Operating Systems: CPU Scheduling",
                "Data Structures: Binary Trees",
                "Operating Systems: Semaphores"
            ),
            weeklySchedule = listOf(
                "Mon: Data Structures, Operating Systems",
                "Tue: Operating Systems",
                "Wed: Data Structures"
            ),
            completedGoals = listOf("Operating Systems: CPU Scheduling"),
            missedGoals = listOf("Operating Systems: Semaphores")
        )
        repository.saveStudyPlan(initialPlan)

        val subjectName = "Operating Systems"
        val remainingSubjects = listOf(
            SubjectData(
                id = "sub_ds",
                name = "Data Structures",
                importance = "High",
                difficulty = "Medium",
                availableStudyMinutesPerDay = 60,
                mode = mode,
                userId = userId
            )
        )

        val updatedGoals = initialPlan.dailyGoals.filterNot { it.contains(subjectName, ignoreCase = true) }
        val updatedCompleted = initialPlan.completedGoals.filterNot { it.contains(subjectName, ignoreCase = true) }
        val updatedMissed = initialPlan.missedGoals.filterNot { it.contains(subjectName, ignoreCase = true) }

        val fallbackSubj = remainingSubjects.firstOrNull()
        val updatedSchedule = initialPlan.weeklySchedule.mapNotNull { item ->
            if (!item.contains(subjectName, ignoreCase = true)) {
                item
            } else {
                val colonIdx = item.indexOf(':')
                if (colonIdx != -1) {
                    val prefix = item.substring(0, colonIdx).trim()
                    val rest = item.substring(colonIdx + 1).trim()
                    val separators = listOf(",", ";", "|")
                    val sep = separators.firstOrNull { rest.contains(it) }
                    if (sep != null) {
                        val parts = rest.split(sep)
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !it.contains(subjectName, ignoreCase = true) }
                        if (parts.isNotEmpty()) {
                            "$prefix: " + parts.joinToString(", ")
                        } else if (fallbackSubj != null) {
                            "$prefix: Intensive Study on ${fallbackSubj.name} (${fallbackSubj.availableStudyMinutesPerDay} mins)"
                        } else null
                    } else if (fallbackSubj != null) {
                        "$prefix: Intensive Study on ${fallbackSubj.name} (${fallbackSubj.availableStudyMinutesPerDay} mins)"
                    } else null
                } else null
            }
        }

        val cleanedPlan = initialPlan.copy(
            dailyGoals = updatedGoals,
            weeklySchedule = updatedSchedule,
            completedGoals = updatedCompleted,
            missedGoals = updatedMissed
        )
        repository.saveStudyPlan(cleanedPlan)

        val savedPlan = db.studyPlanDao().getStudyPlan(mode)!!
        assertEquals(1, savedPlan.dailyGoals.size)
        assertEquals("Data Structures: Binary Trees", savedPlan.dailyGoals[0])

        assertTrue("Completed goals should not contain OS", savedPlan.completedGoals.isEmpty())
        assertTrue("Missed goals should not contain OS", savedPlan.missedGoals.isEmpty())

        assertTrue(savedPlan.weeklySchedule[0].contains("Data Structures"))
        assertFalse(savedPlan.weeklySchedule[0].contains("Operating Systems"))

        assertTrue(savedPlan.weeklySchedule[1].contains("Data Structures"))
        assertFalse(savedPlan.weeklySchedule[1].contains("Operating Systems"))
    }

    @Test
    fun test4_deleteSubject_otherSubjectsRemainCompletelyIntact() = runBlocking {
        val mode = PreparationMode.ACADEMIC
        val userId = "test_user@example.com"

        val subA = SubjectData("s_a", "Subject A", "High", "Easy", 60, mode = mode, userId = userId)
        val subB = SubjectData("s_b", "Subject B", "Medium", "Medium", 60, mode = mode, userId = userId)
        val subC = SubjectData("s_c", "Subject C", "Low", "Hard", 60, mode = mode, userId = userId)
        repository.saveSubject(subA)
        repository.saveSubject(subB)
        repository.saveSubject(subC)

        val topA = SyllabusData("top_a", "Subject A", "Topic A", "sub", mode, false, userId)
        val topB = SyllabusData("top_b", "Subject B", "Topic B", "sub", mode, false, userId)
        val topC = SyllabusData("top_c", "Subject C", "Topic C", "sub", mode, false, userId)
        repository.addSyllabusTopic(topA)
        repository.addSyllabusTopic(topB)
        repository.addSyllabusTopic(topC)

        repository.deleteSubject("s_a")
        repository.deleteSyllabusTopicsBySubjectName("Subject A", userId, mode)

        val remainingSubjects = db.subjectDao().getSubjectsByMode(mode, userId)
        assertEquals(2, remainingSubjects.size)
        assertTrue(remainingSubjects.any { it.name == "Subject B" })
        assertTrue(remainingSubjects.any { it.name == "Subject C" })
        assertFalse(remainingSubjects.any { it.name == "Subject A" })

        val remainingTopics = db.syllabusDao().getSyllabusByMode(mode, userId)
        assertEquals(2, remainingTopics.size)
        assertTrue(remainingTopics.any { it.subject == "Subject B" })
        assertTrue(remainingTopics.any { it.subject == "Subject C" })
        assertFalse(remainingTopics.any { it.subject == "Subject A" })
    }

    @Test
    fun test5_progressRecalculation_afterSubjectDeletion() = runBlocking {
        val mode = PreparationMode.ACADEMIC
        val userId = "test_user@example.com"

        // Math: 4/5 completed = 80%
        for (i in 1..4) {
            repository.addSyllabusTopic(SyllabusData("m_$i", "Mathematics", "MTopic$i", "sub", mode, true, userId))
        }
        repository.addSyllabusTopic(SyllabusData("m_5", "Mathematics", "MTopic5", "sub", mode, false, userId))

        // OS: 3/5 completed = 60%
        for (i in 1..3) {
            repository.addSyllabusTopic(SyllabusData("os_$i", "Operating Systems", "OSTopic$i", "sub", mode, true, userId))
        }
        for (i in 4..5) {
            repository.addSyllabusTopic(SyllabusData("os_$i", "Operating Systems", "OSTopic$i", "sub", mode, false, userId))
        }

        // CN: 2/5 completed = 40%
        for (i in 1..2) {
            repository.addSyllabusTopic(SyllabusData("cn_$i", "Computer Networks", "CNTopic$i", "sub", mode, true, userId))
        }
        for (i in 3..5) {
            repository.addSyllabusTopic(SyllabusData("cn_$i", "Computer Networks", "CNTopic$i", "sub", mode, false, userId))
        }

        val (initialDone, initialTotal) = repository.getSyllabusProgress(mode, userId)
        assertEquals(15, initialTotal)
        assertEquals(9, initialDone)

        repository.deleteSyllabusTopicsBySubjectName("Operating Systems", userId, mode)

        val (finalDone, finalTotal) = repository.getSyllabusProgress(mode, userId)
        assertEquals(10, finalTotal)
        assertEquals(6, finalDone)

        val topics = db.syllabusDao().getSyllabusByMode(mode, userId)
        val subjectMap = topics.groupBy { it.subject }
        assertFalse("Operating Systems should not be in subject breakdown", subjectMap.containsKey("Operating Systems"))
        assertTrue(subjectMap.containsKey("Mathematics"))
        assertTrue(subjectMap.containsKey("Computer Networks"))
    }

    @Test
    fun test6_academicGateIsolation_sameSubjectName_deletingAcademicDoesNotAffectGate() = runBlocking {
        val userId = "common_user@example.com"
        val subjectName = "Operating Systems"

        // 1. Academic Operating Systems
        val acadSub = SubjectData("acad_os", subjectName, "High", "Hard", 60, mode = PreparationMode.ACADEMIC, userId = userId)
        repository.saveSubject(acadSub)
        val acadTopic = SyllabusData("acad_top_os1", subjectName, "Processes", "sub", PreparationMode.ACADEMIC, true, userId)
        repository.addSyllabusTopic(acadTopic)
        val acadNote = NoteData("acad_note_os", subjectName, "Processes", "sub", "Academic note", PreparationMode.ACADEMIC)
        repository.addNote(acadNote)

        // 2. GATE Operating Systems
        val gateSub = SubjectData("gate_os", subjectName, "High", "Hard", 120, mode = PreparationMode.PROFESSIONAL_GATE, userId = userId)
        repository.saveSubject(gateSub)
        val gateTopic = SyllabusData("gate_top_os1", subjectName, "Virtual Memory", "sub", PreparationMode.PROFESSIONAL_GATE, false, userId)
        repository.addSyllabusTopic(gateTopic)
        val gateNote = NoteData("gate_note_os", subjectName, "Virtual Memory", "sub", "GATE note", PreparationMode.PROFESSIONAL_GATE)
        repository.addNote(gateNote)

        // Delete Academic Operating Systems ONLY
        repository.deleteSubject("acad_os")
        repository.deleteSyllabusTopicsBySubjectName(subjectName, userId, PreparationMode.ACADEMIC)
        repository.deleteNotesBySubject(subjectName, PreparationMode.ACADEMIC)

        // Verify Academic is gone
        val acadSubjects = db.subjectDao().getSubjectsByMode(PreparationMode.ACADEMIC, userId)
        assertTrue("Academic subject must be deleted", acadSubjects.isEmpty())
        val acadTopics = db.syllabusDao().getSyllabusByMode(PreparationMode.ACADEMIC, userId)
        assertTrue("Academic topics must be deleted", acadTopics.isEmpty())
        val acadNotes = db.noteDao().getNotesBySubject(PreparationMode.ACADEMIC, subjectName)
        assertTrue("Academic notes must be deleted", acadNotes.isEmpty())

        // Verify GATE Operating Systems STILL EXISTS completely untouched
        val gateSubjects = db.subjectDao().getSubjectsByMode(PreparationMode.PROFESSIONAL_GATE, userId)
        assertEquals(1, gateSubjects.size)
        assertEquals("gate_os", gateSubjects[0].id)
        assertEquals("Operating Systems", gateSubjects[0].name)

        val gateTopics = db.syllabusDao().getSyllabusByMode(PreparationMode.PROFESSIONAL_GATE, userId)
        assertEquals(1, gateTopics.size)
        assertEquals("Virtual Memory", gateTopics[0].topic)

        val gateNotes = db.noteDao().getNotesBySubject(PreparationMode.PROFESSIONAL_GATE, subjectName)
        assertEquals(1, gateNotes.size)
        assertEquals("gate_note_os", gateNotes[0].id)
    }

    @Test
    fun test7_gateDelete_deletingGateSubject_doesNotAffectAcademicData() = runBlocking {
        val userId = "user@example.com"

        // Academic subject
        val acadSub = SubjectData("acad_db", "Databases", "High", "Medium", 60, mode = PreparationMode.ACADEMIC, userId = userId)
        repository.saveSubject(acadSub)
        val acadTopic = SyllabusData("acad_top_db", "Databases", "SQL", "Queries", PreparationMode.ACADEMIC, false, userId)
        repository.addSyllabusTopic(acadTopic)

        // GATE subject
        val gateSub = SubjectData("gate_db", "Databases", "High", "Hard", 90, mode = PreparationMode.PROFESSIONAL_GATE, userId = userId)
        repository.saveSubject(gateSub)
        val gateTopic = SyllabusData("gate_top_db", "Databases", "B+ Trees", "Storage", PreparationMode.PROFESSIONAL_GATE, false, userId)
        repository.addSyllabusTopic(gateTopic)

        // Delete GATE Databases
        repository.deleteSubject("gate_db")
        repository.deleteSyllabusTopicsBySubjectName("Databases", userId, PreparationMode.PROFESSIONAL_GATE)
        repository.deleteNotesBySubject("Databases", PreparationMode.PROFESSIONAL_GATE)

        // GATE Databases is deleted
        val gateSubjects = db.subjectDao().getSubjectsByMode(PreparationMode.PROFESSIONAL_GATE, userId)
        assertTrue("GATE Databases should be deleted", gateSubjects.isEmpty())

        // Academic Databases is 100% untouched
        val acadSubjects = db.subjectDao().getSubjectsByMode(PreparationMode.ACADEMIC, userId)
        assertEquals(1, acadSubjects.size)
        assertEquals("Databases", acadSubjects[0].name)

        val acadTopics = db.syllabusDao().getSyllabusByMode(PreparationMode.ACADEMIC, userId)
        assertEquals(1, acadTopics.size)
        assertEquals("SQL", acadTopics[0].topic)
    }
}
