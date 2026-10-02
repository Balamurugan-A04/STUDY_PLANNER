package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiTutorRepository
import com.example.data.AppDatabase
import com.example.data.AuthRepository
import com.example.data.StudyRepository
import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object AcademicRegister : Screen()
    object GateRegister : Screen()
    object Dashboard : Screen()
    object Syllabus : Screen()
    object Notes : Screen()
    object Pyq : Screen()
    object Practice : Screen()
    object MockTestConfig : Screen()
    data class MockTestActive(val subject: String, val questionCount: Int, val durationMinutes: Int, val questions: List<QuestionData>) : Screen()
    data class MockTestResult(val questions: List<QuestionData>, val userAnswers: Map<Int, Int>, val timeTakenSeconds: Int, val userNatAnswers: Map<Int, String> = emptyMap()) : Screen()
    object StudyPlanRoute : Screen()
    object Progress : Screen()
    object AiTutor : Screen()
    object Profile : Screen()
    data class SubjectDetail(val subjectId: String) : Screen()
    object SubjectSetup : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val authRepo = AuthRepository(db.userDao(), FirebaseAuth.getInstance())
    val studyRepo = StudyRepository(db.syllabusDao(), db.noteDao(), db.questionDao(), db.mockTestDao(), db.studyPlanDao(), db.subjectDao())
    val aiTutorRepo = AiTutorRepository()
    val academicAiService = com.example.data.AcademicAiService()
    val gateAiService = com.example.data.GateAiService()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _syllabusList = MutableStateFlow<List<SyllabusData>>(emptyList())
    val syllabusList: StateFlow<List<SyllabusData>> = _syllabusList.asStateFlow()

    private val _subjectDataList = MutableStateFlow<List<SubjectData>>(emptyList())
    val subjectDataList: StateFlow<List<SubjectData>> = _subjectDataList.asStateFlow()

    private val _notesList = MutableStateFlow<List<NoteData>>(emptyList())
    val notesList: StateFlow<List<NoteData>> = _notesList.asStateFlow()

    private val _pyqYears = MutableStateFlow<List<String>>(listOf("All"))
    val pyqYears: StateFlow<List<String>> = _pyqYears.asStateFlow()

    private val _subjects = MutableStateFlow<List<String>>(listOf("All"))
    val subjects: StateFlow<List<String>> = _subjects.asStateFlow()

    private val _filteredQuestions = MutableStateFlow<List<QuestionData>>(emptyList())
    val filteredQuestions: StateFlow<List<QuestionData>> = _filteredQuestions.asStateFlow()

    private val _testHistory = MutableStateFlow<List<MockTest>>(emptyList())
    val testHistory: StateFlow<List<MockTest>> = _testHistory.asStateFlow()

    private val _availableQuestionCount = MutableStateFlow(0)
    val availableQuestionCount: StateFlow<Int> = _availableQuestionCount.asStateFlow()

    fun updateAvailableQuestionCount(subject: String, subtopics: List<String>) {
        viewModelScope.launch {
            val user = _currentUser.value
            val mode = user?.mode ?: PreparationMode.ACADEMIC
            val userId = user?.email ?: ""
            
            if (mode == PreparationMode.PROFESSIONAL_GATE) {
                val questions = studyRepo.getGateMockTestQuestions(getApplication(), subject, subtopics)
                _availableQuestionCount.value = questions.distinctBy { it.id }.size
            } else {
                val userSubjects = db.subjectDao().getSubjectsByMode(mode, userId).map { it.name.lowercase().trim() }
                val questions = studyRepo.getPracticeQuestions(mode, subject, userId)
                
                val filteredQuestions = questions.filter { q ->
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
                
                _availableQuestionCount.value = filteredBySubtopics
                    .filter { it.question.isNotBlank() }
                    .distinctBy { it.question.trim() }
                    .size
            }
        }
    }

    private val _studyPlan = MutableStateFlow<StudyPlan?>(null)
    val studyPlan: StateFlow<StudyPlan?> = _studyPlan.asStateFlow()

    init {
        viewModelScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            var active = authRepo.getCurrentUser()

            if (firebaseUser != null) {
                if (active == null || active.email != firebaseUser.email) {
                    active = authRepo.authRepoGetUserByEmail(firebaseUser.email ?: "")
                }

                if (active != null) {
                    _currentUser.value = active
                    seedSampleDataIfNeeded()
                    refreshModeData(active.mode)
                    
                    val subjectsCount = db.subjectDao().getUserSpecificCount(active.mode, active.email)
                    if (subjectsCount == 0) {
                        _currentScreen.value = Screen.SubjectSetup
                    } else {
                        _currentScreen.value = Screen.Dashboard
                    }
                } else {
                    FirebaseAuth.getInstance().signOut()
                    _currentScreen.value = Screen.Login
                }
            } else {
                _currentScreen.value = Screen.Login
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    private suspend fun loadDataForMode(mode: PreparationMode) {
        val userId = _currentUser.value?.email ?: ""
        viewModelScope.launch { studyRepo.getSyllabusFlow(mode, userId).collect { _syllabusList.value = it } }
        viewModelScope.launch {
            studyRepo.getSubjectsFlow(mode, userId).collect { list ->
                _subjectDataList.value = list
                _subjects.value = (listOf("All") + list.map { it.name }).distinct()
            }
        }
    }

    private var dataJobs = mutableListOf<kotlinx.coroutines.Job>()

    fun refreshModeData(mode: PreparationMode) {
        dataJobs.forEach { it.cancel() }
        dataJobs.clear()
        val userId = _currentUser.value?.email ?: ""
        _studyPlan.value = null

        val j1 = viewModelScope.launch {
            _pyqYears.value = studyRepo.getPyqYears(mode, userId)
            _filteredQuestions.value = studyRepo.getFilteredPyqs(mode, "All", "All", userId)

            studyRepo.getSyllabusFlow(mode, userId).collect { list ->
                _syllabusList.value = list.filter { it.mode == mode }
            }
        }
        val j2 = viewModelScope.launch {
            studyRepo.getSubjectsFlow(mode, userId).collect { list ->
                val filtered = list.filter { it.mode == mode }
                _subjectDataList.value = filtered
                _subjects.value = (listOf("All") + filtered.map { it.name }).distinct()
            }
        }
        val j3 = viewModelScope.launch {
            studyRepo.getNotesFlow(mode).collect { list ->
                _notesList.value = list.filter { it.mode == mode }
            }
        }
        val j4 = viewModelScope.launch {
            studyRepo.getMockTestsFlow(mode).collect { list ->
                _testHistory.value = list.filter { it.mode == mode }
            }
        }
        val j5 = viewModelScope.launch {
            studyRepo.getStudyPlanFlow(mode).collect { plan ->
                if (plan != null && plan.mode == mode) {
                    _studyPlan.value = plan
                } else if (_currentUser.value?.mode == mode && plan == null) {
                    _studyPlan.value = null
                }
            }
        }
        dataJobs.addAll(listOf(j1, j2, j3, j4, j5))
    }

    fun addSubject(
        name: String,
        importance: String,
        difficulty: String,
        availableStudyMinutesPerDay: Int,
        examDate: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val mode = user?.mode ?: PreparationMode.ACADEMIC
            val userId = user?.email ?: ""
            
            // Prevent adding GATE subjects manually if not allowed
            if (mode == PreparationMode.PROFESSIONAL_GATE) return@launch

            val newSubj = SubjectData(
                id = "subj_${System.currentTimeMillis()}",
                name = name,
                importance = importance,
                difficulty = difficulty,
                availableStudyMinutesPerDay = availableStudyMinutesPerDay,
                examDate = examDate,
                progress = 0f,
                isUserCreated = true,
                mode = mode,
                userId = userId
            )
            studyRepo.saveSubject(newSubj)

            val newTopic = SyllabusData(
                id = "syl_${newSubj.id}",
                subject = name,
                topic = "$name - Core Overview",
                subtopic = "Importance: $importance | Difficulty: $difficulty | Exam: $examDate",
                mode = mode,
                isCompleted = false,
                userId = userId
            )
            studyRepo.addSyllabusTopic(newTopic)
            refreshModeData(mode)
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val targetSubj = _subjectDataList.value.find { it.id == subjectId }
            val mode = targetSubj?.mode ?: (user?.mode ?: PreparationMode.ACADEMIC)
            val userId = user?.email ?: ""
            val subjectName = targetSubj?.name ?: ""

            studyRepo.deleteSubject(subjectId)
            studyRepo.deleteSyllabusTopic("syl_$subjectId")
            if (subjectName.isNotEmpty()) {
                studyRepo.deleteSyllabusTopicsBySubjectName(subjectName, userId, mode)
                studyRepo.deleteNotesBySubject(subjectName, mode)
            }

            val remainingSubjects = _subjectDataList.value.filter { it.id != subjectId && it.mode == mode }
            val plan = _studyPlan.value

            if (remainingSubjects.isEmpty()) {
                val emptyPlan = StudyPlan(
                    mode = mode,
                    dailyHours = 0,
                    targetExam = if (mode == PreparationMode.PROFESSIONAL_GATE) "GATE CS 2026" else "Academic Preparation",
                    dailyGoals = emptyList(),
                    weeklySchedule = emptyList(),
                    completedGoals = emptyList(),
                    missedGoals = emptyList()
                )
                studyRepo.saveStudyPlan(emptyPlan)
                _studyPlan.value = emptyPlan
            } else if (plan != null && subjectName.isNotEmpty()) {
                val updatedGoals = plan.dailyGoals.filterNot { goal ->
                    goal.contains(subjectName, ignoreCase = true)
                }
                val updatedCompleted = plan.completedGoals.filterNot { it.contains(subjectName, ignoreCase = true) }
                val updatedMissed = plan.missedGoals.filterNot { it.contains(subjectName, ignoreCase = true) }

                val fallbackSubj = remainingSubjects.firstOrNull()
                val updatedSchedule = plan.weeklySchedule.mapNotNull { item ->
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
                                    "$prefix: ${parts.joinToString(", ")}"
                                } else if (fallbackSubj != null) {
                                    "$prefix: Intensive Study on ${fallbackSubj.name} (${fallbackSubj.availableStudyMinutesPerDay} mins)"
                                } else null
                            } else if (fallbackSubj != null) {
                                "$prefix: Intensive Study on ${fallbackSubj.name} (${fallbackSubj.availableStudyMinutesPerDay} mins)"
                            } else null
                        } else null
                    }
                }

                val finalGoals = if (updatedGoals.isNotEmpty()) {
                    updatedGoals
                } else {
                    remainingSubjects.mapIndexed { idx, subj ->
                        val priorityLabel = when (idx) {
                            0 -> "Top Priority (Rank 1)"
                            1 -> "High Priority (Rank 2)"
                            2 -> "Core Priority (Rank 3)"
                            else -> "Standard Priority (Rank ${idx + 1})"
                        }
                        "${subj.name}: $priorityLabel [${subj.importance} Importance, ${subj.difficulty} Difficulty, ${subj.availableStudyMinutesPerDay} mins/day]"
                    }
                }

                val updatedPlan = plan.copy(
                    dailyGoals = finalGoals,
                    weeklySchedule = updatedSchedule,
                    completedGoals = updatedCompleted,
                    missedGoals = updatedMissed
                )
                studyRepo.saveStudyPlan(updatedPlan)
                _studyPlan.value = updatedPlan
            }

            refreshModeData(mode)
        }
    }

    fun generateStudyPlan() {
        viewModelScope.launch {
            val user = _currentUser.value
            val mode = user?.mode ?: PreparationMode.ACADEMIC
            val userId = user?.email ?: ""
            val activeSubjects = _subjectDataList.value.filter { it.mode == mode }

            if (activeSubjects.isEmpty()) {
                val emptyPlan = StudyPlan(
                    mode = mode,
                    dailyHours = 0,
                    targetExam = if (mode == PreparationMode.PROFESSIONAL_GATE) "GATE CS 2026" else "Academic Preparation",
                    dailyGoals = emptyList(),
                    weeklySchedule = emptyList()
                )
                studyRepo.saveStudyPlan(emptyPlan)
                _studyPlan.value = emptyPlan
                return@launch
            }

            if (mode == PreparationMode.PROFESSIONAL_GATE) {
                val planResult = gateAiService.generateGateStudySchedule(activeSubjects, _syllabusList.value, userId)
                val generatedPlan = planResult.getOrNull() ?: StudyPlan(
                    mode = PreparationMode.PROFESSIONAL_GATE,
                    dailyHours = 4,
                    targetExam = "GATE CS 2026",
                    dailyGoals = listOf("Algorithms - Dynamic Programming", "Practice 10 PYQs on OS", "Review Digital Logic notes"),
                    weeklySchedule = listOf("Mon: Algorithms", "Tue: OS", "Wed: Computer Networks", "Thu: DBMS", "Fri: Theory of Comp", "Sat: Mock Test", "Sun: Revision"),
                    completedGoals = emptyList(),
                    missedGoals = emptyList()
                )
                studyRepo.saveStudyPlan(generatedPlan)
                _studyPlan.value = generatedPlan
                return@launch
            } else {
                val planResult = academicAiService.generateAcademicStudySchedule(activeSubjects, _syllabusList.value, userId)
                val generatedPlan = planResult.getOrNull() ?: StudyPlan(
                    mode = PreparationMode.ACADEMIC,
                    dailyHours = 3,
                    targetExam = "Academic Preparation",
                    dailyGoals = listOf("Complete Core Modules", "Review Class Notes", "Prepare Chapter Summaries"),
                    weeklySchedule = listOf("Mon: Core Subject", "Tue: Secondary Subject", "Wed: Laboratory Work", "Thu: Tutorial", "Fri: Assignments", "Sat: Revision", "Sun: Weekly Review"),
                    completedGoals = emptyList(),
                    missedGoals = emptyList()
                )
                studyRepo.saveStudyPlan(generatedPlan)
                _studyPlan.value = generatedPlan
                return@launch
            }
        }
    }

    fun toggleGoalCompletion(goalText: String) {
        viewModelScope.launch {
            val currentPlan = _studyPlan.value ?: return@launch
            val currentCompleted = currentPlan.completedGoals.toMutableList()
            if (currentCompleted.contains(goalText)) {
                currentCompleted.remove(goalText)
            } else {
                currentCompleted.add(goalText)
            }
            val updated = currentPlan.copy(completedGoals = currentCompleted)
            studyRepo.saveStudyPlan(updated)
            _studyPlan.value = updated
        }
    }

    fun rescheduleMissedTasks() {
        viewModelScope.launch {
            val user = _currentUser.value
            val mode = user?.mode ?: PreparationMode.ACADEMIC
            val userId = user?.email ?: ""

            val currentPlan = _studyPlan.value ?: return@launch
            val activeSubjects = _subjectDataList.value.filter { it.mode == mode }
            val res = if (mode == PreparationMode.PROFESSIONAL_GATE) {
                gateAiService.rescheduleGateMissedTasks(
                    currentPlan = currentPlan,
                    completedGoalsList = currentPlan.completedGoals,
                    subjects = activeSubjects,
                    syllabus = _syllabusList.value,
                    userId = userId
                )
            } else {
                academicAiService.rescheduleAcademicMissedTasks(
                    currentPlan = currentPlan,
                    completedGoalsList = currentPlan.completedGoals,
                    subjects = activeSubjects,
                    syllabus = _syllabusList.value,
                    userId = userId
                )
            }
            val updatedPlan = res.getOrNull() ?: currentPlan
            studyRepo.saveStudyPlan(updatedPlan)
            _studyPlan.value = updatedPlan
        }
    }

    suspend fun login(email: String, password: String, targetMode: PreparationMode, isRemembered: Boolean): Result<UserProfile> {
        val res = authRepo.loginWithPassword(email, password, targetMode, isRemembered)
        if (res.isSuccess) {
            val user = res.getOrNull()!!
            _currentUser.value = user
            seedSampleDataIfNeeded()
            refreshModeData(user.mode)
            
            viewModelScope.launch {
                val subjectsCount = db.subjectDao().getUserSpecificCount(user.mode, user.email)
                if (subjectsCount == 0) {
                    _currentScreen.value = Screen.SubjectSetup
                } else {
                    _currentScreen.value = Screen.Dashboard
                }
            }
        }
        return res
    }

    suspend fun registerAcademic(fullName: String, email: String, password: String, isRemembered: Boolean): Result<Unit> {
        val res = authRepo.registerAcademic(fullName, email, password, isRemembered)
        return if (res.isSuccess) {
            val user = res.getOrNull()!!
            _currentUser.value = user
            seedSampleDataIfNeeded()
            refreshModeData(PreparationMode.ACADEMIC)
            _currentScreen.value = Screen.SubjectSetup
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Registration failed"))
        }
    }

    suspend fun registerGate(
        fullName: String,
        email: String,
        password: String,
        age: Int,
        collegeName: String,
        department: GateDepartment,
        isRemembered: Boolean
    ): Result<Unit> {
        val res = authRepo.registerGate(fullName, email, password, age, collegeName, department, isRemembered)
        return if (res.isSuccess) {
            val user = res.getOrNull()!!
            _currentUser.value = user
            seedSampleDataIfNeeded()
            refreshModeData(PreparationMode.PROFESSIONAL_GATE)
            _currentScreen.value = Screen.Dashboard
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Registration failed"))
        }
    }

    fun switchMode(newMode: PreparationMode) {
        viewModelScope.launch {
            authRepo.updatePreparationMode(newMode)
            val current = _currentUser.value
            if (current != null) {
                _currentUser.value = current.copy(mode = newMode)
            }
            seedSampleDataIfNeeded()
            refreshModeData(newMode)
            _currentScreen.value = Screen.Dashboard
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _currentUser.value = null
            _currentScreen.value = Screen.Login
        }
    }

    fun toggleSyllabusTopic(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            studyRepo.toggleSyllabusTopic(id, isCompleted)
            val currentList = _syllabusList.value
            val updatedList = currentList.map {
                if (it.id == id) it.copy(isCompleted = isCompleted) else it
            }
            _syllabusList.value = updatedList

            val targetTopic = updatedList.find { it.id == id }
            if (targetTopic != null) {
                updateSubjectProgress(targetTopic.subject, targetTopic.mode, updatedList)
            }
        }
    }

    private suspend fun updateSubjectProgress(subjectName: String, mode: PreparationMode, currentList: List<SyllabusData> = _syllabusList.value) {
        val subjectTopics = currentList.filter { it.subject.equals(subjectName, ignoreCase = true) && it.mode == mode }
        val targetSubject = _subjectDataList.value.find { it.name.equals(subjectName, ignoreCase = true) && it.mode == mode }
        if (targetSubject != null) {
            val progress = if (subjectTopics.isNotEmpty()) {
                val completed = subjectTopics.count { it.isCompleted }
                val total = subjectTopics.size
                completed.toFloat() / total.toFloat()
            } else {
                0f
            }
            db.subjectDao().updateProgress(targetSubject.id, progress)
            _subjectDataList.value = _subjectDataList.value.map {
                if (it.id == targetSubject.id) it.copy(progress = progress) else it
            }
        }
    }

    fun addSubtopic(subjectId: String, subtopicName: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val subject = _subjectDataList.value.find { it.id == subjectId } ?: return@launch
            val mode = subject.mode
            val userId = user?.email ?: ""

            val newTopic = SyllabusData(
                id = "sub_${System.currentTimeMillis()}",
                subject = subject.name,
                topic = subtopicName,
                subtopic = "Manual entry",
                mode = mode,
                isCompleted = false,
                userId = userId
            )
            studyRepo.addSyllabusTopic(newTopic)
            refreshModeData(mode)
            val updatedTopics = _syllabusList.value + newTopic
            updateSubjectProgress(subject.name, mode, updatedTopics)
        }
    }

    fun deleteSubtopic(subtopicId: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val targetTopic = _syllabusList.value.find { it.id == subtopicId }
            val mode = targetTopic?.mode ?: (user?.mode ?: PreparationMode.ACADEMIC)
            val subjectName = targetTopic?.subject ?: ""
            val topicName = targetTopic?.topic ?: ""

            studyRepo.deleteSyllabusTopic(subtopicId)
            if (topicName.isNotBlank()) {
                studyRepo.deleteNotesByTopic(topicName, mode)
            }

            val plan = _studyPlan.value
            if (plan != null && topicName.isNotBlank()) {
                val updatedGoals = plan.dailyGoals.mapNotNull { goal ->
                    if (!goal.contains(topicName, ignoreCase = true)) {
                        goal
                    } else {
                        if (goal.contains("| Topics:", ignoreCase = true)) {
                            val parts = goal.split("| Topics:")
                            val remainingTopics = parts[1].split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() && !it.contains(topicName, ignoreCase = true) }
                            if (remainingTopics.isNotEmpty()) {
                                "${parts[0].trim()} | Topics: ${remainingTopics.joinToString(", ")}"
                            } else {
                                parts[0].trim()
                            }
                        } else {
                            null
                        }
                    }
                }
                val updatedCompleted = plan.completedGoals.filterNot { it.contains(topicName, ignoreCase = true) }
                val updatedMissed = plan.missedGoals.filterNot { it.contains(topicName, ignoreCase = true) }
                val updatedPlan = plan.copy(
                    dailyGoals = updatedGoals,
                    completedGoals = updatedCompleted,
                    missedGoals = updatedMissed
                )
                studyRepo.saveStudyPlan(updatedPlan)
                _studyPlan.value = updatedPlan
            }

            refreshModeData(mode)
            if (subjectName.isNotBlank()) {
                val remainingTopics = _syllabusList.value.filter { it.id != subtopicId }
                updateSubjectProgress(subjectName, mode, remainingTopics)
            }
        }
    }

    fun toggleNoteCompleted(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            studyRepo.toggleNoteCompleted(id, isCompleted)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            studyRepo.deleteNote(id)
            val mode = _currentUser.value?.mode ?: PreparationMode.ACADEMIC
            refreshModeData(mode)
        }
    }

    fun addFileNote(uri: android.net.Uri, fileName: String, fileType: String) {
        viewModelScope.launch {
            val mode = _currentUser.value?.mode ?: PreparationMode.ACADEMIC
            val newNote = NoteData(
                id = "note_file_${System.currentTimeMillis()}",
                subject = "External Material",
                topic = fileName,
                subtopic = "Added from local storage",
                content = "File URI: $uri",
                mode = mode,
                isCompleted = false,
                fileUri = uri.toString(),
                fileName = fileName,
                fileType = fileType
            )
            studyRepo.addNote(newNote)
            refreshModeData(mode)
        }
    }

    fun filterPyqs(subject: String, year: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val mode = user?.mode ?: PreparationMode.PROFESSIONAL_GATE
            val userId = user?.email ?: ""
            var list = studyRepo.getFilteredPyqs(mode, subject, year, userId)
            if (mode == PreparationMode.PROFESSIONAL_GATE && list.isEmpty()) {
                val allQuestions = com.example.data.CsvQuestionParser.parseAllGateQuestions(getApplication(), userId)
                if (allQuestions.isNotEmpty()) {
                    studyRepo.insertQuestions(allQuestions)
                    list = studyRepo.getFilteredPyqs(mode, subject, year, userId)
                }
            }
            _filteredQuestions.value = list.shuffled()
        }
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun getPracticeQuestionsForSubject(subject: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val mode = user?.mode ?: PreparationMode.ACADEMIC
            val userId = user?.email ?: ""
            var list = studyRepo.getPracticeQuestions(mode, subject, userId)
            
            if (mode == PreparationMode.PROFESSIONAL_GATE && list.size < 50) {
                val allQuestions = com.example.data.CsvQuestionParser.parseAllGateQuestions(getApplication(), userId)
                if (allQuestions.isNotEmpty()) {
                    studyRepo.insertQuestions(allQuestions)
                    list = studyRepo.getPracticeQuestions(mode, subject, userId)
                }
            } else if (mode == PreparationMode.ACADEMIC && list.size < 10 && subject != "All" && subject != "All Subjects" && subject != "Full Syllabus") {
                generateAiQuestions(subject, subject)
                list = studyRepo.getPracticeQuestions(mode, subject, userId)
            }
            
            _filteredQuestions.value = list.shuffled()
        }
    }

    private fun parseAcademicQuestionsJson(
        rawJson: String,
        defaultSubject: String,
        defaultTopic: String,
        userId: String
    ): List<QuestionData> {
        val result = mutableListOf<QuestionData>()
        try {
            var cleaned = rawJson.replace("```json", "").replace("```", "").trim()
            val firstBrace = cleaned.indexOf('{')
            val lastBrace = cleaned.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1)
            } else {
                val firstBracket = cleaned.indexOf('[')
                val lastBracket = cleaned.lastIndexOf(']')
                if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                    cleaned = "{\"questions\": ${cleaned.substring(firstBracket, lastBracket + 1)}}"
                }
            }

            val root = org.json.JSONObject(cleaned)
            val questionsArray = root.optJSONArray("questions") ?: return emptyList()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.optJSONObject(i) ?: continue
                val questionText = qObj.optString("question", "").trim()
                if (questionText.isBlank()) continue

                val options = mutableListOf<String>()
                val optArray = qObj.optJSONArray("options")
                if (optArray != null && optArray.length() > 0) {
                    for (j in 0 until optArray.length()) {
                        val opt = optArray.optString(j, "").trim()
                        if (opt.isNotBlank()) options.add(opt)
                    }
                } else {
                    val optA = qObj.optString("optionA", "").trim()
                    val optB = qObj.optString("optionB", "").trim()
                    val optC = qObj.optString("optionC", "").trim()
                    val optD = qObj.optString("optionD", "").trim()
                    if (optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()) {
                        options.addAll(listOf(optA, optB, optC, optD))
                    }
                }

                if (options.size != 4) continue

                val correctAnsRaw = qObj.opt("correctAnswer")
                val correctIndex: Int = when (correctAnsRaw) {
                    is Int -> correctAnsRaw.coerceIn(0, 3)
                    is String -> {
                        val s = correctAnsRaw.trim()
                        when (s.uppercase()) {
                            "A", "0" -> 0
                            "B", "1" -> 1
                            "C", "2" -> 2
                            "D", "3" -> 3
                            else -> {
                                val matchIdx = options.indexOfFirst { it.equals(s, ignoreCase = true) }
                                if (matchIdx != -1) matchIdx else 0
                            }
                        }
                    }
                    else -> 0
                }

                val explanation = qObj.optString("explanation", "Verified academic concept explanation.").trim()
                val qSubject = qObj.optString("subject", defaultSubject).trim().ifEmpty { defaultSubject }
                val qSubtopic = qObj.optString("subtopic", qObj.optString("topic", defaultTopic)).trim().ifEmpty { defaultTopic }
                val difficulty = qObj.optString("difficulty", "Medium").trim().ifEmpty { "Medium" }

                result.add(
                    QuestionData(
                        id = "mock_acad_${System.currentTimeMillis()}_$i",
                        year = "2024",
                        subject = qSubject,
                        topic = qSubtopic,
                        question = questionText,
                        options = options,
                        correctAnswer = correctIndex,
                        explanation = explanation,
                        difficulty = difficulty,
                        sourceType = SourceType.AI_PRACTICE,
                        mode = PreparationMode.ACADEMIC,
                        userId = userId
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to parse Academic questions JSON", e)
        }
        return result
    }

    private fun generateLocalAcademicFallbackQuestions(
        subjects: List<String>,
        subtopics: List<String>,
        count: Int,
        userId: String
    ): List<QuestionData> {
        val questions = mutableListOf<QuestionData>()
        val baseTopics = if (subtopics.isNotEmpty()) subtopics else listOf("Core Principles", "Foundations", "Practical Implementation", "System Design", "Architecture & Optimization")
        val availableSubjects = if (subjects.isNotEmpty()) subjects else listOf("Academic Subject")

        val conceptualTemplates = listOf(
            Triple(
                "What is the primary role of %s in %s?",
                listOf(
                    "Provides foundational correctness, modular structure, and consistent execution.",
                    "Permanently removes the need for physical or virtual memory allocation.",
                    "Forces all operations to execute in an unverified single-threaded environment.",
                    "Restricts access strictly to deprecated legacy protocols without extension."
                ),
                "In %s, %s is essential for ensuring correctness, modular abstraction, and reliable program execution."
            ),
            Triple(
                "Which of the following best characterizes %s within %s?",
                listOf(
                    "High algorithmic efficiency combined with systematic abstraction boundaries.",
                    "Complete reliance on non-deterministic hardware configurations.",
                    "Unbounded latency with no error-handling or fallback guarantees.",
                    "Inability to handle concurrent access or state persistence."
                ),
                "%s in %s emphasizes efficiency, clarity of interface boundaries, and reliable resource handling."
            ),
            Triple(
                "When analyzing %s in the context of %s, which principle is most critical?",
                listOf(
                    "Adherence to standardized protocols, rigorous validation, and error resilience.",
                    "Disabling all compile-time and runtime validation checks.",
                    "Bypassing thread safety in favor of uncoordinated concurrent memory writes.",
                    "Hardcoding static parameters that cannot be adapted to runtime conditions."
                ),
                "Standardized protocols and verification ensure that %s operates dependably in %s."
            )
        )

        for (i in 0 until count) {
            val subj = availableSubjects[i % availableSubjects.size]
            val top = baseTopics[i % baseTopics.size]
            val tmpl = conceptualTemplates[i % conceptualTemplates.size]

            val qText = String.format(tmpl.first, top, subj)
            val exp = String.format(tmpl.third, top, subj)

            questions.add(
                QuestionData(
                    id = "acad_fallback_${System.currentTimeMillis()}_$i",
                    year = "2024",
                    subject = subj,
                    topic = top,
                    question = qText,
                    options = tmpl.second,
                    correctAnswer = 0,
                    explanation = exp,
                    marks = 1,
                    difficulty = when (i % 3) { 0 -> "Easy"; 1 -> "Medium"; else -> "Hard" },
                    sourceType = SourceType.AI_PRACTICE,
                    isVerified = true,
                    mode = PreparationMode.ACADEMIC,
                    userId = userId
                )
            )
        }
        return questions
    }

    private suspend fun generateAiQuestions(subject: String, topic: String) {
        val user = _currentUser.value
        val mode = user?.mode ?: PreparationMode.ACADEMIC
        val userId = user?.email ?: ""
        if (mode == PreparationMode.ACADEMIC) {
            val result = academicAiService.generateAcademicMockQuestions(
                subject = subject,
                subtopics = listOf(topic),
                count = 5,
                difficulty = "Medium",
                userId = userId
            )
            if (result.isSuccess) {
                val newQuestions = result.getOrNull() ?: emptyList()
                if (newQuestions.isNotEmpty()) {
                    studyRepo.insertQuestions(newQuestions)
                }
            }
        }
    }

    suspend fun generateMockTestQuestions(subject: String, subtopics: List<String>, count: Int): List<QuestionData> {
        val user = _currentUser.value
        val mode = user?.mode ?: PreparationMode.PROFESSIONAL_GATE
        val dept = user?.gateDepartment?.displayName ?: "CS"
        val userId = user?.email ?: ""
        
        if (mode == PreparationMode.ACADEMIC) {
            val isAllSubjects = subject.isBlank() || 
                                subject.equals("All", ignoreCase = true) || 
                                subject.equals("All Subjects", ignoreCase = true) || 
                                subject.equals("Full Syllabus", ignoreCase = true)
            
            val targetSubjectsList = if (isAllSubjects) {
                val currentSubs = _subjectDataList.value.filter { it.mode == PreparationMode.ACADEMIC }.map { it.name }
                if (currentSubs.isNotEmpty()) {
                    currentSubs
                } else {
                    val dbSubs = db.subjectDao().getSubjectsByMode(PreparationMode.ACADEMIC, userId).map { it.name }
                    if (dbSubs.isNotEmpty()) dbSubs else listOf("Academic Studies")
                }
            } else {
                listOf(subject.trim())
            }
            
            val subjectsToUse = targetSubjectsList.joinToString(", ")
            val result = academicAiService.generateAcademicMockQuestions(
                subject = subjectsToUse,
                subtopics = subtopics,
                count = count,
                difficulty = "Medium",
                userId = userId
            )

            val generatedQuestions = if (result.isSuccess) {
                result.getOrNull() ?: emptyList()
            } else {
                emptyList()
            }
            
            val finalQuestions = if (generatedQuestions.size < count) {
                val needed = count - generatedQuestions.size
                val fallback = generateLocalAcademicFallbackQuestions(targetSubjectsList, subtopics, needed, userId)
                (generatedQuestions + fallback).take(count)
            } else {
                generatedQuestions.take(count)
            }
            
            studyRepo.insertQuestions(finalQuestions)
            return finalQuestions
        } else {
            val candidateQuestions = studyRepo.getGateMockTestQuestions(getApplication(), subject, subtopics)
            val selected = candidateQuestions.shuffled()
                .distinctBy { it.id }
                .distinctBy { com.example.data.CsvQuestionParser.normalizeQuestionText(it.question) }
                .take(count)
            studyRepo.insertQuestions(selected)
            return selected
        }
    }


    private suspend fun enrichGateQuestionWithAi(q: QuestionData): QuestionData? {
        val prompt = """
            You are a GATE CS exam expert. Given the following GATE question text, generate 4 options, the correct answer, and a short explanation.
            
            Subject: ${q.subject}
            Topic: ${q.topic}
            Question: ${q.question}
            
            Format the response as a JSON object:
            {
              "optionA": "...",
              "optionB": "...",
              "optionC": "...",
              "optionD": "...",
              "correctAnswer": "A/B/C/D",
              "explanation": "...",
              "difficulty": "Easy/Medium/Hard"
            }
            Return ONLY the raw JSON.
        """.trimIndent()
        
        try {
            val res = aiTutorRepo.askAiTutor(prompt, "GATE Preparation")
            val json = res.getOrNull() ?: return null
            val cleanedJson = json.replace("```json", "").replace("```", "").trim()
            val adapter = moshi.adapter<Map<String, String>>(Map::class.java)
            val map = adapter.fromJson(cleanedJson) ?: return null
            
            val options = listOf(
                map["optionA"] ?: "",
                map["optionB"] ?: "",
                map["optionC"] ?: "",
                map["optionD"] ?: ""
            )
            val correctStr = map["correctAnswer"] ?: "A"
            val correctIndex = when(correctStr.uppercase()) {
                "A" -> 0 "B" -> 1 "C" -> 2 "D" -> 3 else -> 0
            }
            
            return q.copy(
                options = options,
                correctAnswer = correctIndex,
                explanation = map["explanation"] ?: "",
                difficulty = map["difficulty"] ?: "Medium",
                isVerified = true
            )
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun generateAdditionalGateQuestions(subject: String, subtopics: List<String>, count: Int): List<QuestionData> {
        val user = _currentUser.value
        val userId = user?.email ?: ""
        val topicStr = if (subtopics.isEmpty()) subject else subtopics.joinToString(", ")
        
        val result = gateAiService.generateGatePracticeQuestions(subject, topicStr, count, "Medium", userId)
        return result.getOrNull() ?: emptyList()
    }

    fun saveMockTestResult(
        questions: List<QuestionData>,
        userAnswers: Map<Int, Int>,
        timeTakenSeconds: Int,
        userNatAnswers: Map<Int, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val mode = _currentUser.value?.mode ?: PreparationMode.PROFESSIONAL_GATE
            val dept = _currentUser.value?.gateDepartment?.displayName ?: "CS"

            var score = 0.0
            var correctCount = 0
            var incorrectCount = 0
            var unansweredCount = 0

            questions.forEachIndexed { idx, q ->
                if (q.options.isEmpty()) {
                    val userNat = userNatAnswers[idx]
                    if (userNat.isNullOrBlank()) {
                        unansweredCount++
                    } else if (com.example.ui.components.isNatAnswerCorrect(userNat, q.explanation)) {
                        correctCount++
                        score += q.marks
                    } else {
                        incorrectCount++
                    }
                } else {
                    val choice = userAnswers[idx]
                    when {
                        choice == null -> unansweredCount++
                        choice == q.correctAnswer -> {
                            correctCount++
                            score += q.marks
                        }
                        else -> {
                            incorrectCount++
                            if (mode == PreparationMode.PROFESSIONAL_GATE) {
                                score -= (q.marks * 0.33)
                            }
                        }
                    }
                }
            }
            if (score < 0) score = 0.0
            val totalMarks = questions.sumOf { it.marks }.toDouble()
            val accuracy = if (correctCount + incorrectCount > 0) (correctCount.toDouble() / (correctCount + incorrectCount)) * 100 else 0.0

            val mockTest = MockTest(
                id = "mt_${System.currentTimeMillis()}",
                testName = if (mode == PreparationMode.PROFESSIONAL_GATE) "GATE Mock Test" else "Academic Quiz",
                mode = mode,
                department = dept,
                selectedSubject = questions.firstOrNull()?.subject ?: "General",
                totalQuestions = questions.size,
                difficulty = "Medium",
                score = score,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                unansweredCount = unansweredCount,
                accuracy = accuracy,
                timeUsedSeconds = timeTakenSeconds.toLong(),
                isCompleted = true
            )
            studyRepo.saveMockTestResult(mockTest)
        }
    }

    suspend fun askAiTutor(query: String, subject: String? = null, subtopic: String? = null): String {
        val mode = _currentUser.value?.mode ?: PreparationMode.ACADEMIC
        return if (mode == PreparationMode.PROFESSIONAL_GATE) {
            val res = gateAiService.askGateAiTutor(getApplication(), query, subject, subtopic)
            res.getOrDefault("Unable to generate the explanation. Please check your internet connection and try again.")
        } else {
            val res = academicAiService.askAcademicTutor(query, subject, subtopic)
            res.getOrElse {
                "I couldn't generate a response right now. Please check your internet connection and try again."
            }
        }
    }


    private suspend fun seedSampleDataIfNeeded() {
        val syllabusDao = db.syllabusDao()
        val noteDao = db.noteDao()
        val questionDao = db.questionDao()
        val studyPlanDao = db.studyPlanDao()
        val subjectDao = db.subjectDao()

        val user = _currentUser.value ?: return
        val mode = user.mode
        val userId = user.email

        if (mode == PreparationMode.ACADEMIC) {
            syllabusDao.deleteLegacyAcademicSeedTopics()
            subjectDao.deleteLegacyAcademicSeedSubjects()
            noteDao.deleteLegacyAcademicSeedNotes()
            return
        }

        if (mode == PreparationMode.PROFESSIONAL_GATE) {
            questionDao.deleteLegacySampleQuestions(PreparationMode.PROFESSIONAL_GATE)
            val gateQuestionCount = questionDao.getPracticeQuestions(PreparationMode.PROFESSIONAL_GATE, "All Subjects", userId).size
            if (gateQuestionCount < 60) {
                val gateQuestions = com.example.data.CsvQuestionParser.parseAllGateQuestions(getApplication(), userId)
                if (gateQuestions.isNotEmpty()) {
                    questionDao.insertAll(gateQuestions)
                }
            }

            if (subjectDao.getUserSpecificCount(PreparationMode.PROFESSIONAL_GATE, userId) == 0) {
                val gateSubjects = listOf(
                    "Engineering Mathematics",
                    "Digital Logic",
                    "Computer Organization and Architecture",
                    "Programming and Data Structures",
                    "Algorithms",
                    "Theory of Computation",
                    "Compiler Design",
                    "Operating Systems",
                    "Databases",
                    "Computer Networks"
                ).map { name ->
                    SubjectData(
                        id = "gate_${name.replace(" ", "_").lowercase()}_$userId",
                        name = name,
                        importance = "High",
                        difficulty = "Medium",
                        availableStudyMinutesPerDay = 120,
                        progress = 0f,
                        isUserCreated = false,
                        mode = PreparationMode.PROFESSIONAL_GATE,
                        userId = userId
                    )
                }
                subjectDao.insertAll(gateSubjects)

                val gateSubtopics = mutableListOf<SyllabusData>()
                fun addTopics(subject: String, topics: List<String>) {
                    topics.forEachIndexed { idx, topic ->
                        gateSubtopics.add(SyllabusData("gs_${subject.take(3).lowercase()}_${idx}_$userId", subject, topic, "GATE Syllabus Topic", PreparationMode.PROFESSIONAL_GATE, false, userId))
                    }
                }

                addTopics("Engineering Mathematics", listOf("Discrete Mathematics", "Propositional and First Order Logic", "Sets", "Relations", "Functions", "Partial Orders", "Lattices", "Monoids", "Groups", "Graphs", "Graph Connectivity", "Matching", "Graph Colouring", "Combinatorics", "Counting", "Recurrence Relations", "Generating Functions", "Linear Algebra", "Matrices", "Determinants", "System of Linear Equations", "Eigenvalues and Eigenvectors", "LU Decomposition", "Calculus", "Limits", "Continuity", "Differentiability", "Maxima and Minima", "Mean Value Theorem", "Integration", "Probability and Statistics", "Random Variables", "Uniform Distribution", "Normal Distribution", "Exponential Distribution", "Poisson Distribution", "Binomial Distribution", "Mean", "Median", "Mode", "Standard Deviation", "Conditional Probability", "Bayes Theorem"))
                addTopics("Digital Logic", listOf("Boolean Algebra", "Boolean Minimization", "Algebraic Technique", "Karnaugh Map", "Tabular Method", "Combinational Circuits", "Sequential Circuits", "Number Representation", "Fixed Point Representation", "Floating Point Representation", "Arithmetic"))
                addTopics("Computer Organization and Architecture", listOf("Instruction Set", "Addressing Modes", "Arithmetic and Logic Unit (ALU)", "Control Unit", "Hardwired Control", "Microprogrammed Control", "Memory Interfacing", "Memory Hierarchy", "Memory Performance", "Cache Memory", "Cache Mapping", "I/O Interface", "Interrupt", "DMA", "Instruction Pipelining", "Pipeline Hazards"))
                addTopics("Programming and Data Structures", listOf("Programming in C", "Recursion", "Arrays", "Stacks", "Queues", "Linked Lists", "Trees", "Binary Search Trees", "Binary Heaps", "Graphs"))
                addTopics("Algorithms", listOf("Searching", "Sorting", "Hashing", "Asymptotic Time Complexity", "Asymptotic Space Complexity", "Worst Case Complexity", "Greedy Algorithms", "Dynamic Programming", "Divide and Conquer", "Graph Traversal", "Minimum Spanning Trees", "Shortest Paths"))
                addTopics("Theory of Computation", listOf("Regular Expressions", "Finite Automata", "Context-Free Grammars", "Push-Down Automata", "Regular Languages", "Context-Free Languages", "Pumping Lemma", "Turing Machines", "Undecidability"))
                addTopics("Compiler Design", listOf("Lexical Analysis", "Parsing", "Syntax-Directed Translation", "Runtime Environments", "Intermediate Code Generation", "Local Optimization", "Data Flow Analysis", "Constant Propagation", "Liveness Analysis", "Common Subexpression Elimination"))
                addTopics("Operating Systems", listOf("System Calls", "Processes", "Threads", "Inter-Process Communication", "Concurrency", "Synchronization", "Deadlock", "CPU Scheduling", "I/O Scheduling", "Memory Management", "Virtual Memory", "File Systems"))
                addTopics("Databases", listOf("ER Model", "Relational Model", "Relational Algebra", "Tuple Calculus", "SQL", "Integrity Constraints", "Normal Forms", "File Organization", "Indexing", "B Trees", "B+ Trees", "Transactions", "Concurrency Control"))
                addTopics("Computer Networks", listOf("Principles of Layering", "Switching", "Circuit Switching", "Packet Switching", "Virtual Circuit", "Performance Metrics", "Data Link Layer", "Error Detection", "Medium Access Control", "Ethernet", "Distance Vector Routing", "Link State Routing", "IPv4", "IPv4 Fragmentation", "CIDR Notation", "Network Address Translation", "TCP", "Flow Control", "Congestion Control", "Socket API", "DNS", "HTTP"))
                syllabusDao.insertAll(gateSubtopics)
            }

            if (noteDao.getTotalNotesCount(PreparationMode.PROFESSIONAL_GATE) == 0) {
                noteDao.insertAll(listOf(
                    NoteData("gate_note_1", "Algorithms", "Dynamic Programming", "Core Principles", "Dynamic Programming solves complex problems by breaking them into subproblems...", PreparationMode.PROFESSIONAL_GATE),
                    NoteData("gate_note_2", "Operating Systems", "Process Synchronization", "Semaphores and Mutexes", "Semaphores are integer variables used to solve critical section problems...", PreparationMode.PROFESSIONAL_GATE),
                    NoteData("gate_note_3", "Computer Networks", "IP Addressing", "Subnetting & CIDR", "CIDR uses variable length subnet masking...", PreparationMode.PROFESSIONAL_GATE)
                ))
            }

            if (studyPlanDao.getStudyPlan(PreparationMode.PROFESSIONAL_GATE) == null) {
                studyPlanDao.insertOrUpdate(StudyPlan(mode = PreparationMode.PROFESSIONAL_GATE, dailyHours = 4, targetExam = "GATE CS 2026", dailyGoals = listOf("Algorithms - Dynamic Programming", "Practice 10 PYQs on OS", "Review Digital Logic notes"), weeklySchedule = listOf("Mon: Algorithms", "Tue: OS", "Wed: Computer Networks", "Thu: DBMS", "Fri: Theory of Comp", "Sat: Mock Test", "Sun: Revision")))
            }
        }
    }
}
