package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val syllabusDao: SyllabusDao,
    private val noteDao: NoteDao,
    private val questionDao: QuestionDao,
    private val mockTestDao: MockTestDao,
    private val studyPlanDao: StudyPlanDao,
    private val subjectDao: SubjectDao
) {

    // Subjects
    fun getSubjectsFlow(mode: PreparationMode, userId: String): Flow<List<SubjectData>> =
        subjectDao.getSubjectsByModeFlow(mode, userId)

    suspend fun saveSubject(subject: SubjectData) {
        subjectDao.insertSubject(subject)
    }

    suspend fun deleteSubject(id: String) {
        subjectDao.deleteSubjectById(id)
    }

    // Syllabus
    fun getSyllabusFlow(mode: PreparationMode, userId: String): Flow<List<SyllabusData>> =
        syllabusDao.getSyllabusByModeFlow(mode, userId)

    suspend fun addSyllabusTopic(topic: SyllabusData) {
        syllabusDao.insertTopic(topic)
    }

    suspend fun deleteSyllabusTopic(id: String) {
        syllabusDao.deleteTopicById(id)
    }

    suspend fun deleteSyllabusTopicsBySubjectName(subjectName: String, userId: String, mode: PreparationMode) {
        syllabusDao.deleteTopicsBySubjectName(subjectName, userId, mode)
    }

    suspend fun toggleSyllabusTopic(id: String, isCompleted: Boolean) {
        syllabusDao.updateTopicStatus(id, isCompleted)
    }

    // Notes
    fun getNotesFlow(mode: PreparationMode): Flow<List<NoteData>> =
        noteDao.getNotesByModeFlow(mode)

    suspend fun toggleNoteCompleted(id: String, isCompleted: Boolean) {
        noteDao.updateNoteStatus(id, isCompleted)
    }

    suspend fun addNote(note: NoteData) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    suspend fun deleteNotesBySubject(subject: String, mode: PreparationMode) {
        noteDao.deleteNotesBySubject(subject, mode)
    }

    suspend fun deleteNotesByTopic(topic: String, mode: PreparationMode) {
        noteDao.deleteNotesByTopic(topic, mode)
    }

    // Questions / PYQ
    suspend fun getPyqYears(mode: PreparationMode, userId: String): List<String> {
        val years = questionDao.getPyqYears(mode, userId).toMutableList()
        if (!years.contains("All")) years.add(0, "All")
        return years
    }

    suspend fun getSubjects(mode: PreparationMode, userId: String): List<String> {
        val subjects = questionDao.getSubjects(mode, userId).toMutableList()
        if (!subjects.contains("All")) subjects.add(0, "All")
        return subjects
    }

    suspend fun getFilteredPyqs(mode: PreparationMode, subject: String, year: String, userId: String): List<QuestionData> =
        questionDao.getFilteredPyqs(mode, subject, year, userId)

    suspend fun getPracticeQuestions(mode: PreparationMode, subject: String, userId: String): List<QuestionData> =
        questionDao.getPracticeQuestions(mode, subject, userId)

    suspend fun insertQuestions(questions: List<QuestionData>) {
        questionDao.insertAll(questions)
    }

    // Mock Tests
    fun getMockTestsFlow(mode: PreparationMode): Flow<List<MockTest>> =
        mockTestDao.getMockTestsByModeFlow(mode)

    suspend fun getGateMockTestQuestions(
        context: android.content.Context,
        subject: String,
        subtopics: List<String>
    ): List<QuestionData> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allGateQuestions = CsvQuestionParser.parseAllGateQuestions(context)
        
        val filteredBySubject = if (subject == "All" || subject == "All Subjects" || subject == "Full Syllabus") {
            allGateQuestions
        } else {
            allGateQuestions.filter { it.subject.trim().equals(subject.trim(), ignoreCase = true) }
        }
        
        if (subtopics.isNotEmpty()) {
            val matchingSubtopics = filteredBySubject.filter { q ->
                subtopics.any { st ->
                    val stClean = st.trim()
                    q.topic.contains(stClean, ignoreCase = true) ||
                    stClean.contains(q.topic.trim(), ignoreCase = true) ||
                    q.question.contains(stClean, ignoreCase = true) ||
                    stClean.split(":", "-", ",").any { part ->
                        val p = part.trim()
                        p.isNotBlank() && (q.topic.contains(p, ignoreCase = true) || q.question.contains(p, ignoreCase = true))
                    }
                }
            }
            if (matchingSubtopics.isNotEmpty()) matchingSubtopics else filteredBySubject
        } else {
            filteredBySubject
        }
    }

    suspend fun generateMockTest(
        mode: PreparationMode,
        userId: String,
        department: String,
        subject: String,
        subtopics: List<String>,
        count: Int,
        difficulty: String
    ): Pair<MockTest, List<QuestionData>> {
        val userSubjects = subjectDao.getSubjectsByMode(mode, userId).map { it.name.lowercase().trim() }
        
        val rawQuestions = questionDao.getPracticeQuestions(mode, subject, userId)

        // Strict mode and user subject filter
        val filteredQuestions = rawQuestions.filter { q ->
            val modeMatch = q.mode == mode
            val subjectMatch = if (subject == "All" || subject == "All Subjects" || subject == "Full Syllabus") {
                userSubjects.contains(q.subject.lowercase().trim()) || q.mode == mode
            } else {
                q.subject.trim().equals(subject.trim(), ignoreCase = true)
            }
            modeMatch && subjectMatch
        }

        val filteredBySubtopics = if (subtopics.isNotEmpty()) {
            filteredQuestions.filter { q ->
                subtopics.any { st ->
                    val stClean = st.trim()
                    q.topic.contains(stClean, ignoreCase = true) ||
                    stClean.contains(q.topic.trim(), ignoreCase = true) ||
                    q.question.contains(stClean, ignoreCase = true) ||
                    stClean.split(":", "-", ",").any { part ->
                        val p = part.trim()
                        p.isNotBlank() && (q.topic.contains(p, ignoreCase = true) || q.question.contains(p, ignoreCase = true))
                    }
                }
            }
        } else {
            filteredQuestions
        }

        val validQuestions = filteredBySubtopics
            .filter { it.question.isNotBlank() }
            .distinctBy { it.question.trim() } // Unique questions

        val selectedQuestions = validQuestions
            .shuffled()
            .take(count)

        val testName = if (mode == PreparationMode.PROFESSIONAL_GATE) "GATE Mock Test - $subject" else "Academic Quiz - $subject"

        val test = MockTest(
            id = "mt_${System.currentTimeMillis()}",
            testName = testName,
            mode = mode,
            department = department,
            selectedSubject = subject,
            totalQuestions = selectedQuestions.size,
            difficulty = difficulty,
            unansweredCount = selectedQuestions.size
        )
        return Pair(test, selectedQuestions)
    }

    suspend fun saveMockTestResult(mockTest: MockTest) {
        mockTestDao.insertMockTest(mockTest)
    }

    // Study Plan
    fun getStudyPlanFlow(mode: PreparationMode): Flow<StudyPlan?> =
        studyPlanDao.getStudyPlanFlow(mode)

    suspend fun saveStudyPlan(studyPlan: StudyPlan) {
        studyPlanDao.insertOrUpdate(studyPlan)
    }

    // Progress stats
    suspend fun getSyllabusProgress(mode: PreparationMode, userId: String): Pair<Int, Int> {
        val total = syllabusDao.getTotalCount(mode, userId)
        val completed = syllabusDao.getCompletedCount(mode, userId)
        return Pair(completed, total)
    }

    suspend fun getNotesProgress(mode: PreparationMode): Pair<Int, Int> {
        val total = noteDao.getTotalNotesCount(mode)
        val completed = noteDao.getCompletedNotesCount(mode)
        return Pair(completed, total)
    }
}
