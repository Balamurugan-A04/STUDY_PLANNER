package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.PreparationMode
import com.example.ui.components.NavTab
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = LocalContext.current

            val currentScreen by viewModel.currentScreen.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val syllabusList by viewModel.syllabusList.collectAsState()
            val subjectDataList by viewModel.subjectDataList.collectAsState()
            val notesList by viewModel.notesList.collectAsState()
            val filteredQuestions by viewModel.filteredQuestions.collectAsState()
            val testHistory by viewModel.testHistory.collectAsState()
            val studyPlan by viewModel.studyPlan.collectAsState()
            val pyqYears by viewModel.pyqYears.collectAsState()
            val subjects by viewModel.subjects.collectAsState()

            val mode = currentUser?.mode ?: PreparationMode.ACADEMIC
            val coroutineScope = rememberCoroutineScope()

            var lastBackPressedTime by remember { mutableLongStateOf(0L) }
            var backToast by remember { mutableStateOf<Toast?>(null) }

            BackHandler(enabled = true) {
                when (currentScreen) {
                    is Screen.Dashboard, is Screen.Login -> {
                        val currentTime = System.currentTimeMillis()
                        if (lastBackPressedTime != 0L && currentTime - lastBackPressedTime < 2000L) {
                            backToast?.cancel()
                            finish()
                        } else {
                            lastBackPressedTime = currentTime
                            backToast?.cancel()
                            backToast = Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).apply {
                                show()
                            }
                        }
                    }
                    is Screen.AcademicRegister, is Screen.GateRegister -> {
                        viewModel.navigateTo(Screen.Login)
                    }
                    is Screen.SubjectDetail -> {
                        viewModel.navigateTo(Screen.Syllabus)
                    }
                    is Screen.SubjectSetup -> {
                        if (currentUser != null && subjectDataList.isNotEmpty()) {
                            viewModel.navigateTo(Screen.Dashboard)
                        } else {
                            viewModel.navigateTo(Screen.Login)
                        }
                    }
                    is Screen.Splash -> {
                        finish()
                    }
                    else -> {
                        viewModel.navigateTo(Screen.Dashboard)
                    }
                }
            }

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val fileName = getFileName(it)
                    val mimeType = contentResolver.getType(it) ?: "application/octet-stream"
                    viewModel.addFileNote(it, fileName, mimeType)
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                when (val screen = currentScreen) {
                    is Screen.Splash -> {
                        SplashScreen(
                            onSplashFinished = {
                                if (currentUser != null) {
                                    viewModel.navigateTo(Screen.Dashboard)
                                } else {
                                    viewModel.navigateTo(Screen.Login)
                                }
                            }
                        )
                    }

                    is Screen.Login -> {
                        LoginScreen(
                            onLoginSuccess = { selectedMode ->
                                viewModel.navigateTo(Screen.Dashboard)
                            },
                            onNavigateRegister = { targetMode ->
                                when (targetMode) {
                                    PreparationMode.PROFESSIONAL_GATE -> viewModel.navigateTo(Screen.GateRegister)
                                    else -> viewModel.navigateTo(Screen.AcademicRegister)
                                }
                            },
                            onPerformLogin = { email, password, targetMode, isRemember ->
                                val res = viewModel.login(email, password, targetMode, isRemember)
                                if (res.isSuccess) {
                                    Result.success(Unit)
                                } else {
                                    Result.failure(res.exceptionOrNull() ?: Exception("Login failed"))
                                }
                            }
                        )
                    }

                    is Screen.AcademicRegister -> {
                        AcademicRegisterScreen(
                            onRegisterSuccess = { viewModel.navigateTo(Screen.Dashboard) },
                            onNavigateLogin = { viewModel.navigateTo(Screen.Login) },
                            onPerformRegister = { name, email, password, isRemember ->
                                viewModel.registerAcademic(name, email, password, isRemember)
                            }
                        )
                    }

                    is Screen.GateRegister -> {
                        GateRegisterScreen(
                            onRegisterSuccess = { viewModel.navigateTo(Screen.Dashboard) },
                            onNavigateLogin = { viewModel.navigateTo(Screen.Login) },
                            onPerformRegister = { name, email, password, age, college, dept, isRemember ->
                                viewModel.registerGate(name, email, password, age, college, dept, isRemember)
                            }
                        )
                    }

                    is Screen.Dashboard -> {
                        val activeUser = currentUser
                        if (activeUser == null) {
                            viewModel.navigateTo(Screen.Login)
                        } else {
                            when (activeUser.mode) {
                                PreparationMode.PROFESSIONAL_GATE -> {
                                    GateDashboardScreen(
                                        user = activeUser,
                                        onNavigateSyllabus = { viewModel.navigateTo(Screen.Syllabus) },
                                        onNavigateStudyPlan = { viewModel.navigateTo(Screen.StudyPlanRoute) },
                                        onNavigatePractice = {
                                            viewModel.getPracticeQuestionsForSubject("All")
                                            viewModel.navigateTo(Screen.Practice)
                                        },
                                        onNavigateMockTests = { viewModel.navigateTo(Screen.MockTestConfig) },
                                        onNavigateNotes = { viewModel.navigateTo(Screen.Notes) },
                                        onNavigatePyqs = {
                                            viewModel.filterPyqs("All", "All")
                                            viewModel.navigateTo(Screen.Pyq)
                                        },
                                        onNavigateProgress = { viewModel.navigateTo(Screen.Progress) },
                                        onNavigateAiTutor = { viewModel.navigateTo(Screen.AiTutor) },
                                        onNavigateProfile = { viewModel.navigateTo(Screen.Profile) }
                                    )
                                }
                                PreparationMode.ACADEMIC -> {
                                    AcademicDashboardScreen(
                                        user = activeUser,
                                        onNavigateStudyPlan = { viewModel.navigateTo(Screen.StudyPlanRoute) },
                                        onNavigateSubjects = { viewModel.navigateTo(Screen.Syllabus) },
                                        onNavigateAiTutor = { viewModel.navigateTo(Screen.AiTutor) },
                                        onNavigateNotes = { viewModel.navigateTo(Screen.Notes) },
                                        onNavigateMockTests = { viewModel.navigateTo(Screen.MockTestConfig) },
                                        onNavigateProgress = { viewModel.navigateTo(Screen.Progress) },
                                        onNavigateProfile = { viewModel.navigateTo(Screen.Profile) }
                                    )
                                }
                            }
                        }
                    }

                    is Screen.Syllabus -> {
                        SyllabusScreen(
                            mode = mode,
                            syllabusList = syllabusList,
                            subjectDataList = subjectDataList,
                            onToggleTopic = { id, done -> viewModel.toggleSyllabusTopic(id, done) },
                            onAddSubject = { name, imp, diff, mins, date ->
                                viewModel.addSubject(name, imp, diff, mins, date)
                            },
                            onDeleteSubject = { id ->
                                viewModel.deleteSubject(id)
                            },
                            onDeleteSubtopic = { id ->
                                viewModel.deleteSubtopic(id)
                            },
                            onNavigateSubjectDetail = { id ->
                                viewModel.navigateTo(Screen.SubjectDetail(id))
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.SubjectDetail -> {
                        val subject = subjectDataList.find { it.id == screen.subjectId }
                        if (subject == null) {
                            viewModel.navigateTo(Screen.Syllabus)
                        } else {
                            val topics = syllabusList.filter { it.subject == subject.name && it.mode == subject.mode }
                            SubjectDetailScreen(
                                subject = subject,
                                subtopics = topics,
                                onAddSubtopic = { name -> viewModel.addSubtopic(subject.id, name) },
                                onDeleteSubtopic = { id -> viewModel.deleteSubtopic(id) },
                                onDeleteSubject = { id -> viewModel.deleteSubject(id) },
                                onToggleSubtopic = { id, done -> viewModel.toggleSyllabusTopic(id, done) },
                                onNavigateBack = { viewModel.navigateTo(Screen.Syllabus) }
                            )
                        }
                    }

                    is Screen.SubjectSetup -> {
                        SubjectSetupScreen(
                            mode = mode,
                            subjects = subjectDataList,
                            onAddSubject = { name, imp, diff, mins, date ->
                                viewModel.addSubject(name, imp, diff, mins, date)
                            },
                            onDeleteSubject = { id ->
                                viewModel.deleteSubject(id)
                            },
                            onContinue = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.Notes -> {
                        NotesScreen(
                            mode = mode,
                            notesList = notesList,
                            onToggleCompleted = { id, done -> viewModel.toggleNoteCompleted(id, done) },
                            onDeleteNote = { id -> viewModel.deleteNote(id) },
                            onAddMaterial = {
                                filePickerLauncher.launch("*/*")
                            },
                            onOpenFile = { uriStr ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(uriStr)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle no app to open file
                                }
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.Pyq -> {
                        PyqScreen(
                            mode = mode,
                            years = pyqYears,
                            subjects = subjects,
                            onFilterChanged = { sub, yr -> viewModel.filterPyqs(sub, yr) },
                            questions = filteredQuestions,
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.Practice -> {
                        PracticeScreen(
                            mode = mode,
                            subjects = subjects,
                            onSubjectSelected = { sub -> viewModel.getPracticeQuestionsForSubject(sub) },
                            questions = filteredQuestions,
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.MockTestConfig -> {
                        val availableCount by viewModel.availableQuestionCount.collectAsState()
                        MockTestConfigScreen(
                            mode = mode,
                            subjects = subjects,
                            syllabusList = syllabusList,
                            availableQuestionCount = availableCount,
                            onUpdateConfig = { sub, subtopics ->
                                viewModel.updateAvailableQuestionCount(sub, subtopics)
                            },
                            onStartTest = { sub: String, subtopics: List<String>, count: Int, dur: Int, onError: (String) -> Unit ->
                                coroutineScope.launch {
                                    val qs = viewModel.generateMockTestQuestions(sub, subtopics, count)
                                    if (qs.isEmpty()) {
                                        onError("Unable to generate Academic Mock Test at this moment. Please check your connection and try again.")
                                    } else {
                                        viewModel.navigateTo(Screen.MockTestActive(sub, qs.size, dur, qs))
                                    }
                                }
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.MockTestActive -> {
                        MockTestActiveScreen(
                            mode = mode,
                            questions = screen.questions,
                            durationMinutes = screen.durationMinutes,
                            onSubmitTest = { answers, timeSpent, natAnswers ->
                                viewModel.saveMockTestResult(screen.questions, answers, timeSpent, natAnswers)
                                viewModel.navigateTo(Screen.MockTestResult(screen.questions, answers, timeSpent, natAnswers))
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.MockTestResult -> {
                        MockTestResultScreen(
                            mode = mode,
                            questions = screen.questions,
                            userAnswers = screen.userAnswers,
                            timeTakenSeconds = screen.timeTakenSeconds,
                            userNatAnswers = screen.userNatAnswers,
                            onRetakeTest = { viewModel.navigateTo(Screen.MockTestConfig) },
                            onNavigateDashboard = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.StudyPlanRoute -> {
                        StudyPlanScreen(
                            mode = mode,
                            studyPlan = studyPlan,
                            onGenerateStudyPlan = { viewModel.generateStudyPlan() },
                            onToggleGoal = { goal -> viewModel.toggleGoalCompletion(goal) },
                            onRescheduleMissed = { viewModel.rescheduleMissedTasks() },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.Progress -> {
                        ProgressScreen(
                            mode = mode,
                            syllabusList = syllabusList,
                            testHistory = testHistory,
                            onNavigateTab = { tab ->
                                when (tab) {
                                    NavTab.HOME -> viewModel.navigateTo(Screen.Dashboard)
                                    NavTab.PROGRESS -> {}
                                    NavTab.AI_TUTOR -> viewModel.navigateTo(Screen.AiTutor)
                                    NavTab.SYLLABUS -> viewModel.navigateTo(Screen.Syllabus)
                                    NavTab.PROFILE -> viewModel.navigateTo(Screen.Profile)
                                }
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.AiTutor -> {
                        AiTutorScreen(
                            mode = mode,
                            subjects = subjectDataList,
                            syllabusList = syllabusList,
                            onSendMessage = { query, subj, subtop -> viewModel.askAiTutor(query, subj, subtop) },
                            onNavigateTab = { tab ->
                                when (tab) {
                                    NavTab.HOME -> viewModel.navigateTo(Screen.Dashboard)
                                    NavTab.PROGRESS -> viewModel.navigateTo(Screen.Progress)
                                    NavTab.AI_TUTOR -> {}
                                    NavTab.SYLLABUS -> viewModel.navigateTo(Screen.Syllabus)
                                    NavTab.PROFILE -> viewModel.navigateTo(Screen.Profile)
                                }
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }

                    is Screen.Profile -> {
                        ProfileScreen(
                            user = currentUser,
                            onSwitchMode = { newMode -> viewModel.switchMode(newMode) },
                            onLogout = { viewModel.logout() },
                            onNavigateTab = { tab ->
                                when (tab) {
                                    NavTab.HOME -> viewModel.navigateTo(Screen.Dashboard)
                                    NavTab.PROGRESS -> viewModel.navigateTo(Screen.Progress)
                                    NavTab.AI_TUTOR -> viewModel.navigateTo(Screen.AiTutor)
                                    NavTab.SYLLABUS -> viewModel.navigateTo(Screen.Syllabus)
                                    NavTab.PROFILE -> {}
                                }
                            },
                            onNavigateBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unknown_file"
    }
}
